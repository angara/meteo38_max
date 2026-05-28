(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [mount.core :as mount]
            [meteomax.config :as config]
            [meteomax.db.pg :as pg]
            [meteomax.app.meteo-api :as meteo-api]
            [meteomax.app.maxapi :as max]
            [meteomax.main]
            ))

(defn start []
  (mount/start-with-args (config/make-config)))

(defn stop []
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


(comment
  ;; Start with: make dev-env && make dev
  
  (start)
  (stop)
  (restart)

  
  (max/get-subscriptions (:max-api-token (mount/args)))
  
  )
