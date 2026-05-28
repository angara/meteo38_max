(ns meteomax.lib.envvar-test
  (:require [clojure.test :refer [deftest is testing]]
            [meteomax.lib.envvar :as env]))

(deftest test-env-bool-smoke
  (testing "true-like values are parsed as true"
    (with-redefs [env/getenv {"TEST_VAR" "true"}]
      (is (= true (env/env-bool "TEST_VAR" false))))
    (with-redefs [env/getenv {"TEST_VAR" "TRUE"}]
      (is (= true (env/env-bool "TEST_VAR" false))))
    (with-redefs [env/getenv {"TEST_VAR" "1"}]
      (is (= true (env/env-bool "TEST_VAR" false))))
    (with-redefs [env/getenv {"TEST_VAR" "yes"}]
      (is (= true (env/env-bool "TEST_VAR" false)))))

  (testing "false-like values are parsed as false"
    (with-redefs [env/getenv {"TEST_VAR" "false"}]
      (is (= false (env/env-bool "TEST_VAR" true))))
    (with-redefs [env/getenv {"TEST_VAR" "0"}]
      (is (= false (env/env-bool "TEST_VAR" true))))
    (with-redefs [env/getenv {"TEST_VAR" "no"}]
      (is (= false (env/env-bool "TEST_VAR" true))))
    (with-redefs [env/getenv {"TEST_VAR" ""}]
      (is (= false (env/env-bool "TEST_VAR" true)))))

  (testing "default is returned when var is not set"
    (with-redefs [env/getenv {}]
      (is (= true (env/env-bool "__UNSET_VAR" true)))
      (is (= false (env/env-bool "__UNSET_VAR" false)))))

  (testing "bug: false value does not fall back to default"
    (with-redefs [env/getenv {"DEBUG" "false"}]
      (is (= false (env/env-bool "DEBUG" true))))))
