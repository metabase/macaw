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
  [updated-ast updated-nodes sql & {:as _opts}]
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
  [updated-nodes table-renames schema-renames known-tables opts ^Table t _ctx]
  (when-let [rename (u/find-relevant table-renames (get known-tables t) [:table :schema])]
    ;; Handle both raw string renames, as well as more precise element based ones.
    (vswap! updated-nodes conj [t rename])
    (let [identifier (as-> (val rename) % (:table % %))]
      (.setName t identifier)))
  (let [raw-schema-name (.getSchemaName t)
        schema-name     (collect/normalize-reference raw-schema-name opts)]
    (when-let [schema-rename (u/seek (comp (partial u/match-component schema-name) key) schema-renames)]
      (vswap! updated-nodes conj [raw-schema-name schema-rename])
      (let [identifier (as-> (val schema-rename) % (:table % %))]
        (.setSchemaName t identifier)))))

(defn- rename-column
  [updated-nodes column-renames known-columns ^Column c _ctx]
  (when-let [rename (u/find-relevant column-renames (get known-columns c) [:column :table :schema])]
    ;; Handle both raw string renames, as well as more precise element based ones.
    (vswap! updated-nodes conj [c rename])
    (let [identifier (as-> (val rename) % (:column % %))]
      (.setColumnName c identifier))))

(defn- alert-unused! [updated-nodes renames]
  (let [known-rename? (set (map second updated-nodes))]
    (doseq [[k items] renames]
      (when-let [unknown (first (remove known-rename? items))]
        (throw (ex-info (str "Unknown rename: " unknown) {:type k
                                                          :rename unknown}))))))

(defn- index-by-instances [xs]
  (into {} (for [x xs
                 :let [c (:component x)]
                 i (:instances c)]
             [i c])))

(defn replace-names
  "Given a SQL query and its corresponding (untransformed) AST, apply the given table and column renames."
  [sql parsed-ast renames & {:as opts}]
  (let [{schema-renames :schemas
         table-renames  :tables
         column-renames :columns} renames
        comps          (collect/query->components parsed-ast (assoc opts :with-instance true))
        columns        (index-by-instances (:columns comps))
        tables         (index-by-instances (:tables-superset comps))
        ;; execute rename
        updated-nodes  (volatile! [])
        rename-table*  (partial rename-table updated-nodes table-renames schema-renames tables opts)
        rename-column* (partial rename-column updated-nodes column-renames columns)
        res            (-> parsed-ast
                           (mw/walk-query
                            {:table            rename-table*
                             :column-qualifier rename-table*
                             :column           rename-column*})
                           (update-query @updated-nodes sql opts))]
    (when-not (:allow-unused? opts)
      (alert-unused! @updated-nodes renames))
    res))
