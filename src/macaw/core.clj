(ns macaw.core
  (:require
   [macaw.rewrite :as rewrite]
   [macaw.walk :as mw])
  (:import
   (net.sf.jsqlparser.expression Alias)
   (net.sf.jsqlparser.parser CCJSqlParserUtil)
   (net.sf.jsqlparser.schema Column Table)
   (net.sf.jsqlparser.statement Statement)
   (net.sf.jsqlparser.statement.select AllTableColumns)))

(set! *warn-on-reflection* true)

(defn- conj-to
  [key-name]
  (fn item-conjer [results item]
    (update results key-name conj item)))

(defn- query->raw-components
  [^Statement parsed-query]
  (mw/fold-query parsed-query
                 {:column         (conj-to :columns)
                  :mutation       (conj-to :mutation-commands)
                  :wildcard       (fn [results _all-columns]
                                    (assoc results :has-wildcard? true))
                  :table          (conj-to :tables)
                  :table-wildcard (conj-to :table-wildcards)}
                 {:columns           #{}
                  :has-wildcard?     false
                  :mutation-commands #{}
                  :tables            #{}
                  :table-wildcards   #{}}))

(defn- alias-mapping
  [^Table table]
  (when-let [^Alias table-alias (.getAlias table)]
    [(.getName table-alias) (.getName table)]))

(defn- resolve-table-name
  "JSQLParser can't tell whether the `f` in `select f.*` refers to a real table or an alias. Therefore, we have to
  disambiguate them based on our own map of aliases->table names. So this function will return the real name of the table
  referenced in a table-wildcard (as far as can be determined from the query)."
  [alias->name ^AllTableColumns atc]
  (let [table-name (-> atc .getTable .getName)]
    (or (alias->name table-name)
        table-name)))

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the elements found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [^Statement parsed-query]
  (let [{:keys [columns has-wildcard?
                mutation-commands
                tables table-wildcards]} (query->raw-components parsed-query)
        aliases                   (into {} (map alias-mapping tables))]
    {:columns           (into #{} (map #(.getColumnName ^Column %) columns))
     :has-wildcard?     has-wildcard?
     :mutation-commands (into #{} mutation-commands)
     :tables            (into #{} (map #(.getName ^Table %) tables))
     :table-wildcards   (into #{} (map (partial resolve-table-name aliases) table-wildcards))}))

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  (CCJSqlParserUtil/parse query))

(defn resolve-columns
  "TODO: Make this use metadata we know about.
  TODO: might want to live in another ns"
  [tables columns]
  (let [cartesian-product (for [table  tables
                                column columns]
                            {:table  table
                             :column column})]
    (update-vals (group-by :table cartesian-product)
                 #(merge-with concat (map :column %)))))

(defn lineage
  "Returns a sequence of the columns used in / referenced by the query"
  [query]
  (let [parsed                   (parsed-query query)
        {:keys [columns tables]} (query->components parsed)]
    (resolve-columns tables columns)))

(defn replace-names
  "Given a SQL query, apply the given table and column renames."
  [sql renames]
  (rewrite/replace-names sql (parsed-query sql) renames))
