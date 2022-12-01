(ns flandre.contract
  (:require [clojure.spec.alpha :as s]))

(defn- max-len [n]
  (fn [v]
    (< (.length v) n)))

(s/def ::delete-token (s/and
                       string?
                       (max-len 100)))

(s/def ::delete-file-request
  (s/keys :req-in [::delete-token]))

(s/def ::redirect-to string?)

(s/def ::register-url-request
  (s/keys :req-un [::redirect-to]))

