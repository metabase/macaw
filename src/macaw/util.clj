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

(defn- match-keys
  "Returns a predicate that checks if a map entry matches the element.
   - ks-prefix: keys to match on (element has non-nil values)
   - ks-suffix: keys stripped from element (element has nil/sentinel values)
   - mode: :exact (key present with nil value), :wildcard (key absent), :omitted (allow any value)"
  [element ks-prefix ks-suffix mode]
  (let [expected (map element ks-prefix)]
    (fn [entry]
      (let [k (key entry)]
        (and
         ;; The prefix keys must match
         (every? true? (map match-component expected (map k ks-prefix)))
         ;; For suffix keys, behavior depends on mode and sentinel values
         ;; In :omitted mode, we accept any value so skip the check entirely
         (or (= mode :omitted)
             (every? (fn [suffix-key]
                       (let [elem-val (element suffix-key)]
                         ;; Sentinel value - allow any value in map key (lenient matching)
                         (if (and elem-val (not (non-sentinel elem-val)))
                           true
                           (case mode
                             ;; Exact: key must be present with explicit nil value
                             :exact    (and (contains? k suffix-key) (nil? (k suffix-key)))
                             ;; Wildcard: key must be absent entirely
                             :wildcard (not (contains? k suffix-key))))))
                     ks-suffix)))))))

(defn find-relevant
  "Search the given map for the entry corresponding to [[element]], considering only the relevant keys.
  The relevant keys are obtained by ignoring any suffix of [[ks]] for which [[element]] has nil or missing values.
  We require that there is at least one relevant key to find a match.

  Matching priority (for element {:table \"x\"} with no :schema key):
  1. Exact match: {:schema nil :table \"x\"} - element's nil/missing schema matches key's nil schema
  2. Wildcard match: {:table \"x\"} - key doesn't have :schema key at all
  3. Omitted match: {:schema \"s\" :table \"x\"} - naked reference matches qualified key as fallback"
  [m element ks]
  (when element
    ;; Strip off keys from right-to-left where they are nil, and relax search to only consider these keys.
    ;; We need at least one non-generate key to remain for the search.
    (when-let [ks-prefix (->> ks reverse (drop-while (comp not non-sentinel element)) reverse seq)]
      (let [ks-suffix (drop (count ks-prefix) ks)]
        (or (seek (match-keys element ks-prefix ks-suffix :exact) m)
            (seek (match-keys element ks-prefix ks-suffix :wildcard) m)
            (seek (match-keys element ks-prefix ks-suffix :omitted) m))))))

(def ^:private nil-val? (comp nil? val))
(defn- nil-or-empty? [entry]
  (let [v (val entry)]
    (or (nil? v)
        (and (seqable? v)
             (empty? v)))))

(defn strip-nils
  "Remove any keys corresponding to nil values from the given map."
  ([m]
   (strip-nils m false))
  ([m strip-empty?]
   (let [pred (if strip-empty?
                nil-or-empty?
                nil-val?)]
     (if (some pred m)
       (with-meta (into {} (remove pred) m) (meta m))
       m))))
