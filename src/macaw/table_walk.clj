(ns macaw.table-walk
  (:import
   (com.metabase.macaw TableExtractor AstWalker$CallbackKey)))

(set! *warn-on-reflection* true)

(def ->callback-key
  "keyword->key map for the AST-folding callbacks."
  ;; TODO: Move this to a Malli schema to simplify the indirection
  {:table            AstWalker$CallbackKey/TABLE})

(defn- preserve
  "Lift a side effecting callback so that it preserves the accumulator."
  [f]
  (fn [acc & args]
    (apply f args)
    acc))

;; work around ast walker repeatedly visiting the same expressions (bug ?!)
(defn- deduplicate-visits [f]
  (let [seen (volatile! #{})]
    (fn [& [acc visitable & _ :as args]]
      (if (contains? @seen visitable)
        acc
        (do (vswap! seen conj visitable)
            (apply f args))))))

(defn- update-keys-vals [m key-f val-f]
  (let [ret (persistent!
             (reduce-kv (fn [acc k v]
                          (assoc! acc (key-f k) (val-f v)))
                        (transient {})
                        m))]
    (with-meta ret (meta m))))

(defn fold-query
  "Fold over the query's AST, using the callbacks to update the accumulator."
  [parsed-query callbacks init-val]
  (let [callbacks (update-keys-vals callbacks ->callback-key deduplicate-visits)]
    (.fold (TableExtractor. callbacks init-val) parsed-query)))
