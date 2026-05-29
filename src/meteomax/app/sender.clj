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


(defn- days-match?
  "Check if current day matches subscription days string (1=пн … 7=вс)."
  [days-of-week]
  (let [day-num (case (t/day-of-week (t/today))
                  :monday 1 :tuesday 2 :wednesday 3 :thursday 4
                  :friday 5 :saturday 6 :sunday 7
                  0)]
    (str/includes? (str days-of-week) (str day-num))))


(defn- time-match?
  "Check if current time matches subscription time string."
  [time-str]
  (let [now (t/now)
        [sub-hour sub-min] (map #(Integer/parseInt %) (str/split time-str #":"))]
    (and (= (t/hour now) sub-hour)
         (= (t/minute now) sub-min))))


;;  TamTam/MAX error code when the user stops the bot
(def ^:private bot-stopped-code 909)

(defn- send-subscription
  "Send weather data for a subscription."
  [config db sub]
  (try
    (let [station-info (meteo-api/get-station-info config (:station_name sub))
          msg    (str "🔔 Погода: " (:station_name sub) "\n\n"
                      (fmt/format-station-info station-info))
          result (maxapi/send-message (:max-api-token config) (:chat_id sub) msg)]
      (if (:ok result)
        (log! {:level :info
               :id    :sender/subscription-sent
               :data  {:sub-id (:id sub) :chat-id (:chat_id sub)}})
        (do
          (when (= bot-stopped-code (:error-code result))
            (log! {:level :info :id :sender/user-blocked-bot
                   :msg "User blocked the bot, deactivating"
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


(defn- check-and-send
  "Check all active subscriptions and send matching ones."
  [config db]
  (let [subs (subscriptions/get-all-active-subs db)]
    (doseq [sub subs]
      (when (and (days-match? (:days_of_week sub))
                 (time-match? (:time_str sub)))
        (send-subscription config db sub)))))


(defn start-sender
  "Start subscription sender scheduled job."
  [config db]
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


(defn stop-sender
  "Stop subscription sender."
  [sender]
  (log! {:level :info :id :sender/stop :msg "Stopping subscription sender"})
  (.close ^java.io.Closeable (:chime sender)))

