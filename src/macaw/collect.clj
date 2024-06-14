(ns macaw.collect
  (:require
   [clojure.string :as str]
   [macaw.util :as u]
   [macaw.walk :as mw])
  (:import
   (com.metabase.macaw AstWalker$Scope)
   (java.util.regex Pattern)
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
                                                 (fn [^AstWalker$Scope x]
                                                   [(keyword (.getType x)) (.getLabel x) (.getId x)])
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

(defn- setting->relax-case [{:keys [case-insensitive]}]
  (when case-insensitive
    (case case-insensitive
      :lower str/lower-case
      :upper str/upper-case
      ;; This will work for replace, but not for analyzing where we need a literal to accumulate
      :agnostic (fn [s] (re-pattern (str "(?i)" (Pattern/quote s)))))))

(defn normalize-reference
  "Normalize a schema, table, column, etc. references so that we can match them regardless of syntactic differences."
  [s {:keys [preserve-identifiers? quotes-preserve-case?] :as opts}]
  (if preserve-identifiers?
    s
    (when s
      (let [quoted     (quoted? s)
            relax-case (when-not (and quotes-preserve-case? quoted)
                         (setting->relax-case opts))]
        (cond-> s
          quoted     strip-quotes
          relax-case relax-case)))))

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
  (let [{:keys [schema table]} (maybe-column-table opts c)]
    (u/strip-nils
     {:schema    schema
      :table     table
      :column    (normalize-reference (.getColumnName c) opts)
      :alias     (let [[k y] (first ctx)]
                   (when (= k :alias) y))
      :instances (when (:with-instance opts) [c])})))

;;; get them together

(defn- only-query-context [ctx]
  (into [] (comp (filter #(= (first %) :query))
                 (map (comp vec rest)))
    ctx))

(defn- update-components
  ([f]
   (map #(-> %
             (update :component f (:context %))
             (update :context only-query-context))))
  ([f components]
   (eduction (update-components f) components)))

(defn- merge-with-instances
  "Merge two nodes, keeping the union of their instances."
  [a b]
  (let [cs-a (-> a :component :instances)]
    (cond-> (merge a b)
      cs-a (update-in [:component :instances] into cs-a))))

(defn- remove-redundancies
  "Remove any unqualified references that would resolve to a given qualified reference"
  [column-set]
  ;; TODO as far as "used columns" go, we don't really care about context, and should drop it before doing this
  (let [{qualified true, unqualified false} (group-by (comp boolean :table :component) column-set)
        qualifications (into #{} (mapcat #(keep (comp % :component) qualified)) [:column :alias])]
    (into qualified (remove (comp qualifications :column :component)) unqualified)))

(defn query->components
  "See macaw.core/query->components doc."
  [^Statement parsed-ast & {:as opts}]
  (let [{:keys [columns
                qualifiers
                has-wildcard?
                mutation-commands
                tables
                table-wildcards]} (query->raw-components parsed-ast)
        alias-map                 (into {} (map #(-> % :component ((partial alias-mapping opts) (:context %))) tables))
        ;; we're parsing qualifiers here for a single purpose - rewrite uses instances to find tables for renaming
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
        table-map                 (merge-with merge-with-instances qualifier-map table-map)
        raw-columns               (into #{} (update-components (partial make-column opts)) columns)
        strip-alias               (fn [c] (dissoc c :alias))]
    {:columns           raw-columns
     :source-columns    (into #{} (map #(update % :component strip-alias)) (remove-redundancies raw-columns))
     :elements          (into #{} (map #(update % :component strip-alias)) raw-columns)
     :has-wildcard?     (into #{} (update-components (fn [x & _args] x)) has-wildcard?)
     :mutation-commands (into #{} mutation-commands)
     :tables            (into #{} (vals table-map))
     :table-wildcards   (into #{} (update-components (partial resolve-table-name opts)) table-wildcards)}))
