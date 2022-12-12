(ns flandre.middleware
  (:require [reitit.ring.coercion]
            [reitit.coercion :as c]
            [flandre.responses :as resp]))

(defn- get-exception-response [ex]
  (case (:type (ex-data ex))
    ::c/request-coercion (resp/bad-request "invalid request format")
    ::c/response-coercion (resp/server-error "error occurred while preparing response")
    (resp/server-error)))

(def handle-exceptions
  (fn [handler]
    (fn [req]
      (try
        (handler req)
        (catch Exception ex
          (get-exception-response ex))))))
