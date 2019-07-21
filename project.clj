(defproject clj-git "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/algo.generic "0.1.3"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [ring/ring-defaults "0.3.2"]
                 [clj-http "3.10.0"]
                 [ring/ring-json "0.3.1"]]
  :plugins [[lein-ring "0.12.5"]
            [lein-pprint "1.2.0"]]
  :ring {:handler clj-git.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
