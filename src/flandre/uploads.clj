(ns flandre.uploads
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [ring.util.response :as r]
            [flandre.limits :as limits]
            [flandre.queries :as queries]
            [flandre.files :as files]
            [flandre.core :as core]))

(defn- rate-limit-exceeded []
  (-> (r/response "slow down")
      (r/status 429)))

(defn- not-found []
  (-> (r/response "not found")
      (r/status 404)))

(defn- bad-token-format []
  (-> (r/response "bad token format")
      (r/status 400)))

(defn get-file-handler [req]
  (let [tag (get-in req [:path-params :tag])
        files-root (get-in req [:cfg :files-root])
        db (:db req)
        info (queries/get-file-info db tag)]
    (if info
      (let [file (files/get-file tag files-root)]
        (-> (r/response (:contents file))
            (r/header "content-length" (:length file))
            (r/header "content-disposition"
                      (format "attachment;filename=\"%s\"" tag))))
      (not-found))))

(defn- bad-token-format? [req]
  (let [length-str (get-in req [:headers "content-length"])
        length (Integer/parseInt length-str)]
    (> length 100)))

(defn delete-file-handler [req]
  (let [tag (get-in req [:path-params :tag])
        db (:db req)]
    (if (bad-token-format? req)
      (bad-token-format)
      (do
        (->> (:body req)
             slurp
             core/get-token-digest
             (queries/mark-file-deleted db tag))
        (r/response nil)))))

(defn- past-rate-limit? [req]
  (let [db (:db req)
        cfg (:cfg req)
        uploader (:remote-addr req)
        query (partial queries/count-files-in-last db)]
    (limits/past-rate-limit? cfg uploader query)))

(defn- upload-file [req]
  (let [db (:db req)
        cfg (:cfg req)
        files-root (:files-root cfg)
        expiry-time (:expiry-time cfg)
        tag (core/create-tag)
        delete-token (core/create-delete-token)]
    (files/upload-file tag (:body req) files-root)
    (let [expiry (queries/insert-file-info
                  db tag tag (:remote-addr req) 30 (:digest delete-token))]
      (-> (r/response {:url (str (:base-url cfg) "/f/" tag)
                       :expiry expiry
                       :delete-token (:token delete-token)})))))

(defn upload-file-handler [req]
  (if (past-rate-limit? req)
    (rate-limit-exceeded)
    (upload-file req)))
