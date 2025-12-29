(ns macaw.util-test
  (:require
   [clojure.test :refer :all]
   [macaw.util :as u]))

(def ^:private haystack
  {{:a 3 :b 2 :c 1}   2
   {:a 3 :b nil :c 1} 3
   {:a 1 :b 2 :c 3}   1})

(deftest ^:parallel relevant-find-test
  (testing "We ignore any suffix of degenerate keys"
    (doseq [x [{:a 1}
               {:a 1 :b nil}
               {:a 1 :b 2}
               {:a 1 :b 2 :c nil}
               {:a 1 :b 2 :c 3}
               {:a 1 :b 2 :c 3 :d 4}]]
      (is (= [{:a 1 :b 2 :c 3} 1]
             (u/find-relevant haystack x [:a :b :c])))))

  (testing "We need at least one non-degenerate key"
    (is (nil? (u/find-relevant haystack {} [:a :b :c]))))

  (testing "We don't ignore non-suffix degenerate keys"
    (doseq [x [{:a nil :b 2}
               {:a 1 :b nil :c 3}
               {:a nil :b 2 :c 3}]]
      (is (nil?
           (u/find-relevant haystack x [:a :b :c]))))))

;; Test the 3-tier priority: exact → wildcard → fallback
(deftest ^:parallel find-relevant-priority-test
  (testing "Exact match has highest priority (key has explicit nil)"
    (let [m {{:table "x" :schema nil} :exact
             {:table "x"}             :wildcard
             {:table "x" :schema "s"} :fallback}]
      (is (= [{:table "x" :schema nil} :exact]
             (u/find-relevant m {:table "x"} [:table :schema])))))

  (testing "Wildcard match when no exact match (key lacks the suffix key entirely)"
    (let [m {{:table "x"}             :wildcard
             {:table "x" :schema "s"} :fallback}]
      (is (= [{:table "x"} :wildcard]
             (u/find-relevant m {:table "x"} [:table :schema])))))

  (testing "Fallback match (naked ref matches qualified key)"
    (let [m {{:table "x" :schema "s"} :fallback}]
      (is (= [{:table "x" :schema "s"} :fallback]
             (u/find-relevant m {:table "x"} [:table :schema])))))

  (testing "Exact match (explicit nil) preferred over wildcard (key absent)"
    (let [m {{:table "x" :schema nil} :exact
             {:table "x"}             :wildcard}]
      (is (= [{:table "x" :schema nil} :exact]
             (u/find-relevant m {:table "x"} [:table :schema])))))

  (testing "No match when prefix keys don't match"
    (let [m {{:table "y" :schema nil} :exact
             {:table "y"}             :wildcard
             {:table "y" :schema "s"} :fallback}]
      (is (nil? (u/find-relevant m {:table "x"} [:table :schema]))))))
