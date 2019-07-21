(ns clj-git.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]]
            [clojure.algo.generic.functor :refer [fmap]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn repositories
  [name] 
  {
    :oauth-token "9ebfb7d53950444f8d3362f29848e968d3b05ec5"
    :content-type :json
    :body (json/write-str {"query" "query ($name: String!) { user(login: $name) { repositories(first: 20) { nodes { name id } } } }",
                           "variables" {"name" name}})
    :async? true
})

(defn languages
  [repos]
  {
    :oauth-token "9ebfb7d53950444f8d3362f29848e968d3b05ec5"
    :content-type :json
    :body (json/write-str {"query" "query ($repos: [ID!]!) { nodes(ids: $repos) { ...on Repository { languages(first: 20) { totalSize totalCount edges { size node { name } } } } } }",
                           "variables" {"repos" repos}})
    :async? true
  }
)

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/:name"
    [name]
    (do (println (json/write-str {:version "1.0", :name name}))
        (let [result (promise)]
          (client/post "https://api.github.com/graphql" (repositories name)
            (fn [res]
              (let [nodes (-> (json/read-str (res :body) :key-fn keyword) :data :user :repositories :nodes)]
                (let [repos (map #(% :id) nodes)]
                  (println repos)
                  (client/post "https://api.github.com/graphql" (languages repos)
                    (fn [res]
                      (let [node (-> (json/read-str (res :body) :key-fn keyword) :data :nodes)]
                        ;(println node)
                        ;(println (map #(-> % :languages :edges) node))
                        ;calculate sizes of each language
                        (let [langs (group-by :name (flatten (map #(vector (map (fn [edge] {:size (edge :size), :name (-> edge :node :name)}) (-> % :languages :edges))
                        ) node)))]
                          (println langs)
                          (deliver result langs))
                        ;(deliver result (map #(hash-map :languages (-> % :languages :edges)) node))
                      ))
                    (fn [exception]
                      (println (.getMessage exception)))))))
            (fn [exception]
              (println (.getMessage exception))))
          (response {:version "1.0", :name name, :body @result}))))
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body)
      middleware/wrap-json-response))
