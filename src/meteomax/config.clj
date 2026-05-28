(ns meteomax.config
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [meteomax.lib.envvar :as env])
  (:import [java.time ZoneId]))


(defn- normalize-webhook-path
  [path]
  (cond
    (nil? path) nil
    (= path "") "/"
    (str/starts-with? path "/") path
    :else (str "/" path)))


(defn- webhook-path-from-url
  [webhook-url]
  (let [path (.getPath (java.net.URI. webhook-url))]
    (normalize-webhook-path path)))


(defn- parse-zone-id
  [timezone]
  (try
    (ZoneId/of timezone)
    (catch Exception _
      (throw (ex-info "Invalid configuration"
                      {:timezone [(str "Invalid timezone: " timezone)]})))))


(def ConfigSchema
  "Malli schema for configuration validation."
  [:map
   [:max-api-token {:description "MAX Bot API token"}
    [:re {:error/message "MAX API token cannot be blank"} ".+"]]
   
   [:database-url {:description "PostgreSQL connection URI"}
    [:re {:error/message "Database URL cannot be blank"} ".+"]]
   
   [:meteo-api-url {:description "Meteo data API base URL"
                    :default "https://angara.net/meteo/api"}
    string?]
   [:meteo-api-auth {:description "Authorization header for meteo API"}
    [:re {:error/message "Meteo API auth cannot be blank"} ".+"]]
   [:meteo-api-timeout {:description "HTTP timeout in milliseconds"
                        :default 5000}
    pos-int?]
   
   [:metrics-bind {:description "Metrics server bind address"
                   :default "localhost"}
    string?]
    [:metrics-port {:description "Metrics server port"
                    :default 7937}
     pos-int?]
   
    [:webhook-bind {:description "Webhook server bind address"
                    :default "localhost"}
     string?]
    [:webhook-port {:description "Webhook server port"
                    :default 8005}
     pos-int?]
    [:webhook-url {:description "Webhook URL for Max API callbacks"}
     [:re {:error/message "Webhook URL cannot be blank"} ".+"]]
    [:webhook-path {:description "Webhook path for Max API callbacks"}
     [:re {:error/message "Webhook path must start with /"} #"/.+"]]
   
[:timezone {:description "Application timezone"
                :default "Asia/Irkutsk"}
      string?]
    
     [:zone-id {:description "Parsed ZoneId from timezone"}
      [:fn #(instance? ZoneId %)]]])


(defn make-config
  "Create configuration from environment variables with validation."
  []
  (let [webhook-url  (env/env-str "WEBHOOK_URL")
        webhook-path (or (some-> (env/env-str "WEBHOOK_PATH") normalize-webhook-path)
                         (some-> webhook-url webhook-path-from-url))
        timezone     (env/env-str "TIMEZONE" "Asia/Irkutsk")
        cfg {:max-api-token   (env/env-str "MAX_API_TOKEN")
             :database-url    (env/env-str "DATABASE_URL")
             :meteo-api-url   (env/env-str "METEO_API_URL" "https://angara.net/meteo/api")
             :meteo-api-auth  (env/env-str "METEO_API_AUTH")
             :meteo-api-timeout (env/env-int "METEO_API_TIMEOUT" 5000)
             :metrics-bind    (env/env-str "METRICS_BIND" "localhost")
             :metrics-port    (env/env-int "METRICS_PORT" 7937)
             :webhook-bind    (env/env-str "WEBHOOK_BIND" "localhost")
             :webhook-port    (env/env-int "WEBHOOK_PORT" 8005)
             :webhook-url     webhook-url
             :webhook-path    webhook-path
             :timezone        timezone
             :zone-id         (parse-zone-id timezone)}]
    (if (m/validate ConfigSchema cfg)
      cfg
      (let [errors (me/humanize (m/explain ConfigSchema cfg))]
        (throw (ex-info "Invalid configuration" errors))))))
