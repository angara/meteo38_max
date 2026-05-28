(ns build
  (:import
   [java.time ZonedDateTime ZoneId]
   [java.time.format DateTimeFormatter])
  (:require 
    [clojure.java.io :as io]
    [clojure.tools.build.api :as b]
  ))


(def APP_NAME   (System/getenv "APP_NAME"))
(def VER_MAJOR  (System/getenv "VER_MAJOR"))
(def VER_MINOR  (System/getenv "VER_MINOR"))
(def MAIN_CLASS (System/getenv "MAIN_CLASS"))


(def JAVA_SRC         "./java")
(def RESOURCES        "./resources")
(def TARGET           "./target")
(def CLASS_DIR        "./target/classes")
(def TARGET_RESOURCES "./target/resources")

(def BUILD_INFO_EDN "build-info.edn")
(def JAR_NAME (str APP_NAME ".jar"))


(defn timestamp-str ^String []
  (-> (ZonedDateTime/now (ZoneId/of "UTC"))
      (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssX"))))


(defn clean [_]
  (b/delete {:path TARGET}))


(defn version [{:keys [ver-major ver-minor]}]
  (format "%s.%s.%s" ver-major ver-minor (b/git-count-revs nil)))


(defn build-info [{:keys [app-name] :as opts}]
  {:appname app-name
   :version (version opts)
   :branch (b/git-process {:git-args "branch --show-current"})
   :commit (b/git-process {:git-args "rev-parse --short HEAD"})
   :timestamp (timestamp-str)})


(defn write-build-info [{:keys [dest-dir] :as opts}]
  (let [build-info (build-info opts)
        out-file (io/file dest-dir BUILD_INFO_EDN)]
    (io/make-parents out-file)
    (spit out-file (pr-str build-info))
    build-info
    ))


;; https://clojure.org/guides/tools_build
;;
(defn javac [{basis :basis}]
  (b/javac {:src-dirs [JAVA_SRC]
            :class-dir CLASS_DIR
            :basis (or basis (b/create-basis {:project "deps.edn"}))
            :javac-opts ["-proc:none" "--release" "21"]
           }))


(defn uberjar [_]
  (let [uber-file (io/file TARGET JAR_NAME)
        basis     (b/create-basis {:project "deps.edn"})]
    
    (println "build:" 
             (write-build-info 
              {:app-name APP_NAME
               :ver-major VER_MAJOR
               :ver-minor VER_MINOR
               :dest-dir CLASS_DIR}))

    ; (println "compile Java")
    ; (javac {:basis basis}) 
    
    (b/copy-dir {:src-dirs ["src" RESOURCES TARGET_RESOURCES]
                 :target-dir CLASS_DIR})

    (println "compile Clojure")
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir CLASS_DIR
                    })
    
    (println "pack uberjar")
    (b/uber {:class-dir CLASS_DIR
             :uber-file (str uber-file)
             :basis basis
             :main MAIN_CLASS})
    
    (println "complete:" (str uber-file))
    ,))
