;; Borrowed from second-date

(ns hooks.clojure.test
  (:require [clj-kondo.hooks-api :as hooks]))

(def disallowed-parallel-forms
  '#{with-redefs clojure.core/with-redefs})

(defn warn-about-disallowed-parallel-forms [form]
  (letfn [(f [form]
            (when (hooks/token-node? form)
              (let [sexpr (hooks/sexpr form)]
                (when (symbol? sexpr)
                  (when (or (disallowed-parallel-forms sexpr)
                            #_(= (last (str sexpr)) \!))
                    (hooks/reg-finding! (assoc (meta form)
                                               :message (format "%s is not allowed inside a ^:parallel test" sexpr)
                                               :type :macaw/validate-deftest)))))))
          (walk [form]
            (f form)
            (doseq [child (:children form)]
              (walk child)))]
    (walk form)))

(defn deftest [{{[_ test-name & body] :children, :as node} :node}]
  (let [test-metadata     (:meta test-name)
        metadata-sexprs   (map hooks/sexpr test-metadata)
        combined-metadata (transduce
                           (map (fn [x]
                                  (if (map? x)
                                    x
                                    {x true})))
                           (completing merge)
                           {}
                           metadata-sexprs)
        parallel?     (:parallel combined-metadata)
        synchronized? (:synchronized combined-metadata)]
    (when-not (or parallel? synchronized?)
      (hooks/reg-finding! (assoc (meta test-name)
                                 :message "Test should be marked either ^:parallel or ^:synchronized"
                                 :type :macaw/validate-deftest)))
    (when (and parallel? synchronized?)
      (hooks/reg-finding! (assoc (meta test-name)
                                 :message "Test should not be marked both ^:parallel and ^:synchronized"
                                 :type :macaw/validate-deftest)))
    (when parallel?
      (doseq [form body]
        (warn-about-disallowed-parallel-forms form))))
  {:node node})
