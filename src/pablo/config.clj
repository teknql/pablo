(ns pablo.config
  (:refer-clojure :exclude [key])
  (:require [pablo.version :as version]))

(def key
  "The key used to access the pablo config in a deps.edn"
  :pablo/config)

(defn config
  "Extracts the pablo config out of the provided `deps-edn` map."
  [deps-edn]
  (pablo.config/key deps-edn))


(defn qualified-symbol
  "Extracts the qualified symbol for a deps-edn

  Optionally takes the following options:

  `:artifact-id` - explicit artifact ID override
  `:group-id` - explicit group ID override"
  ([deps-edn] (qualified-symbol deps-edn {}))
  ([deps-edn {:keys [artifact-id group-id]}]
   (let [cfg (config deps-edn)]
     (-> (str (or group-id (:group-id cfg))
              "/"
              (or artifact-id (:artifact-id cfg)))
         (symbol)))))


(defn jar-name
  "Returns the implied JAR name for the given `deps-edn` map

  Takes the following options:
  `:artifact-id` - explicit artifact ID override
  `:group-id` - explicit group ID override"
  ([deps-edn] (jar-name deps-edn {}))
  ([deps-edn opts]
   (let [cfg (config deps-edn)]
     (format "%s-%s.jar"
             (or (:artifact-id opts)
                 (:artifact-id cfg))
             (or (:version opts)
                 (version/from-git))))))


(defn targets?
  "Returns whether the config entry for the given `deps-edn` map targets the provided target-keyword.

  If target-keyword is nil, returns true"
  [deps-edn target-keyword]
  (if-not target-keyword
    true
    (let [cfg    (config deps-edn)
          target (:target cfg)]
      (or (= target-keyword target)
          (and (coll? target) (contains? target target-keyword))))))

(defn monorepo?
  "Returns whether the `deps-edn` specifies a monorepo type project"
  [deps-edn]
  (contains? (config deps-edn) :projects))
