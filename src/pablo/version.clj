(ns pablo.version
  "Namespace for detecting versioning"
  (:require [clj-jgit.porcelain :as git]))

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
  (git/with-repo "."
    (let [tags   (git/git-tag-list repo)
          dirty? (some? (->> (git/git-status repo)
                             (keep not-empty)))]
      (from-tags tags dirty?))))

(comment
  (from-git))
