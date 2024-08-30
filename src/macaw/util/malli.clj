(ns macaw.util.malli
  (:refer-clojure :exclude [defn defn-])
  (:require
   [macaw.util.malli.defn :as mu.defn]
   [potemkin :as p]))

(p/import-vars
 [mu.defn defn defn-])
