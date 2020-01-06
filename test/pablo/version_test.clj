(ns pablo.version-test
  (:require [pablo.version :as sut]
            [clojure.test :refer [deftest testing is]]))


(deftest from-tags-test
  (testing "returns 0.0.1 if no semvar tag exists"
    (is (= "0.0.1" (sut/from-tags [] false))))

  (testing "adds a snapshot if instructed to"
    (is (= "0.0.1-SNAPSHOT" (sut/from-tags [] true))))

  (testing "finds semvar tags"
    (is (= "1.0.0"
           (sut/from-tags ["v0.15.5" "v1.0.0"] false)))))
