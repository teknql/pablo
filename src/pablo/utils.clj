(ns pablo.utils
  "Misc. Utils for Pablo"
  (:require [pathetic.core :as path]
            [me.raynes.fs :as fs]))

(defmacro with-cwd
  "Macro which attempts to set the cwd for as many places as possible."
  [dir & body]
  `(let [old-path# (System/getProperty "user.dir")
         new-path# (path/normalize (path/resolve old-path# ~dir))]
     (System/setProperty "user.dir" new-path#)
     (binding [fs/*cwd* new-path#]
       (try
         ~@body
         (finally
           (System/setProperty "user.dir" old-path#))))))

(defn path-join
  "Joins multiple paths into a single normalized path"
  [& paths]
  (->> paths
       (interpose "/")
       (apply str)
       (path/normalize)))
