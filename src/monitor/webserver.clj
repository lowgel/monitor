(ns monitor.webserver
  (:require [org.httpkit.server :as http-kit]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.string :as s]
            [clojure.java.io :as io])
  (:import [org.jsoup Jsoup]))



;;HTML File Shenanigans
(defn strip-posts 
  "removes everything except the post containers from a thread's source.html"
[html]
  (let [source (Jsoup/parse html)
        posts (.select source "div.postContainer")
        posts-only-html (apply str (map #(.outerHtml %) posts))]
    posts-only-html))

(defn fix-file-hrefs [folder-name html]
      (s/replace html #"//i.4cdn.org/[a-z]*/" (str "downloads/" folder-name "/")))

(defn add-head 
  "adds a head and some styling"
  [html]
  (str "<html lang=\"en\">
        <head>
        <meta charset=\"UTF-8\">
        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
        <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">
        <title>monitor</title>
        <link rel=\"stylesheet\" type=\"text/css\" href=\"css/tomorrow.css\">
        </head>
        <body>"
       html
       "</body>
        </html>"))

(defn download-modified-html
  "downloads only the post container html (we don't need the other stuff)
  NOTE: does NOT return modified html (input same as output)"
  [dl-dir folder-name html]
  (let [filepath (str dl-dir folder-name "/source.html")
        fixed-html (->> html
                        (strip-posts)
                        (fix-file-hrefs folder-name)
                        (add-head))
        _ (io/make-parents filepath)
        _ (spit filepath fixed-html)]
    html))








(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "test"})



(defn test-server []
  (http-kit/run-server (wrap-resource handler "public") {:port 8080})) ;;TODO switch this to ring instead of http-kit
