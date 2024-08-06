;; Further info: https://clojure.org/guides/tools_build#_mixed_java_clojure_build

(ns build
  (:refer-clojure :exclude [compile])
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def lib 'io.github.metabase/macaw)
(def github-url "https://github.com/metabase/macaw")
(def scm-url    "git@github.com:metabase/macaw.git")

(def version-template (str/trim (slurp "VERSION")))
(assert (re-matches #"\d+\.\d+\.x" version-template))

(def major-minor-version (str/replace version-template #"\.x$" ""))

(defn sh [& args]
  (let [{:keys [exit out]} (apply sh/sh args)]
    (assert (zero? exit))
    (str/trim out)))

(defn- commits-since-version-changed []
  (let [last-sha (sh "git" "log" "-1" "--format=%H" "--" "VERSION")]
    (parse-long (sh "git" "rev-list" "--count" (str last-sha "..HEAD")))))

(defn commit-number []
  (if (= "master" (sh "git" "rev-parse" "--abbrev-ref" "HEAD"))
    (commits-since-version-changed)
    "9999-SNAPSHOT"))

(def sha
  (or (not-empty (System/getenv "GITHUB_SHA"))
      (not-empty (-> (sh/sh "git" "rev-parse" "HEAD")
                     :out
                     str/trim))))

(def version (str major-minor-version \. (commit-number)))
(def target "target")
(def class-dir (format "%s/classes" target))

(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def basis (delay (b/create-basis {:project "deps.edn"})))

(def pom-template
  [[:description "A Clojure wrapper for JSqlParser"]
   [:url github-url]
   [:licenses
    [:license
     [:name "Eclipse Public License"]
     [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
   [:developers
    [:developer
     [:name "Tim Macdonald"]]
    [:developer
     [:name "Chris Truter"]]]
   [:scm
    [:url github-url]
    [:connection (str "scm:git:" scm-url)]
    [:developerConnection (str "scm:git:" scm-url)]
    [:tag sha]]])

(def default-options
  {:lib       lib
   :version   version
   :jar-file  jar-file
   :basis     @basis
   :class-dir class-dir
   :target    target
   :src-dirs  ["src" "java"]
   :pom-data  pom-template})

(defn clean [_]
  (b/delete {:path target}))

(defn compile [opts]
  (println "\nCompiling Java files...")
  (b/javac (merge default-options
                  opts
                  {:src-dirs   ["java"]
                   :javac-opts ["--release" "11" "-Xlint:-options" #_"-Xlint:unchecked"]})))

(defn jar [opts]
  (println "\nStarting to build a JAR...")
  (compile nil)
  (println "\tWriting pom.xml...")
  (b/write-pom (merge default-options opts))
  (println "\tCopying source...")
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (printf "\tBuilding %s...\n" jar-file)
  (b/jar {:class-dir class-dir
          :jar-file  jar-file})
  (println "Done! 🦜"))


(defn deploy [opts]
  (let [opts (merge default-options opts)]
    (printf "Deploying %s...\n" jar-file)
    (dd/deploy {:installer :remote
                :artifact  (b/resolve-path jar-file)
                :pom-file  (b/pom-path (select-keys opts [:lib :class-dir]))})
    (println "Deployed! 🦜")))
