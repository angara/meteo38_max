(ns meteomax.meteo-data.core
  "HTTP client for meteo_data API.
   API documentation: https://github.com/angara/meteo_data"
  (:require [org.httpkit.client :as http]
            [jsonista.core :as json]
            [clojure.core.memoize :as memo]
            [clojure.string :as str]
            [taoensso.telemere :refer [log!]]))


(defn- fetch-json [url auth timeout]
  (let [opts {:headers {"Authorization" auth}
              :timeout timeout}]
    (log! {:level :debug
           :id :meteo-api/request
           :msg "Meteo API request"
           :data {:url url}})
    (try
      (let [{:keys [status body]} @(http/get url opts)]
        (if (= 200 status)
          (json/read-value body json/keyword-keys-object-mapper)
          (do
            (log! {:level :warn
                   :id :meteo-api/response-error
                   :msg "Meteo API error"
                   :data {:url url :status status :body body}})
            nil)))
      (catch Exception e
        (log! {:level :error
               :id :meteo-api/request-exception
               :msg "Meteo API exception"
               :data {:url url :error (ex-message e)}})
        nil))))


(defn- url-encode [s]
  (java.net.URLEncoder/encode (str s) "UTF-8"))


(defn- active-stations-url [base-url lat lon {:keys [last-hours search]}]
  (let [params (cond-> [["lat" lat] ["lon" lon]]
                 last-hours (conj ["last-hours" last-hours])
                 search (conj ["search" search]))]
    (str base-url "/active-stations?"
         (str/join "&" (map (fn [[k v]] (str k "=" (url-encode v))) params)))))


(defn- station-info-url [base-url station-name]
  (str base-url "/station-info?st=" (url-encode station-name)))


(defn- fetch-active-stations* [base-url auth timeout lat lon opts]
  (fetch-json (active-stations-url base-url lat lon opts) auth timeout))


(defn- fetch-station-info* [base-url auth timeout station-name]
  (fetch-json (station-info-url base-url station-name) auth timeout))


(def ^:private fetch-active-stations
  (memo/ttl fetch-active-stations* :ttl/threshold 120000))


(def ^:private fetch-station-info
  (memo/ttl fetch-station-info* :ttl/threshold 120000))


(defn get-active-stations
  "Get active weather stations near location.

   Returns: {:stations [{:st, :title, :lat, :lon, :distance, :last, ...}]}"
  [config lat lon & {:keys [last-hours search]}]
  (fetch-active-stations
   (:meteo-api-url config)
   (:meteo-api-auth config)
   (:meteo-api-timeout config)
   lat lon
   (cond-> {}
     last-hours (assoc :last-hours last-hours)
     search (assoc :search search))))


(defn get-station-info
  "Get detailed information about a weather station.

   Returns: {:st, :title, :lat, :lon, :last {:t, :p, :w, :g, :b, ...}}"
  [config station-name]
  (fetch-station-info
   (:meteo-api-url config)
   (:meteo-api-auth config)
   (:meteo-api-timeout config)
   station-name))
