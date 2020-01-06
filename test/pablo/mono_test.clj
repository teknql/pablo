(ns pablo.mono-test
  (:require [pablo.mono :as sut]
            [clojure.test :as t :refer [testing deftest is]]
            [me.raynes.fs :as fs]
            [pathetic.core :as path]))

(defmacro with-temp-dir
  "Helper macro which instantiates a temp directory with the the provided
  file paths. Optionally takes additional let bindings

  ```
  (with-temp-dir [dir-path [\"file1.txt\" \"file2.txt\"]])
  ```"
  [[bind-sym defs & other-bindings] & body]
  `(let [tmp-path# (fs/temp-dir "mono-test")
         ~bind-sym tmp-path#
         ~@other-bindings]
     (binding [fs/*cwd* tmp-path#]
       (doseq [file# ~defs]
         (let [abs-path# (path/normalize (str tmp-path# "/" file#))]
           (fs/mkdirs (fs/parent abs-path#))
           (fs/touch abs-path#)))
       (try
         ~@body
         (finally (fs/delete-dir ~bind-sym))))))

(deftest project-files-test
  (let [base-deps '{:paths ["src" "test"]
                    :pablo/config
                    {:group-id sample
                     :projects {core {}}}}]
    (testing "auto-resolution using paths + project symbol"
      (with-temp-dir
        [path ["/src/sample/core.clj"
               "/test/sample/core.clj"
               "/test/sample/core/something.clj"
               "/test/sample/something.clj"]]
        (is (= #{(str path "/src/sample/core.clj")
                 (str path "/test/sample/core.clj")
                 (str path "/test/sample/core")}
               (sut/project-files base-deps 'core)))))))
