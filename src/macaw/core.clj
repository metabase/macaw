(ns macaw.core
  (:require
   [clojure.string :as str]
   [macaw.collect :as collect]
   [macaw.rewrite :as rewrite])
  (:import
   (net.sf.jsqlparser.parser CCJSqlParserUtil)))

(set! *warn-on-reflection* true)

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query]
  ;; Dialects like SQLite and Databricks treat consecutive blank lines as implicit semicolons.
  ;; JSQLParser, as a polyglot parser, always has this behavior, and there is no option to disable it.
  ;; For Metabase, we are always dealing with single queries, so there's no point ever having this behavior.
  ;; TODO When JSQLParser 4.10 is released, move to the more robust [[CCJSqlParserUtil.sanitizeSingleSql]] helper.
  ;; See https://github.com/JSQLParser/JSqlParser/issues/1988
  (CCJSqlParserUtil/parse (str/replace query #"\n{2,}" "\n")))

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the elements found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [statement & {:as opts}]
  ;; By default, we will preserve identifiers verbatim, to be agnostic of case and quote behavior.
  ;; This may result in duplicate components, which are left to the caller to deduplicate.
  ;; In Metabase's case, this is done during the stage where the database metadata is queried.
  (collect/query->components statement (merge {:preserve-identifiers? true} opts)))

(defn replace-names
  "Given an SQL query, apply the given table, column, and schema renames.

  Supported options:

  - case-insensitive: whether to relax the comparison
    - :upper    - identifiers are implicitly case to uppercase, as per the SQL-92 standard.
    - :lower    - identifiers are implicitly cast to lowercase, as per Postgres et al.
    - :agnostic - case is ignored when comparing identifiers in code to replacement \"from\" strings.

  - quotes-preserve-case: whether quoted identifiers should override the previous option."
  [sql renames & {:as opts}]
  ;; We need to pre-sanitize the SQL before its analyzed so that the AST token positions match up correctly.
  ;; Currently we use a more complex and expensive sanitization method, so that it's reversible.
  ;; If we decide that it's OK to normalize whitespace etc. during replacement then we can use the same helper.
  (let [sql' (str/replace sql #"(?m)^\n" " \n")
        opts' (select-keys opts [:case-insensitive :quotes-preserve-case? :allow-unused?])]
    (str/replace (rewrite/replace-names sql' (parsed-query sql') renames opts')
                 #"(?m)^ \n" "\n")))
