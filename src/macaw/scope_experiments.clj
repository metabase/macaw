(ns macaw.scope-experiments
  (:require
   [macaw.core :as m]
   [macaw.walk :as mw])
  (:import
   (net.sf.jsqlparser.schema Column Table)))

(defn- node->clj [node]
  (cond
    (instance? Column node) [:column
                             (some-> (.getTable node) .getName)
                             (.getColumnName node)]
    (instance? Table node) [:table (.getName node)]
    :else [(type node) node]))

(defn- semantic-map [sql]
  (mw/fold-query (m/parsed-query sql)
                 {:every-node (fn [acc node ctx]
                                (let [id   (m/scope-id (first ctx))
                                      node (node->clj node)]
                                  (-> acc
                                      (update-in [:scopes id]
                                                 (fn [scope]
                                                   (-> scope
                                                       (update :path #(or % (mapv m/scope-label (reverse ctx))))
                                                       (update :children (fnil conj []) node))))
                                      ((fn [acc']
                                         (if-let [parent-id (some-> (second ctx) m/scope-id)]
                                           (-> acc'
                                               (update :parents assoc id parent-id)
                                               (update-in [:children parent-id] (fnil conj #{}) id))
                                           acc')))
                                      (update :sequence (fnil conj []) [id node]))))}
                 {:scopes   {}                             ;; id -> {:path [labels], :children [nodes]}
                  :parents  {}                             ;; what scope is this inside?
                  :children {}                             ;; what scopes are inside?
                  :sequence []}))                          ;; [scope-id, node]

(comment
 (semantic-map "select x from t, u, v left join w on w.id = v.id where t.id = u.id and u.id = v.id limit 3")
 ;{:scopes {1 {:path ["SELECT"], :children [[:column "x"]]},
 ;          2 {:path ["SELECT" "FROM"], :children [[:table "t"]]},
 ;          4 {:path ["SELECT" "JOIN" "FROM"], :children [[:table "u"]]},
 ;          5 {:path ["SELECT" "JOIN" "FROM"], :children [[:table "v"]]},
 ;          6 {:path ["SELECT" "JOIN" "FROM"], :children [[:table "w"]]},
 ;          3 {:path ["SELECT" "JOIN"], :children [[:column "id"] [:table "w"] [:column "id"] [:table "v"]]},
 ;          7 {:path ["SELECT" "WHERE"],
 ;             :children [[:column "id"]
 ;                        [:table "t"]
 ;                        [:column "id"]
 ;                        [:table "u"]
 ;                        [:column "id"]
 ;                        [:table "u"]
 ;                        [:column "id"]
 ;                        [:table "v"]]}},
 ; :parents {2 1, 4 3, 5 3, 6 3, 3 1, 7 1},
 ; :children {1 #{7 3 2}, 3 #{4 6 5}},
 ; :sequence [[1 [:column "x"]]
 ;            [2 [:table "t"]]
 ;            [4 [:table "u"]]
 ;            [5 [:table "v"]]
 ;            [6 [:table "w"]]
 ;            [3 [:column "id"]]
 ;            [3 [:table "w"]]
 ;            [3 [:column "id"]]
 ;            [3 [:table "v"]]
 ;            [7 [:column "id"]]
 ;            [7 [:table "t"]]
 ;            [7 [:column "id"]]
 ;            [7 [:table "u"]]
 ;            [7 [:column "id"]]
 ;            [7 [:table "u"]]
 ;            [7 [:column "id"]]
 ;            [7 [:table "v"]]]}


 (semantic-map "select t.a,b,c,d from t")
 ;{:scopes {1 {:path ["select"], :children [[:column "a"] [:column "b"] [:column "c"] [:column "d"]]},
 ;          2 {:path ["select" "from"], :children [[:table "t"]]}},
 ; :parents {2 1},
 ; :children {1 #{2}},
 ; :sequence [[1 [:column "a"]] [1 [:column "b"]] [1 [:column "c"]] [1 [:column "d"]] [2 [:table "t"]]]}
 )

(defn- get-descendants-map [parent-children-map]
  (letfn [(get-all-descendants [parent]
            (let [children (get parent-children-map parent [])]
              (into #{} (concat children
                                (mapcat #(get-all-descendants %)
                                        children)))))]
    (into {}
          (for [parent (keys parent-children-map)]
            [parent (get-all-descendants parent)]))))

(defn fields->tables-in-scope [sql]
  (let [sm                   (semantic-map sql)
        tables               (filter (comp #{:table} first second) (:sequence sm))
        scope->tables        (reduce
                              (fn [m [scope-id [_ table-name]]]
                                (update m scope-id (fnil conj #{}) table-name))
                              {}
                              tables)
        scope->descendants   (get-descendants-map (:children sm))
        scope->nested-tables (reduce
                              (fn [m parent-id]
                                (assoc m parent-id
                                       (into (set (scope->tables parent-id)) (mapcat scope->tables (scope->descendants parent-id)))))
                              {}
                              (keys (:scopes sm)))
        columns              (filter (comp #{:column} first second) (:sequence sm))]

    (vec (distinct (for [[scope-id [_ table-name column-name]] columns]
                     [[scope-id column-name]
                      (if table-name
                        #{table-name}
                        (scope->nested-tables scope-id))])))))

(defn- fields-to-search [f->ts]
  (into (sorted-set)
        (mapcat (fn [[[_ column-name] table-names]]
                  (map #(vector :table % :column column-name) table-names)))

        f->ts))

(comment
 ;; like source-columns, but understands scope
 (fields-to-search
  (fields->tables-in-scope "select x from t, u, v left join w on w.a = v.a where t.b = u.b and u.c = v.c limit 3"))
  ;#{[:table "t" :column "b"]
  ;  [:table "t" :column "x"]
  ;  [:table "u" :column "b"]
  ;  [:table "u" :column "c"]
  ;  [:table "u" :column "x"]
  ;  [:table "v" :column "a"]
  ;  [:table "v" :column "c"]
  ;  [:table "v" :column "x"]
  ;  [:table "w" :column "a"]
  ;  [:table "w" :column "x"]}
 )
