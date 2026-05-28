(ns meteomax.app.sender
  "Scheduled subscription sender using Chime."
  (:require [clojure.string :as str]
            [chime.core :as chime]
            [clojure.core.async :as async]
            [tick.core :as t]
            [meteomax.app.fmt :as fmt]
            [meteomax.app.maxapi :as maxapi]
            [meteomax.app.meteo-api :as meteo-api]
            [meteomax.db.subscriptions :as subscriptions]
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
      (log! :info {:msg "Subscription sent"
                   :sub-id (:id sub)
                   :chat-id (:chat_id sub)}))
    (catch Exception e
      (log! :error {:msg "Failed to send subscription"
                    :sub-id (:id sub)
                    :error (ex-message e)}))))


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
  (log! :info {:msg "Starting subscription sender"})
  (let [ch (async/chan)]
    (chime/chime-at
     (chime/periodic-seq (t/now) (t/new-duration 60 :seconds))
     (fn [_instant]
        (try
          (check-and-send config db)
          (catch Exception e
            (log! :error {:msg "Sender error" :error (ex-message e)})))))
    {:channel ch
     :running true}))


(defn stop-sender
  "Stop subscription sender."
  [sender]
  (log! :info {:msg "Stopping subscription sender"})
  (async/close! (:channel sender))
  (alter-meta! sender assoc :running false))


(comment
  ;; Test sender:
  (def config {:max-api-token "test"
               :meteo-api-url "https://angara.net/meteo/api"
               :meteo-api-auth "Bearer test"
               :meteo-api-timeout 5000})

  ;; Start sender:
  (def s (start-sender config nil))

  ;; Stop sender:
  (stop-sender s))
