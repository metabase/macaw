(ns macaw.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [macaw.collect :as collect]
   [macaw.rewrite :as rewrite])
  (:import
   (com.metabase.macaw AstWalker$Scope BasicTableExtractor AnalysisError CompoundTableExtractor)
   (java.util.function Consumer)
   (net.sf.jsqlparser JSQLParserException)
   (net.sf.jsqlparser.parser CCJSqlParser CCJSqlParserUtil)
   (net.sf.jsqlparser.parser.feature Feature)
   (net.sf.jsqlparser.schema Table)))

(set! *warn-on-reflection* true)

(defn- escape-keywords ^String [sql keywords]
  (reduce
   (fn [sql k]
     (str/replace sql (re-pattern (str "(?i)\\b(" (name k) ")\\b")) "$1____escaped____"))
   sql
   keywords))

(defn- unescape-keywords [sql _keywords]
  (str/replace sql "____escaped____" ""))

(def ^:private features
  {:backslash-escape-char  Feature/allowBackslashEscapeCharacter
   :complex-parsing        Feature/allowComplexParsing
   :postgres-syntax        Feature/allowPostgresSpecificSyntax
   :square-bracket-quotes  Feature/allowSquareBracketQuotation
   :unsupported-statements Feature/allowUnsupportedStatements})

(defn- ->Feature ^Feature [k]
  (get features k))

(def ^:private default-timeout-seconds 5)

(defn- ->parser-fn ^Consumer [opts]
  (reify Consumer
    (accept [_this parser]
      (let [^long timeout-ms (:timeout opts (* default-timeout-seconds 1000))]
        (.withFeature ^CCJSqlParser parser Feature/timeOut timeout-ms))
      (doseq [[f ^boolean v] (:features opts)]
        (.withFeature ^CCJSqlParser parser (->Feature f) v)))))

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions."
  [^String query & {:as opts}]
  ;; Dialects like SQLite and Databricks treat consecutive blank lines as implicit semicolons.
  ;; JSQLParser, as a polyglot parser, always has this behavior, and there is no option to disable it.
  ;; For Metabase, we are always dealing with single queries, so there's no point ever having this behavior.
  ;; TODO When JSQLParser 4.10 is released, move to the more robust [[CCJSqlParserUtil.sanitizeSingleSql]] helper.
  ;; See https://github.com/JSQLParser/JSqlParser/issues/1988
  (try
    (-> query
        (str/replace #"\n{2,}" "\n")
        (escape-keywords (:non-reserved-words opts))
        (CCJSqlParserUtil/parse (->parser-fn opts)))
    (catch JSQLParserException e
      {:error   :macaw.error/unable-to-parse
       :context {:cause e}})))

(defn scope-id
  "A unique identifier for the given scope."
  [^AstWalker$Scope s]
  (.getId s))

(defn scope-label
  "The type of scope we're talking about e.g., a top-level SELECT."
  [^AstWalker$Scope s]
  (.getLabel s))

(defn- ->macaw-error [^AnalysisError analysis-error]
  {:error (keyword "macaw.error" (-> (.-errorType analysis-error)
                                     str/lower-case
                                     (str/replace #"_" "-")))})

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the elements found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)"
  [parsed & {:as opts}]
  ;; By default, we will preserve identifiers verbatim, to be agnostic of case and quote behavior.
  ;; This may result in duplicate components, which are left to the caller to deduplicate.
  ;; In Metabase's case, this is done during the stage where the database metadata is queried.
  (try
    (if (map? parsed)
      parsed
      (->> (collect/query->components parsed (merge {:preserve-identifiers? true} opts))
           (walk/postwalk (fn [x]
                            (if (string? x)
                              (unescape-keywords x (:non-reserved-words opts))
                              x)))))
    (catch AnalysisError e
      (->macaw-error e))))

(defn- raw-components [xs]
  (into (empty xs) (keep :component) xs))

(defn- table->identifier
  "Given a table object, return a map with the schema and table names."
  [^Table t]
  (if (.getSchemaName t)
    {:schema (.getSchemaName t)
     :table  (.getName t)}
    {:table (.getName t)}))

(defn- tables->identifiers [expr]
  {:tables (set (map table->identifier expr))})

(defn query->tables
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a set of all the table identifiers found within it."
  [sql & {:keys [mode] :as opts}]
  (try
    (let [parsed (parsed-query sql opts)]
      (if (map? parsed)
        parsed
        (case mode
          :ast-walker-1 (-> (query->components parsed opts) :tables raw-components (->> (hash-map :tables)))
          :basic-select (-> (BasicTableExtractor/getTables parsed) tables->identifiers)
          :compound-select (-> (CompoundTableExtractor/getTables parsed) tables->identifiers))))
    (catch AnalysisError e
      (->macaw-error e))))

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
  ;; Currently, we use a more complex and expensive sanitization method, so that it's reversible.
  ;; If we decide that it's OK to normalize whitespace etc. during replacement, then we can use the same helper.
  (let [sql'     (escape-keywords (str/replace sql #"(?m)^\n" " \n") (:non-reserved-words opts))
        opts'    (select-keys opts [:case-insensitive :quotes-preserve-case? :allow-unused?])
        renames' (walk/postwalk (fn [x]
                                  (if (string? x)
                                    (escape-keywords x (:non-reserved-words opts))
                                    x))
                                renames)
        parsed   (parsed-query sql' opts)]
    (-> (rewrite/replace-names sql' parsed renames' opts')
        (str/replace #"(?m)^ \n" "\n")
        (unescape-keywords (:non-reserved-words opts)))))
