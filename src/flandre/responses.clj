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

(defn server-error
  ([] (server-error nil))
  ([desc] (-> (error-response "server error" desc)
              (r/status 500))))

(defn too-large []
  (-> (error-response "too large" "payload too large")
      (r/status 413)))

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

(defn- content-type [resp type]
  (-> resp
      (r/header "content-type" type)))

(defn uploaded-file [file-info name]
  (-> (file file-info)
      (attachment name)))

(defn paste [file-info]
  (-> (file file-info)
      (inline)
      (content-type "text/plain")))
