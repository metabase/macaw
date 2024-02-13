(ns macaw.core
  (:import
   (net.sf.jsqlparser
    Model)
   (net.sf.jsqlparser.parser
    CCJSqlParserUtil)
   (net.sf.jsqlparser.schema
    Column)
   (net.sf.jsqlparser.statement
    Statement
    StatementVisitor)
   (net.sf.jsqlparser.statement.update
    Update)
   (net.sf.jsqlparser.util
    TablesNamesFinder)))

#_(set! *warn-on-reflection* true)

(defn query->tables
  "Given a parsed query (i.e., a subclass of `Statement`) return a list of fully-qualified table names found within it.

  Note: 'fully-qualified'  means 'as found in the query'; it doesn't extrapolate schema names from other data sources."
  [^Statement parsed-query]
  (let [table-finder (TablesNamesFinder.)]
    (.getTableList table-finder parsed-query)))

(defprotocol ConjColumn
  (conj-column! [x known-columns]))

(extend-protocol ConjColumn
  Model
  (conj-column! [_ _known-column-names]
    (println "nothing to add")
    nil)

  Column
  (conj-column! [^Column column known-column-names]
    (println "CONJing!")
    (swap! known-column-names conj (.getColumnName column))))

(defn query->columns
  "TODO: implement!"
  [^Statement parsed-query]
  (println "query->columns")
  (let [column-names  (atom [])
        column-finder (proxy
                          [TablesNamesFinder]
                          []
                          (visit [visitable]
                            (println "visiting")
                            (println {:visitable visitable
                                      :column-names @column-names})
                            (let [^TablesNamesFinder this this]
                              (conj-column! visitable column-names)
                              (proxy-super visit visitable))))]
#_    (.init column-finder false)
#_    (.accept parsed-query column-finder)

    (.getTables column-finder parsed-query)
    #_    @column-names)
)


(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  (CCJSqlParserUtil/parse query))

(-> "select foo, bar from baz;"
    parsed-query
    query->columns)


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
  (let [parsed (parsed-query query)
        tables (query->tables parsed)
        columns (query->columns parsed)]
    (resolve-columns tables columns)))





(comment



  @(u/prog1 (atom [])
     (conj-column! 1 <>)
     (conj-column! 2.0 <>)
     (conj-column! (Integer. 8) <>))


  )
