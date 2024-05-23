(ns macaw.util-test
  (:require
   [clojure.test :refer :all]
   [macaw.util :as u]))

(def ^:private haystack
  {{:a 3 :b 2 :c 1}   2
   {:a 3 :b nil :c 1} 3
   {:a 1 :b 2 :c 3}   1})

(deftest cascading-find-test
  (testing "We ignore any suffix of degenerate keys"
    (doseq [x [{:a 1}
               {:a 1 :b 2}
               {:a 1 :b 2 :c 3}
               {:a 1 :b 2 :c 3 :d 4}]]
      (is (= [{:a 1 :b 2 :c 3} 1]
             (u/cascading-find haystack x [:a :b :c])))))

  (testing "We need at least one non-degenerate key"
    (is (nil? (u/cascading-find haystack {} [:a :b :c]))))

  (testing "We don't ignore non-suffix degenerate keys"
    (doseq [x [{:a nil :b 2}
               {:a 1 :b nil :c 3}
               {:a nil :b 2 :c 3}]]
      (is (nil?
           (u/cascading-find haystack x [:a :b :c]))))))
