(ns macaw.clj-test
  (:require
   [clojure.test :refer :all]
   [macaw.clj :as m.clj]
   [macaw.core :as m]))

(deftest node-test
  (is (= {:foo 1
          :instance :an-instance}
         (#'m.clj/node {:foo 1} :an-instance {:with-instance? true})))
  (is (= {:foo 1}
         (#'m.clj/node {:foo 1} :an-instance {:with-instance? false}))))

(defn- ->ast [query]
  (-> query m/parsed-query (m.clj/->ast {:with-instance? false})))

(deftest basic-select-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/wildcard}],
          :from
          {:type :macaw.clj/select,
           :select
           [{:type :macaw.clj/column, :column "a"}
            {:type :macaw.clj/column, :column "b"}],
           :from {:type :macaw.clj/table, :table "products"}}}
         (->ast "select * from (select a, b from products)"))))

(deftest basic-join-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/table-wildcard,
            :table "products"}
           {:type :macaw.clj/column,
            :table "orders",
            :column "id"}],
          :from {:type :macaw.clj/table, :table "products"},
          :join
          [{:type :macaw.clj/join,
            :source {:type :macaw.clj/table, :table "orders"},
            :condition
            [{:type :macaw.clj/binary-expression,
              :operator "="
              :left
              {:type :macaw.clj/column,
               :table "products",
               :column "id"},
              :right
              {:type :macaw.clj/column,
               :table "orders",
               :column "product_id"}}]}]}
         (->ast "select products.*, orders.id from products inner join orders on products.id = orders.product_id"))))

(deftest basic-alias-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/column, :table "p", :column "a", :alias "b"}
           {:type :macaw.clj/column, :table "p", :column "c", :alias "d"}],
          :from {:alias "p", :type :macaw.clj/table, :table "products"}}
         (->ast "select p.a as b, p.c as d from products p"))))

(deftest basic-where-test
  (is (= {:type :macaw.clj/select,
          :select [{:type :macaw.clj/wildcard}],
          :from {:type :macaw.clj/table, :table "products"},
          :where
          {:type :macaw.clj/binary-expression,
           :operator "="
           :left {:type :macaw.clj/column, :column "category"},
           :right {:type :macaw.clj/literal, :value "hello"}}}
         (->ast "select * from products where category = 'hello'"))))

(deftest basic-aggregation-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/function,
            :name "count",
            :params [{:type :macaw.clj/wildcard}]}],
          :from {:type :macaw.clj/table, :table "products"}}
         (->ast "select count(*) from products"))))

(deftest extra-names-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:database "db",
            :type :macaw.clj/column,
            :schema "schema",
            :table "table",
            :column "col"}],
          :from
          {:database "db",
           :type :macaw.clj/table,
           :schema "schema",
           :table "table"}}
         (->ast "select db.schema.table.col from db.schema.table"))))

(deftest basic-grouping-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/function,
            :name "sum",
            :params [{:type :macaw.clj/column, :column "total"}]}
           {:type :macaw.clj/column, :column "category"}],
          :from {:type :macaw.clj/table, :table "orders"},
          :group-by [{:type :macaw.clj/column, :column "category"}]}
         (->ast "select sum(total), category from orders group by category"))))

(deftest basic-arg-test
  (is (= {:type :macaw.clj/select,
          :select [{:type :macaw.clj/wildcard}],
          :from {:type :macaw.clj/table, :table "products"},
          :where
          {:type :macaw.clj/binary-expression,
           :operator "=",
           :left {:type :macaw.clj/column, :column "category"},
           :right {:type :macaw.clj/jdbc-parameter}}}
         (->ast "select * from products where category = ?"))))

(deftest basic-case-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/case,
            :else {:type :macaw.clj/column, :column "total"},
            :when-clauses
            [{:when {:type :macaw.clj/binary-expression,
                     :operator "<",
                     :left {:type :macaw.clj/column, :column "total"},
                     :right {:type :macaw.clj/literal, :value 0}}
              :then {:type :macaw.clj/signed-expression,
                     :expression {:type :macaw.clj/column, :column "total"},
                     :sign "-"}}]}],
          :from {:type :macaw.clj/table, :table "orders"}}
         (->ast "select case when total < 0 then -total else total end from orders"))))

(deftest switch-case-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/case,
            :switch {:type :macaw.clj/column, :column "category"},
            :else {:type :macaw.clj/literal, :value "is not gizmo"},
            :when-clauses
            [{:when {:type :macaw.clj/literal, :value "Gizmo"},
              :then {:type :macaw.clj/literal, :value "is gizmo"}}]}],
          :from {:type :macaw.clj/table, :table "products"}}
         (->ast "select case category when 'Gizmo' then 'is gizmo' else 'is not gizmo' end from products"))))

(deftest week-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/expression-list,
            :alias "\"created_at\"",
            :expressions
            [{:type :macaw.clj/binary-expression,
              :operator "+",
              :left
              {:type :macaw.clj/function,
               :name "DATE_TRUNC",
               :params
               [{:type :macaw.clj/literal, :value "week"}
                {:type :macaw.clj/expression-list,
                 :expressions
                 [{:type :macaw.clj/binary-expression,
                   :operator "+",
                   :left
                   {:type :macaw.clj/column,
                    :schema "\"public\"",
                    :table "\"orders\"",
                    :column "\"created_at\""},
                   :right {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
              :right {:type :macaw.clj/interval, :value "'-1 day'"}}]}
           {:type :macaw.clj/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.clj/wildcard}]}],
          :from
          {:type :macaw.clj/table, :schema "\"public\"", :table "\"orders\""},
          :group-by
          [{:type :macaw.clj/binary-expression,
            :operator "+",
            :left
            {:type :macaw.clj/function,
             :name "DATE_TRUNC",
             :params
             [{:type :macaw.clj/literal, :value "week"}
              {:type :macaw.clj/expression-list,
               :expressions
               [{:type :macaw.clj/binary-expression,
                 :operator "+",
                 :left
                 {:type :macaw.clj/column,
                  :schema "\"public\"",
                  :table "\"orders\"",
                  :column "\"created_at\""},
                 :right {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
            :right {:type :macaw.clj/interval, :value "'-1 day'"}}],
          :order-by
          [{:type :macaw.clj/expression-list,
            :expressions
            [{:type :macaw.clj/binary-expression,
              :operator "+",
              :left
              {:type :macaw.clj/function,
               :name "DATE_TRUNC",
               :params
               [{:type :macaw.clj/literal, :value "week"}
                {:type :macaw.clj/expression-list,
                 :expressions
                 [{:type :macaw.clj/binary-expression,
                   :operator "+",
                   :left
                   {:type :macaw.clj/column,
                    :schema "\"public\"",
                    :table "\"orders\"",
                    :column "\"created_at\""},
                   :right {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
              :right {:type :macaw.clj/interval, :value "'-1 day'"}}]}]}
         (->ast "SELECT
  (
    DATE_TRUNC(
      'week',
      (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
    ) + INTERVAL '-1 day'
  ) AS \"created_at\",
  COUNT(*) AS \"count\"
FROM
  \"public\".\"orders\"
GROUP BY
  (
    DATE_TRUNC(
      'week',
      (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
    ) + INTERVAL '-1 day'
  )
ORDER BY
  (
    DATE_TRUNC(
      'week',
      (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
    ) + INTERVAL '-1 day'
  ) ASC"))))

(deftest week-of-year-test
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/function,
            :alias "\"created_at\"",
            :name "CEIL",
            :params
            [{:type :macaw.clj/expression-list,
              :expressions
              [{:type :macaw.clj/binary-expression,
                :operator "/",
                :left
                {:type :macaw.clj/cast,
                 :expression
                 {:type :macaw.clj/extract,
                  :expression
                  {:type :macaw.clj/expression-list,
                   :expressions
                   [{:type :macaw.clj/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.clj/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.clj/literal, :value "week"}
                       {:type :macaw.clj/expression-list,
                        :expressions
                        [{:type :macaw.clj/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.clj/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.clj/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.clj/literal, :value 7.0}}]}]}
           {:type :macaw.clj/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.clj/wildcard}]}],
          :from
          {:type :macaw.clj/table, :schema "\"public\"", :table "\"orders\""},
          :group-by
          [{:type :macaw.clj/function,
            :name "CEIL",
            :params
            [{:type :macaw.clj/expression-list,
              :expressions
              [{:type :macaw.clj/binary-expression,
                :operator "/",
                :left
                {:type :macaw.clj/cast,
                 :expression
                 {:type :macaw.clj/extract,
                  :expression
                  {:type :macaw.clj/expression-list,
                   :expressions
                   [{:type :macaw.clj/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.clj/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.clj/literal, :value "week"}
                       {:type :macaw.clj/expression-list,
                        :expressions
                        [{:type :macaw.clj/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.clj/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.clj/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.clj/literal, :value 7.0}}]}]}],
          :order-by
          [{:type :macaw.clj/function,
            :name "CEIL",
            :params
            [{:type :macaw.clj/expression-list,
              :expressions
              [{:type :macaw.clj/binary-expression,
                :operator "/",
                :left
                {:type :macaw.clj/cast,
                 :expression
                 {:type :macaw.clj/extract,
                  :expression
                  {:type :macaw.clj/expression-list,
                   :expressions
                   [{:type :macaw.clj/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.clj/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.clj/literal, :value "week"}
                       {:type :macaw.clj/expression-list,
                        :expressions
                        [{:type :macaw.clj/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.clj/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.clj/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.clj/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.clj/literal, :value 7.0}}]}]}]}
         (->ast "SELECT
  CEIL(
    (
      CAST(
        extract(
          doy
          from
            (
              DATE_TRUNC(
                'week',
                (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
              ) + INTERVAL '-1 day'
            )
        ) AS integer
      ) / 7.0
    )
  ) AS \"created_at\",
  COUNT(*) AS \"count\"
FROM
  \"public\".\"orders\"
GROUP BY
  CEIL(
    (
      CAST(
        extract(
          doy
          from
            (
              DATE_TRUNC(
                'week',
                (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
              ) + INTERVAL '-1 day'
            )
        ) AS integer
      ) / 7.0
    )
  )
ORDER BY
  CEIL(
    (
      CAST(
        extract(
          doy
          from
            (
              DATE_TRUNC(
                'week',
                (\"public\".\"orders\".\"created_at\" + INTERVAL '1 day')
              ) + INTERVAL '-1 day'
            )
        ) AS integer
      ) / 7.0
    )
  ) ASC"))))

(deftest complicated-test-1
  (is (= {:type :macaw.clj/select,
          :select
          [{:type :macaw.clj/function,
            :alias "\"created_at\"",
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.clj/literal, :value "month"}
             {:type :macaw.clj/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.clj/column,
            :alias "\"upper_category\"",
            :table "\"source\"",
            :column "\"upper_category\""}
           {:type :macaw.clj/function,
            :alias "\"sum\"",
            :name "SUM",
            :params
            [{:type :macaw.clj/column,
              :table "\"source\"",
              :column "\"total\""}]}
           {:type :macaw.clj/function,
            :alias "\"sum_2\"",
            :name "SUM",
            :params
            [{:type :macaw.clj/column,
              :table "\"source\"",
              :column "\"discount\""}]}
           {:type :macaw.clj/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.clj/wildcard}]}],
          :from
          {:type :macaw.clj/select,
           :alias "\"source\"",
           :select
           [{:type :macaw.clj/column,
             :alias "\"product_id\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"product_id\""}
            {:type :macaw.clj/column,
             :alias "\"total\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"total\""}
            {:type :macaw.clj/column,
             :alias "\"discount\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"discount\""}
            {:type :macaw.clj/column,
             :alias "\"created_at\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"created_at\""}
            {:type :macaw.clj/function,
             :alias "\"upper_category\"",
             :name "UPPER",
             :params
             [{:type :macaw.clj/column,
               :table "\"Products\"",
               :column "\"category\""}]}
            {:type :macaw.clj/column,
             :alias "\"Products__id\"",
             :table "\"Products\"",
             :column "\"id\""}
            {:type :macaw.clj/column,
             :alias "\"Products__category\"",
             :table "\"Products\"",
             :column "\"category\""}],
           :from
           {:type :macaw.clj/table,
            :schema "\"public\"",
            :table "\"orders\""},
           :where
           {:type :macaw.clj/binary-expression,
            :operator ">"
            :left
            {:type :macaw.clj/column,
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"discount\""},
            :right {:type :macaw.clj/literal, :value 0}},
           :join
           [{:type :macaw.clj/join,
             :source
             {:type :macaw.clj/select,
              :alias "\"Products\"",
              :select
              [{:type :macaw.clj/column,
                :alias "\"id\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"id\""}
               {:type :macaw.clj/column,
                :alias "\"ean\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"ean\""}
               {:type :macaw.clj/column,
                :alias "\"title\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"title\""}
               {:type :macaw.clj/column,
                :alias "\"category\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"category\""}
               {:type :macaw.clj/column,
                :alias "\"vendor\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"vendor\""}
               {:type :macaw.clj/column,
                :alias "\"price\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"price\""}
               {:type :macaw.clj/column,
                :alias "\"rating\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"rating\""}
               {:type :macaw.clj/column,
                :alias "\"created_at\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"created_at\""}],
              :from
              {:type :macaw.clj/table,
               :schema "\"public\"",
               :table "\"products\""}},
             :condition
             [{:type :macaw.clj/binary-expression,
               :operator "="
               :left
               {:type :macaw.clj/column,
                :schema "\"public\"",
                :table "\"orders\"",
                :column "\"product_id\""},
               :right
               {:type :macaw.clj/column,
                :table "\"Products\"",
                :column "\"id\""}}]}]},
          :group-by
          [{:type :macaw.clj/function,
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.clj/literal, :value "month"}
             {:type :macaw.clj/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.clj/column,
            :table "\"source\"",
            :column "\"upper_category\""}]
          :order-by
          [{:type :macaw.clj/function,
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.clj/literal, :value "month"}
             {:type :macaw.clj/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.clj/column,
            :table "\"source\"",
            :column "\"upper_category\""}]}
         (->ast "SELECT
  DATE_TRUNC('month', \"source\".\"created_at\") AS \"created_at\",
  \"source\".\"upper_category\" AS \"upper_category\",
  SUM(\"source\".\"total\") AS \"sum\",
  SUM(\"source\".\"discount\") AS \"sum_2\",
  COUNT(*) AS \"count\"
FROM
  (
    SELECT
      \"public\".\"orders\".\"product_id\" AS \"product_id\",
      \"public\".\"orders\".\"total\" AS \"total\",
      \"public\".\"orders\".\"discount\" AS \"discount\",
      \"public\".\"orders\".\"created_at\" AS \"created_at\",
      UPPER(\"Products\".\"category\") AS \"upper_category\",
      \"Products\".\"id\" AS \"Products__id\",
      \"Products\".\"category\" AS \"Products__category\"
    FROM
      \"public\".\"orders\"

LEFT JOIN (
        SELECT
          \"public\".\"products\".\"id\" AS \"id\",
          \"public\".\"products\".\"ean\" AS \"ean\",
          \"public\".\"products\".\"title\" AS \"title\",
          \"public\".\"products\".\"category\" AS \"category\",
          \"public\".\"products\".\"vendor\" AS \"vendor\",
          \"public\".\"products\".\"price\" AS \"price\",
          \"public\".\"products\".\"rating\" AS \"rating\",
          \"public\".\"products\".\"created_at\" AS \"created_at\"
        FROM
          \"public\".\"products\"
      ) AS \"Products\" ON \"public\".\"orders\".\"product_id\" = \"Products\".\"id\"

WHERE
      \"public\".\"orders\".\"discount\" > 0
  ) AS \"source\"
GROUP BY
  DATE_TRUNC('month', \"source\".\"created_at\"),
  \"source\".\"upper_category\"
ORDER BY
  DATE_TRUNC('month', \"source\".\"created_at\") ASC,
  \"source\".\"upper_category\" ASC"))))
