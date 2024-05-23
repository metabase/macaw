(ns macaw.test.util-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer :all]
   [macaw.test.util :refer [ws=]]))

(deftest ^:parallel ws=-test
  (testing "Code indentation is ignored"
    (is (ws= "ABC
                DEF
              XXX"
             (str/replace "ABC
                             DEF
                           GHI"
                          "GHI" "XXX"))))

  (testing "Comparison is still whitespace sensitive"
    (is (not (ws= "A    B" "A B")))
    (is (not (ws= "A
                   B
                   C"
                  "A
                    B
                   C")))))
