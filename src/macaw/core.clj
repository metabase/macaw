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
                 {:column     (conj-to :columns)
                  :star       (fn [results _all-columns]
                                (update results :select-star? (constantly true)))
                  :table      (conj-to :tables)
                  :table-star (conj-to :table-stars)}
                 {:columns      #{}
                  :select-star? false
                  :tables       #{}
                  :table-stars  #{}}))

(defn- alias-mapping
  [^Table table]
  (when-let [^Alias table-alias (.getAlias table)]
    [(.getName table-alias) (.getName table)]))

(defn- resolve-table-name
  "JSQLParser can't tell whether the `f` in `select f.*` refers to a real table or an alias. Therefore, we have to
  disambiguate them based on our own map of aliases->table names. So this function will return the real name of the table
  referenced in a table-star (as far as can be determined from the query)."
  [alias->name ^AllTableColumns atc]
  (let [table-name (-> atc .getTable .getName)]
    (or (alias->name table-name)
        table-name)))

(defn- remove-aliases
  [aliases table-names]
  (let [alias? (into #{} (keys aliases))]
    (filter (complement alias?) table-names)))

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the `:tables` and `:columns` found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [^Statement parsed-query]
  (let [{:keys [columns select-star? tables table-stars]} (query->raw-components parsed-query)
        aliases                                           (into {} (map alias-mapping tables))]
    {:columns      (into #{} (map #(.getColumnName ^Column %) columns))
     :select-star? select-star?
     :tables       (into #{} (remove-aliases aliases (map #(.getName ^Table %) tables)))
     :table-stars  (into #{} (map (partial resolve-table-name aliases) table-stars))}))

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  (CCJSqlParserUtil/parse query))

(defn resolve-columns
  "TODO: Make this use metadata we know about.
  TODO: If nil is a column (from a select *) then no need for the rest of the entries
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
