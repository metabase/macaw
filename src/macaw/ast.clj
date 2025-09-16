(ns macaw.ast
  (:require
   [macaw.util :as u])
  (:import
   (net.sf.jsqlparser.statement.select AllColumns AllTableColumns Join OrderByElement
                                       ParenthesedSelect PlainSelect SelectItem)
   (net.sf.jsqlparser.schema Column Database Table)
   (net.sf.jsqlparser.expression Alias BinaryExpression CaseExpression CastExpression DateValue
                                 DoubleValue ExtractExpression Function IntervalExpression
                                 JdbcParameter LongValue NullValue SignedExpression StringValue
                                 TimeValue TimestampValue)
   (net.sf.jsqlparser.expression.operators.relational ExpressionList)))

(defn- node [data instance {:keys [with-instance?] :as _opts}]
  (-> (merge data
             (if with-instance?
               {:instance instance}
               {}))
      (u/strip-nils true)))


(defmulti ->ast (fn [parsed _opts]
                       (type parsed)))


(defmethod ->ast :default
  [parsed _opts]
  #_(throw (ex-info "Unsupported value passed to ->ast"
                    {:value parsed}))
  parsed)


(defmethod ->ast PlainSelect
  [parsed opts]
  (node
   {:type ::select
    :select (mapv #(->ast % opts) (.getSelectItems parsed))
    :from (->ast (.getFromItem parsed) opts)
    :where (->ast (.getWhere parsed) opts)
    :join (mapv #(->ast % opts) (.getJoins parsed))
    :group-by (some->> (.getGroupBy parsed)
                       .getGroupByExpressionList
                       (mapv #(->ast % opts)))
    :order-by (mapv #(->ast % opts) (.getOrderByElements parsed))}
   parsed opts))

(defmethod ->ast SelectItem
  [parsed opts]
  (node
   (merge
    (->ast (.getAlias parsed) opts)
    (->ast (.getExpression parsed) opts))
   parsed opts))

(defmethod ->ast AllColumns
  [parsed opts]
  (node
   {:type ::wildcard}
   parsed opts))

(defmethod ->ast AllTableColumns
  [parsed opts]
  (node
   (merge
    (->ast (.getTable parsed) opts)
    {:type ::table-wildcard})
   parsed opts))

(defmethod ->ast ParenthesedSelect
  [parsed opts]
  (node
   (merge
    (->ast (.getAlias parsed) opts)
    (->ast (.getPlainSelect parsed) opts))
   parsed opts))

(defmethod ->ast Column
  [parsed opts]
  (node
   (merge
    (->ast (.getTable parsed) opts)
    {:type ::column
     :column (.getColumnName parsed)})
   parsed opts))

(defmethod ->ast Table
  [parsed opts]
  (node
   (merge
    (->ast (.getAlias parsed) opts)
    (->ast (.getDatabase parsed) opts)
    {:type ::table
     :schema (.getSchemaName parsed)
     :table (.getName parsed)})
   parsed opts))

(defmethod ->ast Database
  [parsed opts]
  (node
   {:database (.getDatabaseName parsed)}
   parsed opts))

(defmethod ->ast Join
  [parsed opts]
  (node
   {:type ::join
    :source (->ast (.getRightItem parsed) opts)
    :condition (mapv #(->ast % opts) (.getOnExpressions parsed))}
   parsed opts))

(defmethod ->ast BinaryExpression
  [parsed opts]
  (node
   {:type ::binary-expression
    :operator (.getStringExpression parsed)
    :left (->ast (.getLeftExpression parsed) opts)
    :right (->ast (.getRightExpression parsed) opts)}
   parsed opts))

(defmethod ->ast Alias
  [parsed opts]
  (node
   {:type ::alias
    :alias (.getName parsed)}
   parsed opts))

(doseq [valueClass [DateValue DoubleValue LongValue NullValue
                    StringValue TimeValue TimestampValue]]
  (defmethod ->ast valueClass
    [parsed opts]
    (node
     {:type ::literal
      :value (.getValue parsed)}
     parsed opts)))

(defmethod ->ast Function
  [parsed opts]
  (node
   {:type ::function
    :name (.getName parsed)
    :params (mapv #(->ast % opts) (.getParameters parsed))}
   parsed opts))

(defmethod ->ast ExpressionList
  [parsed opts]
  (node
   {:type ::expression-list
    :expressions (mapv #(->ast % opts) (.getExpressions parsed))}
   parsed opts))

(defmethod ->ast IntervalExpression
  [parsed opts]
  (node
   {:type ::interval
    :value (.getParameter parsed)}
   parsed opts))

(defmethod ->ast OrderByElement
  [parsed opts]
  (node
   (->ast (.getExpression parsed) opts)
   parsed opts))

(defmethod ->ast CastExpression
  [parsed opts]
  (node
   {:type ::cast
    :expression (->ast (.getLeftExpression parsed) opts)
    :datatype (some-> (.getColDataType parsed) str)}
   parsed opts))

(defmethod ->ast ExtractExpression
  [parsed opts]
  (node
   {:type ::extract
    :expression (->ast (.getExpression parsed) opts)
    :part (.getName parsed)}
   parsed opts))

(defmethod ->ast JdbcParameter
  [parsed opts]
  (node
   {:type ::jdbc-parameter}
   parsed opts))

(defmethod ->ast CaseExpression
  [parsed opts]
  (node
   {:type ::case
    :switch (->ast (.getSwitchExpression parsed) opts)
    :else (->ast (.getElseExpression parsed) opts)
    :when-clauses (map (fn [when-clause]
                         {:when (->ast (.getWhenExpression when-clause) opts)
                          :then (->ast (.getThenExpression when-clause) opts)})
                       (.getWhenClauses parsed))}
   parsed opts))

(defmethod ->ast SignedExpression
  [parsed opts]
  (node
   {:type ::signed-expression
    :expression (->ast (.getExpression parsed) opts)
    :sign (str (.getSign parsed))}
   parsed opts))
