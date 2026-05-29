(ns meteomax.meteo-data.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [meteomax.meteo-data.core :as core]))

(def ^:private active-stations-url #'core/active-stations-url)
(def ^:private station-info-url #'core/station-info-url)

(def ^:private base-url "https://angara.net/meteo/api")


(deftest active-stations-url-with-latlon
  (let [url (active-stations-url base-url {:lat 52.28 :lon 104.28})]
    (is (str/starts-with? url (str base-url "/active-stations?")))
    (is (str/includes? url "lat=52.28"))
    (is (str/includes? url "lon=104.28"))))


(deftest active-stations-url-with-last-hours
  (let [url (active-stations-url base-url {:lat 52.28 :lon 104.28 :last-hours 24})]
    (is (str/includes? url "lat=52.28"))
    (is (str/includes? url "lon=104.28"))
    (is (str/includes? url "last-hours=24"))))


(deftest active-stations-url-search-only
  (let [url (active-stations-url base-url {:search "test" :last-hours 24})]
    (is (str/includes? url "search=test"))
    (is (not (str/includes? url "lat=")))
    (is (not (str/includes? url "lon=")))))


(deftest active-stations-url-with-search-cyrillic
  (let [url (active-stations-url base-url {:search "Байкал"})]
    (is (str/includes? url "search="))
    (is (not (str/includes? url "search=Байкал")))
    (is (str/includes? url "search=%D0%91%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))


(deftest active-stations-url-with-all-params
  (let [url (active-stations-url base-url {:lat 52.28 :lon 104.28 :last-hours 6 :search "Байкал"})]
    (is (str/includes? url "lat=52.28"))
    (is (str/includes? url "lon=104.28"))
    (is (str/includes? url "last-hours=6"))
    (is (str/includes? url "search=%D0%91%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))


(deftest station-info-url-test
  (testing "builds correct url"
    (is (= (str base-url "/station-info?st=uiii")
           (station-info-url base-url "uiii"))))

  (testing "url-encodes station name"
    (is (str/includes?
         (station-info-url base-url "байкал")
         "st=%D0%B1%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))
