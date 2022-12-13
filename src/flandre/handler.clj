(ns flandre.handler
  (:require [reitit.ring :as ring]
            [reitit.ring.coercion]
            [reitit.coercion :as co]
            [reitit.coercion.spec :as cs]
            [ring.middleware.resource :as res]
            [flandre.contract :as c]
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

(defn create-default-handler [cfg]
  (if (:serve-resources? cfg)
    (let [handler (ring/create-resource-handler {:path "/"})]
      (fn [req]
        (let [resp (handler req)]
          (if-not resp
            (resp/bad-request)
            resp))))
    (fn [_] (resp/bad-request))))
  

(defn- with-handler [handler]
  {:handler handler})

(defn- path-coerce [data key spec]
  (-> (assoc data :coercion cs/coercion)
      (assoc-in [:parameters :path key] spec)))

(defn- body-coerce [data spec]
  (-> (assoc data :coercion cs/coercion)
      (assoc-in [:parameters :body] spec)))

(defn- middleware [data mw]
  (update-in data [:middleware]
             (fn [mws]
               (if mws (conj mws mw) [mw]))))

(defn- add-routes [root disabled? routes]
  (if-not disabled?
    (into root routes)
    root))



(defn- add-file-routes [root cfg]
  (let [max-size (get-in cfg [:files :max-size])]
    (add-routes
     root
     (get-in cfg [:files :disabled?])
     [["/f/:tag" {:get (-> (with-handler uploads/get-file-handler)
                           (path-coerce :tag ::c/tag))
                  :delete (-> (with-handler uploads/delete-file-handler)
                              (path-coerce :tag ::c/tag)
                              (body-coerce ::c/delete-file-request))}]
      ["/f" {:post (-> (with-handler uploads/upload-file-handler)
                       (middleware (mw/max-size max-size)))}]])))

(defn- add-url-routes [root cfg]
  (add-routes
   root
   (get-in cfg [:urls :disabled?])
   [["/u/:tag" {:get (-> (with-handler urls/get-url-handler)
                         (path-coerce :tag ::c/tag))}]
    ["/u" {:post (-> (with-handler urls/register-url-handler)
                     (body-coerce ::c/register-url-request))}]]))

(defn- add-paste-routes [root cfg]
  (let [max-size (get-in cfg [:pastes :max-size])]
    (add-routes
     root
     (get-in cfg [:pastes :disabled?])
     [["/p/:tag" {:get (-> (with-handler pastes/get-paste-handler)
                           (path-coerce :tag ::c/tag))}]
      ["/p" {:post (-> (with-handler pastes/upload-paste-handler)
                       (middleware (mw/max-size max-size)))}]])))

(defn router [db cfg]
  (ring/ring-handler
   (ring/router
    [(-> ["/api"]
         (add-file-routes cfg)
         (add-url-routes cfg)
         (add-paste-routes cfg))]
    {:data {:middleware [mw/handle-exceptions
                         reitit.ring.coercion/coerce-request-middleware]}})
   (create-default-handler cfg)
   {:middleware [(inject-depends {:db db :cfg cfg})
                 mu/wrap-format]}))
