(ns flandre.uploads
  (:require [clojure.string :as s]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [ring.util.response :as r]
            [ring.middleware.multipart-params :as mp]
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

(defn delete-file-handler [req]
  (let [tag (get-in req [:path-params :tag])
        delete-token (get-in req [:body-params :delete-token])
        db (:db req)]
    (->> delete-token
         core/get-token-digest
         (queries/mark-file-deleted db tag))
        (r/response nil)))

(defn- past-rate-limit? [req]
  (let [db (:db req)
        cfg (:cfg req)
        uploader (:remote-addr req)
        query (partial queries/count-files-in-last db)]
    (limits/past-rate-limit? cfg uploader query)))

(defn- store-file [db root expires-in name uploader stream]
  (let [tag (core/create-tag)
        delete-token (core/create-delete-token)]
    (files/upload-file tag stream root)
    (let [expiry (queries/insert-file-info db tag name uploader expires-in (:digest delete-token))]
      {:filename name
       :tag tag
       :expiry expiry
       :delete-token (:token delete-token)})))

(defn- make-store-fn [req]
  (let [db (:db req)
        cfg (:cfg req)]
    (fn [{:keys [filename content-type stream]}]
      (store-file db
                  (:files-root cfg)
                  (:expiry-time cfg)
                  filename
                  (:remote-addr req)
                  stream))))

(defn- upload-files [req]
  (let [store-fn (make-store-fn req)
        req (mp/multipart-params-request req {:store store-fn})
        files (vals (:multipart-params req))
        file-count (count files)]
    (cond
      (empty? files) (-> (r/response "bad request")
                         (r/status 400))
      (= 1 file-count) (r/response (nth files 0))
      :else (r/response files))))

(defn upload-file-handler [req]
  (if (past-rate-limit? req)
    (rate-limit-exceeded)
    (upload-files req)))
