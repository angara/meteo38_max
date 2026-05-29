(ns meteomax.db.subs
  "Database queries for weather subscriptions. Assumes a subscriptions table
   with chat_id, station_name, time_str, days_of_week and active columns."
  (:require [pg.core :as pg]))


(defn create-subscription!
  [db chat-id station-name time-str days-of-week]
  (first
   (pg/execute db
               "insert into subscriptions (chat_id, station_name, time_str, days_of_week, active)
                values ($1, $2, $3, $4, true)
                returning *"
               {:params [chat-id station-name time-str days-of-week]})))


(defn get-user-subs
  [db chat-id]
  (pg/execute db
              "select *
               from subscriptions
               where chat_id = $1
               order by time_str, station_name"
              {:params [chat-id]}))


(defn get-all-active-subs
  [db]
  (pg/execute db
              "select *
               from subscriptions
               where active = true
               order by time_str, chat_id, station_name"))
