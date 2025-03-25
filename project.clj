(defproject monitor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/lowgel/monitor"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [etaoin "1.1.42"]
                 [http-kit "2.5.3"]
                 [org.jsoup/jsoup "1.14.3"]
                 [ring/ring-core "1.9.3"]]
  :main ^:skip-aot monitor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
