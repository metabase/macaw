(ns macaw.util.malli.defn
  (:refer-clojure :exclude [defn defn-])
  (:require
   [clojure.core :as core]
   [clojure.string :as str]
   [macaw.util.malli.fn :as mu.fn]
   [malli.destructure]))

(set! *warn-on-reflection* true)

;;; TODO -- this should generate type hints from the schemas and from the return type as well.
(core/defn- deparameterized-arglist [{:keys [args]}]
  (-> (malli.destructure/parse args)
      :arglist
      (with-meta (let [args-meta    (meta args)
                       tag          (:tag args-meta)
                       resolved-tag (when (symbol? tag)
                                      (let [resolved (ns-resolve *ns* tag)]
                                        (when (class? resolved)
                                          (symbol (.getName ^Class resolved)))))]
                   (cond-> args-meta
                     resolved-tag (assoc :tag resolved-tag))))))

(core/defn- deparameterized-arglists [{:keys [arities], :as _parsed}]
  (let [[arities-type arities-value] arities]
    (case arities-type
      :single   (list (deparameterized-arglist arities-value))
      :multiple (map deparameterized-arglist (:arities arities-value)))))

(core/defn- annotated-docstring
  "Generate a docstring with additional information about inputs and return type using a parsed fn tail (as parsed
  by [[mx/SchematizedParams]])."
  [{original-docstring           :doc
    [arities-type arities-value] :arities
    :keys                        [return]
    :as                          _parsed}]
  (str/trim
   (str "Inputs: " (case arities-type
                     :single   (pr-str (:args arities-value))
                     :multiple (str "("
                                    (str/join "\n           "
                                              (map (comp pr-str :args)
                                                   (:arities arities-value)))
                                    ")"))
        "\n  Return: " (str/replace (:schema return :any) ; used to be a pprint
                                    "\n"
                                    (str "\n          "))
        (when (not-empty original-docstring)
          (str "\n\n  " original-docstring)))))

(defmacro defn
  "Implementation of [[metabase.util.malli/defn]] taken from Metabase. Like [[schema.core/defn]], but for Malli.

  See notes/justification in the main Metabase repo."
  [& [fn-name :as fn-tail]]
  (let [parsed           (mu.fn/parse-fn-tail fn-tail)
        cosmetic-name    (gensym (munge (str fn-name)))
        {attr-map :meta} parsed
        attr-map         (merge
                          {:arglists (list 'quote (deparameterized-arglists parsed))
                           :schema   (mu.fn/fn-schema parsed {:target :target/metadata})}
                          attr-map)
        docstring        (annotated-docstring parsed)
        instrument?      (mu.fn/instrument-ns? *ns*)]
    (if-not instrument?
      `(def ~(vary-meta fn-name merge attr-map)
         ~docstring
         ~(mu.fn/deparameterized-fn-form parsed cosmetic-name))
      `(def ~(vary-meta fn-name merge attr-map)
         ~docstring
         ~(let [error-context {:fn-name (list 'quote fn-name)}]
            (mu.fn/instrumented-fn-form error-context parsed cosmetic-name))))))

(defmacro defn-
  "Same as defn, but creates a private def."
  [fn-name & fn-tail]
  `(defn
     ~(with-meta fn-name (assoc (meta fn-name) :private true))
     ~@fn-tail))
