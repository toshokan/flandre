(ns flandre.middleware
  (:require [reitit.ring.coercion]
            [reitit.coercion :as c]
            [ring.util.request :as rq]
            [flandre.responses :as resp]))

(defn- get-exception-response [ex]
  (case (:type (ex-data ex))
    ::c/request-coercion (resp/bad-request "invalid request format")
    ::c/response-coercion (resp/server-error "error occurred while preparing response")
    (resp/server-error)))

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

(def handle-exceptions
  (fn [handler]
    (fn [req]
      (try
        (handler req)
        (catch Exception ex
          (get-exception-response ex))))))
