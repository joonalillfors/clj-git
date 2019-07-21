(ns clj-git.query
  (:require [clojure.data.json :as json]
            [environ.core :refer [env]]))

(def token (env :client-token))

(defn options
  [body async]
  {
    :oauth-token token
    :content-type :json
    :body body
    :async? async
})

(defn repositories
  [name async] 
  (options (json/write-str {
    "query" "query ($name: String!) { 
      user(login: $name) { 
        repositories(first: 20) { 
          nodes { 
            name
            id 
          } 
        } 
      } 
    }",
    "variables" {"name" name}}) async)
)
  
(defn languages
  [repos async]
  (options (json/write-str {
    "query" "query ($repos: [ID!]!) {
      nodes(ids: $repos) {
        ...on Repository {
          languages(first: 20) {
            totalSize 
            totalCount 
            edges {
              size
              node {
                name 
              }
            } 
          } 
        } 
      } 
    }",
    "variables" {"repos" repos} }) async)
)
  
(defn getAll
  [name async]
  (options (json/write-str {
    "query" "query ($name: String!) {
      user(login: $name) {
        repositories(first: 20) {
          nodes {
            name
            languages (first: 20) {
              totalSize 
              totalCount 
              edges { 
                size
                node {
                  name
                }
              }
            }
          } 
        } 
      } 
    }",
    "variables" {"name" name} }) async)
)