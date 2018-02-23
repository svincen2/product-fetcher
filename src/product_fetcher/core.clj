(ns product-fetcher.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]))

(def api-url "http://localhost/api/search/search.json")

(defn basic-params
  [site-id]
  {"siteId" site-id "resultsFormat" "native"})

(defn parse-json-response
  [response]
  (let [body (:body response)]
    (try (json/read-str body)
         (catch Exception e {:error (str "Error parsing JSON: " (.getMessage e))}))))

(defn get-pagination
  [params page-size]
  (let [params (assoc params "resultsPerPage" page-size)
        response @(http/get api-url {:query-params params})]
    (get (parse-json-response response) "pagination")))

(defn fetch-pages
  [params page-size pagination]
  (let [total-pages (get pagination "totalPages")
        pages (map inc (range 0 total-pages))
        params (assoc params "resultsPerPage" page-size)
        responses (map #(http/get api-url {:query-params (assoc params "page" %)}) pages)]
    (doall (map #(-> % deref parse-json-response (get "results")) responses))))

(defn fetch-products
  [site-id]
  (let [params (basic-params site-id)
        page-size 500
        pagination (get-pagination params page-size)
        products (apply concat (fetch-pages params page-size pagination))]
    (assert (= (count products) (get pagination "totalResults"))
            "Number of products fetched differs from reported totalResults")
    products))

