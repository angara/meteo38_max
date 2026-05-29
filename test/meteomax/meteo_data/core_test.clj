(ns meteomax.meteo-data.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [meteomax.meteo-data.core :as core]))

(def ^:private active-stations-url #'core/active-stations-url)
(def ^:private station-info-url #'core/station-info-url)

(def ^:private base-url "https://angara.net/meteo/api")


(deftest active-stations-url-required-params
  (let [url (active-stations-url base-url 52.28 104.28 {})]
    (is (clojure.string/starts-with? url (str base-url "/active-stations?")))
    (is (clojure.string/includes? url "lat=52.28"))
    (is (clojure.string/includes? url "lon=104.28"))))


(deftest active-stations-url-with-last-hours
  (let [url (active-stations-url base-url 52.28 104.28 {:last-hours 24})]
    (is (clojure.string/includes? url "lat=52.28"))
    (is (clojure.string/includes? url "lon=104.28"))
    (is (clojure.string/includes? url "last-hours=24"))))


(deftest active-stations-url-with-search-ascii
  (let [url (active-stations-url base-url 52.28 104.28 {:search "test"})]
    (is (clojure.string/includes? url "search=test"))))


(deftest active-stations-url-with-search-cyrillic
  (let [url (active-stations-url base-url 52.0 104.0 {:search "Байкал"})]
    (is (clojure.string/includes? url "search="))
    (is (not (clojure.string/includes? url "search=Байкал")))
    (is (clojure.string/includes? url "search=%D0%91%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))


(deftest active-stations-url-with-all-params
  (let [url (active-stations-url base-url 52.28 104.28 {:last-hours 6 :search "Байкал"})]
    (is (clojure.string/includes? url "lat=52.28"))
    (is (clojure.string/includes? url "lon=104.28"))
    (is (clojure.string/includes? url "last-hours=6"))
    (is (clojure.string/includes? url "search=%D0%91%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))


(deftest station-info-url-test
  (testing "builds correct url"
    (is (= (str base-url "/station-info?st=uiii")
           (station-info-url base-url "uiii"))))

  (testing "url-encodes station name"
    (is (clojure.string/includes?
         (station-info-url base-url "байкал")
         "st=%D0%B1%D0%B0%D0%B9%D0%BA%D0%B0%D0%BB"))))
