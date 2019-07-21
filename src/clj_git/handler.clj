(ns clj-git.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]]
            [clojure.algo.generic.functor :refer [fmap]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def token (env :client-token))

(defn repositories
  [name] 
  {
    :oauth-token token
    :content-type :json
    :body (json/write-str {"query" "query ($name: String!) { user(login: $name) { repositories(first: 20) { nodes { name id } } } }",
                           "variables" {"name" name}})
    :async? true
})

(defn languages
  [repos]
  {
    :oauth-token token
    :content-type :json
    :body (json/write-str {"query" "query ($repos: [ID!]!) { nodes(ids: $repos) { ...on Repository { languages(first: 20) { totalSize totalCount edges { size node { name } } } } } }",
                           "variables" {"repos" repos}})
    :async? true
  }
)

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/api/:name"
    [name]
    (do (println (json/write-str {:version "1.0", :name name}))
        (let [result (promise)]
          ;(client/with-async-connection-pool {}
            (client/post "https://api.github.com/graphql" (repositories name)
              (fn [resp]
                (let [nodes (-> (json/read-str (resp :body) :key-fn keyword) :data :user :repositories :nodes)]
                  (let [repos (map #(% :id) nodes)]
                    (client/post "https://api.github.com/graphql" (client/reuse-pool (languages repos) resp)
                      (fn [res]
                        (let [node (-> (json/read-str (res :body) :key-fn keyword) :data :nodes)]
                          (let [langs (group-by :name (flatten (map #(vector (map (fn [edge] {:size (edge :size), :name (-> edge :node :name)}) (-> % :languages :edges))
                          ) node)))]
                            (deliver result (fmap #(reduce (fn [sum lang] (+ (lang :size) sum)) 0 %) langs)))
                        ))
                      (fn [exception]
                        (println (.getMessage exception)))))))
              (fn [exception]
                (println (.getMessage exception)))) ;)
          (response {:version "1.0", :name name, :body @result}))))
    (GET "/test"
      []
      (let [repos (promise)
            result (promise)]
        (client/with-connection-pool {}
          (let [res (client/post "https://api.github.com/graphql" {
            :oauth-token token
            :content-type :json
            :body (json/write-str {"query" "query ($name: String!) { user(login: $name) { repositories(first: 20) { nodes { name id } } } }",
                                  "variables" {"name" "joonalillfors"}})
          })]
            (deliver repos (map #(% :id) (-> (json/read-str (res :body) :key-fn keyword) :data :user :repositories :nodes)))
          )
          (let [res (client/post "https://api.github.com/graphql" {
            :oauth-token token
            :content-type :json
            :body (json/write-str {"query" "query ($repos: [ID!]!) { nodes(ids: $repos) { ...on Repository { languages(first: 20) { totalSize totalCount edges { size node { name } } } } } }",
                                  "variables" {"repos" @repos}})
          })]
            (let [node (-> (json/read-str (res :body) :key-fn keyword) :data :nodes)]
              (deliver result (group-by :name (flatten (map #(vector (map (fn [edge] {:size (edge :size), :name (-> edge :node :name)}) (-> % :languages :edges))) node))))
            )
          )
        )
        (response @result)
      ))
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body)
      middleware/wrap-json-response))
