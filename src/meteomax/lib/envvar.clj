(ns meteomax.lib.envvar
  (:require [clojure.string :as str]))

(defn getenv
  [name]
  (System/getenv name))

(defn env-str
  ([name]
   (getenv name))
  ([name default]
   (or (getenv name) default)))

(defn env-int
  ([name]
   (when-let [v (getenv name)]
     (Integer/parseInt v)))
  ([name default]
   (or (env-int name) default)))

(defn env-bool
  "Get environment variable as boolean.
   Recognizes \"true\", \"1\", \"yes\", \"on\" (case-insensitive) as true."
  ([name]
   (when-let [v (getenv name)]
     (boolean (#{"true" "1" "yes" "on"} (str/lower-case v)))))
  ([name default]
   (let [v (getenv name)]
     (if v
       (boolean (#{"true" "1" "yes" "on"} (str/lower-case v)))
       default))))

(comment
  (env-str "HOME")
  (env-int "PORT" 8080)
  (env-bool "DEBUG" false))
