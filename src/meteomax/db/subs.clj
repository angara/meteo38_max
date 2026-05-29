(ns meteomax.db.subs
  "Database queries for weather subscriptions. Assumes a subscriptions table
   with chat_id, station_name, time_str, days_of_week and active columns."
  (:require [pg.core :as pg]))


(defn create-subscription!
  [db chat-id station-name time-str days-of-week]
  (first
   (pg/execute db
               "insert into subs (chat_id, station_name, time_str, days_of_week, active)
                values ($1, $2, $3, $4, true)
                returning *"
               {:params [chat-id station-name time-str days-of-week]})))


(defn get-user-subs
  [db chat-id]
  (pg/execute db
              "select *
               from subs
               where chat_id = $1
               order by time_str, station_name"
              {:params [chat-id]}))


(defn delete-sub!
  [db id]
  (pg/execute db "delete from subs where id = $1" {:params [id]}))


(defn get-sub
  [db id]
  (first (pg/execute db "select * from subs where id = $1" {:params [id]})))


(defn get-user-sub
  [db id chat-id]
  (first (pg/execute db "select * from subs where id = $1 and chat_id = $2" {:params [id chat-id]})))


(defn update-sub!
  [db id time-str days-of-week]
  (first (pg/execute db
                     "update subs
                      set time_str = $2, days_of_week = $3
                      where id = $1
                      returning *"
                     {:params [id time-str days-of-week]})))


(defn get-all-active-subs
  [db]
  (pg/execute db
              "select *
               from subs
               where active = true
               order by time_str, chat_id, station_name"))
