(ns meteomax.data.store
  "Cached data access layer for weather data and user preferences."
  (:require [clojure.core.memoize :as memo]
            [clojure.core.cache :as cache]
            [meteomax.data.meteo-api :as meteo-api]
            [meteomax.data.sql :as sql]
            [taoensso.telemere :as log]))

;; Station data cache (2 min TTL)
(defn- cached-stations
  "Fetch stations with caching."
  [config lat lon & opts]
  (apply meteo-api/get-active-stations config lat lon opts))

(def get-stations-near
  "Get stations near location with 2-minute TTL cache."
  (memo/ttl cached-stations :ttl/threshold 120000))

;; User location cache (2 hours TTL)
(defonce user-locations
  (atom (cache/ttl-cache-factory {} :ttl 7200000)))

(defn get-user-location
  "Get user's cached location or from database."
  [db chat-id]
  (let [loc (when (cache/has? @user-locations chat-id)
              (-> @user-locations (cache/lookup chat-id)))]
    (or loc
        (when-let [db-loc (sql/get-user-location db {:chat-id chat-id})]
          (swap! user-locations cache/cache chat-id db-loc)
          db-loc))))

(defn set-user-location!
  "Cache user location and persist to database."
  [db chat-id latitude longitude]
  (let [loc {:latitude latitude :longitude longitude}]
    (swap! user-locations cache/cache chat-id loc)
    (sql/upsert-user-location db {:chat-id chat-id
                                  :latitude latitude
                                  :longitude longitude})
    loc))

(defn get-favorites
  "Get user's favorite stations."
  [db chat-id]
  (map :station_name (sql/get-favorite-stations db {:chat-id chat-id})))

(defn add-favorite!
  "Add station to user's favorites."
  [db chat-id station-name]
  (sql/add-favorite-station db {:chat-id chat-id :station-name station-name}))

(defn remove-favorite!
  "Remove station from user's favorites."
  [db chat-id station-name]
  (sql/remove-favorite-station db {:chat-id chat-id :station-name station-name}))

(defn get-user-subs
  "Get user's subscriptions."
  [db chat-id]
  (sql/get-subscriptions db {:chat-id chat-id}))

(defn get-all-active-subs
  "Get all active subscriptions for sender."
  [db]
  (sql/get-active-subscriptions db {}))

(defn create-sub!
  "Create new subscription."
  [db chat-id station-name time-str days-of-week]
  (sql/insert-subscription db {:chat-id chat-id
                               :station-name station-name
                               :time-str time-str
                               :days-of-week days-of-week}))

(defn update-sub!
  "Update subscription."
  [db id station-name time-str days-of-week active]
  (sql/update-subscription db {:id id
                               :station-name station-name
                               :time-str time-str
                               :days-of-week days-of-week
                               :active active}))

(defn delete-sub!
  "Delete subscription."
  [db id chat-id]
  (sql/delete-subscription db {:id id :chat-id chat-id}))

(defn ensure-user!
  "Ensure user exists in database."
  [db chat-id username first-name]
  (sql/upsert-user db {:chat-id chat-id
                       :username (or username "")
                       :first-name (or first-name "")}))

(comment
  ;; Example usage:
  (require '[maxbot.data.pg :as pg])
  
  (def db (pg/make-pool "postgresql://localhost/maxbot"))
  (def config {:meteo-api-url "https://angara.net/meteo/api"
               :meteo-api-auth "Bearer test"
               :meteo-api-timeout 5000})
  
  ;; Get stations near location (cached):
  (get-stations-near config 52.28 104.28 :last-hours 24)
  
  ;; Manage user location:
  (set-user-location! db "123" 52.28 104.28)
  (get-user-location db "123")
  
  ;; Manage favorites:
  (add-favorite! db "123" "uiii")
  (get-favorites db "123")
  
  ;; Manage subscriptions:
  (create-sub! db "123" "uiii" "08:00" 127)
  (get-user-subs db "123")
  
  (pg/close-pool db))
