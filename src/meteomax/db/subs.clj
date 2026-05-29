(ns meteomax.db.subs
  "Database queries for weather subscriptions. Assumes a subscriptions table
   with chat_id, station_name, time_str, days_of_week and active columns."
  (:require [pg.core :as pg]))


(defn create-subscription!
  [db chat-id station-name time-str days-of-week]
  (first
   (pg/execute db
               "insert into meteomax_subs(chat_id, station_name, time_str, days_of_week, active)
                values ($1, $2, $3, $4, true)
                returning *"
               {:params [chat-id station-name time-str days-of-week]})))


(defn get-user-subs
  [db chat-id]
  (pg/execute db
              "select *
               from meteomax_subs
               where chat_id = $1
               order by time_str, station_name"
              {:params [chat-id]}))


(defn delete-sub!
  [db id]
  (pg/execute db "delete from meteomax_subs where id = $1" {:params [id]}))


(defn get-sub
  [db id]
  (first (pg/execute db "select * from meteomax_subs where id = $1" {:params [id]})))


(defn get-user-sub
  [db id chat-id]
  (first (pg/execute db "select * from meteomax_subs where id = $1 and chat_id = $2" {:params [id chat-id]})))


(defn update-sub!
  [db id time-str days-of-week]
  (first (pg/execute db
                     "update meteomax_subs
                      set time_str = $2, days_of_week = $3
                      where id = $1
                      returning *"
                     {:params [id time-str days-of-week]})))


(defn get-all-active-subs
  [db]
  (pg/execute db
              "select s.*
               from meteomax_subs s
               join meteomax_users u on u.chat_id = s.chat_id
               where s.active = true
                 and u.active = true
               order by s.time_str, s.chat_id, s.station_name"))
