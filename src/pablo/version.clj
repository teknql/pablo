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
  (try
    (git/with-repo (fs/file ".")
      (let [tags   (git/git-tag-list repo)
            dirty? (seq (->> (git/git-status repo)
                             vals
                             (keep not-empty)))]
        (from-tags tags dirty?)))
    (catch java.io.FileNotFoundException _
      nil)))

(defn last-commit-sha
  "Return a string of the last commit sha of the project"
  []
  (try
    (git/with-repo (fs/file ".")
      (->> (git/git-log repo)
           first
           :id
           (git.query/commit-info repo)
           :id))
    (catch java.io.FileNotFoundException _
      nil)))

(comment
  (last-commit-sha))
