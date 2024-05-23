(ns ^:parallel macaw.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [macaw.core :as m]
   #_{:clj-kondo/ignore [:refer]}
   [macaw.test-utils :refer [ws=]]
   [macaw.walk :as mw])
  (:import
   (net.sf.jsqlparser.schema Table)))

(set! *warn-on-reflection* true)

(defn- and*
  [x y]
  (and x y))

(def components     (comp m/query->components m/parsed-query))
(def raw-components (comp (partial into #{}) (partial map :component)))
(def columns        (comp raw-components :columns components))
(def has-wildcard?  (comp (partial reduce and*) raw-components :has-wildcard? components))
(def mutations      (comp raw-components :mutation-commands components))
(def tables         (comp raw-components :tables components))
(def table-wcs      (comp raw-components :table-wildcards components))

(defn column-qualifiers
  [query]
  (mw/fold-query (m/parsed-query query)
                 {:column-qualifier (fn [acc tbl _ctx] (conj acc (.getName ^Table tbl)))}
                 #{}))

(deftest query->tables-test
  (testing "Simple queries"
    (is (= #{{:table "core_user"}}
           (tables "SELECT * FROM core_user;")))
    (is (= #{{:table "core_user"}}
           (tables "SELECT id, email FROM core_user;"))))
  (testing "With a schema (Postgres)" ;; TODO: only run this against supported DBs
    (is (= #{{:table "core_user" :schema "the_schema_name"}}
           (tables "SELECT * FROM the_schema_name.core_user;")))
    (is (= #{{:table "orders" :schema "public"}
             {:table "orders" :schema "private"}}
           (tables "SELECT a.x FROM public.orders a, private.orders"))))
  (testing "Sub-selects"
    (is (= #{{:table "core_user"}}
           (tables "SELECT * FROM (SELECT DISTINCT email FROM core_user) q;")))))


(deftest tables-with-complex-aliases-issue-14-test
  (testing "With an alias that is also a table name"
    (is (= #{{:table "user"}
             {:table "user2_final"}}
           (tables
            "SELECT legacy_user.id AS old_id,
                    user.id AS new_id
             FROM user AS legacy_user
             OUTER JOIN user2_final AS user
             ON legacy_user.email = user2_final.email;")))))

(deftest column-qualifier-test
  (testing "column-qualifiers works with tables and aliases"
    (is (= #{"user" "legacy_user"}
           (column-qualifiers "SELECT
                                 user.id AS user_id,
                                 legacy_user.id AS old_id
                               FROM user
                               OUTER JOIN user as legacy_user
                               ON user.email = user.electronic_mail_address
                               JOIN unrelated_table on foo = user.unrelated_id;")))))

(deftest query->columns-test
  (testing "Simple queries"
    (is (= #{{:column "foo"}
             {:column "bar"}
             {:column "id" :table "quux"}
             {:column "quux_id" :table "baz"}}
           (columns "SELECT foo, bar FROM baz INNER JOIN quux ON quux.id = baz.quux_id"))))
  (testing "'group by' columns present"
    (is (= #{{:column "id" :table "orders"}
             {:column "user_id" :table "orders"}}
           (columns "SELECT id FROM orders GROUP BY user_id"))))
  (testing "table alias present"
    (is (= #{{:column "id" :table "orders" :schema "public"}}
           (columns "SELECT o.id FROM public.orders o"))))
  (testing "schema is determined correctly"
    (is (= #{{:column "x" :table "orders" :schema "public"}}
           (columns "SELECT public.orders.x FROM public.orders, private.orders")))))

(deftest infer-test
  (testing "We can first column through a few hoops"
    (is (= #{{:column "amount" :table "orders"}}
           (columns "SELECT amount FROM (SELECT amount FROM orders)")))
    (is (= #{{:column "amount" :alias "cost" :table "orders"}
             ;; FIXME: we need to figure out that `cost` is an alias from subquery
             {:column "cost", :table "orders"}}
           (columns "SELECT cost FROM (SELECT amount AS cost FROM orders)")))))

(deftest mutation-test
  (is (= #{"alter-sequence"}
         (mutations "ALTER SEQUENCE serial RESTART WITH 42")))
  (is (= #{"alter-session"}
         (mutations "ALTER SESSION SET foo = 'bar'")))
  (is (= #{"alter-system"}
         (mutations "ALTER SYSTEM RESET ALL")))
  (is (= #{"alter-table"}
         (mutations "ALTER TABLE orders ADD COLUMN email text")))
  (is (= #{"alter-view"}
         (mutations "ALTER VIEW foo AS SELECT bar;")))
  (is (= #{"create-function"}           ; Postgres syntax
         (mutations "CREATE FUNCTION multiply(integer, integer) RETURNS integer AS 'SELECT $1 * $2;' LANGUAGE SQL
         IMMUTABLE RETURNS NULL ON NULL INPUT;")))
  (is (= #{"create-function"}           ; Conventional syntax
         (mutations "CREATE FUNCTION multiply(a integer, b integer) RETURNS integer LANGUAGE SQL IMMUTABLE RETURNS
         NULL ON NULL INPUT RETURN a + b;")))
  (is (= #{"create-index"}
         (mutations "CREATE INDEX idx_user_id ON orders(user_id);")))
  (is (= #{"create-schema"}
         (mutations "CREATE SCHEMA perthshire")))
  (is (= #{"create-sequence"}
         (mutations "CREATE SEQUENCE users_seq START WITH 42 INCREMENT BY 17")))
  (is (= #{"create-synonym"}
         (mutations "CREATE SYNONYM folk FOR people")))
  (is (= #{"create-table"}
         (mutations "CREATE TABLE poets (name text, id integer)")))
  (is (= #{"create-view"}
         (mutations "CREATE VIEW folk AS SELECT * FROM people WHERE id > 10")))
  (is (= #{"delete"}
         (mutations "DELETE FROM people")))
  (is (= #{"drop"}
         (mutations "DROP TABLE people")))
  (is (= #{"grant"}
         (mutations "GRANT SELECT, UPDATE, INSERT ON people TO myself")))
  (is (= #{"insert"}
         (mutations "INSERT INTO people(name, source) VALUES ('Robert Fergusson', 'Twitter'), ('Robert Burns',
         'Facebook')")))
  (is (= #{"purge"}
         (mutations "PURGE TABLE people")))
  (is (= #{"rename-table"}
         (mutations "RENAME TABLE people TO folk")))
  (is (= #{"truncate"}
         (mutations "TRUNCATE TABLE people")))
  (is (= #{"update"}
         (mutations "UPDATE people SET name = 'Robert Fergusson' WHERE id = 23"))))

(deftest complicated-mutations-test
  ;; https://github.com/metabase/macaw/issues/18
  #_  (is (= #{"delete" "insert"}
             (mutations "WITH outdated_orders AS (
                       DELETE FROM orders
                       WHERE
                         date <= '2018-01-01'
                       RETURNING *
                     )
                     INSERT INTO order_log
                     SELECT * from outdated_orders;")))
    (is (= #{ "insert"}
         (mutations "WITH outdated_orders AS (
                       SELECT * from orders
                     )
                     INSERT INTO order_log
                     SELECT * from outdated_orders;"))))

(deftest alias-inclusion-test
  (testing "Aliases are not included"
    (is (= #{{:table "orders"} {:table "foo"}}
           (tables "SELECT id, o.id FROM orders o JOIN foo ON orders.id = foo.order_id")))))

(deftest select-*-test
  (is (true? (has-wildcard? "SELECT * FROM orders")))
  (is (true? (has-wildcard? "SELECT id, * FROM orders JOIN foo ON orders.id = foo.order_id"))))

(deftest table-wildcard-test-without-aliases
  (is (= #{{:table "orders"}}
         (table-wcs "SELECT orders.* FROM orders JOIN foo ON orders.id = foo.order_id")))
  (is (= #{{:table "foo" :schema "public"}}
         (table-wcs "SELECT foo.* FROM orders JOIN public.foo f ON orders.id = foo.order_id"))))

(deftest table-star-test-with-aliases
  (is (= #{{:table "orders"}}
         (table-wcs "SELECT o.* FROM orders o JOIN foo ON orders.id = foo.order_id")))
  (is (= #{{:table "foo"}}
         (table-wcs "SELECT f.* FROM orders o JOIN foo f ON orders.id = foo.order_id"))))

(deftest context-test
  (testing "Sub-select with outer wildcard"
    (is (= {:columns
            #{{:component {:column "total" :table "orders"}, :context ["SELECT" "SUB_SELECT" "FROM" "SELECT"]}
              {:component {:column "id"    :table "orders"}, :context ["SELECT" "SUB_SELECT" "FROM" "SELECT"]}
              {:component {:column "total" :table "orders"}, :context ["WHERE" "JOIN" "FROM" "SELECT"]}},
            :has-wildcard?     #{{:component true, :context ["SELECT"]}},
            :mutation-commands #{},
            :tables            #{{:component {:table "orders"}, :context ["FROM" "SELECT" "SUB_SELECT" "FROM" "SELECT"]}},
            :table-wildcards   #{}}
           (components "SELECT * FROM (SELECT id, total FROM orders) WHERE total > 10"))))
  (testing "Sub-select with inner wildcard"
    (is (= {:columns
            #{{:component {:column "id"    :table "orders"}, :context ["SELECT"]}
              {:component {:column "total" :table "orders"}, :context ["SELECT"]}
              {:component {:column "total" :table "orders"}, :context ["WHERE" "JOIN" "FROM" "SELECT"]}},
            :has-wildcard?     #{{:component true, :context ["SELECT" "SUB_SELECT" "FROM" "SELECT"]}},
            :mutation-commands #{},
            :tables            #{{:component {:table "orders"}, :context ["FROM" "SELECT" "SUB_SELECT" "FROM" "SELECT"]}},
            :table-wildcards   #{}}
           (components "SELECT id, total FROM (SELECT * FROM orders) WHERE total > 10"))))
  (testing "Sub-select with dual wildcards"
    (is (= {:columns           #{{:component {:column "total" :table "orders"}, :context ["WHERE" "JOIN" "FROM" "SELECT"]}},
            :has-wildcard?
            #{{:component true, :context ["SELECT" "SUB_SELECT" "FROM" "SELECT"]}
              {:component true, :context ["SELECT"]}},
            :mutation-commands #{},
            :tables            #{{:component {:table "orders"}, :context ["FROM" "SELECT" "SUB_SELECT" "FROM" "SELECT"]}},
            :table-wildcards   #{}}
           (components "SELECT * FROM (SELECT * FROM orders) WHERE total > 10"))))
  (testing "Join; table wildcard"
    (is (= {:columns           #{{:component {:column "order_id" :table "foo"}, :context ["JOIN" "SELECT"]}
                                 {:component {:column "id" :table "orders"}, :context ["JOIN" "SELECT"]}},
            :has-wildcard?     #{},
            :mutation-commands #{},
            :tables            #{{:component {:table "foo"}, :context ["FROM" "JOIN" "SELECT"]}
                                 {:component {:table "orders"}, :context ["FROM" "SELECT"]}},
            :table-wildcards   #{{:component {:table "orders"}, :context ["SELECT"]}}}
         (components "SELECT o.* FROM orders o JOIN foo ON orders.id = foo.order_id")))))

(deftest replace-names-test
  (is (= "SELECT aa.xx, b.x, b.y FROM aa, b;"
         (m/replace-names  "SELECT a.x, b.x, b.y FROM a, b;"
                           {:tables  {{:schema "public" :table "a"} "aa"}
                            :columns {{:schema "public" :table "a" :column "x"} "xx"}})))

  (is (= "SELECT qwe FROM orders"
         (m/replace-names "SELECT id FROM orders"
                          {:columns {{:schema "public" :table "orders" :column "id"} "qwe"}})))

  (is (= "SELECT p.id, q.id FROM public.whatever p join private.orders q"
         (m/replace-names "SELECT p.id, q.id FROM public.orders p join private.orders q"
                          {:tables {{:schema "public" :table "orders"} "whatever"}})))

  (is (ws= "SELECT SUM(public.orders.total) AS s,
            MAX(orders.total) AS max,
            MIN(total) AS min
            FROM public.orders"
         (m/replace-names
          "SELECT SUM(public.orders.amount) AS s,
           MAX(orders.amount) AS max,
           MIN(amount) AS min
           FROM public.orders"
          {:columns {{:schema "public" :table "orders" :column "amount"} "total"}})))

  (is (ws= "SELECT *, sturmunddrang
                    , oink AS oink
            FROM /* /* lore */
                 floor_muser,
                 user,  /* more */ vigilant_user ;"
           (m/replace-names
            "SELECT *, boink
                     , yoink AS oink
             FROM /* /* lore */
                  core_user,
                  bore_user,  /* more */ snore_user ;"
            {:tables  {{:schema "public" :table "core_user"}  "floor_muser"
                       {:schema "public" :table "bore_user"}  "user"
                       {:schema "public" :table "snore_user"} "vigilant_user"}
             :columns {{:schema "public" :table "core_user" :column "boink"}  "sturmunddrang"
                       {:schema "public" :table "snore_user" :column "yoink"} "oink"}})))

  (is (thrown? Exception #"Unknown rename"
               (m/replace-names "SELECT 1" {:tables {{:schema "public" :table "a"} "aa"}}))))

(deftest replace-schema-test
  (is (= "SELECT totally_private.purchases.xx FROM totally_private.purchases, private.orders WHERE xx = 1"
         (m/replace-names "SELECT public.orders.x FROM public.orders, private.orders WHERE x = 1"
                          {:schemas {"public" "totally_private"}
                           :tables  {{:schema "public" :table "orders"} "purchases"}
                           :columns {{:schema "public" :table "orders" :column "x"} "xx"}}))))
