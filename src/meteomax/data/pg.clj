(ns meteomax.data.pg
  "PostgreSQL connection pool management using pg2."
  (:require [pg.core :as pg]
            [taoensso.telemere :as log]))

(defn make-pool
  "Create PostgreSQL connection pool.
   
   Parameters:
   - url: PostgreSQL connection URI (e.g. 'postgresql://user:pass@localhost/db')"
  [url]
  (log/log! :info {:msg "Creating PostgreSQL connection pool" :url url})
  (pg/make-connection-pool {:jdbc-url url}))

(defn close-pool
  "Close PostgreSQL connection pool."
  [pool]
  (log/log! :info {:msg "Closing PostgreSQL connection pool"})
  (pg/close-connection-pool pool))

(defmacro with-transaction
  "Execute body within a database transaction.
   
   Usage:
   (with-transaction [tx pool]
     (db-fn tx ...))"
  [[tx-sym pool] & body]
  `(pg/with-transaction [~tx-sym ~pool]
     ~@body))

(comment
  ;; Example usage:
  (def pool (make-pool "postgresql://localhost/maxbot"))
  
  ;; Execute query:
  (require '[maxbot.data.sql :as sql])
  (sql/get-user pool {:chat-id "123"})
  
  ;; Execute in transaction:
  (with-transaction [tx pool]
    (sql/insert-user tx {:chat-id "123" :username "test"})
    (sql/insert-preference tx {:chat-id "123" :key "lang" :value "ru"}))
  
  ;; Close pool:
  (close-pool pool))
