(ns meteomax.metrics.reg
  "Prometheus metric definitions and hook registration."
  (:require [iapetos.core :as prom]
            [iapetos.collector :as collector]))

(defonce registry
  "Global Prometheus registry."
  (atom (prom/registry)))

(def messages-processed
  "Counter for total messages processed."
  (prom/counter
   :meteomax_messages_processed_total
   {:help "Total number of messages processed"
    :label-names [:chat-type]}))

(def api-calls-total
  "Counter for meteo API calls."
  (prom/counter
   :meteomax_meteo_api_calls_total
   {:help "Total number of meteo API calls"
    :label-names [:endpoint :status]}))

(def api-latency
  "Histogram for meteo API latency."
  (prom/histogram
   :meteomax_meteo_api_latency_seconds
   {:help "Meteo API request latency in seconds"
    :label-names [:endpoint]}))

(def subscriptions-sent
  "Counter for subscription notifications sent."
  (prom/counter
   :meteomax_subscriptions_sent_total
   {:help "Total number of subscription notifications sent"}))

(def errors-total
  "Counter for total errors."
  (prom/counter
   :meteomax_errors_total
   {:help "Total number of errors"
    :label-names [:type]}))

(defn register-metrics
  "Register all metrics in the registry."
  []
  (swap! registry
         #(-> %
              (prom/register messages-processed)
              (prom/register api-calls-total)
              (prom/register api-latency)
              (prom/register subscriptions-sent)
              (prom/register errors-total))))

(defn inc-messages
  "Increment messages processed counter."
  [chat-type]
  (prom/inc messages-processed chat-type))

(defn inc-api-call
  "Increment API call counter."
  [endpoint status]
  (prom/inc api-calls-total endpoint status))

(defn observe-api-latency
  "Record API latency."
  [endpoint seconds]
  (prom/observe api-latency endpoint seconds))

(defn inc-subscriptions-sent
  "Increment subscriptions sent counter."
  []
  (prom/inc subscriptions-sent))

(defn inc-error
  "Increment error counter."
  [type]
  (prom/inc errors-total type))

(comment
  ;; Register metrics:
  (register-metrics)
  
  ;; Use metrics:
  (inc-messages "private")
  (inc-api-call "active-stations" 200)
  (observe-api-latency "station-info" 0.5)
  (inc-subscriptions-sent)
  (inc-error "api-timeout")
)
