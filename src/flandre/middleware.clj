(ns flandre.middleware
  (:require [reitit.ring.coercion]
            [reitit.coercion :as c]
            [ring.util.request :as rq]
            [ring.middleware.params :as rp]
            [flandre.responses :as resp])
  (:import (java.io File)))

(defn- get-exception-response [ex]
  (case (:type (ex-data ex))
    ::c/request-coercion (resp/bad-request "invalid request format")
    ::c/response-coercion (resp/server-error "error occurred while preparing response")
    (resp/server-error)))

(defn wrap-query-string [handler]
  (fn [req]
    (let [encoding (or (rq/character-encoding req)
                       "UTF-8")]
      (handler (rp/assoc-query-params req encoding)))))

(defn max-size [cap]
  (fn [handler]
    (if-not cap
      handler
      (fn [req]
        (let [len (rq/content-length req)]
          (cond
            (nil? len) (resp/bad-request)
            (> len cap) (resp/too-large)
            :else (handler req)))))))

(defn ensure-readable-files [handler]
  (fn [req]
    (let [resp (handler req)
          body (:body resp)]
      (if (and (instance? File body)
               (not (and (.exists body)
                         (.canRead body))))
        (resp/server-error)
        resp))))

(defn handle-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception ex
        (get-exception-response ex)))))
