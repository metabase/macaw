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
           (tables "SELECT * FROM core_user;")))
    (is (= #{"core_user"}
           (tables "SELECT id, email FROM core_user;"))))
  (testing "With a schema (Postgres)" ;; TODO: only run this against supported DBs
    ;; It strips the schema
    (is (= #{"core_user"}
           (tables "SELECT * FROM the_schema_name.core_user;"))))
  (testing "Sub-selects"
    (is (= #{"core_user"}
           (tables "SELECT * FROM (SELECT DISTINCT email FROM core_user) q;")))))

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
           (columns "SELECT foo, bar FROM baz INNER JOIN quux ON quux.id = baz.quux_id"))))
  (testing "'group by' columns present"
    (is (= #{"id" "user_id"}
           (columns "SELECT id FROM orders GROUP BY user_id")))))

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

(deftest alias-inclusion-test
  (testing "Aliases are not included"
    (is (= #{"orders" "foo"}
           (tables "SELECT id, o.id FROM orders o JOIN foo ON orders.id = foo.order_id")))))

(deftest resolve-columns-test
  (let [cols ["name" "id" "email"]]
    (is (= {"core_user"   cols
            "report_card" cols}
           (m/resolve-columns ["core_user" "report_card"] cols)))))

(deftest select-*-test
  (is (true? (has-wildcard? "SELECT * FROM orders")))
  (is (true? (has-wildcard? "SELECT id, * FROM orders JOIN foo ON orders.id = foo.order_id"))))

(deftest table-wildcard-test-without-aliases
  (is (= #{"orders"}
         (table-wcs "SELECT orders.* FROM orders JOIN foo ON orders.id = foo.order_id")))
    (is (= #{"foo"}
         (table-wcs "SELECT foo.* FROM orders JOIN foo ON orders.id = foo.order_id"))))

(deftest table-star-test-with-aliases
  (is (= #{"orders"}
         (table-wcs "SELECT o.* FROM orders o JOIN foo ON orders.id = foo.order_id")))
    (is (= #{"foo"}
         (table-wcs "SELECT f.* FROM orders o JOIN foo f ON orders.id = foo.order_id"))))

(defn test-replacement [before replacements after]
  (is (= after (m/replace-names before replacements))))

(deftest replace-names-test
  (test-replacement "SELECT a.x, b.y FROM a, b;"
                    {:tables {"a" "aa"}
                     :columns  {"x" "xx"}}
                    "SELECT aa.xx, b.y FROM aa, b;")

  (test-replacement
   "SELECT *, boink
  , yoink AS oink
 FROM /* /* lore */
    core_user,
  bore_user,  /* more */ snore_user ;"

   {:tables  {"core_user"  "floor_muser"
              "bore_user"  "user"
              "snore_user" "vigilant_user"
              "cruft"      "tuft"}
    :columns {"boink" "sturmunddrang"
              "yoink" "oink"
              "hoi"   "polloi"}}

   "SELECT *, sturmunddrang
  , oink AS oink
 FROM /* /* lore */
    floor_muser,
  user,  /* more */ vigilant_user ;"))
