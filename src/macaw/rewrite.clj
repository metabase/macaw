(ns macaw.rewrite
  (:require
   [macaw.walk :as mw])
  (:import
   (net.sf.jsqlparser.parser ASTNodeAccess SimpleNode)
   (net.sf.jsqlparser.schema Column Table)))

(set! *warn-on-reflection* true)

(defn- index-of-nth [^String haystack ^String needle n]
  (assert (not (neg? n)))
  (if (zero? n)
    -1
    (loop [n   n
           idx 0]
      (let [next-id (.indexOf haystack needle idx)]
        (cond
          (= 1 n) next-id
          (neg? next-id) next-id
          :else (recur (dec n) (inc next-id)))))))

(defn- ->idx [^String sql line col]
  (+ col 1 (index-of-nth sql "\n" (dec line))))

(defn- splice-token [seen before [^String during offset] ^SimpleNode node ^String value]
  (if (nil? node)
    [during offset]
    ;; work around ast visitor processing (inexplicably and incorrectly) duplicating expressions visits
    (if (contains? @seen node)
      [during offset]
      (let [_           (vswap! seen conj node)
            first-token (.jjtGetFirstToken node)
            last-token  (.jjtGetLastToken node)
            first-idx   (->idx before
                               (.-beginLine first-token)
                               (.-beginColumn first-token))
            last-idx    (->idx before
                               (.-endLine last-token)
                               (.-endColumn last-token))
            before      (.substring during 0 (+ offset (dec first-idx)))
            after       (.substring during (+ offset last-idx))
            offset'     (+ offset (- (.length value) (inc (- last-idx first-idx))))]
        ;; Optimization: rather than incrementally building strings, we accumulate range-replacement pairs and then
        ;;               reduce over a string builder
        [(str before value after)
         offset']))))

(defn- update-query
  "Emit a SQL string for an updated AST, preserving the comments and whitespace from the original SQL."
  [updated-ast sql]
  ;; work around ast visitor processing (inexplicably and incorrectly) duplicating expressions visits
  (let [seen         (volatile! #{})
        replace-name (fn [->s] (fn [acc visitable]
                                 (splice-token seen sql acc
                                               (.getASTNode ^ASTNodeAccess visitable)
                                               (->s visitable))))]
    (first
     (mw/fold-query
      updated-ast
      {:table  (replace-name (fn [^Table table] (.getFullyQualifiedName table)))
       :column (replace-name (fn [^Column column] (.getFullyQualifiedName column)))}
      [sql 0]))))

(defn replace-names
  "Given a SQL query and its corresponding (untransformed) AST, apply the given table and column renames."
  [sql parsed-ast {table-renames :tables, column-renames :columns}]
  (-> parsed-ast
      (mw/walk-query
       {:table  (fn [^Table table] (when-let [name' (get table-renames (.getName table))]
                                     (.setName table name')))
        :column (fn [^Column column] (when-let [name' (get column-renames (.getColumnName column))]
                                       (.setColumnName column name')))})
      (update-query sql)))
