(ns macaw.ast-types
  (:require
   [macaw.ast :as ast]
   [malli.core :as m]
   [malli.registry :as mr]
   [malli.util :as mu]))

(def ^:private base-node
  [:map
   [:type :keyword]
   [:instance {:optional true} :any]])

(def ^:private unrecognized-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/unrecognized-node]]
    [:instance :any]]])

(def ^:private select-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/select]]
    [:select {:optional true} [:sequential [:ref ::ast-node]]]
    [:from {:optional true} [:ref ::ast-node]]
    [:where {:optional true} [:ref ::ast-node]]
    [:join {:optional true} [:sequential [:ref ::ast-node]]]
    [:group-by {:optional true} [:sequential [:ref ::ast-node]]]
    [:order-by {:optional true} [:sequential [:ref ::ast-node]]]
    [:with {:optional true} [:sequential [:ref ::ast-node]]]
    [:alias {:optional true} :string]
    [:table-alias {:optional true} :string]]])

(def ^:private wildcard-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/wildcard]]]])

(def  ^:private table-wildcard-node
  [:merge
   base-node
   [:ref ::table-node]
   [:map
    [:type [:= ::ast/table-wildcard]]]])

(def ^:private column-node
  [:merge
   base-node
   [:ref ::table-node]
   [:map
    [:type [:= ::ast/column]]
    [:column :string]]])

(def ^:private table-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/table]]
    [:database {:optional true} :string]
    [:schema {:optional true} :string]
    [:table {:optional true} :string]
    [:table-alias {:optional true} :string]]])

(def ^:private join-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/join]]
    [:source [:ref ::ast-node]]
    [:condition {:optional true} [:sequential [:ref ::ast-node]]]]])

(def ^:private binary-expression-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/binary-expression]]
    [:operator :string]
    [:left [:ref ::ast-node]]
    [:right [:ref ::ast-node]]]])

(def ^:private literal-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/literal]]
    [:value {:optional true} :any]]])

(def ^:private function-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/function]]
    [:name :string]
    [:params {:optional true} [:sequential [:ref ::ast-node]]]]])

(def ^:private table-function-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/table-function]]
    [:name :string]
    [:params {:optional true} [:sequential [:ref ::ast-node]]]
    [:alias {:optional true} :string]]])

(def ^:private expression-list-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/expression-list]]
    [:expressions {:optional true} [:sequential [:ref ::ast-node]]]]])

(def ^:private interval-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/interval]]
    [:value :string]]])

(def ^:private base-unary-expression-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/unary-expression]]
    [:operation :keyword]
    [:expression [:ref ::ast-node]]]])

(def ^:private cast-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :cast]]
    [:datatype :string]]])

(def ^:private extract-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :extract]]
    [:part :string]]])

(def ^:private sign-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :sign]]
    [:sign :string]]])

(def ^:private exists-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :exists]]]])

(def ^:private is-null-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :is-null]]
    [:not :boolean]]])

(def ^:private not-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :not]]]])

(def ^:private jdbc-parameter-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/jdbc-parameter]]]])

(def ^:private case-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/case]]
    [:switch {:optional true} [:ref ::ast-node]]
    [:when-clauses {:optional true} [:sequential
                                     [:map
                                      [:when [:ref ::ast-node]]
                                      [:then [:ref ::ast-node]]]]]
    [:else {:optional true} [:ref ::ast-node]]]])

(def ^:private set-operation-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/set-operation]]
    [:selects {:optional true} [:sequential [:ref ::ast-node]]]
    [:operations {:optional true} [:sequential :string]]]])

(def ^:private analytic-expression-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/analytic-expression]]
    [:expression {:optional true} [:ref ::ast-node]]
    [:offset {:optional true} [:ref ::ast-node]]
    [:window {:optional true} [:ref ::ast-node]]
    [:name :string]
    [:partition-by {:optional true} [:sequential [:ref ::ast-node]]]
    [:order-by {:optional true} [:sequential [:ref ::ast-node]]]]])

(def ^:private between-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/between]]
    [:expression [:ref ::ast-node]]
    [:start [:ref ::ast-node]]
    [:end [:ref ::ast-node]]]])

(def ^:private time-key-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/time-key]]
    [:value :string]]])

(def ^:private unary-expression-node
  [:multi {:dispatch :operation}
   [:cast [:ref ::cast-node]]
   [:extract [:ref ::extract-node]]
   [:sign [:ref ::sign-node]]
   [:exists [:ref ::exists-node]]
   [:is-null [:ref ::is-null-node]]
   [:not [:ref ::not-node]]])

(def ^:private ast-node
  [:multi {:dispatch :type}
   [::ast/unrecognized-node [:ref ::unrecognized-node]]
   [::ast/select [:ref ::select-node]]
   [::ast/wildcard [:ref ::wildcard-node]]
   [::ast/table-wildcard [:ref ::table-wildcard-node]]
   [::ast/column [:ref ::column-node]]
   [::ast/table [:ref ::table-node]]
   [::ast/join [:ref ::join-node]]
   [::ast/binary-expression [:ref ::binary-expression-node]]
   [::ast/literal [:ref ::literal-node]]
   [::ast/function [:ref ::function-node]]
   [::ast/table-function [:ref ::table-function-node]]
   [::ast/expression-list [:ref ::expression-list-node]]
   [::ast/interval [:ref ::interval-node]]
   [::ast/jdbc-parameter [:ref ::jdbc-parameter-node]]
   [::ast/case [:ref ::case-node]]
   [::ast/set-operation [:ref ::set-operation-node]]
   [::ast/analytic-expression [:ref ::analytic-expression-node]]
   [::ast/between [:ref ::between-node]]
   [::ast/time-key [:ref ::time-key-node]]
   [::ast/unary-expression [:ref ::unary-expression-node]]])

(def registry
  "The registry value that contains all of individual node malli schemas."
  {::unrecognized-node unrecognized-node
   ::select-node select-node
   ::wildcard-node wildcard-node
   ::table-wildcard-node table-wildcard-node
   ::column-node column-node
   ::table-node table-node
   ::join-node join-node
   ::binary-expression-node binary-expression-node
   ::literal-node literal-node
   ::function-node function-node
   ::table-function-node table-function-node
   ::expression-list-node expression-list-node
   ::interval-node interval-node
   ::cast-node cast-node
   ::extract-node extract-node
   ::sign-node sign-node
   ::exists-node exists-node
   ::is-null-node is-null-node
   ::not-node not-node
   ::jdbc-parameter-node jdbc-parameter-node
   ::case-node case-node
   ::set-operation-node set-operation-node
   ::analytic-expression-node analytic-expression-node
   ::between-node between-node
   ::time-key-node time-key-node
   ::unary-expression-node unary-expression-node
   ::ast-node ast-node})

(def ast
  "A malli spec for an ast node (as returned by ->ast)."
  [:ref {:registry registry} ::ast-node])

(def base-registry
  "A base registry that works with the ast malli schema"
  (mr/composite-registry
   (m/default-schemas)
   (mu/schemas)))
