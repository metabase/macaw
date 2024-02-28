(ns macaw.core-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [macaw.core :as m]))

(def tables (comp :tables m/query->components m/parsed-query))

(deftest ^:parallel query->tables-test
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

(def columns (comp :columns m/query->components m/parsed-query))

(deftest ^:parallel query->columns-test
  (testing "Simple queries"
    (is (= #{"foo" "bar" "id" "quux_id"}
           (columns "select foo, bar from baz inner join quux on quux.id = baz.quux_id")))))

(deftest ^:parallel resolve-columns-test
  (let [cols ["name" "id" "email"]]
    (is (= {"core_user"   cols
            "report_card" cols}
           (m/resolve-columns ["core_user" "report_card"] cols)))))

(defn test-replacement [before replacements after]
  (is (= after (m/replace-names before replacements))))

(deftest ^:parallel replace-names-test
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
