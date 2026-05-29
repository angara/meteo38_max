(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [mount.core :as mount]
            [taoensso.telemere :as telemere]
            [meteomax.config :as config]
            [meteomax.db.pg :as pg]
            [meteomax.meteo-data.core :as meteo-api]
            [meteomax.app.maxapi :as max]
            [meteomax.main]))

(defn set-log-level! [level]
  (telemere/set-min-level! level))

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


(set-log-level! :debug)


(comment
  ;; Start with: make dev-env && make dev
  
  (set-log-level! :debug)
  (set-log-level! :info)

  (start)
  (stop)
  (restart)

  (def cfg (mount/args))
  
  (max/get-subscriptions (:max-api-token (mount/args)))
  
  (meteo-api/get-active-stations cfg (:default-lat cfg) (:default-lon cfg) :search "анг")
  


  )
