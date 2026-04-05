(ns meteomax.app.sender
  "Scheduled subscription sender using Chime."
  (:require [jarohen.chime :as chime]
            [meteomax.data.store :as store]
            [meteomax.data.meteo-api :as meteo-api]
            [meteomax.app.fmt :as fmt]
            [mlib.max.botapi :as botapi]
            [clojure.core.async :as async]
            [java-time.api :as t]
            [taoensso.telemere :as log]))

(defn- days-match?
  "Check if current day matches subscription days bitmask."
  [days-of-week]
  (let [day-of-week (t/day-of-week (t/local-date))
        day-bit (case day-of-week
                  :monday 1
                  :tuesday 2
                  :wednesday 4
                  :thursday 8
                  :friday 16
                  :saturday 32
                  :sunday 64
                  0)]
    (bit-test days-of-week (int (Math/log day-bit 2)))))

(defn- time-match?
  "Check if current time matches subscription time string."
  [time-str]
  (let [now (t/local-time)
        [sub-hour sub-min] (map Integer/parseInt (clojure.string/split time-str #":"))
        sub-time (t/local-time sub-hour sub-min)]
    (and (= (t/hour now) sub-hour)
         (= (t/minute now) sub-min))))

(defn- send-subscription
  "Send weather data for a subscription."
  [config db sub]
  (try
    (let [station-info (meteo-api/get-station-info config (:station_name sub))
          msg (str "🔔 Погода: " (:station_name sub) "\n\n"
                   (fmt/format-station-info station-info))]
      (botapi/send-message
       (:max-api-token config)
       (:chat_id sub)
       msg)
      (log/log! :info {:msg "Subscription sent"
                       :sub-id (:id sub)
                       :chat-id (:chat_id sub)}))
    (catch Exception e
      (log/log! :error {:msg "Failed to send subscription"
                        :sub-id (:id sub)
                        :error (.getMessage e)}))))

(defn- check-and-send
  "Check all active subscriptions and send matching ones."
  [config db]
  (let [subs (store/get-all-active-subs db)]
    (doseq [sub subs]
      (when (and (days-match? (:days_of_week sub))
                 (time-match? (:time_str sub)))
        (send-subscription config db sub)))))

(defn start-sender
  "Start subscription sender scheduled job."
  [config db]
  (log/log! :info {:msg "Starting subscription sender"})
  (let [ch (async/chan)]
    (chime/chime-at
     (chime/periodic-seq (t/instant) (t/seconds 60))
     (fn [instant]
       (try
         (check-and-send config db)
         (catch Exception e
           (log/log! :error {:msg "Sender error" :error (.getMessage e)})))))
    {:channel ch
     :running true}))

(defn stop-sender
  "Stop subscription sender."
  [sender]
  (log/log! :info {:msg "Stopping subscription sender"})
  (async/close! (:channel sender))
  (alter-meta! sender assoc :running false))

(comment
  ;; Test sender:
  (def config {:max-api-token "test"
               :meteo-api-url "https://angara.net/meteo/api"
               :meteo-api-auth "Bearer test"
               :meteo-api-timeout 5000})
  
  ;; Start sender:
  (def s (start-sender config db))
  
  ;; Stop sender:
  (stop-sender s)
)
