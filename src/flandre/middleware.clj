(ns flandre.middleware
  (:require [reitit.ring.coercion]))

(def coerce-exceptions
  (fn [handler]
    (fn [req]
      (try
        (handler req)
        (catch Exception e
          (if-let [[code body]
                   (case (:type (ex-data e))
                     :reitit.coercion/request-coercion [400 "bad request"]
                     :reitit.coercion/response-coercion [500 "server error"]
                     nil)]
            {:status code
             :body body}
            (throw e)))))))
