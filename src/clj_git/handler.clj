(ns clj-git.handler
  (:require [clj-git.query :refer :all]
            [clj-git.routes :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]]
            [clojure.algo.generic.functor :refer [fmap]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] (response "Hello World"))
  (GET "/api/languages/:name"
    [name]
    (let [result (promise)]
      (client/post "https://api.github.com/graphql" (repositories name true)
        (fn [res]
          (let [node (-> (json/read-str (res :body) :key-fn keyword) :data :user :repositories :nodes)]
            (let [langs (group-by :name (flatten (map #(vector (map (fn [edge] {:size (edge :size), :name (-> edge :node :name)}) (-> % :languages :edges))) node)))]
              (deliver result (fmap #(hash-map :size (reduce (fn [sum lang] (+ (lang :size) sum)) 0 %), :count (count %) ) langs)))))
        (fn [exception]
          (deliver result (.getMessage exception)))
      )
      (response {:version "1.0", :name name, :languages @result})))
  (GET "/api/all/:name"
    [name]
    (let [result (promise)]
      (client/post "https://api.github.com/graphql" (repositories name true)
        (fn [res]
          (let [node (-> (json/read-str (res :body) :key-fn keyword) :data :user :repositories :nodes)]
            (deliver result node)))
        (fn [exception]
          (deliver result (.getMessage exception)))
      )
      (response {:version "1.0", :name name, :languages @result})))
(route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body)
      middleware/wrap-json-response))
