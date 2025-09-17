(ns macaw.ast
  (:require
   [macaw.util :as u])
  (:import
   (net.sf.jsqlparser.statement.select AllColumns AllTableColumns Join OrderByElement
                                       ParenthesedSelect PlainSelect SelectItem SetOperationList
                                       WithItem)
   (net.sf.jsqlparser.schema Column Database Table)
   (net.sf.jsqlparser.expression Alias AnalyticExpression BinaryExpression CaseExpression
                                 CastExpression DateValue DoubleValue ExtractExpression Function
                                 IntervalExpression JdbcParameter LongValue NotExpression NullValue
                                 SignedExpression StringValue TimeValue TimestampValue WhenClause)
   (net.sf.jsqlparser.expression.operators.relational ExistsExpression ExpressionList
                                                      IsNullExpression)))

(set! *warn-on-reflection* true)

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
  [^PlainSelect parsed opts]
  (node
   {:type ::select
    :select (mapv #(->ast % opts) (.getSelectItems parsed))
    :from (->ast (.getFromItem parsed) opts)
    :where (->ast (.getWhere parsed) opts)
    :join (mapv #(->ast % opts) (.getJoins parsed))
    :group-by (some->> (.getGroupBy parsed)
                       .getGroupByExpressionList
                       (mapv #(->ast % opts)))
    :order-by (mapv #(->ast % opts) (.getOrderByElements parsed))
    :with (mapv #(->ast % opts) (.getWithItemsList parsed))}
   parsed opts))

(defmethod ->ast SelectItem
  [^SelectItem parsed opts]
  (node
   (merge
    {:alias (some-> (.getAlias parsed)
                    .getName)}
    (->ast (.getExpression parsed) opts))
   parsed opts))

(defmethod ->ast AllColumns
  [parsed opts]
  (node
   {:type ::wildcard}
   parsed opts))

(defmethod ->ast AllTableColumns
  [^AllTableColumns parsed opts]
  (node
   (merge
    (->ast (.getTable parsed) opts)
    {:type ::table-wildcard})
   parsed opts))

(defmethod ->ast ParenthesedSelect
  [^ParenthesedSelect parsed opts]
  (node
   (merge
    {:table-alias (some-> (.getAlias parsed)
                          .getName)}
    (->ast (try (.getPlainSelect parsed)
                (catch ClassCastException e
                  (.getSetOperationList parsed))) opts))
   parsed opts))

(defmethod ->ast Column
  [^Column parsed opts]
  (node
   (merge
    (->ast (.getTable parsed) opts)
    {:type ::column
     :column (.getColumnName parsed)})
   parsed opts))

(defmethod ->ast Table
  [^Table parsed opts]
  (node
   (merge
    (->ast (.getDatabase parsed) opts)
    {:type ::table
     :table-alias (some-> (.getAlias parsed)
                          .getName)
     :schema (.getSchemaName parsed)
     :table (.getName parsed)})
   parsed opts))

(defmethod ->ast Database
  [^Database parsed opts]
  (node
   {:database (.getDatabaseName parsed)}
   parsed opts))

(defmethod ->ast Join
  [^Join parsed opts]
  (node
   {:type ::join
    :source (->ast (.getRightItem parsed) opts)
    :condition (mapv #(->ast % opts) (.getOnExpressions parsed))}
   parsed opts))

(defmethod ->ast BinaryExpression
  [^BinaryExpression parsed opts]
  (node
   {:type ::binary-expression
    :operator (.getStringExpression parsed)
    :left (->ast (.getLeftExpression parsed) opts)
    :right (->ast (.getRightExpression parsed) opts)}
   parsed opts))

(defmacro value->ast [value-class]
  (let [parsed-sym (gensym "parsed")]
    `(defmethod ->ast ~value-class
       [~(with-meta parsed-sym {:tag value-class}) opts#]
       (node
        {:type ::literal
         :value (.getValue ~parsed-sym)}
        ~parsed-sym opts#))))

(value->ast DateValue)
(value->ast DoubleValue)
(value->ast LongValue)
(value->ast StringValue)
(value->ast TimeValue)
(value->ast TimestampValue)

(defmethod ->ast NullValue
  [parsed opts]
  (node
   {:type ::literal
    :value nil}
   parsed opts))

(defmethod ->ast Function
  [^Function parsed opts]
  (node
   {:type ::function
    :name (.getName parsed)
    :params (mapv #(->ast % opts) (.getParameters parsed))}
   parsed opts))

(defmethod ->ast ExpressionList
  [^ExpressionList parsed opts]
  (node
   {:type ::expression-list
    :expressions (mapv #(->ast % opts) (.getExpressions parsed))}
   parsed opts))

(defmethod ->ast IntervalExpression
  [^IntervalExpression parsed opts]
  (node
   {:type ::interval
    :value (.getParameter parsed)}
   parsed opts))

(defmethod ->ast OrderByElement
  [^OrderByElement parsed opts]
  (node
   (->ast (.getExpression parsed) opts)
   parsed opts))

(defmethod ->ast CastExpression
  [^CastExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :cast
    :expression (->ast (.getLeftExpression parsed) opts)
    :datatype (some-> (.getColDataType parsed) str)}
   parsed opts))

(defmethod ->ast ExtractExpression
  [^ExtractExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :extract
    :expression (->ast (.getExpression parsed) opts)
    :part (.getName parsed)}
   parsed opts))

(defmethod ->ast JdbcParameter
  [parsed opts]
  (node
   {:type ::jdbc-parameter}
   parsed opts))

(defmethod ->ast CaseExpression
  [^CaseExpression parsed opts]
  (node
   {:type ::case
    :switch (->ast (.getSwitchExpression parsed) opts)
    :else (->ast (.getElseExpression parsed) opts)
    :when-clauses (map (fn [^WhenClause when-clause]
                         {:when (->ast (.getWhenExpression when-clause) opts)
                          :then (->ast (.getThenExpression when-clause) opts)})
                       (.getWhenClauses parsed))}
   parsed opts))

(defmethod ->ast SignedExpression
  [^SignedExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :sign
    :expression (->ast (.getExpression parsed) opts)
    :sign (str (.getSign parsed))}
   parsed opts))

(defmethod ->ast ExistsExpression
  [^ExistsExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :exists
    :expression (->ast (.getRightExpression parsed) opts)}
   parsed opts))

(defmethod ->ast IsNullExpression
  [^IsNullExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :is-null
    :expression (->ast (.getLeftExpression parsed) opts)
    :not (.isNot parsed)}
   parsed opts))

(defmethod ->ast WithItem
  [^WithItem parsed opts]
  (node
   (merge
    {:table-alias (some-> (.getAlias parsed)
                          .getName)}
    (->ast (.getSelect parsed) opts))
   parsed opts))

(defmethod ->ast SetOperationList
  [^SetOperationList parsed opts]
  (node
   {:type ::set-operation
    :selects (mapv #(->ast % opts) (.getSelects parsed))
    :operations (mapv str (.getOperations parsed))}
   parsed opts))

(defmethod ->ast AnalyticExpression
  [^AnalyticExpression parsed opts]
  (node
   {:type ::analytic-expression
    :expression (->ast (.getExpression parsed) opts)
    :offset (->ast (.getOffset parsed) opts)
    :window (->ast (.getWindowElement parsed) opts)
    :name (.getName parsed)
    :partition-by (mapv #(->ast % opts) (.getPartitionExpressionList parsed))
    :order-by (mapv #(->ast % opts) (.getOrderByElements parsed))}
   parsed opts))

(defmethod ->ast NotExpression
  [^NotExpression parsed opts]
  (node
   {:type ::unary-expression
    :operation :not
    :expression (->ast (.getExpression parsed) opts)}
   parsed opts))
