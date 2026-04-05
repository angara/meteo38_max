(ns meteomax.main
  (:require [mount.core :as mount]
            [meteomax.config :as config]
            [meteomax.data.pg :as pg]
            [meteomax.app.serv :as serv]
            [meteomax.app.sender :as sender]
            [meteomax.metrics.export :as export]
            [taoensso.telemere :as log])
  (:gen-class))

(mount/defstate config
  :start (config/make-config)
  :stop nil)

(mount/defstate dbc
  :start (pg/make-pool (:database-url config))
  :stop (pg/close-pool dbc))

(mount/defstate poller
  :start (serv/start-poller config dbc)
  :stop (serv/stop-poller poller))

(mount/defstate sender-proc
  :start (sender/start-sender config dbc)
  :stop (sender/stop-sender sender-proc))

(mount/defstate metrics-endpoint
  :start (export/start-metrics-server config)
  :stop (export/stop-metrics-server metrics-endpoint))

(defn -main
  "Application entry point."
  [& args]
  (let [build-info (try
                     (read-string (slurp "build-info.edn"))
                     (catch Exception _ {}))]
    (log/log! :info {:msg "Starting MAX Weather Bot"
                     :version (:version build-info "unknown")
                     :build-time (:build-time build-info "unknown")})
    (try
      (mount/start-with-args {:config config})
      (log/log! :info {:msg "MAX Weather Bot started successfully"})
      (catch Exception e
        (log/log! :error {:msg "Failed to start MAX Weather Bot"
                          :error (.getMessage e)})
        (System/exit 1)))))

(comment
  ;; Start in REPL:
  (-main)
  
  ;; Stop all:
  (mount/stop)
  
  ;; Restart specific component:
  (mount/stop #'poller)
  (mount/start #'poller)
)
