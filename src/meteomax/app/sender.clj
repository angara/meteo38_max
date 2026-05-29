(ns meteomax.app.sender
  "Scheduled subscription sender using Chime."
  (:require [clojure.string :as str]
            [chime.core :as chime]
            [tick.core :as t]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.meteo-data.core :as meteo-api]
            [meteomax.db.subs :as subscriptions]
            [taoensso.telemere :refer [log!]]))


(defn- day-bit-index
  [day-bit]
  (int (/ (Math/log day-bit) (Math/log 2))))


(defn- days-match?
  "Check if current day matches subscription days bitmask."
  [days-of-week]
  (let [day-of-week (t/day-of-week (t/today))
        day-bit (case day-of-week
                  :monday 1
                  :tuesday 2
                  :wednesday 4
                  :thursday 8
                  :friday 16
                  :saturday 32
                  :sunday 64
                  0)]
    (bit-test days-of-week (day-bit-index day-bit))))


(defn- time-match?
  "Check if current time matches subscription time string."
  [time-str]
  (let [now (t/now)
        [sub-hour sub-min] (map #(Integer/parseInt %) (str/split time-str #":"))]
    (and (= (t/hour now) sub-hour)
         (= (t/minute now) sub-min))))


(defn- send-subscription
  "Send weather data for a subscription."
  [config _db sub]
  (try
    (let [station-info (meteo-api/get-station-info config (:station_name sub))
          msg (str "🔔 Погода: " (:station_name sub) "\n\n"
                   (fmt/format-station-info station-info))]
      (maxapi/send-message
       (:max-api-token config)
       (:chat_id sub)
       msg)
      (log! {:level :info
             :id    :sender/subscription-sent
             :data  {:sub-id (:id sub) :chat-id (:chat_id sub)}}))
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

