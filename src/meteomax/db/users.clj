(ns meteomax.db.users
  "Database queries for bot users. Assumes a users table with profile,
   location and favorites columns."
  (:require [pg.core :as pg]))


(defn ensure-user!
  [db chat-id username first-name]
  (first
   (pg/execute db
               "insert into users (chat_id, username, first_name)
                values ($1, $2, $3)
                on conflict (chat_id) do update
                set username = excluded.username,
                    first_name = excluded.first_name
                returning *"
               {:params [chat-id username first-name]})))


(defn get-user-location
  [db chat-id]
  (let [row (first
             (pg/execute db
                         "select latitude, longitude
                          from users
                          where chat_id = $1"
                         {:params [chat-id]}))]
    (when (and (:latitude row) (:longitude row))
      row)))


(defn set-user-location!
  [db chat-id latitude longitude]
  (first
   (pg/execute db
               "insert into users (chat_id, latitude, longitude)
                values ($1, $2, $3)
                on conflict (chat_id) do update
                set latitude = excluded.latitude,
                    longitude = excluded.longitude
                returning latitude, longitude"
               {:params [chat-id latitude longitude]})))


(defn get-favorites
  [db chat-id]
  (or (:favorites
       (first
        (pg/execute db
                    "select favorites
                     from users
                     where chat_id = $1"
                    {:params [chat-id]})))
      []))


(defn add-favorite!
  [db chat-id station-name]
  (first
   (pg/execute db
               "insert into users (chat_id, favorites)
                values ($1, array[$2]::text[])
                on conflict (chat_id) do update
                set favorites = case
                                  when $2 = any(coalesce(users.favorites, '{}'::text[]))
                                    then coalesce(users.favorites, '{}'::text[])
                                  else array_append(coalesce(users.favorites, '{}'::text[]), $2)
                                end
                returning favorites"
               {:params [chat-id station-name]})))
