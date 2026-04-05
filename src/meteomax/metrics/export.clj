(ns meteomax.metrics.export
  "HTTP metrics endpoint using http-kit."
  (:require [org.httpkit.server :as http-server]
            [iapetos.core :as prom]
            [iapetos.ring :as prom-ring]
            [meteomax.metrics.reg :as reg]
            [taoensso.telemere :as log]))

(defn metrics-handler
  "Ring handler for /metrics endpoint."
  [req]
  (if (= (:uri req) "/metrics")
    (prom-ring/metrics-handler @reg/registry req)
    {:status 404 :body "Not found"}))

(defn start-metrics-server
  "Start HTTP server for Prometheus metrics.
   
   Parameters:
   - config: Application config with :metrics-bind and :metrics-port
   
   Returns: Server map with :server key"
  [config]
  (let [bind (:metrics-bind config "localhost")
        port (:metrics-port config 7937)]
    (log/log! :info {:msg "Starting metrics server" :bind bind :port port})
    (reg/register-metrics)
    (let [server (http-server/run-server
                  (fn [req]
                    (try
                      (metrics-handler req)
                      (catch Exception e
                        (log/log! :error {:msg "Metrics handler error"
                                          :error (.getMessage e)})
                        {:status 500 :body "Internal error"})))
                  {:ip bind
                   :port port
                   :thread 1})]
      {:server server
       :bind bind
       :port port})))

(defn stop-metrics-server
  "Stop metrics HTTP server."
  [metrics-server]
  (log/log! :info {:msg "Stopping metrics server"})
  (when-let [server (:server metrics-server)]
    (server)))

(comment
  ;; Start metrics server:
  (def config {:metrics-bind "localhost" :metrics-port 7937})
  (def m (start-metrics-server config))
  
  ;; Test endpoint:
  ;; curl http://localhost:7937/metrics
  
  ;; Stop server:
  (stop-metrics-server m)
)
