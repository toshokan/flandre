(ns flandre.responses
  (:require [ring.util.response :as r]))

(defn- error-response [err desc]
  (if desc
    (r/response {:error err :description desc})
    (r/response {:error err})))

(defn bad-request
  ([] (bad-request nil))
  ([desc] (-> (error-response "bad request" desc)
              (r/status 400))))

(defn rate-limit-exceeded []
  (-> (error-response "slow down" "rate limit exceeded")
      (r/status 429)))

(defn not-found []
  (-> (error-response "not found" nil)
      (r/status 404)))

(defn redirect [to]
  (-> (r/response nil)
      (r/header "location" to)
      (r/status 302)))

(defn- file [file-info]
  (-> (r/response (:contents file-info))
      (r/header "content-length" (:length file-info))))

(defn- attachment [resp filename]
  (-> resp
      (r/header "content-disposition"
                (format "attachment;filename=\"%s\"" filename))))

(defn- inline [resp]
  (-> resp
      (r/header "content-disposition" "inline")))

(defn uploaded-file [file-info name]
  (-> (file file-info)
      (attachment name)))
