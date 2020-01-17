(ns pablo.mono
  "Utilitites for working with mono-repos"
  (:require [me.raynes.fs :as fs]
            [pathetic.core :as path]
            [clojure.java.io :as io]
            [pablo.utils :as utils]
            [pablo.config :as cfg]
            [pablo.pom :as pom]
            [clojure.string :as string]))

(defn projects
  "Returns a list of project symbols for the provided mono-repo"
  [deps-edn]
  (some-> (cfg/config deps-edn)
          :projects
          keys))

(defn project-files
  "Returns a list of files that are included in the project."
  [deps-edn project]
  (let [project-sym (symbol project)
        cfg         (cfg/config deps-edn)
        project-cfg (some-> cfg :projects project-sym)]
    (if-some [explicit-files (:files project-cfg)]
      explicit-files
      (let [group-id   (-> (or (:group-id project-cfg)
                               (:group-id cfg))
                           (string/replace "-" "_"))
            auto-paths (->> (for [path   (:paths deps-edn)
                                  suffix [".clj" ".cljs" ".cljc" "/"]]
                              (utils/path-join fs/*cwd* path group-id (str project-sym suffix)))
                            (into #{}))]
        (->> auto-paths
             (filter fs/exists?)
             (into #{}))))))

(defn project-deps
  "Generates a deps.edn map for the provided `project` using the provided source `deps-edn`"
  [deps-edn project opts]
  (let [project      (symbol project)
        cfg          (cfg/config deps-edn)
        project-cfgs (:projects cfg)
        project-cfg  (get project-cfgs project)
        local-deps   (->> (:local-deps project-cfg)
                          (map (fn [s]
                                 (let [group-id (or (get-in project-cfgs [s :group-id])
                                                    (:group-id opts)
                                                    (:group-id cfg))]
                                   [(symbol (str group-id "/" s))
                                    {:mvn/version (:version opts)}])))
                          (into {}))]
    (-> deps-edn
        (update :deps (fnil select-keys {}) (:deps project-cfg))
        (update :deps merge local-deps)
        (update cfg/key dissoc :projects)
        (update cfg/key assoc :artifact-id project)
        (update cfg/key merge (dissoc project-cfg :deps :local-deps)))))

(defn extract-project!
  "Extracts a given project using the source definitons in `deps-edn`.

  Allows a map of explicit option overrides:

  `:target-dir` - the parent directory to extract the project into
  Plus all of the options in `pablo.pom/pom` for the generated pom.xml "
  [deps-edn project {:keys [target-dir] :as opts}]
  (let [project-root (utils/path-join target-dir project)]
    (fs/delete-dir project-root)
    (fs/mkdirs project-root)
    (doseq [file (project-files deps-edn project)]
      (let [relative-path (path/relativize fs/*cwd* file)
            tgt-path      (utils/path-join project-root relative-path)]
        (fs/mkdirs (fs/parent tgt-path))
        (if (fs/directory? file)
          (fs/copy-dir file tgt-path)
          (fs/copy file tgt-path))))
    (let [deps-path        (utils/path-join project-root "deps.edn")
          pom-path         (utils/path-join project-root "pom.xml")
          project-deps-edn (project-deps deps-edn project opts)]
      (clojure.pprint/pprint project-deps-edn (io/writer deps-path))
      (spit (io/writer (fs/file pom-path))
            (pom/pom project-deps-edn opts)))))
