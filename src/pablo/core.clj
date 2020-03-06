(ns pablo.core
  "High-level interface to pablo"
  (:require [pablo.version :as version]
            [pathetic.core :as path]
            [pablo.mono :as mono]
            [pablo.config :as config]
            [badigeon.jar]
            [badigeon.sign]
            [badigeon.deploy]
            [badigeon.install]
            [me.raynes.fs :as fs]
            [pablo.pom :as pom]
            [pablo.utils :as utils]))

(defn- read-deps-edn
  []
  (when (fs/exists? "deps.edn")
    (-> "deps.edn" fs/file slurp read-string)))

(defn- default-opts
  "Extends opts with some defaults"
  [opts]
  (-> opts
      (update :version (fnil identity (version/from-git)))
      (update :target-dir (fnil (comp str fs/absolute) "target/"))))

(defn- run-on-projects
  "Higher order function which calls `f` with a project symbol and its
  `deps.edn` for each project. In a normal repo, runs on the root. For a mono-repo
  uses the `:project` option, or runs on all projects if it's `nil`."
  [{:keys [target skip-cwd]} opts f]
  (let [deps-edn       (read-deps-edn)
        opts           (default-opts opts)
        run-on-project #(let [project-deps-edn (mono/project-deps deps-edn % opts)]
                          (if (config/targets? project-deps-edn target)
                            (if skip-cwd
                              (f project-deps-edn opts)
                              (utils/with-cwd (path/resolve (:target-dir opts) (str %))
                                (f project-deps-edn (merge opts {:target-dir "../"}))))
                            (println (format "Skipping non-%s project %s"
                                             (name target)
                                             (config/qualified-symbol deps-edn)))))]
    (when-not deps-edn
      (throw (ex-info "Could not find deps.edn" {:type :missing-deps})))

    (cond
      (and (config/monorepo? deps-edn) (:project opts)) (run-on-project (:project opts))
      (config/monorepo? deps-edn)                       (doseq [project (mono/projects deps-edn)]
                                                          (run-on-project project))
      (config/targets? deps-edn target)                 (f deps-edn opts)
      :else                                             (println
                                                          (format "Skipping non-%s project %s"
                                                                  (name target)
                                                                  (config/qualified-symbol deps-edn))))))

(defn jar!
  "Builds a JAR library.

  Takes the following options:
  `:project` - project selector for a mono-repo. If not set, runs on all projects in a mono-repo.
  `:version` - explicit version override
  `:target-dir` - where the JAR will be output to
  `:artifact-id` - the name of the artifact used in the JAR
  `:group-id` - the name of the group used in the JAR"
  ([] (jar! {}))
  ([opts]
   (run-on-projects
     {:target :jar}
     opts
     (fn [deps-edn opts]
       (let [qualified-sym (config/qualified-symbol deps-edn opts)
             jar-name      (config/jar-name deps-edn opts)]
         (println "Building JAR for" qualified-sym)
         (badigeon.jar/jar qualified-sym {:mvn/version (:version opts)}
                           {:out-path                (path/resolve (:target-dir opts) jar-name)
                            :deps                    (:deps deps-edn)
                            :paths                   (:paths deps-edn)
                            :allow-all-dependencies? true}))))))


(defn install!
  "Installs a JAR library.

  Takes the following options:
  `:project` - the project selector for a mono-repo. If not set, runs on all projects.
  `:target-dir` - The target directory to source things from.
  `:jar-path` - explicit path to a JAR. Otherwise computed via artifact ID, group ID, and version
  `:version` - explicit version override, otherwise set via git tags
  `:artifact-id` - explicit artifact override, otherwise set via pablo config
  `:group-id` - explicit group override, otherwise set via pablo config
  `:desc` - explicit description override, otherwise set via pablo config "
  ([] (install! {}))
  ([opts]
   (run-on-projects
     {:target :jar}
     opts
     (fn [deps-edn opts]
       (let [qualified-sym (config/qualified-symbol deps-edn opts)
             jar-path      (-> (or (:jar-path opts)
                                   (path/resolve (:target-dir opts) (config/jar-name deps-edn opts)))
                               (fs/file)
                               (str))
             pom-file      (if (fs/exists? "pom.xml")
                             (fs/file "pom.xml")
                             (let [temp-file (fs/temp-file "pom")]
                               (spit temp-file (pom/pom deps-edn opts))
                               (fs/file temp-file)))]
         (println "Installing" qualified-sym)
         (badigeon.install/install qualified-sym {:mvn/version (:version opts)}
                                   jar-path
                                   pom-file))))))


(defn extract!
  "Extracts a mono-repo project.

  Takes the following options:
  `:project` - the project selector for a mono-repo. If not set runs on all projects.
  `:target-dir` - The target directory to source things from.
  `:version` - explicit version override, otherwise set via git tags
  `:artifact-id` - explicit artifact override, otherwise set via pablo config
  `:group-id` - explicit group override, otherwise set via pablo config
  `:desc` - explicit description override, otherwise set via pablo config"
  ([] (extract! {}))
  ([opts]
   (let [root-deps-edn (read-deps-edn)]
     (run-on-projects
       {:skip-cwd true}
       opts
       (fn [deps-edn opts]
         (let [qualified-sym (config/qualified-symbol deps-edn)
               artifact-id   (:artifact-id (config/config deps-edn))]
           (println "Extracting project" qualified-sym)
           (mono/extract-project! root-deps-edn artifact-id opts)))))))

(defn publish!
  "Publishes a build artifact to a maven repository.

  Takes the following options:

  `:project` - the project to publish (if a monorepo)
  `:target-dir` - The target directory to source things from.
  `:jar-path` - override of the path of the JAR
  `:artifact-id` - explicit artifact override, otherwise set via pablo config
  `:group-id` - explicit group override, otherwise set via pablo config
  `:desc` - explicit description override, otherwise set via pablo config
  `:version` - explicit version override, otherwise set via git tags"
  ([] (publish! {}))
  ([opts]
   (run-on-projects
     {:target :jar}
     opts
     (fn [deps-edn opts]
       (let [qualified-sym (config/qualified-symbol deps-edn)
             pom-file      (let [temp-file (fs/temp-file "pom")]
                             (spit temp-file (pom/pom deps-edn opts))
                             (str temp-file))
             jar-path      (-> (or (:jar-path opts)
                                   (path/resolve (:target-dir opts) (config/jar-name deps-edn opts)))
                               (fs/file)
                               (str))
             artifacts     [{:file-path pom-file :extension "pom"}
                            {:file-path jar-path :extension "jar"}]
             version       (:version opts)
             snapshot?     (re-find #"SNAPSHOT$" version)]
         (println "Publishing project" qualified-sym)
         (badigeon.deploy/deploy qualified-sym
                                 version
                                 (if snapshot?
                                   artifacts
                                   (badigeon.sign/sign artifacts))
                                 {:id  "clojars"
                                  :url "https://clojars.org/repo"}
                                 opts))))))


(comment
  (utils/with-cwd "/home/ryan/dev/teknql/wing"
    (let [opts {:project     'core
                :credentials {}}]
      (extract! opts)
      (jar! opts)
      (publish! opts))))
