(ns flandre.handler
  (:require [reitit.ring :as ring]
            [reitit.ring.coercion]
            [reitit.coercion :as co]
            [reitit.coercion.spec :as cs]
            [ring.middleware.resource :as res]
            [flandre.uploads :as uploads]
            [flandre.urls :as urls]
            [muuntaja.middleware :as mu]))

(defn inject-depends [depends]
  (fn [handler]
    (fn [req]
      (handler (merge req depends)))))

(def coerce-exceptions-middleware
  (fn [handler]
    (fn [req]
      (try
        (handler req)
        (catch Exception e
          (if-let [[code body]
                   (case (:type (ex-data e))
                     :reitit.coercion/request-coercion [400 "bad request"]
                     :reitit.coercion/response-coercion [500 "server error"]
                     nil)]
            {:status code
             :body body}
            (throw e)))))))

(defn router [db cfg]
  (ring/ring-handler
   (ring/router
    [["/api"
      ["/f/:tag" {:get uploads/get-file-handler
                  :delete {:handler uploads/delete-file-handler
                           :coercion cs/coercion
                           :parameters {:body :flandre.contract/delete-file-request}}}]
      ["/f" {:post uploads/upload-file-handler}]
      ["/u/:tag" {:get urls/get-url-handler}]
      ["/u" {:post {:handler urls/register-url-handler
                    :coercion cs/coercion
                    :parameters {:body :flandre.contract/register-url-request}}}]]]
    {:data {:middleware [coerce-exceptions-middleware
                         reitit.ring.coercion/coerce-request-middleware]}})
   (ring/create-resource-handler {:path "/"})
   {:middleware [(inject-depends {:db db :cfg cfg})
                 mu/wrap-format]}))
