(ns flandre.handler
  (:require [reitit.ring :as ring]
            [reitit.ring.coercion]
            [reitit.coercion :as co]
            [reitit.coercion.spec :as cs]
            [ring.middleware.resource :as res]
            [flandre.contract]
            [flandre.uploads :as uploads]
            [flandre.pastes :as pastes]
            [flandre.urls :as urls]
            [flandre.responses :as resp]
            [flandre.middleware :as mw]
            [muuntaja.middleware :as mu]))

(defn inject-depends [depends]
  (fn [handler]
    (fn [req]
      (handler (merge req depends)))))

(defn create-default-handler []
  (let [handler (ring/create-resource-handler {:path "/"})]
    (fn [req]
      (let [resp (handler req)]
        (if-not resp
          (resp/bad-request)
          resp)))))

(defn router [db cfg]
  (ring/ring-handler
   (ring/router
    [["/api"
      ["/f/:tag" {:get {:handler uploads/get-file-handler
                        :coercion cs/coercion
                        :parameters {:path {:tag :flandre.contract/tag}}}
                  :delete {:handler uploads/delete-file-handler
                           :coercion cs/coercion
                           :parameters {:path {:tag :flandre.contract/tag}
                                        :body :flandre.contract/delete-file-request}}}]
      ["/f" {:post uploads/upload-file-handler}]
      ["/u/:tag" {:get urls/get-url-handler}]
      ["/u" {:post {:handler urls/register-url-handler
                    :coercion cs/coercion
                    :parameters {:body :flandre.contract/register-url-request}}}]
      ["/p/:tag" {:get pastes/get-paste-handler}]
      ["/p" {:post pastes/upload-paste-handler}]]]
    {:data {:middleware [mw/handle-exceptions
                         reitit.ring.coercion/coerce-request-middleware]}})
   (create-default-handler)
   {:middleware [(inject-depends {:db db :cfg cfg})
                 mu/wrap-format]}))
