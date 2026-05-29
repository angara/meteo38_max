(ns meteomax.db.pg
  "PostgreSQL connection pool management using pg2."
  (:require [clojure.string :as str]
            [mount.core :as mount]
            [pg.core :as pg]
            [taoensso.telemere :refer [log!]]))


(defn- redact-uri
  "Remove credentials from database connection URI before logging."
  [url]
  (some-> url
          (str/replace #"(?i)(//[^:/?#@]+):([^@/?#]*)@" "$1:***@")
          (str/replace #"(?i)(password=)[^&]*" "$1***")))


(defn make-pool
  "Create PostgreSQL connection pool.

   Parameters:
   - url: PostgreSQL connection URI (e.g. 'postgresql://user:pass@localhost/db')"
  [url]
  (log! {:level :info
         :id :db.pg/create-pool
         :msg "creating PostgreSQL connection pool"
         :data {:url (redact-uri url)}})
  (pg/pool url))


(defn close-pool
  "Close PostgreSQL connection pool."
  [pool]
  (log! {:level :info
         :id :db.pg/close-pool
         :msg "closing PostgreSQL connection pool"})
  (pg/close pool))


(mount/defstate conn
  :start (make-pool (:database-url (mount/args)))
  :stop (close-pool conn))
