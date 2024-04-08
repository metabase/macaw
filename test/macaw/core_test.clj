(ns ^:parallel macaw.core-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [macaw.core :as m]))

(def components    (comp m/query->components m/parsed-query))
(def columns       (comp :columns components))
(def has-wildcard? (comp :has-wildcard? components))
(def mutations     (comp :mutation-commands components))
(def tables        (comp :tables components))
(def table-wcs     (comp :table-wildcards components))

(deftest query->tables-test
  (testing "Simple queries"
    (is (= #{"core_user"}
           (tables "select * from core_user;")))
    (is (= #{"core_user"}
           (tables "select id, email from core_user;"))))
  (testing "With a schema (Postgres)" ;; TODO: only run this against supported DBs
    ;; It strips the schema
    (is (= #{"core_user"}
           (tables "select * from the_schema_name.core_user;"))))
  (testing "Sub-selects"
    (is (= #{"core_user"}
           (tables "select * from (select distinct email from core_user) q;")))))

(deftest tables-with-complex-aliases-issue-14-test
  (testing "With an alias that is also a table name"
    #_(is (= #{"user" "user2_final"}
           (tables
            "SELECT legacy_user.id AS old_id,
                    user.id AS new_id
             FROM user AS legacy_user
             OUTER JOIN user2_final AS user
             ON legacy_user.email = user2_final.email;")))))

(deftest query->columns-test
  (testing "Simple queries"
    (is (= #{"foo" "bar" "id" "quux_id"}
           (columns "select foo, bar from baz inner join quux on quux.id = baz.quux_id"))))
  (testing "'group by' columns present"
    (is (= #{"id" "user_id"}
           (columns "select id from orders group by user_id")))))

(deftest mutation-test
  (is (= #{"alter-sequence"}
         (mutations "alter sequence serial restart with 42")))
  (is (= #{"alter-session"}
         (mutations "alter session set foo = 'bar'")))
  (is (= #{"alter-system"}
         (mutations "alter system reset all")))
  (is (= #{"alter-table"}
         (mutations "alter table orders add column email text")))
  (is (= #{"alter-view"}
         (mutations "alter view foo as select bar;")))
  (is (= #{"create-function"}           ; Postgres syntax
         (mutations "create function multiply(integer, integer) returns integer as 'select $1 * $2;' language sql
         immutable returns null on null input;")))
  (is (= #{"create-function"}           ; Conventional syntax
         (mutations "create function multiply(a integer, b integer) returns integer language sql immutable returns
         null on null input return a + b;")))
  (is (= #{"create-index"}
         (mutations "create index idx_user_id on orders(user_id);")))
  (is (= #{"create-schema"}
         (mutations "create schema perthshire")))
  (is (= #{"create-sequence"}
         (mutations "create sequence users_seq start with 42 increment by 17")))
  (is (= #{"create-synonym"}
         (mutations "create synonym folk for people")))
  (is (= #{"create-table"}
         (mutations "create table poets (name text, id integer)")))
  (is (= #{"create-view"}
         (mutations "create view folk as select * from people where id > 10")))
  (is (= #{"delete"}
         (mutations "delete from people")))
  (is (= #{"drop"}
         (mutations "drop table people")))
  (is (= #{"grant"}
         (mutations "grant select, update, insert on people to myself")))
  (is (= #{"insert"}
         (mutations "insert into people(name,source) values ('Robert Fergusson', 'Twitter'), ('Robert Burns',
         'Facebook')")))
  (is (= #{"purge"}
         (mutations "purge table people")))
  (is (= #{"rename-table"}
         (mutations "rename table people to folk")))
  (is (= #{"truncate"}
         (mutations "truncate table people")))
  (is (= #{"update"}
         (mutations "update people set name = 'Robert Fergusson' where id = 23"))))

(deftest alias-inclusion-test
  (testing "Aliases are not included"
    (is (= #{"orders" "foo"}
           (tables "select id, o.id from orders o join foo on orders.id = foo.order_id")))))

(deftest resolve-columns-test
  (let [cols ["name" "id" "email"]]
    (is (= {"core_user"   cols
            "report_card" cols}
           (m/resolve-columns ["core_user" "report_card"] cols)))))

(deftest select-*-test
  (is (true? (has-wildcard? "select * from orders")))
  (is (true? (has-wildcard? "select id, * from orders join foo on orders.id = foo.order_id"))))

(deftest table-wildcard-test-without-aliases
  (is (= #{"orders"}
         (table-wcs "select orders.* from orders join foo on orders.id = foo.order_id")))
    (is (= #{"foo"}
         (table-wcs "select foo.* from orders join foo on orders.id = foo.order_id"))))

(deftest table-star-test-with-aliases
  (is (= #{"orders"}
         (table-wcs "select o.* from orders o join foo on orders.id = foo.order_id")))
    (is (= #{"foo"}
         (table-wcs "select f.* from orders o join foo f on orders.id = foo.order_id"))))

(defn test-replacement [before replacements after]
  (is (= after (m/replace-names before replacements))))

(deftest replace-names-test
  (test-replacement "select a.x, b.y from a, b;"
                    {:tables {"a" "aa"}
                     :columns  {"x" "xx"}}
                    "select aa.xx, b.y from aa, b;")

  (test-replacement
   "select *, boink
  , yoink as oink
 from /* /* lore */
    core_user,
  bore_user,  /* more */ snore_user ;"

   {:tables  {"core_user"  "floor_muser"
              "bore_user"  "user"
              "snore_user" "vigilant_user"
              "cruft"      "tuft"}
    :columns {"boink" "sturmunddrang"
              "yoink" "oink"
              "hoi"   "polloi"}}

   "select *, sturmunddrang
  , oink as oink
 from /* /* lore */
    floor_muser,
  user,  /* more */ vigilant_user ;"))
