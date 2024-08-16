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
   (net.sf.jsqlparser.statement.select AllTableColumns SelectItem)))

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
                 {:alias            (conj-to :aliases (fn [^SelectItem item]
                                                        {:alias      (.getName (.getAlias item))
                                                         :expression (.getExpression item)}))
                  :column           (conj-to :columns)
                  :column-qualifier (conj-to :qualifiers)
                  :mutation         (conj-to :mutation-commands)
                  :pseudo-table     (conj-to :pseudo-tables)
                  :table            (conj-to :tables)
                  :table-wildcard   (conj-to :table-wildcards)
                  :wildcard         (conj-to :has-wildcard? (constantly true))}
                 {:aliases           #{}
                  :columns           #{}
                  :has-wildcard?     #{}
                  :mutation-commands #{}
                  :pseudo-tables     #{}
                  :tables            #{}
                  :table-wildcards   #{}}))

;;; tables

(def ^:private quotes (map str "`\"["))

(def ^:private closing {"[" "]"})

(defn- quoted? [s]
  (some (fn [q]
          (and (str/starts-with? s q)
               (str/ends-with? s (closing q q))))
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

(defn- find-table [{:keys [alias->table name->table keep-internal-tables?] :as opts} ^Table t]
  (let [n      (normalize-reference (.getName t) opts)
        schema (normalize-reference (.getSchemaName t) opts)]
    (or (get alias->table n)
        (:component (last (u/find-relevant name->table {:table n :schema schema} [:table :schema])))
        (when keep-internal-tables?
          {:table n, :schema schema, :internal? true}))))

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

(defn- maybe-column-table [alias? {:keys [name->table] :as opts} ^Column c]
  (if-let [t (.getTable c)]
    (find-table opts t)
    ;; if we see only a single table, we can safely say it's the table of that column
    (when (and (= (count name->table) 1) (not (alias? (.getColumnName c))))
      (:component (val (first name->table))))))

(defn- make-column [aliases opts ^Column c ctx]
  (let [{:keys [schema table]} (maybe-column-table aliases opts c)]
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

(def ^:private strip-non-query-contexts
  (map #(update % :context only-query-context)))

(defn- update-components
  ([f]
   (map #(update % :component f (:context %))))
  ([f components]
   (eduction (update-components f) components)))

(defn- merge-with-instances
  "Merge two nodes, keeping the union of their instances."
  [a b]
  (let [cs-a (-> a :component :instances)]
    (cond-> (merge a b)
      cs-a (update-in [:component :instances] into cs-a))))

(defn- literal? [{:keys [column]}]
  ;; numbers and strings are already handled by JSQLParser
  (#{"true" "false"} (str/lower-case column)))

(defn- remove-redundant-columns
  "Remove any unqualified references that would resolve to an alias or qualified reference"
  [alias? column-set]
  (let [{qualified true, unqualified false} (group-by (comp boolean :table) column-set)
        ;; Get all the bindings introduced by qualified columns
        has-qualification? (into #{} (mapcat #(keep % qualified)) [:column :alias])]
    (into qualified
          (remove (comp (some-fn alias? has-qualification?)
                        :column))
          unqualified)))

(defn- infer-table-schema [columns node]
  (update node :component
          #(let [{:keys [schema table] :as element} %]
             (if schema
               element
               (if-let [schema' (->> columns
                                     (filter (comp #{table} :table))
                                     (some :schema))]
                 (assoc element :schema schema')
                 element)))))

(defn query->components
  "See macaw.core/query->components doc."
  [^Statement parsed-ast & {:as opts}]
  (let [{:keys [aliases
                columns
                qualifiers
                has-wildcard?
                mutation-commands
                pseudo-tables
                tables
                table-wildcards]} (query->raw-components parsed-ast)
        alias-map                 (into {} (map #(-> % :component ((partial alias-mapping opts) (:context %))) tables))
        ;; we're parsing qualifiers here for a single purpose - rewrite uses instances to find tables for renaming
        table-map                 (->> (update-components (partial make-table opts) tables)
                                       (u/group-with #(select-keys (:component %) [:schema :table])
                                                     merge-with-instances))
        pseudo-table-names        (into #{} (comp (map :component)
                                                  (map (fn [^Alias a] (.getName a))))
                                        pseudo-tables)
        table-map                 (into (empty table-map)
                                        (remove (comp pseudo-table-names :table :component val))
                                        table-map)
        ;; we need both aliases and tables for columns
        opts                      (assoc opts
                                         :alias->table alias-map
                                         :name->table table-map)
        qualifier-map             (->> (update-components (partial find-qualifier-table opts) qualifiers)
                                       (u/group-with #(select-keys (:component %) [:schema :table])
                                                     merge-with-instances))
        alias?                    (into #{} (keep (comp :alias :component)) aliases)
        all-columns               (into #{}
                                        (comp (update-components (partial make-column alias?
                                                                          (assoc opts :keep-internal-tables? true)))
                                              strip-non-query-contexts)
                                        columns)
        strip-alias               (fn [c] (dissoc c :alias))
        source-columns            (->> (map :component all-columns)
                                       (remove-redundant-columns alias?)
                                       (remove literal?)
                                       (into #{}
                                             (comp (remove (comp pseudo-table-names :table))
                                                   (remove :internal?)
                                                   (map strip-alias))))
        table-map                 (update-vals table-map (partial infer-table-schema source-columns))]
    {:columns           all-columns
     :source-columns    source-columns
     ;; result-columns ... filter out the elements (and wildcards) in the top level scope only.
     :has-wildcard?     (into #{} strip-non-query-contexts has-wildcard?)
     :mutation-commands (into #{} mutation-commands)
     :tables            (into #{} (comp (map val)
                                        (remove (comp :internal? :component))
                                        strip-non-query-contexts)
                              table-map)
     :tables-superset   (into #{}
                              (comp (map val) strip-non-query-contexts)
                              (merge-with merge-with-instances qualifier-map table-map))
     :table-wildcards   (into #{}
                              (comp strip-non-query-contexts
                                    (update-components (partial resolve-table-name opts)))
                              table-wildcards)}))
