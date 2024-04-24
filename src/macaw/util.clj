(ns macaw.util)

(defn group-with
  "Generalized `group-by`, where you can supply your own reducing function (instead of usual `conj`).

  https://ask.clojure.org/index.php/12319/can-group-by-be-generalized"
  [kf rf coll]
  (persistent!
    (reduce
      (fn [ret x]
        (let [k (kf x)]
          (assoc! ret k (rf (get ret k) x))))
      (transient {})
      coll)))
