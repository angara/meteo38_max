(ns user
  "REPL utilities for development."
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [mount.core :as mount]
            [maxbot.config :as config]
            [maxbot.data.pg :as pg]
            [maxbot.data.store :as store]
            [maxbot.data.meteo-api :as meteo-api]))

(defn start
  "Start application components."
  []
  (mount/start))

(defn stop
  "Stop application components."
  []
  (mount/stop))

(defn restart
  "Stop, refresh namespaces, and start."
  []
  (stop)
  (refresh :after 'user/start))

(defn restart-all
  "Stop, refresh all namespaces, and start."
  []
  (stop)
  (refresh-all :after 'user/start))

;; Test configuration - update with actual values
(def test-config
  {:max-api-token "test-token"
   :database-url "postgresql://localhost/maxbot"
   :meteo-api-url "https://angara.net/meteo/api"
   :meteo-api-auth "Bearer test-auth"
   :meteo-api-timeout 5000
   :metrics-bind "localhost"
   :metrics-port 7937
   :timezone "Asia/Irkutsk"})

(comment
  ;; Start REPL:
  ;; clojure -M:dev:nrepl
  
  ;; In REPL:
  (start)
  (stop)
  (restart)
  
  ;; Test API:
  (meteo-api/get-station-info test-config "uiii")
  (meteo-api/get-active-stations test-config 52.28 104.28 :last-hours 24)
)
