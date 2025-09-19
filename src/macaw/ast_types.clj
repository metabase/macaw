(ns macaw.ast-types
  (:require
   [macaw.ast :as ast]
   [malli.core :as m]
   [malli.registry :as mr]
   [malli.util :as mu]))

(def base-node
  [:map
   [:type :keyword]
   [:instance {:optional true} :any]])

(def unrecognized-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/unrecognized-node]]
    [:instance :any]]])

(def select-node
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

(def wildcard-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/wildcard]]]])

(def table-wildcard-node
  [:merge
   base-node
   [:ref ::table-node]
   [:map
    [:type [:= ::ast/table-wildcard]]]])

(def column-node
  [:merge
   base-node
   [:ref ::table-node]
   [:map
    [:type [:= ::ast/column]]
    [:column :string]]])

(def table-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/table]]
    [:database {:optional true} :string]
    [:schema {:optional true} :string]
    [:table {:optional true} :string]
    [:table-alias {:optional true} :string]]])

(def join-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/join]]
    [:source [:ref ::ast-node]]
    [:condition {:optional true} [:sequential [:ref ::ast-node]]]]])

(def binary-expression-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/binary-expression]]
    [:operator :string]
    [:left [:ref ::ast-node]]
    [:right [:ref ::ast-node]]]])

(def literal-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/literal]]
    [:value {:optional true} :any]]])

(def function-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/function]]
    [:name :string]
    [:params {:optional true} [:sequential [:ref ::ast-node]]]]])

(def expression-list-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/expression-list]]
    [:expressions {:optional true} [:sequential [:ref ::ast-node]]]]])

(def interval-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/interval]]
    [:value :string]]])

(def base-unary-expression-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/unary-expression]]
    [:operation :keyword]
    [:expression [:ref ::ast-node]]]])

(def cast-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :cast]]
    [:datatype :string]]])

(def extract-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :extract]]
    [:part :string]]])

(def sign-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :sign]]
    [:sign :string]]])

(def exists-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :exists]]]])

(def is-null-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :is-null]]
    [:not :boolean]]])

(def not-node
  [:merge
   base-unary-expression-node
   [:map
    [:operation [:= :not]]]])

(def jdbc-parameter-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/jdbc-parameter]]]])

(def case-node
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

(def set-operation-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/set-operation]]
    [:selects {:optional true} [:sequential [:ref ::ast-node]]]
    [:operations {:optional true} [:sequential :string]]]])

(def analytic-expression-node
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

(def between-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/between]]
    [:expression [:ref ::ast-node]]
    [:start [:ref ::ast-node]]
    [:end [:ref ::ast-node]]]])

(def time-key-node
  [:merge
   base-node
   [:map
    [:type [:= ::ast/time-key]]
    [:value :string]]])

(def unary-expression-node
  [:multi {:dispatch :operation}
   [:cast [:ref ::cast-node]]
   [:extract [:ref ::extract-node]]
   [:sign [:ref ::sign-node]]
   [:exists [:ref ::exists-node]]
   [:is-null [:ref ::is-null-node]]
   [:not [:ref ::not-node]]])

(def ast-node
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
  #_{::base-node base-node}
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
  [:ref {:registry registry} ::ast-node])

(def base-registry
  (mr/composite-registry
   (m/default-schemas)
   (mu/schemas)))
