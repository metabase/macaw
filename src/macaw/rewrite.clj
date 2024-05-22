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
  [updated-ast updated-nodes sql]
  (let [updated-node? (set (map first updated-nodes))
        replacement   (fn [->text visitable]
                        (let [ast-node  (.getASTNode ^ASTNodeAccess visitable)
                              idx-range (node->idx-range ast-node sql)
                              node-text (->text visitable)]
                         [idx-range node-text]))
        replace-name  (fn [->text]
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
  [updated-nodes table-renames schema-renames known-tables ^Table t _ctx]
  (let [table (-> (get known-tables t)
                  (select-keys [:table :schema]))]
    (when-let [rename (or (find table-renames table)
                          (when (nil? (:schema table))
                            (u/seek #(= (:table table) (:table (key %))) table-renames)))]
      (vswap! updated-nodes conj [t rename])
      (.setName t (val rename)))
    (when-let [schema-rename (find schema-renames (.getSchemaName t))]
      (vswap! updated-nodes conj [(.getSchemaName t) schema-rename])
      (.setSchemaName t (val schema-rename)))))

(defn- rename-column
  [updated-nodes column-renames known-columns ^Column c _ctx]
  (let [col    (-> (get known-columns c)
                   (select-keys [:column :table :schema]))
        rename (when col
                 (or (find column-renames col)
                     (when (nil? (:schema col))
                       (u/seek #(= (dissoc col :schema)
                                   (dissoc (key %) :schema))
                               column-renames))
                     (when (nil? (:table col))
                       (u/seek #(= (:column col) (:column (key %))) column-renames))))]
    (when rename
      (vswap! updated-nodes conj [c rename])
      (.setColumnName c (val rename)))))

(defn- alert-unused! [updated-nodes renames]
  (let [known-rename? (set (map second updated-nodes))]
    (doseq [[k items] renames]
      (when-let [unknown (first (remove known-rename? items))]
        (throw (ex-info (str "Unknown rename: " unknown) {:type k
                                                          :rename unknown}))))))

(defn replace-names
  "Given a SQL query and its corresponding (untransformed) AST, apply the given table and column renames."
  [sql parsed-ast {schema-renames :schemas
                   table-renames  :tables
                   column-renames :columns
                   :as renames}]
  (let [comps          (collect/query->components parsed-ast {:with-instance true})
        columns        (into {} (for [c (:columns comps)
                                      i (:instances (:component c))]
                                  [i (:component c)]))
        tables         (into {} (for [t (:tables comps)
                                      i (:instances (:component t))]
                                  [i (:component t)]))
        ;; execute rename
        updated-nodes  (volatile! [])
        res            (-> parsed-ast
                           (mw/walk-query
                            {:table            (partial rename-table updated-nodes table-renames schema-renames tables)
                             :column-qualifier (partial rename-table updated-nodes table-renames schema-renames tables)
                             :column           (partial rename-column updated-nodes column-renames columns)})
                           (update-query @updated-nodes sql))]
    (alert-unused! @updated-nodes renames)
    res))
