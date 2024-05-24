(ns macaw.collect
  (:require
   [clojure.string :as str]
   [macaw.util :as u]
   [macaw.walk :as mw])
  (:import
   (com.metabase.macaw AstWalker$QueueItem)
   (net.sf.jsqlparser.expression Alias)
   (net.sf.jsqlparser.schema Column Table)
   (net.sf.jsqlparser.statement Statement)
   (net.sf.jsqlparser.statement.select AllTableColumns)))

(set! *warn-on-reflection* true)

(defn- conj-to
  ([key-name]
   (conj-to key-name identity))
  ([key-name xf]
   (fn item-conjer [results component context]
     (update results key-name conj {:component (xf component)
                                    :context   (mapv
                                                 (fn [^AstWalker$QueueItem x]
                                                   [(keyword (.getKey x)) (.getValue x)])
                                                 context)}))))

(defn- query->raw-components
  [^Statement parsed-ast]
  (mw/fold-query parsed-ast
                 {:column           (conj-to :columns)
                  :column-qualifier (conj-to :qualifiers)
                  :mutation         (conj-to :mutation-commands)
                  :wildcard         (conj-to :has-wildcard? (constantly true))
                  :table            (conj-to :tables)
                  :table-wildcard   (conj-to :table-wildcards)}
                 {:columns           #{}
                  :has-wildcard?     #{}
                  :mutation-commands #{}
                  :tables            #{}
                  :table-wildcards   #{}}))

;;; tables

(def ^:private quotes (map str [\` \"]))

(defn- quoted? [s]
  (some (fn [q]
          (and (str/starts-with? s q)
               (str/ends-with? s q)))
        quotes))

(defn- strip-quotes [s]
  (subs s 1 (dec (count s))))

(defn normalize-reference
  "Normalize a schema, table, column, etc. references so that we can match them regardless of syntactic differences."
  [s {:keys [case-insensitive? quotes-preserve-case?]}]
  (when s
    (let [quoted (quoted? s)
          case-insensitive (and case-insensitive?
                                (not (and quotes-preserve-case?
                                          quoted)))]
      (cond-> s
        quoted           strip-quotes
        case-insensitive str/lower-case))))

(defn- find-table [{:keys [alias->table name->table] :as opts} ^Table t]
  (let [n      (normalize-reference (.getName t) opts)
        schema (normalize-reference (.getSchemaName t) opts)]
    (or (get alias->table n)
        (:component (last (u/find-relevant name->table {:table n :schema schema} [:table :schema]))))))

(defn- find-qualifier-table [opts ^Table q _ctx]
  (when-let [table (find-table opts q)]
    (cond-> table
      (:with-instance opts) (assoc :instances [q]))))

(defn- make-table [{:keys [with-instance qualifier? alias->table] :as opts} ^Table t _ctx]
  (if (and qualifier?
           (get alias->table (.getName t)))
    (get alias->table (.getName t))
    (u/strip-nils
     {:table     (normalize-reference (.getName t) opts)
      :schema    (normalize-reference (.getSchemaName t) opts)
      :instances (when with-instance [t])})))

(defn- alias-mapping
  [opts ^Table table ctx]
  (when-let [^Alias table-alias (.getAlias table)]
    [(.getName table-alias) (make-table opts table ctx)]))

(defn- resolve-table-name
  "JSQLParser can't tell whether the `f` in `select f.*` refers to a real table or an alias. Therefore, we have to
  disambiguate them based on our own map of aliases->table names. So this function will return the real name of the table
  referenced in a table-wildcard (as far as can be determined from the query)."
  [opts ^AllTableColumns atc _ctx]
  (find-table opts (.getTable atc)))

;;; columns

(defn- maybe-column-table [{:keys [name->table] :as opts} ^Column c]
  (if-let [t (.getTable c)]
    (find-table opts t)
    ;; if we see only a single table, we can safely say it's the table of that column
    (when (= (count name->table) 1)
      (:component (val (first name->table))))))

(defn- make-column [opts ^Column c ctx]
  (merge
   (maybe-column-table opts c)
   (u/strip-nils
    {:column    (normalize-reference (.getColumnName c) opts)
     :alias     (let [[k y] (first ctx)]
                  (when (= k :alias) y))
     :instances (when (:with-instance opts) [c])})))

;;; get them together

(defn- only-query-context [ctx]
  (into [] (comp (filter #(= (first %) :query))
                 (map second))
    ctx))

(defn- update-components
  [f components]
  (map #(-> %
            (update :component f (:context %))
            (update :context only-query-context))
       components))

(defn- merge-with-instances [a b]
  (cond-> (merge a b)
    ;; collect all instances so we can refer back to maps
    (or (-> a :component :instances)
        (-> b :component :instances))
    (update-in [:component :instances] concat (-> a :component :instances))))

(defn query->components
  "See macaw.core/query->components doc."
  [^Statement parsed-ast & [opts]]
  (let [{:keys [columns
                qualifiers
                has-wildcard?
                mutation-commands
                tables
                table-wildcards]} (query->raw-components parsed-ast)
        alias-map                 (into {} (map #(-> % :component ((partial alias-mapping opts) (:context %))) tables))
        ;; we're parsing qualifiers here only for a single purpose - rewrite uses instances to find tables for
        ;; renaming
        table-map                 (->> (update-components (partial make-table opts) tables)
                                       (u/group-with #(select-keys (:component %) [:schema :table])
                                                     merge-with-instances))
        ;; we need both aliases and tables for columns
        opts                      (assoc opts
                                         :alias->table alias-map
                                         :name->table table-map)
        qualifier-map             (->> (update-components (partial find-qualifier-table opts) qualifiers)
                                       (u/group-with #(select-keys (:component %) [:schema :table])
                                                     merge-with-instances))
        table-map                 (merge-with merge-with-instances qualifier-map table-map)]
    {:columns           (into #{} (update-components (partial make-column opts) columns))
     :has-wildcard?     (into #{} (update-components (fn [x & _args] x) has-wildcard?))
     :mutation-commands (into #{} mutation-commands)
     :tables            (into #{} (vals table-map))
     :table-wildcards   (into #{} (update-components (partial resolve-table-name opts) table-wildcards))}))
