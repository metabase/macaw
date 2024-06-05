(ns macaw.util
  (:require
   [clojure.string :as str])
  (:import (java.util.regex Pattern)))

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

(defn non-sentinel
  "A hack around the fact that we don't (yet) track what columns are exposed by given sentinels."
  [s]
  (when s
    (nil? (str/index-of s "_sentinel_"))))

(defn match-component
  "Check whether the given literal matches the expected literal or pattern."
  [expected actual]
  (when expected
    (if (instance? Pattern expected)
      (boolean (re-find expected actual))
      (= expected actual))))

(defn- match-prefix [element ks-prefix]
  (let [expected (map element ks-prefix)]
    (fn [entry]
      (every? true? (map match-component expected (map (key entry) ks-prefix))))))

(defn find-relevant
  "Search the given map for the entry corresponding to [[element]], considering only the relevant keys.
  The relevant keys are obtained by ignoring any suffix of [[ks]] for which [[element]] has nil or missing values.
  We require that there is at least one relevant key to find a match."
  [m element ks]
  (when element
    ;; Strip off keys from right-to-left where they are nil, and relax search to only consider these keys.
    ;; We need at least one non-generate key to remain for the search.
    ;; NOTE: we could optimize away calling `non-sentinel` twice in this function, but for now just keeping it simple.
    (when-let [ks-prefix (->> ks reverse (drop-while (comp not non-sentinel element)) reverse seq)]
      (seek (match-prefix element ks-prefix) m))))

(def ^:private nil-val? (comp nil? val))

(defn strip-nils
  "Remove any keys corresponding to nil values from the given map."
  [m]
  (if (some nil-val? m)
    (with-meta (into {} (remove nil-val?) m) (meta m))
    m))
