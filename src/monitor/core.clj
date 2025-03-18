(ns monitor.core
  (:require [monitor.config :as config]
            [etaoin.api :as e]
            [clojure.java.io :as io]))

;;REMEMBER TO SET YOUR PREFFERED DOWNLOAD PATH IN config.clj! :)


(defn thread-alive? [html]
  (if (re-find #"<img src=\"https://sys.4chan.org/image/error/404/rid.php\" alt=\"404 Not Found\">" html)
    false
    true))




(defn get-html [driver url]
  (let [_ (e/go driver url)
        _ (e/wait 1)
        html (e/get-source driver)]
    html))

(defn download-raw-html [folder-name html]
  (let [filepath (str config/directory folder-name "/" folder-name ".html")
        _ (io/make-parents filepath)
        - (spit filepath html)]
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
        (io/copy in out))
      filepath))


;;seems like we can get ~10 files at a time before 429 Too Many Requests exception. Let's take 5 at a time to be safe

(defn monitor [driver url]
  (let [folder-name (get-filename url)]
    (->> url
         (get-html driver)
         (download-raw-html folder-name)
             ;; TODO clean html of non-thumb links
         (get-links)
         (isolate-new-files folder-name)
         (take 5)
         (map #(dl % folder-name))
         (last))))




(defn monitor-loop [url] 
  (let [driver (e/firefox-headless)
        results (while (monitor driver url)
                  (Thread/sleep 1000)
                  (println "test"))
        _ (e/quit driver)]
    results))



