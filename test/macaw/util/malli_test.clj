(ns macaw.util.malli-test
  (:require
   [clojure.test :refer :all]
   [macaw.util.malli :as mu]))

(mu/defn enhance :- :int
  [x :- :int, fail-deliberately? :- :boolean]
  (if fail-deliberately?
    :poppycock
    (int (+ x 43))))

(deftest ^:parallel defn-good-in-good-out-test
  (is (= 50 (enhance 7 false))))

(deftest ^:parallel defn-good-in-bad-out-test
  (is (thrown-with-msg? Exception
                        #"Invalid output.*"
                        (enhance 7 true))))

(deftest ^:parallel defn-bad-in-good-out-test
  (is (thrown-with-msg? Exception
                        #"Invalid input.*"
                        (enhance 7.3 false))))

(deftest ^:parallel defn-bad-in-bad-out-test
  (is (thrown-with-msg? Exception
                        #"Invalid input.*"
                        (enhance 7.3 true))))
