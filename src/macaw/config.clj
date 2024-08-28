(ns macaw.config
  (:require
   [clojure.string :as str]
   [environ.core :as env]))

(def ^:private app-defaults
  "Default configuration; could be overridden by Java properties, environment variables, etc."
  {:macaw-run-mode "prod"})

(defn- config-value
  [k]
  (let [env-val (get env/env k)]
    (or (when-not (str/blank? env-val) env-val)
        (get app-defaults k))))

(def run-mode "The mode (dev/test/prod) in which Macaw is being run."
  (some-> :macaw-run-mode config-value keyword))

(defn is-dev?
  "Is Macaw running in `dev` mode (i.e., in a REPL)?"
  []
  (= run-mode :dev))

(defn is-test?
  "Is Macaw running in `test` mode?"
  []
  (= run-mode :test))

(defn is-prod?
  "Is Macaw running in `prod` mode (i.e., from a JAR)?"
  []
  (= run-mode :prod))
