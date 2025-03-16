(ns monitor.core
  (:require [monitor.config :as config]
            [etaoin.api :as e]
            [clojure.java.io :as io]))

;;REMEMBER TO SET YOUR PREFFERED DOWNLOAD PATH IN config.clj! :)

(defn get-html [url]
  (let [driver (e/firefox-headless)
        _ (e/go driver url)
        _ (e/wait 1)
        html (e/get-source driver)
        _ (e/quit driver)]
    html))

(defn get-links [html]
  (let [matches (re-seq #"\"fileThumb\"\shref=\"(.+?)\"" html)
        links (map #(str "https:" (second %)) matches)]
    links))

(defn get-filename [url]
  (-> url
      java.net.URL.
      .getPath
      java.io.File.
      .getName))


;;seems like we can get 10 files at a time before 429 Too Many Requests exception              
(defn dl [uri]
  (let [filepath (str config/directory (get-filename uri))
        _ (io/make-parents filepath)]
    (if (.exists (io/file filepath)) 
      (println "Already exists. Skipping...")
      (with-open [in (io/input-stream uri)
                  out (io/output-stream filepath)]
        (io/copy in out)))))



(defn -main [url]
  (->> url
       (get-html)
       (get-links)
       (map #(dl %))))


