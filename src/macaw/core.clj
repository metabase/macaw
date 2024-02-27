(ns macaw.core
  (:import
   (com.metabase.macaw
    ASTWalker)
   (net.sf.jsqlparser.parser
    CCJSqlParserUtil)
   (net.sf.jsqlparser.schema
    Column
    Table)
   (net.sf.jsqlparser.statement
    Statement)))

(set! *warn-on-reflection* true)

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the `:tables` and `:columns` found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [^Statement parsed-query]
  (let [column-names (atom #{})
        table-names  (atom #{})
        ast-walker (ASTWalker. {:column (fn [^Column column]
                                          (swap! column-names conj (.getColumnName column)))
                                :table  (fn [^Table table]
                                          (swap! table-names conj (.getName table)))})]
    (.walk ast-walker parsed-query)
    {:columns @column-names
     :tables  @table-names}))

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  (CCJSqlParserUtil/parse query))

(defn resolve-columns
  "TODO: Make this use metadata we know about.
  TODO: If nil is a column (from a select *) then no need for the rest of the entries
  TODO: might want to live in another ns"
  [tables columns]
  (let [cartesian-product (for [table tables
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
