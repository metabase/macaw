(ns macaw.ast-test
  (:require
   [clojure.test :refer :all]
   [macaw.ast :as m.ast]
   [macaw.ast-types :as m.ast-types]
   [macaw.core :as m]
   [malli.core :as malli]))

(deftest ^:parallel node-test
  (is (= {:foo 1
          :instance :an-instance}
         (#'m.ast/node {:foo 1} :an-instance {:with-instance? true})))
  (is (= {:foo 1}
         (#'m.ast/node {:foo 1} :an-instance {:with-instance? false}))))

(defn- check-ast [ast]
  (is (nil? (malli/explain m.ast-types/ast ast {:registry m.ast-types/base-registry})))
  ast)

(defn- ->ast [query]
  (-> query m/parsed-query (m.ast/->ast {:with-instance? false}) check-ast))

(deftest ^:parallel garbage-test
  (let [bad-node (->ast "nothing")]
    (is (= (:type bad-node) :macaw.ast/unrecognized-node))
    (is (some? (:instance bad-node)))))

(deftest ^:parallel lone-select-test
  (is (= {:type :macaw.ast/select
          :select [{:type :macaw.ast/literal
                    :value 1}]}
         (->ast "select 1"))))

(deftest ^:parallel basic-select-test
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

(deftest ^:parallel basic-join-test
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

(deftest ^:parallel basic-alias-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :table "p", :column "a", :alias "b"}
           {:type :macaw.ast/column, :table "p", :column "c", :alias "d"}],
          :from {:table-alias "p", :type :macaw.ast/table, :table "products"}}
         (->ast "select p.a as b, p.c as d from products p"))))

(deftest ^:parallel basic-where-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/binary-expression,
           :operator "="
           :left {:type :macaw.ast/column, :column "category"},
           :right {:type :macaw.ast/literal, :value "hello"}}}
         (->ast "select * from products where category = 'hello'"))))

(deftest ^:parallel basic-aggregation-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :name "count",
            :params [{:type :macaw.ast/wildcard}]}],
          :from {:type :macaw.ast/table, :table "products"}}
         (->ast "select count(*) from products"))))

(deftest ^:parallel extra-names-test
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

(deftest ^:parallel basic-grouping-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/function,
            :name "sum",
            :params [{:type :macaw.ast/column, :column "total"}]}
           {:type :macaw.ast/column, :column "category"}],
          :from {:type :macaw.ast/table, :table "orders"},
          :group-by [{:type :macaw.ast/column, :column "category"}]}
         (->ast "select sum(total), category from orders group by category"))))

(deftest ^:parallel basic-arg-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/binary-expression,
           :operator "=",
           :left {:type :macaw.ast/column, :column "category"},
           :right {:type :macaw.ast/jdbc-parameter}}}
         (->ast "select * from products where category = ?"))))

(deftest ^:parallel basic-case-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/case,
            :else {:type :macaw.ast/column, :column "total"},
            :when-clauses
            [{:when {:type :macaw.ast/binary-expression,
                     :operator "<",
                     :left {:type :macaw.ast/column, :column "total"},
                     :right {:type :macaw.ast/literal, :value 0}}
              :then {:type :macaw.ast/unary-expression,
                     :operation :sign
                     :expression {:type :macaw.ast/column, :column "total"},
                     :sign "-"}}]}],
          :from {:type :macaw.ast/table, :table "orders"}}
         (->ast "select case when total < 0 then -total else total end from orders"))))

(deftest ^:parallel switch-case-test
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

(deftest ^:parallel basic-exists-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :table "u", :column "name"}
           {:type :macaw.ast/column, :table "u", :column "email"}],
          :from {:type :macaw.ast/table, :table-alias "u", :table "users"},
          :where
          {:type :macaw.ast/unary-expression,
           :operation :exists
           :expression
           {:type :macaw.ast/select,
            :select [{:type :macaw.ast/literal, :value 1}],
            :from {:type :macaw.ast/table, :table-alias "o", :table "orders"},
            :where
            {:type :macaw.ast/binary-expression,
             :operator "=",
             :left {:type :macaw.ast/column, :table "o", :column "user_id"},
             :right {:type :macaw.ast/column, :table "u", :column "id"}}},}}
         (->ast "SELECT u.name, u.email
FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)"))))

(deftest ^:parallel basic-not-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/unary-expression,
           :operation :not
           :expression
           {:type :macaw.ast/expression-list,
            :expressions
            [{:type :macaw.ast/binary-expression,
              :operator "=",
              :left {:type :macaw.ast/column, :column "category"},
              :right {:value "Gizmo", :type :macaw.ast/literal}}]}}}
         (->ast "select * from products where not (category = 'Gizmo')"))))

(deftest ^:parallel basic-is-null-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/unary-expression,
           :operation :is-null
           :expression {:type :macaw.ast/column, :column "category"},
           :not false}}
         (->ast "select * from products where category is null"))))

(deftest ^:parallel negated-is-null-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "products"},
          :where
          {:type :macaw.ast/unary-expression,
           :operation :is-null
           :expression {:type :macaw.ast/column, :column "category"},
           :not true}}
         (->ast "select * from products where category is not null"))))

(deftest ^:parallel basic-cte-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "active_users"},
          :with
          [{:type :macaw.ast/select,
            :table-alias "active_users",
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

(deftest ^:parallel recursive-cte-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :column "name"}
           {:type :macaw.ast/column, :column "level"}],
          :from {:type :macaw.ast/table, :table "emp_hierarchy"},
          :with
          [{:type :macaw.ast/set-operation,
            :table-alias "emp_hierarchy",
            :selects
            [{:type :macaw.ast/select,
              :select
              [{:type :macaw.ast/column, :column "id"}
               {:type :macaw.ast/column, :column "name"}
               {:type :macaw.ast/column, :column "manager_id"}
               {:type :macaw.ast/literal, :alias "level", :value 0}],
              :from {:type :macaw.ast/table, :table "employees"},
              :where
              {:type :macaw.ast/unary-expression,
               :operation :is-null
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
              :from {:type :macaw.ast/table, :table-alias "e", :table "employees"},
              :join
              [{:type :macaw.ast/join,
                :source
                {:type :macaw.ast/table, :table-alias "h", :table "emp_hierarchy"},
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

(deftest ^:parallel basic-union-test
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

(deftest ^:parallel basic-between-test
  (is (= {:type :macaw.ast/select,
          :select [{:type :macaw.ast/wildcard}],
          :from {:type :macaw.ast/table, :table "orders"},
          :where
          {:type :macaw.ast/between,
           :expression {:type :macaw.ast/column, :column "created_at"},
           :start {:type :macaw.ast/jdbc-parameter},
           :end {:type :macaw.ast/jdbc-parameter}}}
         (->ast "select * from orders where created_at between ? and ?"))))

(deftest ^:parallel row-number-test
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

(deftest ^:parallel partition-by-test
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

(deftest ^:parallel select-with-column-alias-test
  (is (= {:type :macaw.ast/select,
          :select
          [{:alias "order1",
            :type :macaw.ast/select,
            :select [{:type :macaw.ast/column, :column "order"}],
            :from {:type :macaw.ast/table, :table "products"}}]}
         (->ast "select (select order from products) as order1"))))

(deftest ^:parallel week-test
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

(deftest ^:parallel week-of-year-test
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
                {:type :macaw.ast/unary-expression,
                 :operation :cast
                 :expression
                 {:type :macaw.ast/unary-expression,
                  :operation :extract
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
                {:type :macaw.ast/unary-expression,
                 :operation :cast
                 :expression
                 {:type :macaw.ast/unary-expression,
                  :operation :extract
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
                {:type :macaw.ast/unary-expression,
                 :operation :cast
                 :expression
                 {:type :macaw.ast/unary-expression,
                  :operation :extract
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

(deftest ^:parallel complicated-test-1
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
           :table-alias "\"source\"",
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
              :table-alias "\"Products\"",
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

(deftest ^:parallel complicated-test-2
  (is (= {:type :macaw.ast/select,
          :select
          [{:type :macaw.ast/column, :table "a", :column "id"}
           {:type :macaw.ast/column, :table "a", :column "customer_name"}
           {:type :macaw.ast/column, :table "a", :column "created_at"}
           {:type :macaw.ast/column, :table "a", :column "is_converted"}
           {:type :macaw.ast/column, :table "a", :column "lifetime_value"}
           {:type :macaw.ast/column, :table "a", :column "is_active"}
           {:type :macaw.ast/column,
            :table "a",
            :column "subscription_status"}
           {:type :macaw.ast/column, :table "a", :column "deployment"}
           {:type :macaw.ast/column, :table "a", :column "plan_name"}
           {:type :macaw.ast/column, :table "a", :column "plan_alias"}
           {:type :macaw.ast/column, :table "a", :column "base_fee"}
           {:type :macaw.ast/column, :table "a", :column "annual_value"}
           {:type :macaw.ast/column, :table "mas", :column "month"}
           {:type :macaw.ast/column, :table "mas", :column "is_trialing"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_trialing_accounts_cloud"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_active_accounts_cloud"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_paid_accounts_cloud"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_trialing_accounts_self_hosted"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_active_accounts_self_hosted"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_paid_accounts_self_hosted"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_active_accounts_enterprise"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_paid_accounts_enterprise"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_active_accounts_starter"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_paid_accounts_starter"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_active_accounts_pro"}
           {:type :macaw.ast/column,
            :table "mas",
            :column "is_paid_accounts_pro"}],
          :from
          {:type :macaw.ast/table,
           :table-alias "a",
           :schema "dbt_models",
           :table "account"},
          :join
          [{:type :macaw.ast/join,
            :source
            {:type :macaw.ast/table,
             :table-alias "mas",
             :table "monthly_account_subscription_status"},
            :condition
            [{:type :macaw.ast/binary-expression,
              :operator "=",
              :left {:type :macaw.ast/column, :table "a", :column "id"},
              :right
              {:type :macaw.ast/column, :table "mas", :column "id"}}]}],
          :with
          [{:table-alias "months",
            :type :macaw.ast/select,
            :select
            [{:alias "month",
              :type :macaw.ast/unary-expression,
              :operation :cast,
              :expression
              {:type :macaw.ast/function,
               :name "generate_series",
               :params
               [{:type :macaw.ast/function,
                 :name "date_trunc",
                 :params
                 [{:value "month", :type :macaw.ast/literal}
                  {:type :macaw.ast/unary-expression,
                   :operation :cast,
                   :expression
                   {:value "2024-01-01", :type :macaw.ast/literal},
                   :datatype "date"}]}
                {:type :macaw.ast/binary-expression,
                 :operator "-",
                 :left
                 {:type :macaw.ast/function,
                  :name "date_trunc",
                  :params
                  [{:value "month", :type :macaw.ast/literal}
                   {:type :macaw.ast/time-key, :value "current_date"}]},
                 :right {:type :macaw.ast/interval, :value "'1 month'"}}
                {:value "1 month", :type :macaw.ast/literal}]},
              :datatype "date"}]}
           {:table-alias "monthly_account_subscription_status",
            :type :macaw.ast/select,
            :select
            [{:type :macaw.ast/column, :table "a", :column "id"}
             {:type :macaw.ast/column, :table "m", :column "month"}
             {:alias "is_trialing",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "<=",
                    :left
                    {:type :macaw.ast/column, :table "m", :column "month"},
                    :right
                    {:type :macaw.ast/function,
                     :name "date_trunc",
                     :params
                     [{:value "month", :type :macaw.ast/literal}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "trial_ended_at"}]}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_trialing_accounts_cloud",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/unary-expression,
                     :operation :is-null,
                     :expression
                     {:type :macaw.ast/column,
                      :table "ah",
                      :column "subscription_status"},
                     :not true},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "deployment"},
                     :right {:value "cloud", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "<=",
                    :left
                    {:type :macaw.ast/column, :table "m", :column "month"},
                    :right
                    {:type :macaw.ast/function,
                     :name "date_trunc",
                     :params
                     [{:value "month", :type :macaw.ast/literal}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "trial_ended_at"}]}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_active_accounts_cloud",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "=",
                    :left
                    {:type :macaw.ast/column,
                     :table "a",
                     :column "deployment"},
                    :right {:value "cloud", :type :macaw.ast/literal}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_paid_accounts_cloud",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/binary-expression,
                     :operator ">",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "lifetime_value"},
                     :right {:value 0, :type :macaw.ast/literal}},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "deployment"},
                     :right {:value "cloud", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/expression-list,
                    :expressions
                    [{:type :macaw.ast/binary-expression,
                      :operator "OR",
                      :left
                      {:type :macaw.ast/binary-expression,
                       :operator ">=",
                       :left
                       {:type :macaw.ast/column, :table "m", :column "month"},
                       :right
                       {:type :macaw.ast/function,
                        :name "date_trunc",
                        :params
                        [{:value "month", :type :macaw.ast/literal}
                         {:type :macaw.ast/column,
                          :table "a",
                          :column "trial_ended_at"}]}},
                      :right
                      {:type :macaw.ast/unary-expression,
                       :operation :is-null,
                       :expression
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "trial_ended_at"},
                       :not false}}]}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_trialing_accounts_self_hosted",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/unary-expression,
                     :operation :is-null,
                     :expression
                     {:type :macaw.ast/column,
                      :table "ah",
                      :column "subscription_status"},
                     :not true},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "deployment"},
                     :right
                     {:value "self-hosted", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "<=",
                    :left
                    {:type :macaw.ast/column, :table "m", :column "month"},
                    :right
                    {:type :macaw.ast/function,
                     :name "date_trunc",
                     :params
                     [{:value "month", :type :macaw.ast/literal}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "trial_ended_at"}]}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_active_accounts_self_hosted",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "=",
                    :left
                    {:type :macaw.ast/column,
                     :table "a",
                     :column "deployment"},
                    :right {:value "self-hosted", :type :macaw.ast/literal}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_paid_accounts_self_hosted",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/binary-expression,
                     :operator ">",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "lifetime_value"},
                     :right {:value 0, :type :macaw.ast/literal}},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "deployment"},
                     :right
                     {:value "self-hosted", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/expression-list,
                    :expressions
                    [{:type :macaw.ast/binary-expression,
                      :operator "OR",
                      :left
                      {:type :macaw.ast/binary-expression,
                       :operator ">=",
                       :left
                       {:type :macaw.ast/column, :table "m", :column "month"},
                       :right
                       {:type :macaw.ast/function,
                        :name "date_trunc",
                        :params
                        [{:value "month", :type :macaw.ast/literal}
                         {:type :macaw.ast/column,
                          :table "a",
                          :column "trial_ended_at"}]}},
                      :right
                      {:type :macaw.ast/unary-expression,
                       :operation :is-null,
                       :expression
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "trial_ended_at"},
                       :not false}}]}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_active_accounts_enterprise",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "=",
                    :left
                    {:type :macaw.ast/function,
                     :name "coalesce",
                     :params
                     [{:type :macaw.ast/column,
                       :table "ah",
                       :column "plan_name"}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "plan_name"}]},
                    :right {:value "enterprise", :type :macaw.ast/literal}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_paid_accounts_enterprise",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/binary-expression,
                     :operator ">",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "lifetime_value"},
                     :right {:value 0, :type :macaw.ast/literal}},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/function,
                      :name "coalesce",
                      :params
                      [{:type :macaw.ast/column,
                        :table "ah",
                        :column "plan_name"}
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "plan_name"}]},
                     :right {:value "enterprise", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/expression-list,
                    :expressions
                    [{:type :macaw.ast/binary-expression,
                      :operator "OR",
                      :left
                      {:type :macaw.ast/binary-expression,
                       :operator ">=",
                       :left
                       {:type :macaw.ast/column, :table "m", :column "month"},
                       :right
                       {:type :macaw.ast/function,
                        :name "date_trunc",
                        :params
                        [{:value "month", :type :macaw.ast/literal}
                         {:type :macaw.ast/column,
                          :table "a",
                          :column "trial_ended_at"}]}},
                      :right
                      {:type :macaw.ast/unary-expression,
                       :operation :is-null,
                       :expression
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "trial_ended_at"},
                       :not false}}]}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_active_accounts_starter",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "=",
                    :left
                    {:type :macaw.ast/function,
                     :name "coalesce",
                     :params
                     [{:type :macaw.ast/column,
                       :table "ah",
                       :column "plan_name"}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "plan_name"}]},
                    :right {:value "starter", :type :macaw.ast/literal}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_paid_accounts_starter",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/binary-expression,
                     :operator ">",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "lifetime_value"},
                     :right {:value 0, :type :macaw.ast/literal}},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/function,
                      :name "coalesce",
                      :params
                      [{:type :macaw.ast/column,
                        :table "ah",
                        :column "plan_name"}
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "plan_name"}]},
                     :right {:value "starter", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/expression-list,
                    :expressions
                    [{:type :macaw.ast/binary-expression,
                      :operator "OR",
                      :left
                      {:type :macaw.ast/binary-expression,
                       :operator ">=",
                       :left
                       {:type :macaw.ast/column, :table "m", :column "month"},
                       :right
                       {:type :macaw.ast/function,
                        :name "date_trunc",
                        :params
                        [{:value "month", :type :macaw.ast/literal}
                         {:type :macaw.ast/column,
                          :table "a",
                          :column "trial_ended_at"}]}},
                      :right
                      {:type :macaw.ast/unary-expression,
                       :operation :is-null,
                       :expression
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "trial_ended_at"},
                       :not false}}]}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_active_accounts_pro",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/unary-expression,
                    :operation :is-null,
                    :expression
                    {:type :macaw.ast/column,
                     :table "ah",
                     :column "subscription_status"},
                    :not true},
                   :right
                   {:type :macaw.ast/binary-expression,
                    :operator "=",
                    :left
                    {:type :macaw.ast/function,
                     :name "coalesce",
                     :params
                     [{:type :macaw.ast/column,
                       :table "ah",
                       :column "plan_name"}
                      {:type :macaw.ast/column,
                       :table "a",
                       :column "plan_name"}]},
                    :right {:value "pro", :type :macaw.ast/literal}}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}
             {:alias "is_paid_accounts_pro",
              :type :macaw.ast/function,
              :name "max",
              :params
              [{:type :macaw.ast/case,
                :else {:value 0, :type :macaw.ast/literal},
                :when-clauses
                [{:when
                  {:type :macaw.ast/binary-expression,
                   :operator "AND",
                   :left
                   {:type :macaw.ast/binary-expression,
                    :operator "AND",
                    :left
                    {:type :macaw.ast/binary-expression,
                     :operator ">",
                     :left
                     {:type :macaw.ast/column,
                      :table "a",
                      :column "lifetime_value"},
                     :right {:value 0, :type :macaw.ast/literal}},
                    :right
                    {:type :macaw.ast/binary-expression,
                     :operator "=",
                     :left
                     {:type :macaw.ast/function,
                      :name "coalesce",
                      :params
                      [{:type :macaw.ast/column,
                        :table "ah",
                        :column "plan_name"}
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "plan_name"}]},
                     :right {:value "pro", :type :macaw.ast/literal}}},
                   :right
                   {:type :macaw.ast/expression-list,
                    :expressions
                    [{:type :macaw.ast/binary-expression,
                      :operator "OR",
                      :left
                      {:type :macaw.ast/binary-expression,
                       :operator ">=",
                       :left
                       {:type :macaw.ast/column, :table "m", :column "month"},
                       :right
                       {:type :macaw.ast/function,
                        :name "date_trunc",
                        :params
                        [{:value "month", :type :macaw.ast/literal}
                         {:type :macaw.ast/column,
                          :table "a",
                          :column "trial_ended_at"}]}},
                      :right
                      {:type :macaw.ast/unary-expression,
                       :operation :is-null,
                       :expression
                       {:type :macaw.ast/column,
                        :table "a",
                        :column "trial_ended_at"},
                       :not false}}]}},
                  :then {:value 1, :type :macaw.ast/literal}}]}]}],
            :from
            {:type :macaw.ast/table,
             :table-alias "a",
             :schema "dbt_models",
             :table "account"},
            :join
            [{:type :macaw.ast/join,
              :source
              {:type :macaw.ast/table,
               :table-alias "ah",
               :schema "dbt_models",
               :table "account_history"},
              :condition
              [{:type :macaw.ast/binary-expression,
                :operator "=",
                :left {:type :macaw.ast/column, :table "a", :column "id"},
                :right
                {:type :macaw.ast/column,
                 :table "ah",
                 :column "account_id"}}]}
             {:type :macaw.ast/join,
              :source
              {:type :macaw.ast/table, :table-alias "m", :table "months"},
              :condition
              [{:type :macaw.ast/between,
                :expression
                {:type :macaw.ast/column, :table "m", :column "month"},
                :start
                {:type :macaw.ast/function,
                 :name "date_trunc",
                 :params
                 [{:value "month", :type :macaw.ast/literal}
                  {:type :macaw.ast/column,
                   :table "ah",
                   :column "valid_from"}]},
                :end
                {:type :macaw.ast/function,
                 :name "date_trunc",
                 :params
                 [{:value "month", :type :macaw.ast/literal}
                  {:type :macaw.ast/column,
                   :table "ah",
                   :column "valid_to"}]}}]}],
            :group-by
            [{:value 1, :type :macaw.ast/literal}
             {:value 2, :type :macaw.ast/literal}]}]}
         (->ast "with months as (
  select
    generate_series(date_trunc('month', '2024-01-01'::date),
                    date_trunc('month', current_date) - interval '1 month',
                    '1 month')::date as month

)
, monthly_account_subscription_status as (
  select
    a.id,
    m.month,

    max(case when ah.subscription_status is not null and m.month <= date_trunc('month', a.trial_ended_at) then 1 else 0 end) as is_trialing,

    -- cloud
    max(case when ah.subscription_status is not null and a.deployment = 'cloud' and m.month <= date_trunc('month', a.trial_ended_at) then 1 else 0 end) as is_trialing_accounts_cloud,
    max(case when ah.subscription_status is not null and a.deployment = 'cloud' then 1 else 0 end) as is_active_accounts_cloud,
    max(case when a.lifetime_value > 0 and a.deployment = 'cloud' and (m.month >= date_trunc('month', a.trial_ended_at) or a.trial_ended_at is null) then 1 else 0 end) as is_paid_accounts_cloud,

    -- self-hosted
    max(case when ah.subscription_status is not null and a.deployment = 'self-hosted' and m.month <= date_trunc('month', a.trial_ended_at) then 1 else 0 end) as is_trialing_accounts_self_hosted,
    max(case when ah.subscription_status is not null and a.deployment = 'self-hosted' then 1 else 0 end) as is_active_accounts_self_hosted,
    max(case when a.lifetime_value > 0 and a.deployment = 'self-hosted' and (m.month >= date_trunc('month', a.trial_ended_at) or a.trial_ended_at is null) then 1 else 0 end) as is_paid_accounts_self_hosted,

    -- enterprise
    max(case when ah.subscription_status is not null and coalesce(ah.plan_name, a.plan_name) = 'enterprise' then 1 else 0 end) as is_active_accounts_enterprise,
    max(case when a.lifetime_value > 0 and coalesce(ah.plan_name, a.plan_name) = 'enterprise' and (m.month >= date_trunc('month', a.trial_ended_at) or a.trial_ended_at is null) then 1 else 0 end) as is_paid_accounts_enterprise,

    -- starter
    max(case when ah.subscription_status is not null and coalesce(ah.plan_name, a.plan_name) = 'starter' then 1 else 0 end) as is_active_accounts_starter,
    max(case when a.lifetime_value > 0 and coalesce(ah.plan_name, a.plan_name) = 'starter' and (m.month >= date_trunc('month', a.trial_ended_at) or a.trial_ended_at is null) then 1 else 0 end) as is_paid_accounts_starter,

    -- pro
    max(case when ah.subscription_status is not null and coalesce(ah.plan_name, a.plan_name) = 'pro' then 1 else 0 end) as is_active_accounts_pro,
    max(case when a.lifetime_value > 0 and coalesce(ah.plan_name, a.plan_name) = 'pro' and (m.month >= date_trunc('month', a.trial_ended_at) or a.trial_ended_at is null) then 1 else 0 end) as is_paid_accounts_pro
  from dbt_models.account a
  left join dbt_models.account_history ah
    on a.id = ah.account_id
  left join months m
    on m.month between date_trunc('month', ah.valid_from) and date_trunc('month', ah.valid_to)
  group by 1,2
)
select
  a.id,
  a.customer_name,
  a.created_at,
  a.is_converted,
  a.lifetime_value,
  a.is_active,
  a.subscription_status,
  a.deployment,
  a.plan_name,
  a.plan_alias,
  a.base_fee,
  a.annual_value,

  -- monthly account status
  mas.month,
  mas.is_trialing,
  mas.is_trialing_accounts_cloud,
  mas.is_active_accounts_cloud,
  mas.is_paid_accounts_cloud,
  mas.is_trialing_accounts_self_hosted,
  mas.is_active_accounts_self_hosted,
  mas.is_paid_accounts_self_hosted,
  mas.is_active_accounts_enterprise,
  mas.is_paid_accounts_enterprise,
  mas.is_active_accounts_starter,
  mas.is_paid_accounts_starter,
  mas.is_active_accounts_pro,
  mas.is_paid_accounts_pro

from dbt_models.account a
left join monthly_account_subscription_status mas
  on a.id = mas.id
;"))))
