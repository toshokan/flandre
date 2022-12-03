(ns flandre.pastes
  (:require [flandre.core :as core]
            [flandre.files :as files]
            [flandre.queries :as queries]
            [ring.util.response :as r]))

(defn- store-paste [db root uploader stream]
  (let [tag (core/create-tag)]
    (files/upload-file tag stream root)
    (queries/insert-paste-info db tag uploader)
    (r/response {:tag tag})))

(defn get-paste-handler [req]
  (let [tag (get-in req [:path-params :tag])
        db (:db req)
        pastes-root (get-in req [:cfg :pastes-root])
        info (queries/get-paste-info db tag)]
    (println (pr-str info))
    (if info
      (let [file (files/get-file tag pastes-root)]
        (-> (r/response (:contents file))
            (r/header "content-length" (:length file))
            (r/header "content-type" "text/plain")))
      (-> (r/response "not found")
          (r/status 404)))))

(defn upload-paste-handler [req]
  (let [db (:db req)
        cfg (:cfg req)
        pastes-root (:pastes-root cfg)
        uploader (:remote-addr req)]
    (store-paste db pastes-root uploader (:body req))))
