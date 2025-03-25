(ns monitor.core
 (:require [monitor.download :as dl]
            [etaoin.api :as e]))



(defn -main [& args]
  (let [driver (e/firefox-headless)
        _ (while true (dl/basic-loop driver))]))
