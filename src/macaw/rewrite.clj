(ns macaw.rewrite
  (:require
   [macaw.collect :as collect]
   [macaw.util :as u]
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
  [updated-ast updated-node? sql]
  (let [replacement  (fn [->text visitable]
                       (let [ast-node  (.getASTNode ^ASTNodeAccess visitable)
                             idx-range (node->idx-range ast-node sql)
                             node-text (->text visitable)]
                         [idx-range node-text]))
        replace-name (fn [->text]
                       (fn [acc visitable _ctx]
                         (cond-> acc
                           (updated-node? visitable)
                           (conj (replacement ->text visitable)))))]
    (splice-replacements
     sql
     (mw/fold-query
      updated-ast
      {:table  (replace-name #(.getFullyQualifiedName ^Table %))
       :column (replace-name #(.getFullyQualifiedName ^Column %))}
      []))))

(defn- rename-table
  [updated-nodes table-renames schema-renames ^Table table _ctx]
  (when-let [name' (get table-renames (.getName table))]
    (vswap! updated-nodes conj table)
    (.setName table name'))
  (when-let [new-schema-name (get schema-renames (.getSchemaName table))]
    (.setSchemaName table new-schema-name)))

(defn- rename-column
  [updated-nodes column-renames known-columns ^Column column _ctx]
  (let [col   (get known-columns column)
        name' (when col
                (or (get column-renames (select-keys col [:column :table :schema]))
                    (get column-renames (select-keys col [:column :table]))))]
    (when name'
      (vswap! updated-nodes conj column)
      (.setColumnName column name'))))

(defn replace-names
  "Given a SQL query and its corresponding (untransformed) AST, apply the given table and column renames."
  [sql parsed-ast {schema-renames :schemas
                   table-renames  :tables
                   column-renames :columns}]
  (let [columns (->> (collect/query->components parsed-ast {:with-instance true})
                     :columns
                     (u/group-with #(-> % :component :instance) (fn [_a b] (:component b))))
        updated-nodes     (volatile! #{})]
    (-> parsed-ast
        (mw/walk-query
         {:table            (partial rename-table updated-nodes table-renames schema-renames)
          :column-qualifier (partial rename-table updated-nodes table-renames schema-renames)
          :column           (partial rename-column updated-nodes column-renames columns)})
        (update-query @updated-nodes sql))))
