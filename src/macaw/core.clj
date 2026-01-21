(ns macaw.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [macaw.ast :as m.ast]
   [macaw.collect :as collect]
   [macaw.rewrite :as rewrite]
   [macaw.types :as m.types]
   [macaw.walk :as m.walk]
   [malli.core :as m])
  (:import
   (com.metabase.macaw AnalysisError AstWalker$Scope BasicTableExtractor CompoundTableExtractor)
   (java.util.function Consumer)
   (net.sf.jsqlparser JSQLParserException)
   (net.sf.jsqlparser.expression Alias)
   (net.sf.jsqlparser.parser CCJSqlParser CCJSqlParserUtil)
   (net.sf.jsqlparser.parser.feature Feature)
   (net.sf.jsqlparser.schema Table Column)
   (net.sf.jsqlparser.statement Statement)
   (net.sf.jsqlparser.statement.select SelectItem)))

(set! *warn-on-reflection* true)

(defn- escape-keywords ^String [sql keywords]
  (reduce
   (fn [sql k]
     (str/replace sql (re-pattern (str "(?i)\\b(" (name k) ")\\b")) "$1____escaped____"))
   sql
   keywords))

(defn- unescape-keywords [sql _keywords]
  (str/replace sql "____escaped____" ""))

(defprotocol Unescapable
  (unescape [item keywords ctx]
    "Rewrite identifiers back to their original names, where they were escaped to bypass reserved words."))

(extend-protocol Unescapable

  nil
  (unescape [_item _keywords _ctx] nil)

  Table
  (unescape
    [item keywords ctx]
    (unescape (.getAlias item) keywords ctx)
    (.setName item (unescape-keywords (.getName item) keywords))
    nil)

  Column
  (unescape
    [item keywords _ctx]
    (.setColumnName item (unescape-keywords (.getColumnName item) keywords))
    nil)

  SelectItem
  (unescape
    [item keywords ctx]
    (unescape (.getAlias item) keywords ctx)
    nil)

  Alias
  (unescape
    [item keywords _ctx]
    (when-some [name* (some-> item .getName)]
      (.setName item (unescape-keywords name* keywords)))
    nil))

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

(defn- ->macaw-error [^AnalysisError analysis-error]
  {:error (keyword "macaw.error" (-> (.-errorType analysis-error)
                                     str/lower-case
                                     (str/replace #"_" "-")))})

(defn unescape-parsed
  "_Unescape_ the AST (`parsed`) created by `CCJSqlParserUtil/parse`.
    This compensates for the pre-processing done in [[escape-keywords]]."
  [parsed keywords]
  (try
    (cond-> parsed
      (seq keywords)
      (m.walk/walk-query
       (zipmap
        [:alias
         :column
         :column-qualifier
         :pseudo-table
         :table]
        (repeat #(unescape %1 keywords %2)))))
    (catch AnalysisError e
      (->macaw-error e))))

(defn parsed-query
  "Main entry point: takes a string query and returns a `Statement` object that can be handled by the other functions.

   NOTE: `unescape-parse` does not un-shift token positions (e.g. `net.sf.jsqlparser.parser.Token#-endColumn`),
         they continue to refer to the escaped string. C
         It would be complex and expensive to update every subsequent token, and unnecessary in most use cases.
         We account for this in [[replace-names]], and expect any future code to compensate for it too where needed."
  [^String query & {:as opts}]
  (try
    (-> query
        ;; Dialects like SQLite and Databricks treat consecutive blank lines as implicit semicolons.
        ;; JSQLParser, as a polyglot parser, always has this behavior, and there is no option to disable it.
        ;; This utility pre-processed the query to remove any such blank lines.
        (CCJSqlParserUtil/sanitizeSingleSql)
        (escape-keywords (:non-reserved-words opts))
        (CCJSqlParserUtil/parse (->parser-fn opts))
        (unescape-parsed (:non-reserved-words opts)))
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

(defn query->components
  "Given a parsed query (i.e., a [subclass of] `Statement`) return a map with the elements found within it.

  (Specifically, it returns their fully-qualified names as strings, where 'fully-qualified' means 'as referred to in
  the query'; this function doesn't do additional inference work to find out a table's schema.)

  Note that this is O(N^2) normally, but drops to O(N) when :strip-contexts? is specified."
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
          :ast-walker-1 (-> (query->components parsed (assoc opts :strip-contexts? true)) :tables raw-components (->> (hash-map :tables)))
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
        parsed   (parsed-query sql' (dissoc opts :non-reserved-keywords))]
    (if-let [error (:error parsed)]
      (throw (ex-info (str/capitalize (str/replace (name error) #"-" " "))
                      {:sql sql}
                      (:cause (:context parsed))))
      (-> (rewrite/replace-names sql' parsed renames' opts')
          (str/replace #"(?m)^ \n" "\n")
          (unescape-keywords (:non-reserved-words opts))))))

(defn ->ast
  "Given a sql query, return a clojure ast that represents it.

   This ast can potentially be lossy and generally shouldn't be as part of a round trip back to sql."
  [parsed]
  (m.ast/->ast parsed {:with-instance? false}))
