(ns pablo.pom
  "Namespace for generating POM files"
  (:require [pablo.config :as cfg]
            [pablo.version :as version]
            [clojure.string :as str]
            [hiccup.core]))

(defn- pp-xml
  "Pretty prints the provided hiccup xml"
  [hiccup-xml]
  (let [xml         (hiccup.core/html hiccup-xml)
        in          (javax.xml.transform.stream.StreamSource.
                      (java.io.StringReader. xml))
        writer      (java.io.StringWriter.)
        out         (javax.xml.transform.stream.StreamResult. writer)
        transformer (.newTransformer
                      (javax.xml.transform.TransformerFactory/newInstance))]
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/INDENT "yes")
    (.setOutputProperty transformer
                        "{http://xml.apache.org/xslt}indent-amount" "2")
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/METHOD "xml")
    (.transform transformer in out)
    (-> out .getWriter .toString)))

(defn pom
  "Returns an XML string representing the pom.xml file for the provided `deps-edn` map.

  Optionally takes the following options:

  `:version` - explicit version override, otherwise set via git tags
  `:artifact-id` - explicit artifact override, otherwise set via pablo config
  `:group-id` - explicit group override, otherwise set via pablo config
  `:desc` - explicit description override, otherwise set via pablo config"
  [deps-edn {:keys [group-id artifact-id version desc]}]
  (let [cfg (cfg/config deps-edn)]
    (->> [:project
          {"xmlns"     "http://maven.apache.org/POM/4.0.0"
           "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"
           "xsi:schemaLocation"
           "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"}
          ["modelVersion" "4.0.0"]
          ["groupId" (or group-id (:group-id cfg))]
          ["artifactId" (or artifact-id (:artifact-id cfg))]
          ["version" (or version (version/from-git))]
          ["name" (or artifact-id (:artifact-id cfg))]
          ["description" (or desc (:desc cfg))]
          ["dependencies"
           (->> deps-edn
                :deps
                (keep
                  (fn [[dep-name {version :mvn/version}]]
                    (when version
                      (let [parts (-> dep-name str (str/split #"/"))]
                        ["dependency"
                         ["groupId" (first parts)]
                         ["artifactId" (last parts)]
                         ["version" version]])))))]
          ["build"
           (map #(vector "sourceDirectory" %) (:paths deps-edn))]
          ["distributionManagement"
           ["repository"
            ["id" "clojars"]
            ["name" "Clojars Repository"]
            ["url" "https://clojars.org/repo"]]]
          (when-some [github-path (:github cfg)]
            (apply vector "scm"
                   (let [http-url     (str "https://github.com/" github-path)
                         conn-url     (str "scm:git:git://github.com/" github-path ".git")
                         dev-conn-url (str "scm:git:ssh://git@github.com/" github-path ".git")]
                     [["url" http-url]
                      ["connection" conn-url]
                      ["developerConnection" dev-conn-url]
                      ["tag" "HEAD"]])))]
         (hiccup.core/html)
         (pp-xml))))
