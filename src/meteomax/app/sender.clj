(ns meteomax.app.sender
  "Scheduled subscription sender using Chime."
  (:require [clojure.string :as str]
            [chime.core :as chime]
            [tick.core :as t]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.meteo-data.core :as meteo-api]
            [meteomax.db.subs :as subscriptions]
            [meteomax.db.users :as users]
            [taoensso.telemere :refer [log!]]))


(defn- days-match? [local-now days-of-week]
  (let [day-num (t/int (t/day-of-week local-now))]
    (str/includes? (str days-of-week) (str day-num))))


(defn- time-match? [local-now time-str]
  (let [[sub-hour sub-min] (map #(Integer/parseInt %) (str/split time-str #":"))]
    (and (= (t/hour local-now) sub-hour)
         (= (t/minute local-now) sub-min))))


;;  TamTam/MAX error code when the user stops the bot
(def ^:private bot-stopped-code 909)


(defn- send-subscription [config db sub]
  (try
    (let [station-info (meteo-api/get-station-info config (:station_name sub))
          msg          (fmt/format-station-brief station-info)
          result       (maxapi/send-message (:max-api-token config) (:chat_id sub) msg :format "html")]
      (if (:ok result)
        (log! {:level :info
               :id    :sender/subscription-sent
               :data  {:sub-id (:id sub) :chat-id (:chat_id sub)}})
        (do
          (when (= bot-stopped-code (:error-code result))
            (log! {:level :info :id :sender/user-blocked-bot
                   :msg "User blocked the bot"
                   :data {:chat-id (:chat_id sub)}})
            (users/set-active! db (:chat_id sub) false))
          (log! {:level :warn
                 :id    :sender/subscription-failed
                 :data  {:sub-id (:id sub) :error-code (:error-code result)}}))))
    (catch Exception e
      (log! {:level :error
             :id    :sender/subscription-failed
             :msg   "Failed to send subscription"
             :data  {:sub-id (:id sub) :error (ex-message e)}}))))


(defn check-and-send [config db]
  (let [local-now (t/in (t/now) (:zone-id config))
        subs      (subscriptions/get-all-active-subs db)
        due-subs  (filter #(and (days-match? local-now (:days_of_week %))
                                (time-match? local-now (:time_str %)))
                          subs)]
    (doseq [sub due-subs]
      (send-subscription config db sub))))


(defn start-sender [config db]
  (log! {:level :info :id :sender/start :msg "Starting subscription sender"})
  (let [chime (chime/chime-at
               (chime/periodic-seq (t/now) (t/new-duration 60 :seconds))
               (fn [_instant]
                 (try
                   (check-and-send config db)
                   (catch Exception e
                     (log! {:level :error
                            :id    :sender/error
                            :msg   "Sender error"
                            :data  {:error (ex-message e)}})))))]
    {:chime chime}))


(defn stop-sender [sender]
  (log! {:level :info :id :sender/stop :msg "Stopping subscription sender"})
  (.close ^java.io.Closeable (:chime sender)))

