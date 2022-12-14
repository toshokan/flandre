(ns flandre.uploads
  (:require [flandre.limits :as limits]
            [flandre.queries :as queries]
            [flandre.files :as files]
            [flandre.core :as core]
            [flandre.responses :as resp]))

(defn get-file-handler [req]
  (let [tag (get-in req [:path-params :tag])
        files-root (get-in req [:cfg :files :root])
        db (:db req)
        info (queries/get-file-info db tag)]
    (if info
      (let [file (files/get-file tag files-root)]
        (resp/uploaded-file file (:files/original_name info)))
      (resp/not-found))))

(defn delete-file-handler [req]
  (let [tag (get-in req [:path-params :tag])
        delete-token (get-in req [:body-params :delete-token])
        db (:db req)]
    (->> delete-token
         core/get-token-digest
         (queries/mark-file-deleted db tag))
    (resp/without-body)))

(defn- past-rate-limit? [req]
  (let [db (:db req)
        cfg (:cfg req)
        uploader (:remote-addr req)
        query (partial queries/count-files-in-last db)]
    (limits/past-rate-limit? cfg uploader query)))

(defn- store-file [db root expires-in name uploader stream]
  (let [tag (core/create-tag)
        delete-token (core/create-delete-token)
        name (or name tag)]
    (files/upload-file tag stream root)
    (let [expiry (queries/insert-file-info db tag name uploader expires-in (:digest delete-token))]
      {:filename name
       :tag tag
       :expiry expiry
       :delete-token (:token delete-token)})))

(defn- upload-file [req]
  (let [db (:db req)
        root (get-in req [:cfg :files :root])
        exp (get-in req [:cfg :expiry-time])
        name (get-in req [:parameters :query :filename])
        ip (:remote-addr req)
        stream (:body req)]
    (resp/with-body
     (store-file db root exp name ip stream))))

(defn upload-file-handler [req]
  (if (past-rate-limit? req)
    (resp/rate-limit-exceeded)
    (upload-file req)))
