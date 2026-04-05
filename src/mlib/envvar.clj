(ns mlib.envvar
  "Environment variable helpers.")

(defn env-str
  "Get environment variable as string."
  ([name]
   (System/getenv name))
  ([name default]
   (or (System/getenv name) default)))

(defn env-int
  "Get environment variable as integer."
  ([name]
   (when-let [v (System/getenv name)]
     (Integer/parseInt v)))
  ([name default]
   (or (env-int name) default)))

(defn env-bool
  "Get environment variable as boolean."
  ([name]
   (when-let [v (System/getenv name)]
     (Boolean/parseBoolean v)))
  ([name default]
   (or (env-bool name) default)))

(comment
  (env-str "HOME")
  (env-int "PORT" 8080)
  (env-bool "DEBUG" false))
