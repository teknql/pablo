(ns pablo.version
  "Namespace for detecting versioning"
  (:require [clj-jgit.porcelain :as git]
            [clj-jgit.querying :as git.query]
            [me.raynes.fs :as fs]))

(defn from-tags
  "Pure function to compute the version from a list of tags and whether the repo is dirty.

  Will assume tags are chronologically sorted"
  [tags snapshot?]
  (let [is-semvar? #(re-find #"\d+\.\d+\.\d+" %)
        version    (or (->> tags
                            (keep is-semvar?)
                            (last))
                       "0.0.1")]
    (str version (when snapshot? "-SNAPSHOT"))))


(defn from-git
  "Scans the git repo to determine the version of the project"
  []
  (git/with-repo (fs/file ".")
    (let [tags   (git/git-tag-list repo)
          dirty? (some? (->> (git/git-status repo)
                             (keep not-empty)))]
      (from-tags tags dirty?))))

(defn last-commit-sha
  "Return a string of the last commit sha of the project"
  []
  (git/with-repo (fs/file ".")
    (->> (git/git-log repo)
         first
         :id
         (git.query/commit-info repo)
         :id)))

(comment
  (from-git)

  (last-commit-sha))
