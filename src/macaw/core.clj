(ns macaw.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [macaw.ast :as m.ast]
   [macaw.collect :as collect]
   [macaw.rewrite :as rewrite]
   [macaw.types :as m.types]
   [malli.core :as m])
  (:import
   (com.metabase.macaw AnalysisError AstWalker$Scope BasicTableExtractor CompoundTableExtractor)
   (java.util.function Consumer)
   (net.sf.jsqlparser JSQLParserException)
   (net.sf.jsqlparser.parser CCJSqlParser CCJSqlParserUtil)
   (net.sf.jsqlparser.parser.feature Feature)
   (net.sf.jsqlparser.schema Table)
   (net.sf.jsqlparser.statement Statement)))

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
  (try
    (-> query
        ;; Dialects like SQLite and Databricks treat consecutive blank lines as implicit semicolons.
        ;; JSQLParser, as a polyglot parser, always has this behavior, and there is no option to disable it.
        ;; This utility pre-processed the query to remove any such blank lines.
        (CCJSqlParserUtil/sanitizeSingleSql)
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
  (m/validate [:or m.types/error-result [:fn #(instance? Statement %)]] parsed)
  (m/validate [:maybe m.types/options-map] opts)
  ;; By default, we will preserve identifiers verbatim, to be agnostic of casing and quoting.
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
  (m/validate :string sql)
  (m/validate [:maybe m.types/options-map] opts)
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
  (m/validate :string sql)
  (m/validate :map renames)
  (m/validate [:maybe m.types/options-map] opts)
  ;; We need to pre-sanitize the SQL before its analyzed so that the AST token positions match up correctly.
  ;; Currently, we use a more complex and expensive sanitization method, so that it's reversible.
  ;; If we decide that it's OK to normalize whitespace etc. during replacement, then we can use the same helper.
  (let [sql'     (-> (str/replace sql #"(?m)^\n" " \n")
                     (escape-keywords (:non-reserved-words opts)))
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

(defn ->ast
  "Given an SQL query, return a clojure AST that represents it.

   This AST can potentially be lossy, and generally shouldn't be as part of a round trip back to SQL."
  [parsed]
  (m.ast/->ast parsed {:with-instance? false}))
