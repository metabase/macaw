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

(defn seek
  "Like (first (filter ... )), but doesn't realize chunks of the sequence. Returns the first item in `coll` for which
  `pred` returns a truthy value, or `nil` if no such item is found."
  [pred coll]
  (reduce
   (fn [acc x] (if (pred x) (reduced x) acc))
   nil
   coll))
