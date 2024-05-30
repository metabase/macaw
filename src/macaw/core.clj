(ns macaw.core
  (:require
   [macaw.collect :as collect]
   [macaw.rewrite :as rewrite])
  (:import
   (net.sf.jsqlparser.parser CCJSqlParserUtil)))

(set! *warn-on-reflection* true)

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  (CCJSqlParserUtil/parse query))

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the elements found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [statement & {:as opts}]
  ;; By default, we will preserve identifiers verbatim, to be agnostic of case and quote behaviour.
  ;; This may result in duplicate components, which are left to the caller to deduplicate.
  (collect/query->components statement (merge {:preserve-identifiers? true} opts)))

(defn replace-names
  "Given a SQL query, apply the given table, column, and schema renames."
  [sql renames & {:as opts}]
  (rewrite/replace-names sql (parsed-query sql) renames opts))
