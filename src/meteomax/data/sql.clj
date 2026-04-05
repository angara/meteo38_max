(ns meteomax.data.sql
  "HugSQL function declarations for database access."
  (:require [hugsql.core :as hug]))

(hug/def-db-fns "meteomax/data/api.sql")

(comment
  ;; After loading, these functions are available:
  ;; All functions take a db connection as first arg

  ;; User operations:
  ;; (get-user db {:chat-id "123"})
  ;; (upsert-user db {:chat-id "123" :username "test" :first_name "Test"})

  ;; Preferences:
  ;; (get-user-preferences db {:chat-id "123"})
  ;; (set-user-preference db {:chat-id "123" :key "lang" :value "ru"})

  ;; Favorites:
  ;; (get-favorite-stations db {:chat-id "123"})
  ;; (add-favorite-station db {:chat-id "123" :station-name "uiii"})
  ;; (remove-favorite-station db {:chat-id "123" :station-name "uiii"})

  ;; Subscriptions:
  ;; (get-subscriptions db {:chat-id "123"})
  ;; (get-active-subscriptions db {})
  ;; (insert-subscription db {:chat-id "123" :station-name "uiii"
  ;;                          :time-str "08:00" :days-of-week 127})
  ;; (update-subscription db {:id 1 :active false})
  ;; (delete-subscription db {:id 1 :chat-id "123"})

  ;; Locations:
  ;; (get-user-location db {:chat-id "123"})
  ;; (upsert-user-location db {:chat-id "123" :latitude 52.28 :longitude 104.28})
)
