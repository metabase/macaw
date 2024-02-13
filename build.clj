;; Further info: https://clojure.org/guides/tools_build#_mixed_java_clojure_build

(ns build
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'metabase/macaw)

(def major-minor-version "0.1")

(defn commit-number []
  (or (-> (sh/sh "git" "rev-list" "HEAD" "--count")
          :out
          str/trim
          parse-long)
      "9999-SNAPSHOT"))

(def version (str major-minor-version \. (commit-number)))
(def target "target")
(def class-dir (format "%s/classes" target))

(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path target}))

(defn compile [_]
  (b/javac {:src-dirs   ["java"]
            :class-dir  class-dir
            :basis      @basis
            :javac-opts ["--release" "11"]}))

(defn jar [_]
  (compile nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     @basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))
