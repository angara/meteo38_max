(ns meteomax.lib.random
  (:import [java.security SecureRandom]))


(def ^SecureRandom ^:private secure-random
  (SecureRandom.))


(def ^String ^:private secret-alphabet
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-")


(defn random-string
  "Generate a cryptographically secure random string."
  [length]
  (apply str
         (repeatedly length
                     (fn []
                       (let [idx (.nextInt secure-random (count secret-alphabet))]
                         (.charAt secret-alphabet idx))))))


(defn webhook-secret
  "Generate a MAX webhook secret."
  []
  (random-string 32))


(comment
  (webhook-secret))
