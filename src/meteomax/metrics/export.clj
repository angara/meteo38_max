(ns meteomax.metrics.export
  "HTTP metrics endpoint using http-kit."
  (:require [org.httpkit.server :as http-server]
            [iapetos.collector.ring :as prom-ring]
            [meteomax.metrics.reg :as reg]
            [taoensso.telemere :refer [log!]]))

(defn metrics-handler
  [req]
  (if (= (:uri req) "/metrics")
    (prom-ring/metrics-response @reg/registry)
    {:status 404 :body "Not found"}))

(defn start-metrics-server
  [config]
  (let [bind (:metrics-bind config "localhost")
        port (:metrics-port config 7937)]
    (log! :info {:msg "Starting metrics server" :bind bind :port port})
    (reg/register-metrics)
    (let [server (http-server/run-server
                  (fn [req]
                    (try
                      (metrics-handler req)
                      (catch Exception e
                        (log! :error {:msg "Metrics handler error"
                                      :error (ex-message e)})
                        {:status 500 :body "Internal error"})))
                  {:ip bind
                   :port port
                   :thread 1})]
      {:server server
       :bind bind
       :port port})))

(defn stop-metrics-server
  [metrics-server]
  (log! :info {:msg "Stopping metrics server"})
  (when-let [server (:server metrics-server)]
    (server)))

(comment
  (def config {:metrics-bind "localhost" :metrics-port 7937})
  (def m (start-metrics-server config))
  (stop-metrics-server m))
