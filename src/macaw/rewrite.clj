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
  (+ col (index-of-nth sql "\n" (dec line))))

(defn- node->idx-range
  "Find the start and end index of the underlying tokens for a given AST node from a given SQL string."
  [^SimpleNode node sql]
  (let [first-token (.jjtGetFirstToken node)
        last-token  (.jjtGetLastToken node)
        first-idx   (->idx sql
                           (.-beginLine first-token)
                           (.-beginColumn first-token))
        last-idx    (->idx sql
                           (.-endLine last-token)
                           (.-endColumn last-token))]
    [first-idx last-idx]))

(defn- splice-replacements [^String sql replacements]
  (let [sb     (StringBuilder.)
        append #(.append sb %)]
    (loop [start 0
           [[[first-idx last-idx] value] & rst] replacements]
      (if (nil? last-idx)
        (when (< start (count sql))
          (append (.substring sql start)))
        (do (append (.substring sql start first-idx))
            (append value)
            (recur (inc ^long last-idx) rst))))
    (str sb)))

(defn- update-query
  "Emit a SQL string for an updated AST, preserving the comments and whitespace from the original SQL."
  [updated-ast sql]
  (let [replace-name (fn [->s]
                       (fn [acc ^ASTNodeAccess visitable]
                         (let [node (.getASTNode visitable)]
                           ;; not sure why sometimes we get a phantom visitable without an underlying node
                           (if (nil? node)
                             acc
                             (conj acc [(node->idx-range node sql) (->s visitable)])))))]
    (splice-replacements
     sql
     (mw/fold-query
      updated-ast
      {:table  (replace-name #(.getFullyQualifiedName ^Table %))
       :column (replace-name #(.getFullyQualifiedName ^Column %))}
      []))))

(defn- rename-table
  [table-renames ^Table table]
  (when-let [name' (get table-renames (.getName table))]
    (.setName table name')))

(defn replace-names
  "Given a SQL query and its corresponding (untransformed) AST, apply the given table and column renames."
  [sql parsed-ast {table-renames :tables, column-renames :columns}]
  (-> parsed-ast
      (mw/walk-query
       {:table       (partial rename-table table-renames)
        :table-alias (partial rename-table table-renames)
        :column      (fn [^Column column] (when-let [name' (get column-renames (.getColumnName column))]
                                            (.setColumnName column name')))})
      (update-query sql)))
