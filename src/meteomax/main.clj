(ns meteomax.main
  (:require [mount.core :as mount]
             [meteomax.app.maxapi :as maxapi]
             [meteomax.config :as config]
             [meteomax.db.pg :as pg]
             [meteomax.app.sender :as sender]
             [meteomax.app.webhook]
             [meteomax.metrics.export :as export]
             [taoensso.telemere :refer [log!]])
  (:gen-class))

(mount/defstate sender-proc
  :start (sender/start-sender (mount/args) pg/conn)
  :stop (sender/stop-sender sender-proc))

(mount/defstate metrics-endpoint
  :start (export/start-metrics-server (mount/args))
  :stop (export/stop-metrics-server metrics-endpoint))


(mount/defstate bot-info
  :start (let [bot (maxapi/get-me (:max-api-token (mount/args)))]
           (if bot
             (do
               (log! :info {:msg "Fetched bot information" :data (dissoc bot :full_avatar_url :avatar_url)})
               bot)
             (do
               (log! :warn {:msg "Failed to fetch bot information"})
               nil))))

(defn -main []
  (let [build-info (try
                     (read-string (slurp "build-info.edn"))
                     (catch Exception _ {}))]
    (log! :info {:msg "Starting MAX Weather Bot"
                 :version (:version build-info "unknown")
                 :build-time (:build-time build-info "unknown")})
    (try
      (mount/start-with-args (config/make-config))
      (log! :info {:msg "MAX Weather Bot started successfully"})
      (catch Exception e
        (log! :error {:msg "Failed to start MAX Weather Bot"
                      :error (ex-message e)})
        (System/exit 1)))))

(comment
  ;; Start in REPL:
  (-main)

  ;; Stop all:
  (mount/stop)

  ;; Restart specific component:
  (mount/stop #'sender-proc)
  (mount/start #'sender-proc))
