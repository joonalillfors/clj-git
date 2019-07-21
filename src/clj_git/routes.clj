(ns clj-git.routes
    (:require [clj-git.query :refer :all]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [compojure.handler :as handler]
              [clojure.data.json :as json]
              [clj-http.client :as client]
              [ring.middleware.json :as middleware]
              [ring.util.response :refer [response]]
              [clojure.algo.generic.functor :refer [fmap]]
              [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn namev1
    [name]
    (format "Hello, %s!" name))