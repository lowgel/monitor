(ns monitor.core
 (:require [clojure.tools.cli :refer [parse-opts]]
           [clojure.tools.cli :as cli]))


(def cli/get-default-options
  [["-k" "--keyword KEYWORD" "monitor a keyword"]
  ["-l" "--link LINK" "monitor a link"]
   ["-h --help"]
   ["-d" "--[no-]daemon" "Daemonize the process" :default false]])



