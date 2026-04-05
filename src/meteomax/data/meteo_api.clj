(ns meteomax.data.meteo-api
  "HTTP client for meteo_data API.
   
   API documentation: https://github.com/angara/meteo_data"
  (:require [org.httpkit.client :as http]
            [jsonista.core :as json]
            [clojure.core.memoize :as memo]
            [taoensso.telemere :as log]))

(defn- fetch-json
  "Make HTTP GET request and parse JSON response."
  [url auth timeout]
  (let [headers {"Authorization" auth}
        opts {:headers headers
              :timeout timeout}]
    (log/log! :debug {:msg "Meteo API request" :url url})
    (try
      (let [{:keys [status body]} @(http/get url opts)]
        (if (= 200 status)
          (json/read-value body json/keyword-keys-object-mapper)
          (do
            (log/log! :warn {:msg "Meteo API error" :url url :status status})
            nil)))
      (catch Exception e
        (log/log! :error {:msg "Meteo API exception" :url url :error (.getMessage e)})
        nil))))

(defn- active-stations-url
  "Build URL for active-stations endpoint."
  [base-url lat lon & {:keys [last-hours search]}]
  (let [params (cond-> ["lat" (str lat) "lon" (str lon)]
                       last-hours (concat ["last-hours" (str last-hours)])
                       search (concat ["search" search]))]
    (str base-url "/active-stations?" (apply str (interpose "&" params)))))

(defn- station-info-url
  "Build URL for station-info endpoint."
  [base-url station-name]
  (str base-url "/station-info?st=" station-name))

(defn- fetch-active-stations*
  "Fetch active stations near location (uncached)."
  [base-url auth timeout lat lon & opts]
  (let [url (apply active-stations-url base-url lat lon opts)]
    (fetch-json url auth timeout)))

(defn- fetch-station-info*
  "Fetch station info (uncached)."
  [base-url auth timeout station-name]
  (let [url (station-info-url base-url station-name)]
    (fetch-json url auth timeout)))

;; Cached versions with 2-minute TTL
(def fetch-active-stations
  "Fetch active stations with 2-minute TTL cache."
  (memo/ttl fetch-active-stations* :ttl/threshold 120000))

(def fetch-station-info
  "Fetch station info with 2-minute TTL cache."
  (memo/ttl fetch-station-info* :ttl/threshold 120000))

(defn get-active-stations
  "Get active weather stations near location.
   
   Parameters:
   - config: Application config map
   - lat: Latitude
   - lon: Longitude
   - :last-hours: Include last N hours of data (1-50)
   - :search: Filter stations by substring
   
   Returns: {:stations [{:st, :title, :lat, :lon, :distance, :last, ...}]}"
  [config lat lon & {:keys [last-hours search]}]
  (apply fetch-active-stations
         (:meteo-api-url config)
         (:meteo-api-auth config)
         (:meteo-api-timeout config)
         lat lon
         (cond-> []
                 last-hours (concat [:last-hours last-hours])
                 search (concat [:search search]))))

(defn get-station-info
  "Get detailed information about a weather station.
   
   Parameters:
   - config: Application config map
   - station-name: Station identifier (e.g. 'uiii')
   
   Returns: {:st, :title, :lat, :lon, :last {:t, :p, :w, :g, :b, ...}}"
  [config station-name]
  (fetch-station-info
   (:meteo-api-url config)
   (:meteo-api-auth config)
   (:meteo-api-timeout config)
   station-name))

(comment
  ;; Example usage:
  (def config {:meteo-api-url "https://angara.net/meteo/api"
               :meteo-api-auth "Bearer test-auth"
               :meteo-api-timeout 5000})
  
  ;; Get stations near Irkutsk:
  (get-active-stations config 52.28 104.28 :last-hours 24)
  ;; => {:stations [{:st "uiii", :title "Irkutsk", :distance 5.2, ...}]}
  
  ;; Get station info:
  (get-station-info config "uiii")
  ;; => {:st "uiii", :title "Irkutsk", :last {:t -20.0, :p 986.0, ...}}
  
  ;; Search stations:
  (get-active-stations config 52.0 104.0 :search "Байкал")
)
