(ns meteomax.config
  "Configuration management with Malli validation."
  (:require [malli.core :as m]
            [malli.error :as me]
            [mlib.envvar :as env]))

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
   [:timezone {:description "Application timezone"
               :default "Asia/Irkutsk"}
    string?]])

(defn make-config
  "Create configuration from environment variables with validation."
  []
  (let [cfg {:max-api-token (env/env-str "MAX_API_TOKEN")
             :database-url (env/env-str "DATABASE_URL")
             :meteo-api-url (env/env-str "METEO_API_URL" "https://angara.net/meteo/api")
             :meteo-api-auth (env/env-str "METEO_API_AUTH")
             :meteo-api-timeout (env/env-int "METEO_API_TIMEOUT" 5000)
             :metrics-bind (env/env-str "METRICS_BIND" "localhost")
             :metrics-port (env/env-int "METRICS_PORT" 7937)
             :timezone (env/env-str "TIMEZONE" "Asia/Irkutsk")}]
    (if (m/validate ConfigSchema cfg)
      cfg
      (let [errors (me/humanize (m/explain ConfigSchema cfg))]
        (throw (ex-info "Invalid configuration" errors))))))

(comment
  ;; Test configuration with valid env vars:
  (System/setenv "MAX_API_TOKEN" "test-token")
  (System/setenv "DATABASE_URL" "postgresql://localhost/maxbot")
  (System/setenv "METEO_API_AUTH" "Bearer test-auth")
  
  (make-config)
  ;; => {:max-api-token "test-token", :database-url "...", ...}
  
  ;; Test with missing required vars:
  (System/clearProperty "MAX_API_TOKEN")
  (make-config)
  ;; => throws Exception with validation errors
)
