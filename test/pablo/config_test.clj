(ns pablo.config-test
  (:require [pablo.config :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest qualified-symbol-test
  (testing "extracts the qualified symbol"
    (is (= 'teknql/sample
           (sut/qualified-symbol
            {sut/key
             {:artifact-id "sample" :group-id "teknql"}}))))

  (testing "respects overrides"
    (is (= 'my-group/override
           (sut/qualified-symbol
            {sut/key
             {:artifact-id "sample" :group-id "teknql"}}
            {:artifact-id "override" :group-id "my-group"})))))
