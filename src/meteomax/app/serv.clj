(ns meteomax.app.serv
  "MAX long-polling service."
  (:require [mlib.max.botapi :as botapi]
            [meteomax.app.inbound :as inbound]
            [taoensso.telemere :as log])
  (:import [java.util.concurrent Executors]))

(defn- poll-loop
  "Main polling loop."
  [config db]
  (let [token (:max-api-token config)
        offset (atom 0)]
    (fn []
      (while (:running (meta poll-loop))
        (try
          (let [updates (botapi/get-updates token
                                            :offset @offset
                                            :limit 100
                                            :timeout 30)]
            (when (seq updates)
              (doseq [update updates]
                (try
                  (inbound/handle-update config db update)
                  (catch Exception e
                    (log/log! :error {:msg "Error handling update"
                                      :error (.getMessage e)})))
                (when-let [id (:update_id update)]
                  (reset! offset (inc id)))))
            (Thread/sleep 100))
          (catch InterruptedException _
            (log/log! :info {:msg "Poller interrupted"})
            (return))
          (catch Exception e
            (log/log! :error {:msg "Poller error" :error (.getMessage e)})
            (Thread/sleep 1000)))))))

(defn start-poller
  "Start MAX polling service."
  [config db]
  (log/log! :info {:msg "Starting MAX poller"})
  (let [executor (Executors/newSingleThreadExecutor)
        poller-fn (poll-loop config db)
        _ (future (poller-fn))]
    {:executor executor
     :running true}))

(defn stop-poller
  "Stop MAX polling service."
  [poller]
  (log/log! :info {:msg "Stopping MAX poller"})
  (alter-meta! poller assoc :running false)
  (.shutdown ^Executors (:executor poller)))

(comment
  ;; Start poller:
  (def p (start-poller config db))
  
  ;; Stop poller:
  (stop-poller p)
)
