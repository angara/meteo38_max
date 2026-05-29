(ns meteomax.metrics.reg
  "Prometheus metric definitions and hook registration."
  (:require [iapetos.core :as prom]))

(defonce registry
  (atom (prom/collector-registry)))

(defonce messages-processed
  (prom/counter
   :meteomax/messages_processed_total
   {:description "Total number of messages processed"
    :labels [:chat-type]}))

(defonce api-calls-total
  (prom/counter
   :meteomax/meteo_api_calls_total
   {:description "Total number of meteo API calls"
    :labels [:endpoint :status]}))

(defonce api-latency
  (prom/histogram
   :meteomax/meteo_api_latency_seconds
   {:description "Meteo API request latency in seconds"
    :labels [:endpoint]}))

(defonce subscriptions-sent
  (prom/counter
   :meteomax/subscriptions_sent_total
   {:description "Total number of subscription notifications sent"}))

(defonce errors-total
  (prom/counter
   :meteomax/errors_total
   {:description "Total number of errors"
    :labels [:type]}))

(defn register-metrics
  []
  (reset! registry
          (-> (prom/collector-registry)
              (prom/register messages-processed)
              (prom/register api-calls-total)
              (prom/register api-latency)
              (prom/register subscriptions-sent)
              (prom/register errors-total))))

(defn inc-messages
  [chat-type]
  (prom/inc @registry :meteomax/messages_processed_total {:chat-type chat-type}))

(defn inc-api-call
  [endpoint status]
  (prom/inc @registry :meteomax/meteo_api_calls_total {:endpoint endpoint :status status}))

(defn observe-api-latency
  [endpoint seconds]
  (prom/observe @registry :meteomax/meteo_api_latency_seconds {:endpoint endpoint} seconds))

(defn inc-subscriptions-sent
  []
  (prom/inc @registry :meteomax/subscriptions_sent_total))

(defn inc-error
  [type]
  (prom/inc @registry :meteomax/errors_total {:type type}))

(comment
  (register-metrics)
  (inc-messages "private")
  (inc-api-call "active-stations" 200)
  (observe-api-latency "station-info" 0.5)
  (inc-subscriptions-sent)
  (inc-error "api-timeout"))