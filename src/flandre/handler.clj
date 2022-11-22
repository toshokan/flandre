(ns flandre.handler
  (:require [reitit.ring :as ring]
            [ring.middleware.json :refer [wrap-json-response]]
            [flandre.uploads :as uploads])
  (:import [java.io File]))

(defn inject-depends [depends]
  (fn [handler]
    (fn [req]
      (handler (merge req depends)))))

(defn router [db cfg]
  (ring/ring-handler
   (ring/router
    [["/f/:tag" {:get uploads/get-file-handler
                 :delete uploads/delete-file-handler}]
     ["/f" {:post uploads/upload-file-handler}]])
   nil
   {:middleware [(inject-depends {:db db :cfg cfg})
                 wrap-json-response]}))
