(ns meteomax.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [meteomax.config :as config]
            [meteomax.lib.envvar :as env])
  (:import [java.time ZoneId]))


(def ^:private base-env
  {"MAX_API_TOKEN" "token"
   "DATABASE_URL" "postgresql://db"
   "METEO_API_AUTH" "Bearer auth"
   "WEBHOOK_URL" "https://example.com/webhook/maxapi"})


(deftest make-config-derives-webhook-path-from-url
  (with-redefs [env/getenv (fn [name] (get base-env name))]
    (is (= "/webhook/maxapi"
           (:webhook-path (config/make-config))))))


(deftest make-config-prefers-explicit-webhook-path
  (testing "normalizes explicit path without leading slash"
    (with-redefs [env/getenv (fn [name] (get (assoc base-env "WEBHOOK_PATH" "custom/hook") name))]
      (is (= "/custom/hook"
             (:webhook-path (config/make-config)))))))


(deftest make-config-derives-zone-id-from-timezone
  (testing "zone-id is parsed from timezone string"
    (with-redefs [env/getenv (fn [name] (get base-env name))]
      (let [cfg (config/make-config)]
        (is (= (ZoneId/of "Asia/Irkutsk") (:zone-id cfg)))
        (is (instance? ZoneId (:zone-id cfg))))))

  (testing "zone-id respects custom timezone"
    (with-redefs [env/getenv (fn [name] (get (assoc base-env "TIMEZONE" "Europe/Moscow") name))]
      (is (= (ZoneId/of "Europe/Moscow")
             (:zone-id (config/make-config)))))))
