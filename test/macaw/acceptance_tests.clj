(ns macaw.acceptance-tests
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [macaw.core :as m]
   [macaw.core-test :as ct])
  (:import
   (java.io File)))

(set! *warn-on-reflection* true)

(def expectation-exceptions
  "The SADNESS ZONE
  These are overrides to the correct expectations in the fixture EDN files, where we still need to fix things.
  The reason for putting these here, rather than via comments in the EDN files, is to give central visibility."
  {
   ;; TODO currently all the sources get cancelled out with the derived columns due to analysis having flat scope.
   :cycle/cte        {:source-columns #{}}
   ;; TODO We are missing some fields and some table qualifiers.
   :shadow/subselect {:source-columns #{{:table "departments" :column "id"}
                                        {:table "departments" :column "name"}
                                        {:column "first_name"}
                                        {:column "last_name"}}}})

(def broken-rename?
  "MOAR SADNESS
  Remove fixtures from here as we fix them."
  #{:duplicate-scopes})

(defn- fixture-analysis [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".analysis.edn") io/resource slurp read-string))

(defn- fixture-renames [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".renames.edn") io/resource slurp read-string))

(defn- fixture-rewritten [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".rewritten.sql") io/resource slurp))


(defn- get-component [cs k]
  (case k
    :source-columns (get cs k)
    :columns-with-scope (ct/contexts->scopes (get cs :columns))
    (ct/raw-components (get cs k))))

(defn- test-fixture
  "Test that we can parse a given fixture, and compare against expected analysis and rewrites, where they are defined."
  [fixture]
  (let [prefix      (str "(fixture: " (subs (str fixture) 1) ")")
        sql         (ct/query-fixture fixture)
        expected-cs (fixture-analysis fixture)
        renames     (fixture-renames fixture)
        expected-rw (fixture-rewritten fixture)]
    (when-let [cs (testing (str prefix " analysis does not throw")
                    (is (ct/components sql)))]
      (doseq [[ck cv] expected-cs]
        (testing (str prefix " analysis is correct: " (name ck))
          (let [actual-cv (get-component cs ck)
                expected  (get-in expectation-exceptions [fixture ck] cv)]
            (if (vector? cv)
              (is (= expected (ct/sorted actual-cv)))
              (is (= expected actual-cv)))))))
    (when renames
      (let [rewritten (testing (str prefix " rewriting does not throw")
                        (is (m/replace-names sql renames)))]
        (when expected-rw
          (testing (str prefix " rewritten SQL is correct")
            (if (broken-rename? fixture)
              (is (not= expected-rw rewritten))
              (is (= expected-rw rewritten)))))))))

(defn find-fixtures
  "Find all the fixture symbols within our test resources."
  []
  (->> (io/resource "acceptance")
       io/file
       file-seq
       (keep #(when (.isFile ^File %)
                (let [n (.getName ^File %)]
                  (when (.endsWith n ".sql")
                    (str/replace n #"\.sql$" "")))))
       (remove #(.contains ^String % "."))
       (map ct/stem->fixture)
       (sort-by str)))

(defmacro create-fixture-tests!
  "Find all the fixture files and for each of them run all the tests we can construct from the related files."
  []
  (let [fixtures (find-fixtures)]
    (cons 'do
          (for [f fixtures
                :let [test-name (symbol (str/replace (ct/fixture->filename f "-test") #"(?<!_)_(?!_)" "-"))]]
            `(deftest ~test-name
               (test-fixture ~f))))))

(create-fixture-tests!)

(comment
 ;; Unload all the tests, useful for flushing stale fixture tests
 (doseq [[sym ns-var] (ns-interns *ns*)]
   (when (:test (meta ns-var))
     (ns-unmap *ns* sym)))

 (test-fixture :compound/cte)
 )
