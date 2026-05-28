(ns meteomax.lib.random-test
  (:require [clojure.test :refer [deftest is]]
            [meteomax.lib.random :as random]))


(deftest random-string-uses-expected-length
  (is (= 12 (count (random/random-string 12)))))


(deftest webhook-secret-matches-max-constraints
  (let [secret (random/webhook-secret)]
    (is (= 32 (count secret)))
    (is (re-matches #"[A-Za-z0-9_-]{32}" secret))))
