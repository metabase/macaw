(ns macaw.walk
  (:import
   (com.metabase.macaw AstWalker AstWalker$CallbackKey)))

(set! *warn-on-reflection* true)

(def ->callback-key
  "keyword->key map for the AST-folding callbacks."
  ;; TODO: Move this to a Malli schema to simplify the indirection
  {:column AstWalker$CallbackKey/COLUMN
   :table  AstWalker$CallbackKey/TABLE})

(defn- preserve
  "Lift a side effecting callback so that it preserves the accumulator."
  [f]
  (fn [acc v]
    (f v)
    acc))

(defn- update-keys-vals [m key-f val-f]
  (into {} (map (fn [[k v]] [(key-f k) (val-f v)])) m))

(defn walk-query
  "Walk over the query's AST, using the callbacks for their side-effects, for example to mutate the AST itself."
  [parsed-query callbacks]
  (let [callbacks (update-keys-vals callbacks ->callback-key preserve)]
    (.walk (AstWalker. callbacks ::ignored) parsed-query)))

(defn fold-query
  "Fold over the query's AST, using the callbacks to update the accumulator."
  [parsed-query callbacks init-val]
  (let [callbacks (update-keys callbacks ->callback-key)]
    (.fold (AstWalker. callbacks init-val) parsed-query)))
