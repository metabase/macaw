(ns macaw.core
  (:import
   (com.metabase.macaw
    ASTWalker
    SqlVisitor)
   (net.sf.jsqlparser
    Model)
   (net.sf.jsqlparser.parser
    CCJSqlParserUtil)
   (net.sf.jsqlparser.schema
    Column)
   (net.sf.jsqlparser.statement
    Statement)
   (net.sf.jsqlparser.util
    TablesNamesFinder)))

#_(set! *warn-on-reflection* true)

(defn query->tables
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a list of fully-qualified table names found within it.

  Note: 'fully-qualified'  means 'as found in the query'; it doesn't extrapolate schema names from other data sources."
  [^Statement parsed-query]
  (let [table-finder (TablesNamesFinder.)]
    (.getTableList table-finder parsed-query)))

(defn query->columns
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a list of the column names found within it.)"
  [^Statement parsed-query]
  (let [column-names  (atom [])
        column-finder (reify
                        SqlVisitor
                        (^void visitColumn [_this ^Column column]
                          (swap! column-names conj (.getColumnName column)))
                        (visitTable [_this _table]))]
    (.walk (ASTWalker. column-finder) parsed-query)
    @column-names))

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
                            {:table table
                             :column column})]
    (update-vals (group-by :table cartesian-product)
                 #(merge-with concat (map :column %)))))

(defn lineage
  "Returns a sequence of the columns used in / referenced by the query"
  [query]
  (let [parsed  (parsed-query query)
        tables  (query->tables parsed)
        columns (query->columns parsed)]
    (resolve-columns tables columns)))
