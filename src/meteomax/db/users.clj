(ns meteomax.db.users
  "Database queries for bot users."
  (:require [pg.core :as pg]))


(defn ensure-user!
  [db chat-id userinfo]
  (first
   (pg/execute db
               "insert into meteomax_users(chat_id, userinfo, active)
                values ($1, $2, true)
                on conflict (chat_id) do update
                set userinfo   = excluded.userinfo,
                    active     = true,
                    updated_at = now()
                returning *"
               {:params [chat-id userinfo]})))


(defn get-user-location
  [db chat-id]
  (when-let [latlon (:last_latlon
                     (first (pg/execute db
                                        "select last_latlon
                                         from meteomax_users
                                         where chat_id = $1"
                                        {:params [chat-id]})))]
    {:latitude (nth latlon 0) :longitude (nth latlon 1)}))


(defn set-user-location!
  [db chat-id latitude longitude]
  (first
   (pg/execute db
               "insert into meteomax_users(chat_id, last_latlon)
                values ($1, $2)
                on conflict (chat_id) do update
                set last_latlon = excluded.last_latlon,
                    updated_at  = now()
                returning last_latlon"
               {:params [chat-id [latitude longitude]]})))


(defn get-favorites
  [db chat-id]
  (or (:favs (first (pg/execute db
                                "select favs
                                 from meteomax_users
                                 where chat_id = $1"
                                {:params [chat-id]})))
      []))


(defn set-active!
  [db chat-id active?]
  (pg/execute db
              "update meteomax_users
               set active = $2, updated_at = now()
               where chat_id = $1
                 and (active is distinct from $2)"
              {:params [chat-id active?]}))


(defn set-favs!
  [db chat-id favs]
  (first
   (pg/execute db
               "insert into meteomax_users(chat_id, favs)
                values ($1, $2)
                on conflict (chat_id) do update
                set favs       = excluded.favs,
                    updated_at = now()
                returning favs"
               {:params [chat-id favs]})))
