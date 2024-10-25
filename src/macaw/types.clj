(ns macaw.types)

(def modes
  "The different analyzer strategies that Macaw supports."
  [:ast-walker-1
   :basic-select
   :compound-select])

(def options-map
  "The shape of the options accepted by our API"
  [:map
   [:mode                  {:optional true} (into [:enum] modes)]
   [:non-reserved-words    {:optional true} [:seqable :keyword]]
   [:allow-unused?         {:optional true} :boolean]
   [:case-insensitive      {:optional true} [:enum :upper :lower :agnostic]]
   [:quotes-preserve-case? {:optional true} :boolean]])

(def error-types
  "The different types of errors that Macaw can return."
  [:macaw.error/analysis-error
   :macaw.error/illegal-expression
   :macaw.error/invalid-query
   :macaw.error/unable-to-parse])

(def error-result
  "A map indicating that we were not able to parse the query."
  [:map
   [:error (into [:enum] error-types)]])

(def ^:private table-ident
  [:map
   [:schema {:optional true} :string]
   [:table                   :string]])

(def ^:private column-ident
  [:map
   [:schema {:optional true} :string]
   [:table  {:optional true} :string]
   [:column                  :string]])

(defn- wrapped [t]
  [:map
   [:component t]
   [:context :any]])

(def components-result
  "A map holding all the components that we were able to parse from a query"
  [:map {:closed true}
   [:tables         [:set (wrapped table-ident)]]
   [:columns        [:set (wrapped column-ident)]]
   [:source-columns [:set column-ident]]
   ;; TODO tighten types
   [:tables-superset   [:set :any]]
   [:has-wildcard?     [:set :any]]
   [:mutation-commands [:set :any]]
   [:table-wildcards   [:set :any]]])

(def tables-result
  "A map holding the tables that we were able to parse from a query"
  [:map
   [:tables [:set table-ident]]])
