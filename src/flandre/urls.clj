(ns flandre.urls
  (:require [flandre.core :as core]
            [flandre.queries :as queries]
            [flandre.responses :as resp]
            [ring.util.response :as r]))

(defn get-url-handler [req]
  (let [db (:db req)
        tag (get-in req [:path-params :tag])
        info (queries/get-url-info db tag)]
    (if info
      (resp/redirect (:urls/redirect_to info))
      (resp/not-found))))

(defn register-url-handler [req]
  (let [db (:db req)
        redirect-to (get-in req [:body-params :redirect-to])
        tag (core/create-tag)
        ip (:remote-addr req)]
    (queries/insert-url-info db tag redirect-to ip)
    (r/response {:tag tag})))
