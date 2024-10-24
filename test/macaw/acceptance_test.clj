(ns macaw.acceptance-test
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
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
  {:broken/between       #"Encountered unexpected token: \"BETWEEN\""
   :broken/filter-where  #"Encountered unexpected token: \"\(\""
   :sqlserver/execute    #"Not supported yet"
   :sqlserver/executesql #"Not supported yet"
   :oracle/open-for      #"Encountered unexpected token: \"OPEN\""})

(defn- fixture-analysis [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".analysis.edn") io/resource slurp read-string))

(defn- fixture-renames [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".renames.edn") io/resource slurp read-string))

(defn- fixture-rewritten [fixture]
  (some-> fixture (ct/fixture->filename "acceptance" ".rewritten.sql") io/resource slurp str/trim))

(defn- get-component [cs k]
  (case k
    :source-columns (get cs k)
    :columns-with-scope (ct/contexts->scopes (get cs :columns))
    (ct/raw-components (get cs k))))

(def ^:private test-modes
  #{:ast-walker-1
    :basic-select
    :compound-select})

(def override-hierarchy
  (-> (make-hierarchy)
      (derive :basic-select :select-only)
      (derive :compound-select :select-only)))

(defn- lineage [h k]
  (when k
    (assert (<= (count (parents h k)) 1) "Multiple inheritance not supported for override hierarchy.")
    (cons k (lineage h (first (parents h k))))))

(def global-overrides
  {})

(def ns-overrides
  {:select-only  {"mutation" :macaw.error/invalid-query
                  "dynamic"  :macaw.error/invalid-query}
   :basic-select {"compound" :macaw.error/unsupported-expression}})

(def ^:private merged-fixtures-file "test/resources/acceptance/queries.sql")

(defn- merged-fixtures
  "The fixtures in merged fixtures file, mapped by their identifiers."
  []
  (->> (str/split (slurp merged-fixtures-file) #"-- FIXTURE: ")
       (keep (fn [named-query]
               (when-not (str/blank? named-query)
                 (let [[nm qry] (.split ^String named-query "\n" 2)]
                   [(keyword nm) (str/trim qry)]))))
       (into {})))

(defn- validate-analysis [correct override actual]
  (let [expected (or override correct)]
    (when override
      (testing "Override is still needed"
        (if (and (vector? correct) (not (keyword actual)))
          (is (not= correct (ct/sorted actual)))
          (is (not= correct actual)))))

    (if (and (vector? expected) (not (keyword actual)))
      (is (= expected (ct/sorted actual)))
      (when (not= expected actual)
        (is (= expected actual))))))

(defn- when-keyword [x]
  (when (keyword? x)
    x))

(defn- get-override* [expected-cs mode fixture ck]
  (or (get global-overrides mode)
      (get-in ns-overrides [mode (namespace fixture)])
      (get-in expected-cs [:overrides mode :error])
      (get-in expected-cs [:overrides mode ck])
      (when-keyword (get-in expected-cs [:overrides mode]))))

(defn- get-override [expected-cs mode fixture ck]
  (or
   (some #(get-override* expected-cs % fixture ck)
         (lineage override-hierarchy mode))

   (get-in expected-cs [:overrides :error])
   (get-in expected-cs [:overrides ck])
   (when-keyword (get expected-cs :overrides))))

(defn- test-fixture
  "Test that we can parse a given fixture, and compare against expected analysis and rewrites, where they are defined."
  [fixture]
  (let [prefix      (str "(fixture: " (subs (str fixture) 1) ")")
        merged      (merged-fixtures)
        sql         (or (ct/query-fixture fixture) (get merged fixture))
        expected-cs (fixture-analysis fixture)
        renames     (fixture-renames fixture)
        expected-rw (fixture-rewritten fixture)
        base-opts   {:non-reserved-words [:final], :allow-unused? true}
        opts-mode   (fn [mode] (assoc base-opts :mode mode))]
    (assert sql "Fixture exists")
    (doseq [m test-modes
            :let [opts (opts-mode m)]]
      (if (= m :ast-walker-1)
        ;; Legacy testing path for `components`, which only supports the original walker, and throws exceptions.
        (if-let [expected-msg (broken-queries fixture)]
          (testing (str prefix " analysis cannot be parsed")
            (is (thrown-with-msg? Exception expected-msg (ct/components sql opts))))
          (let [cs (testing (str prefix " analysis does not throw")
                     (is (ct/components sql opts)))]
            (doseq [[ck cv] (dissoc expected-cs :overrides :error)]
              (testing (str prefix " analysis is correct: " (name ck))
                (let [actual-cv (get-component cs ck)
                      override  (get-override expected-cs m fixture ck)]
                  (validate-analysis cv override actual-cv))))))
        ;; Testing path for newer modes.
        (let [correct  (:error expected-cs (:tables expected-cs))
              override (get-override expected-cs m fixture :tables)
              ;; For now, we only support (and test) :tables
              tables   (testing (str prefix " table analysis does not throw for mode " m)
                         (is (ct/tables sql opts)))]
          (when-not (and (nil? correct) (nil? override))
            (testing (str prefix " table analysis is correct for mode " m)
              (validate-analysis correct override tables))))))

    (when renames
      (let [broken?   (:broken? renames)
            rewritten (testing (str prefix " rewriting does not throw")
                        (is (str/trim (m/replace-names sql (dissoc renames :broken?) base-opts))))]
        (when expected-rw
          (testing (str prefix " rewritten SQL is correct")
            (if broken?
              (is (not= expected-rw rewritten))
              (is (= expected-rw rewritten)))))))))

(defn isolated-fixtures
  "Find all the fixture symbols for stand-alone sql files within our test resources."
  []
  (->> (io/resource "acceptance")
       io/file
       file-seq
       (keep #(when (.isFile ^File %)
                (let [n (.getName ^File %)]
                  (when (.endsWith n ".sql")
                    (str/replace n #"\.sql$" "")))))
       (remove #(.contains ^String % "."))
       (remove #{"queries"})
       (map ct/stem->fixture)
       (sort-by str)))

(defn- all-fixtures []
  (let [isolated (isolated-fixtures)
        merged   (keys (merged-fixtures))]
    (assert (empty? (set/intersection (set isolated) (set merged)))
            "No fixtures should be in both the isolated and merged files")
    (sort-by str (distinct (concat isolated merged)))))

(defmacro create-fixture-tests!
  "Find all the fixture files and for each of them run all the tests we can construct from the related files."
  []
  (let [fixtures (all-fixtures)]
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

  (merged-fixtures)
  ;; Append all the isolated fixtures to the merged file.
  ;; For now, we keep the stress-testing fixtures separate, because OH LAWDY they HUGE.
  (spit merged-fixtures-file
        (str/join "\n\n"
                  (for [fixture (isolated-fixtures)]
                    (str "-- FIXTURE: "
                         (when-let [nms (namespace fixture)]
                           (str nms "/"))
                         (name fixture) "\n"
                         (str/trim
                          (ct/query-fixture fixture))))))

  (deftest single-test
    (test-fixture :compound/subselect))

  (test-fixture :compound/cte)
  (test-fixture :compound/cte-nonambiguous)
  (test-fixture :literal/with-table)
  (test-fixture :literal/without-table)

  (test-fixture :broken/filter-where))
