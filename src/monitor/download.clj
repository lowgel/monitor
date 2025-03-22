(ns monitor.download
  (:require [monitor.config :as config]
            [etaoin.api :as e]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as s]))

;;REMEMBER TO SET YOUR PREFFERED DOWNLOAD PATH IN config.clj! :)

;;INDIVIDUAL LINK DOWNLOADING
;;---------------------------------------------------------

;; CURRENTLY UNECESSARY
(defn thread-alive? [html]
  (if (re-find #"<img src=\"https://sys.4chan.org/image/error/404/rid.php\" alt=\"404 Not Found\">" html)
    false
    true))




(defn get-html [driver url]
  (let [_ (e/go driver url)
        _ (e/wait 1)
        html (e/get-source driver)]
    html))

(defn get-filename [url]
  (-> url
      java.net.URL.
      .getPath
      java.io.File.
      .getName))

(defn download-raw-html [folder-name html]
  (let [filepath (str config/directory folder-name "/source.html")
        _ (io/make-parents filepath)
        _ (spit filepath html)]
    html))

(defn get-links [html]
  (let [matches (re-seq #"\"fileThumb\"\shref=\"(.+?)\"" html)
        links (map #(str "https:" (second %)) matches)]
    links))



(defn get-board [url]
  (second (re-find #"https://boards.4chan.org/(.*?)/thread/*" url)))

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


;; CATALOGUE PARSING
;;------------------------------------------------------------------
(defn parse-log 
"filters the catalogs into links and descriptions"  
[driver url]
  (let [_ (e/go driver url)
        titles (->> (e/get-source driver)
                    (re-seq  #"<a href=\"(.*?)\">.*?<div class=\"teaser\">(.*?)</div>")
                    (map  #(into {} (seq {:link (str "https:" (nth % 1))
                                          :description (nth % 2)})))                ;; take link and teaser info of all posts in the 'log
                    (rest))]                     ;; ditch the first one one (it's irrelevant to us)
    titles))

(defn get-match-links
  "returns a collection of all links to threads that match the term (case agnostic)"
  [term parsed-log]
  (reduce #(if (re-find (re-pattern (str "(?i)" term)) (:description %2))
             (conj %1 (:link %2))
             %1)
          [] parsed-log))




(defn get-threads-matching-keywords
  "returns links for a single edn entry"
  [driver edn-entry]
  (let [url (str "https://boards.4chan.org/" (:board edn-entry) "/catalog")
        parsed-log (parse-log driver url)
        matched-links (set (mapcat #(get-match-links % parsed-log) 
                                   (:keywords edn-entry)))
        blacklisted-links (set (mapcat #(get-match-links % parsed-log) 
                                       (:blacklist edn-entry)))]
    (filter #(not (contains? blacklisted-links %)) 
            matched-links)))

(defn get-all-threads-in-keywords-edn
  "return links for all the the entries in keywords.edn"
  [driver]
  (let [kw (edn/read-string (slurp "src/monitor/keywords.edn"))]
    (set (mapcat #(get-threads-matching-keywords driver %) kw))))



;;seems like we can get ~10 files at a time before 429 Too Many Requests exception. Let's take 5 at a time to be safe
(defn monitor
  [driver url]
  (let [folder-name (str (get-board url) "/" (get-filename url))]
    (->> url
         (get-html driver)
         (download-raw-html folder-name)
             ;; TODO clean html of non-thumb links
         (get-links)
         (isolate-new-files folder-name)
         (take 5)
         (map #(dl % folder-name))
         (last)))) ;;prevents early evaluation. 



;; CURRENTLY UNUSED
(defn monitor-loop 
  "monitors a thread until it dies"
  [url]
  (let [driver (e/firefox-headless)
        results (while (thread-alive? (get-html driver url))
                  (monitor driver url)
                  (Thread/sleep 5000))
        _ (e/quit driver)]
    results))



(defn monitor-and-sleep
  "helper fuction that adds a delay to prevent error 429: too many requests"
[driver link]
  (do  (Thread/sleep 5000)
       (monitor driver link)))



(defn basic-loop [driver] 
(let [urls (get-all-threads-in-keywords-edn driver)
      _ (last (map #(monitor-and-sleep driver %) urls))] ;; if we don't use last here, the map function will lazy evaluate
                                                         ;; this could trigger an early e/quit before downloads are done
  0))

(defn -main [& args]
  (let [driver (e/firefox)
        _ (while true (basic-loop driver))]))

