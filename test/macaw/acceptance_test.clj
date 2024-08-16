(ns macaw.acceptance-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [macaw.core :as m]
   [macaw.core-test :as ct])
  (:import
   (java.io File)))

(set! *warn-on-reflection* true)

(def broken-queries
  "The DANGER ZONE
  This map gives a pattern in the exception message we expect to receive when trying to analyze the given fixture."
  {:broken/between      #"Encountered unexpected token: \"BETWEEN\""
   :broken/filter-where #"Encountered unexpected token: \"\(\""})

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
        expected-rw (fixture-rewritten fixture)
        opts        {:non-reserved-words [:final]}]
    (if-let [expected-msg (broken-queries fixture)]
      (testing (str prefix " analysis cannot be parsed")
        (is (thrown-with-msg? Exception
                              expected-msg
                              (ct/components sql opts))))
      (when-let [cs (testing (str prefix " analysis does not throw")
                      (is (ct/components sql opts)))]
        (doseq [[ck cv] (dissoc expected-cs :overrides)]
          (testing (str prefix " analysis is correct: " (name ck))
            (let [actual-cv (get-component cs ck)
                  override  (get-in expected-cs [:overrides ck])
                  expected  (or override cv)]

              (when override
                (if (vector? cv)
                  (is (not= cv (ct/sorted actual-cv)) "Override is still needed")
                  (is (not= cv actual-cv) "Override is still needed")))

              (if (vector? expected)
                (is (= expected (ct/sorted actual-cv)))
                (is (= expected actual-cv))))))))
    (when renames
      (let [broken?   (:broken? renames)
            rewritten (testing (str prefix " rewriting does not throw")
                        (is (m/replace-names sql (dissoc renames :broken?) opts)))]
        (when expected-rw
          (testing (str prefix " rewritten SQL is correct")
            (if broken?
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
 (test-fixture :compound/cte-nonambiguous)
 (test-fixture :literal/with-table)
 (test-fixture :literal/without-table)

 (test-fixture :broken/filter-where))
