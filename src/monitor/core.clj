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



(defn isolate-new-files [folder links]
  (reduce #(if (.exists (io/file (str config/directory folder "/" (get-filename %2))))
             %1
             (conj %1 %2)) 
          [] 
          links))

(defn dl [uri folder]
  (let [filepath (str config/directory folder "/" (get-filename uri))
        _ (io/make-parents filepath)]
      (with-open [in (io/input-stream uri)
                  out (io/output-stream filepath)]
        (io/copy in out))))


;;seems like we can get ~10 files at a time before 429 Too Many Requests exception. Let's take 5 at a time to be safe

(defn monitor [url]
    (->> url
             (get-html)
             (get-links)
             (isolate-new-files (get-filename url))
             (take 5)
             (map #(dl % (get-filename url)))))






