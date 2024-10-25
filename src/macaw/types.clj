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

(defn- with-context [t]
  [:map
   [:component t]
   [:context :any]])

(def components-result
  "A map holding all the components that we were able to parse from a query"
  [:map {:closed true}
   [:tables         [:set (with-context table-ident)]]
   [:columns        [:set (with-context column-ident)]]
   [:source-columns [:set column-ident]]
   ;; TODO Unclear why we would want to wrap any of these.
   [:table-wildcards   [:set (with-context table-ident)]]
   ;; This :maybe would be a problem, if anything actually used this value.
   [:tables-superset   [:set (with-context [:maybe table-ident])]]
   ;; Unclear why we need a collection here
   [:has-wildcard?     [:set (with-context :boolean)]]
   [:mutation-commands [:set (with-context :string)]]])

(def tables-result
  "A map holding the tables that we were able to parse from a query"
  [:map
   [:tables [:set table-ident]]])
