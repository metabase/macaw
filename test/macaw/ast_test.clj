(ns macaw.ast-test
  (:require
   [clojure.test :refer :all]
   [macaw.ast :as m.ast]
   [macaw.core :as m]))

(deftest node-test
  (is (= {:foo 1
          :instance :an-instance}
         (#'m.ast/node {:foo 1} :an-instance {:with-instance? true})))
  (is (= {:foo 1}
         (#'m.ast/node {:foo 1} :an-instance {:with-instance? false}))))

(defn- ->ast [query]
  (-> query m/parsed-query (m.ast/->ast {:with-instance? false})))

(deftest basic-select-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/wildcard}],
          :from
          {:type :macaw.ast/select,
           :select
           [{:type :macaw.ast/column, :column "a"}
            {:type :macaw.ast/column, :column "b"}],
           :from {:type :macaw.ast/table, :table "products"}}}
         (->ast "select * from (select a, b from products)"))))

(deftest basic-join-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/table-wildcard,
            :table "products"}
           {:type :macaw.ast/column,
            :table "orders",
            :column "id"}],
          :from {:type :macaw.ast/table, :table "products"},
          :join
          [{:type :macaw.ast/join,
            :source {:type :macaw.ast/table, :table "orders"},
            :condition
            [{:type :macaw.ast/binary-expression,
              :operator "="
              :left
              {:type :macaw.ast/column,
               :table "products",
               :column "id"},
              :right
              {:type :macaw.ast/column,
               :table "orders",
               :column "product_id"}}]}]}
         (->ast "select products.*, orders.id from products inner join orders on products.id = orders.product_id"))))

(deftest basic-alias-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :table "p", :column "a", :alias "b"}
           {:type :macaw.ast/column, :table "p", :column "c", :alias "d"}],
          :from {:alias "p", :type :macaw.ast/table, :table "products"}}
         (->ast "select p.a as b, p.c as d from products p"))))

(deftest basic-where-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/binary-expression,
           :operator "="
           :left {:type :macaw.ast/column, :column "category"},
           :right {:type :macaw.ast/literal, :value "hello"}}}
         (->ast "select * from products where category = 'hello'"))))

(deftest basic-aggregation-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :name "count",
            :params [{:type :macaw.ast/wildcard}]}],
          :from {:type :macaw.ast/table, :table "products"}}
         (->ast "select count(*) from products"))))

(deftest extra-names-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:database "db",
            :type :macaw.ast/column,
            :schema "schema",
            :table "table",
            :column "col"}],
          :from
          {:database "db",
           :type :macaw.ast/table,
           :schema "schema",
           :table "table"}}
         (->ast "select db.schema.table.col from db.schema.table"))))

(deftest basic-grouping-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :name "sum",
            :params [{:type :macaw.ast/column, :column "total"}]}
           {:type :macaw.ast/column, :column "category"}],
          :from {:type :macaw.ast/table, :table "orders"},
          :group-by [{:type :macaw.ast/column, :column "category"}]}
         (->ast "select sum(total), category from orders group by category"))))

(deftest basic-arg-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/binary-expression,
           :operator "=",
           :left {:type :macaw.ast/column, :column "category"},
           :right {:type :macaw.ast/jdbc-parameter}}}
         (->ast "select * from products where category = ?"))))

(deftest basic-case-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/case,
            :else {:type :macaw.ast/column, :column "total"},
            :when-clauses
            [{:when {:type :macaw.ast/binary-expression,
                     :operator "<",
                     :left {:type :macaw.ast/column, :column "total"},
                     :right {:type :macaw.ast/literal, :value 0}}
              :then {:type :macaw.ast/signed-expression,
                     :expression {:type :macaw.ast/column, :column "total"},
                     :sign "-"}}]}],
          :from {:type :macaw.ast/table, :table "orders"}}
         (->ast "select case when total < 0 then -total else total end from orders"))))

(deftest switch-case-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/case,
            :switch {:type :macaw.ast/column, :column "category"},
            :else {:type :macaw.ast/literal, :value "is not gizmo"},
            :when-clauses
            [{:when {:type :macaw.ast/literal, :value "Gizmo"},
              :then {:type :macaw.ast/literal, :value "is gizmo"}}]}],
          :from {:type :macaw.ast/table, :table "products"}}
         (->ast "select case category when 'Gizmo' then 'is gizmo' else 'is not gizmo' end from products"))))

(deftest basic-exists-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :table "u", :column "name"}
           {:type :macaw.ast/column, :table "u", :column "email"}],
          :from {:type :macaw.ast/table, :alias "u", :table "users"},
          :where
          {:type :macaw.ast/exists-expression,
           :expression
           {:type :macaw.ast/select,
            :select [{:type :macaw.ast/literal, :value 1}],
            :from {:type :macaw.ast/table, :alias "o", :table "orders"},
            :where
            {:type :macaw.ast/binary-expression,
             :operator "=",
             :left {:type :macaw.ast/column, :table "o", :column "user_id"},
             :right {:type :macaw.ast/column, :table "u", :column "id"}}},}}
         (->ast "SELECT u.name, u.email
FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)"))))

(deftest basic-not-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/not,
           :expression
           {:type :macaw.ast/expression-list,
            :expressions
            [{:type :macaw.ast/binary-expression,
              :operator "=",
              :left {:type :macaw.ast/column, :column "category"},
              :right {:value "Gizmo", :type :macaw.ast/literal}}]}}}
         (->ast "select * from products where not (category = 'Gizmo')"))))

(deftest basic-is-null-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/is-null-expression,
           :expression {:type :macaw.ast/column, :column "category"},
           :not false}}
         (->ast "select * from products where category is null"))))

(deftest negated-is-null-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/is-null-expression,
           :expression {:type :macaw.ast/column, :column "category"},
           :not true}}
         (->ast "select * from products where category is not null"))))

(deftest basic-cte-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "active_users"},
          :with
          [{:type :macaw.ast/select,
            :alias "active_users",
            :select
            [{:type :macaw.ast/column, :column "id"}
             {:type :macaw.ast/column, :column "name"}],
            :from {:type :macaw.ast/table, :table "users"},
            :where
            {:type :macaw.ast/binary-expression,
             :operator "=",
             :left {:type :macaw.ast/column, :column "active"},
             :right {:type :macaw.ast/column, :column "true"}}}]}
         (->ast "WITH active_users AS (SELECT id, name FROM users WHERE active = true)
SELECT * FROM active_users"))))

(deftest recursive-cte-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :column "name"}
           {:type :macaw.ast/column, :column "level"}],
          :from {:type :macaw.ast/table, :table "emp_hierarchy"},
          :with
          [{:type :macaw.ast/set-operation,
            :alias "emp_hierarchy",
            :selects
            [{:type :macaw.ast/select,
              :select
              [{:type :macaw.ast/column, :column "id"}
               {:type :macaw.ast/column, :column "name"}
               {:type :macaw.ast/column, :column "manager_id"}
               {:type :macaw.ast/literal, :alias "level", :value 0}],
              :from {:type :macaw.ast/table, :table "employees"},
              :where
              {:type :macaw.ast/is-null-expression,
               :expression {:type :macaw.ast/column, :column "manager_id"},
               :not false}}
             {:type :macaw.ast/select,
              :select
              [{:type :macaw.ast/column, :table "e", :column "id"}
               {:type :macaw.ast/column, :table "e", :column "name"}
               {:type :macaw.ast/column, :table "e", :column "manager_id"}
               {:type :macaw.ast/binary-expression,
                :operator "+",
                :left {:type :macaw.ast/column, :table "h", :column "level"},
                :right {:value 1, :type :macaw.ast/literal}}],
              :from {:type :macaw.ast/table, :alias "e", :table "employees"},
              :join
              [{:type :macaw.ast/join,
                :source
                {:type :macaw.ast/table, :alias "h", :table "emp_hierarchy"},
                :condition
                [{:type :macaw.ast/binary-expression,
                  :operator "=",
                  :left
                  {:type :macaw.ast/column, :table "e", :column "manager_id"},
                  :right
                  {:type :macaw.ast/column, :table "h", :column "id"}}]}]}],
            :operations ["UNION ALL"]}]}
         (->ast "WITH RECURSIVE emp_hierarchy AS (
  SELECT id, name, manager_id, 0 AS level
  FROM employees
  WHERE manager_id IS NULL
  UNION ALL
  SELECT e.id, e.name, e.manager_id, h.level + 1
  FROM employees e
  JOIN emp_hierarchy h ON e.manager_id = h.id
)
SELECT name, level FROM emp_hierarchy"))))

(deftest basic-union-test
  (is (= {:type :macaw.ast/set-operation,
          :selects
          [{:type :macaw.ast/select,
            :select
            [{:type :macaw.ast/column, :column "id"}
             {:type :macaw.ast/column, :column "name"}],
            :from {:type :macaw.ast/table, :table "users"}}
           {:type :macaw.ast/select,
            :select
            [{:type :macaw.ast/column, :column "id"}
             {:type :macaw.ast/column, :column "name"}],
            :from {:type :macaw.ast/table, :table "archived_users"}}],
          :operations ["UNION"]}
         (->ast "SELECT id, name FROM users
UNION
SELECT id, name FROM archived_users"))))

(deftest row-number-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :column "name"}
           {:type :macaw.ast/column, :column "salary"}
           {:type :macaw.ast/analytic-expression,
            :alias "rank",
            :name "ROW_NUMBER",
            :order-by [{:type :macaw.ast/column, :column "salary"}]}],
          :from {:type :macaw.ast/table, :table "employees"}}
         (->ast "SELECT name, salary, ROW_NUMBER() OVER (ORDER BY salary DESC) AS rank
FROM employees"))))

(deftest partition-by-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :column "department"}
           {:type :macaw.ast/column, :column "name"}
           {:type :macaw.ast/column, :column "salary"}
           {:type :macaw.ast/analytic-expression,
            :alias "dept_rank",
            :name "RANK",
            :partition-by [{:type :macaw.ast/column, :column "department"}],
            :order-by [{:type :macaw.ast/column, :column "salary"}]}],
          :from {:type :macaw.ast/table, :table "employees"}}
         (->ast "SELECT department, name, salary,
  RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS dept_rank
FROM employees"))))

(deftest week-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/expression-list,
            :alias "\"created_at\"",
            :expressions
            [{:type :macaw.ast/binary-expression,
              :operator "+",
              :left
              {:type :macaw.ast/function,
               :name "DATE_TRUNC",
               :params
               [{:type :macaw.ast/literal, :value "week"}
                {:type :macaw.ast/expression-list,
                 :expressions
                 [{:type :macaw.ast/binary-expression,
                   :operator "+",
                   :left
                   {:type :macaw.ast/column,
                    :schema "\"public\"",
                    :table "\"orders\"",
                    :column "\"created_at\""},
                   :right {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
              :right {:type :macaw.ast/interval, :value "'-1 day'"}}]}
           {:type :macaw.ast/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.ast/wildcard}]}],
          :from
          {:type :macaw.ast/table, :schema "\"public\"", :table "\"orders\""},
          :group-by
          [{:type :macaw.ast/binary-expression,
            :operator "+",
            :left
            {:type :macaw.ast/function,
             :name "DATE_TRUNC",
             :params
             [{:type :macaw.ast/literal, :value "week"}
              {:type :macaw.ast/expression-list,
               :expressions
               [{:type :macaw.ast/binary-expression,
                 :operator "+",
                 :left
                 {:type :macaw.ast/column,
                  :schema "\"public\"",
                  :table "\"orders\"",
                  :column "\"created_at\""},
                 :right {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
            :right {:type :macaw.ast/interval, :value "'-1 day'"}}],
          :order-by
          [{:type :macaw.ast/expression-list,
            :expressions
            [{:type :macaw.ast/binary-expression,
              :operator "+",
              :left
              {:type :macaw.ast/function,
               :name "DATE_TRUNC",
               :params
               [{:type :macaw.ast/literal, :value "week"}
                {:type :macaw.ast/expression-list,
                 :expressions
                 [{:type :macaw.ast/binary-expression,
                   :operator "+",
                   :left
                   {:type :macaw.ast/column,
                    :schema "\"public\"",
                    :table "\"orders\"",
                    :column "\"created_at\""},
                   :right {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
              :right {:type :macaw.ast/interval, :value "'-1 day'"}}]}]}
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
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :alias "\"created_at\"",
            :name "CEIL",
            :params
            [{:type :macaw.ast/expression-list,
              :expressions
              [{:type :macaw.ast/binary-expression,
                :operator "/",
                :left
                {:type :macaw.ast/cast,
                 :expression
                 {:type :macaw.ast/extract,
                  :expression
                  {:type :macaw.ast/expression-list,
                   :expressions
                   [{:type :macaw.ast/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.ast/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.ast/literal, :value "week"}
                       {:type :macaw.ast/expression-list,
                        :expressions
                        [{:type :macaw.ast/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.ast/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.ast/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.ast/literal, :value 7.0}}]}]}
           {:type :macaw.ast/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.ast/wildcard}]}],
          :from
          {:type :macaw.ast/table, :schema "\"public\"", :table "\"orders\""},
          :group-by
          [{:type :macaw.ast/function,
            :name "CEIL",
            :params
            [{:type :macaw.ast/expression-list,
              :expressions
              [{:type :macaw.ast/binary-expression,
                :operator "/",
                :left
                {:type :macaw.ast/cast,
                 :expression
                 {:type :macaw.ast/extract,
                  :expression
                  {:type :macaw.ast/expression-list,
                   :expressions
                   [{:type :macaw.ast/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.ast/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.ast/literal, :value "week"}
                       {:type :macaw.ast/expression-list,
                        :expressions
                        [{:type :macaw.ast/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.ast/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.ast/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.ast/literal, :value 7.0}}]}]}],
          :order-by
          [{:type :macaw.ast/function,
            :name "CEIL",
            :params
            [{:type :macaw.ast/expression-list,
              :expressions
              [{:type :macaw.ast/binary-expression,
                :operator "/",
                :left
                {:type :macaw.ast/cast,
                 :expression
                 {:type :macaw.ast/extract,
                  :expression
                  {:type :macaw.ast/expression-list,
                   :expressions
                   [{:type :macaw.ast/binary-expression,
                     :operator "+",
                     :left
                     {:type :macaw.ast/function,
                      :name "DATE_TRUNC",
                      :params
                      [{:type :macaw.ast/literal, :value "week"}
                       {:type :macaw.ast/expression-list,
                        :expressions
                        [{:type :macaw.ast/binary-expression,
                          :operator "+",
                          :left
                          {:type :macaw.ast/column,
                           :schema "\"public\"",
                           :table "\"orders\"",
                           :column "\"created_at\""},
                          :right
                          {:type :macaw.ast/interval, :value "'1 day'"}}]}]},
                     :right {:type :macaw.ast/interval, :value "'-1 day'"}}]},
                  :part "doy"},
                 :datatype "integer"},
                :right {:type :macaw.ast/literal, :value 7.0}}]}]}]}
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
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :alias "\"created_at\"",
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.ast/literal, :value "month"}
             {:type :macaw.ast/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.ast/column,
            :alias "\"upper_category\"",
            :table "\"source\"",
            :column "\"upper_category\""}
           {:type :macaw.ast/function,
            :alias "\"sum\"",
            :name "SUM",
            :params
            [{:type :macaw.ast/column,
              :table "\"source\"",
              :column "\"total\""}]}
           {:type :macaw.ast/function,
            :alias "\"sum_2\"",
            :name "SUM",
            :params
            [{:type :macaw.ast/column,
              :table "\"source\"",
              :column "\"discount\""}]}
           {:type :macaw.ast/function,
            :alias "\"count\"",
            :name "COUNT",
            :params [{:type :macaw.ast/wildcard}]}],
          :from
          {:type :macaw.ast/select,
           :alias "\"source\"",
           :select
           [{:type :macaw.ast/column,
             :alias "\"product_id\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"product_id\""}
            {:type :macaw.ast/column,
             :alias "\"total\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"total\""}
            {:type :macaw.ast/column,
             :alias "\"discount\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"discount\""}
            {:type :macaw.ast/column,
             :alias "\"created_at\"",
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"created_at\""}
            {:type :macaw.ast/function,
             :alias "\"upper_category\"",
             :name "UPPER",
             :params
             [{:type :macaw.ast/column,
               :table "\"Products\"",
               :column "\"category\""}]}
            {:type :macaw.ast/column,
             :alias "\"Products__id\"",
             :table "\"Products\"",
             :column "\"id\""}
            {:type :macaw.ast/column,
             :alias "\"Products__category\"",
             :table "\"Products\"",
             :column "\"category\""}],
           :from
           {:type :macaw.ast/table,
            :schema "\"public\"",
            :table "\"orders\""},
           :where
           {:type :macaw.ast/binary-expression,
            :operator ">"
            :left
            {:type :macaw.ast/column,
             :schema "\"public\"",
             :table "\"orders\"",
             :column "\"discount\""},
            :right {:type :macaw.ast/literal, :value 0}},
           :join
           [{:type :macaw.ast/join,
             :source
             {:type :macaw.ast/select,
              :alias "\"Products\"",
              :select
              [{:type :macaw.ast/column,
                :alias "\"id\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"id\""}
               {:type :macaw.ast/column,
                :alias "\"ean\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"ean\""}
               {:type :macaw.ast/column,
                :alias "\"title\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"title\""}
               {:type :macaw.ast/column,
                :alias "\"category\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"category\""}
               {:type :macaw.ast/column,
                :alias "\"vendor\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"vendor\""}
               {:type :macaw.ast/column,
                :alias "\"price\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"price\""}
               {:type :macaw.ast/column,
                :alias "\"rating\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"rating\""}
               {:type :macaw.ast/column,
                :alias "\"created_at\"",
                :schema "\"public\"",
                :table "\"products\"",
                :column "\"created_at\""}],
              :from
              {:type :macaw.ast/table,
               :schema "\"public\"",
               :table "\"products\""}},
             :condition
             [{:type :macaw.ast/binary-expression,
               :operator "="
               :left
               {:type :macaw.ast/column,
                :schema "\"public\"",
                :table "\"orders\"",
                :column "\"product_id\""},
               :right
               {:type :macaw.ast/column,
                :table "\"Products\"",
                :column "\"id\""}}]}]},
          :group-by
          [{:type :macaw.ast/function,
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.ast/literal, :value "month"}
             {:type :macaw.ast/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.ast/column,
            :table "\"source\"",
            :column "\"upper_category\""}]
          :order-by
          [{:type :macaw.ast/function,
            :name "DATE_TRUNC",
            :params
            [{:type :macaw.ast/literal, :value "month"}
             {:type :macaw.ast/column,
              :table "\"source\"",
              :column "\"created_at\""}]}
           {:type :macaw.ast/column,
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
