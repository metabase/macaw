(ns macaw.test.util
  (:require
   [clojure.string :as str]
   [clojure.test :refer :all]
   [clojure.walk :as walk]))

(defn- indentation [s]
  (count (re-find #"^\s*" s)))

(defn- trim-indent* [margin s]
  (if (< (count s) margin)
    ""
    (subs s margin)))

(defn trim-indent
  "Given a multi-line string, remove the common margin from the remaining lines.
  Used so that strings with significant whitespace may be visually aligned."
  [s]
  (let [lines  (str/split-lines s)
        margin (->> (rest lines)
                    (remove str/blank?)
                    (transduce (map indentation) min Integer/MAX_VALUE))]
    (str/join "\n" (cons (first lines) (map (partial trim-indent* margin) (rest lines))))))

(defmacro ws=
  "Trim the extra indentation from all string literals before evaluation a given equality form."
  [& xs]
  `(= ~@(walk/postwalk #(cond-> % (string? %) trim-indent) xs)))
