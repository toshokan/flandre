(ns flandre.contract-test
  (:require [flandre.contract]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defspec max-len-validates-len
  (let [length (gen/generate (gen/choose 10 50))
        f (@#'flandre.contract/max-len length)]
    (prop/for-all [s (gen/resize length gen/string-alphanumeric)]
                  (f s))))

(defspec between-validates-range
  (let [mn (gen/generate (gen/choose 10 50))
        mx (gen/generate (gen/choose 70 100))
        f (@#'flandre.contract/between (dec mn) (inc mx))]
    (prop/for-all [s (gen/choose mn mx)]
                  (f s))))

(deftest valid-url-validates-url
  (let [f @#'flandre.contract/valid-url?]
    (is (true? (f "http://example.com")))
    (is (true? (f "https://example.com/path?query=value")))
    (is (nil?  (f "no-scheme.com")))))
