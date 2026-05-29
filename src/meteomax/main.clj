(ns meteomax.main
  (:require [mount.core :as mount]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.app.webhook]
            [meteomax.db.pg :as pg]
            [meteomax.config :as config]
            [meteomax.metrics.export :as export]
            [meteomax.app.sender :as sender]
            [taoensso.telemere :refer [log!]])
  (:gen-class))


(mount/defstate metrics-endpoint
  :start (export/start-metrics-server (mount/args))
  :stop (export/stop-metrics-server metrics-endpoint))


(mount/defstate sender-proc
  :start (sender/start-sender (mount/args) pg/conn)
  :stop (sender/stop-sender sender-proc))


(mount/defstate bot-info
  :start (let [bot (maxapi/get-me (:max-api-token (mount/args)))]
           (if bot
             (do
               (log! {:id :main/bot-info :data (dissoc bot :full_avatar_url :avatar_url)})
               bot)
             (do
               (log! :warn {:id :main/bot-info-failed :msg "get-me failed"})
               nil))))


(def ^:private bot-commands
  [{:name "/help"  :description "Список команд"}
   {:name "/favs"  :description "Избранные станции"}
   {:name "/subs"  :description "Подписки на уведомления"}])


(mount/defstate bot-commands-registered
  :start (let [token (:max-api-token (mount/args))]
           (if (maxapi/set-commands token bot-commands)
             (log! {:id :main/bot-commands-set :msg "Bot commands registered"})
             (log! {:level :warn :id :main/bot-commands-failed :msg "Failed to register bot commands"}))))


(defn -main []
  (let [build-info @config/build-info]
    (log! {:id :main/start :data build-info})
    (try
      (let [{started :started} (mount/start-with-args (config/make-config))]
        (log! {:msg "Bot started successfully" :data started}))
      (catch Exception e
        (log! :error {:msg "Failed to start Bot" :error (ex-message e)})
        (System/exit 1)))))
