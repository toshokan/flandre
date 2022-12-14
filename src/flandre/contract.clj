(ns flandre.contract
  (:import (java.net URL))
  (:require [clojure.spec.alpha :as s]))

(defn- max-len [n]
  (fn [v]
    (<= (.length v) n)))

(defn- between [min max]
  (fn [v]
    (and
     (< min v)
     (> max v))))

(defn- valid-url? [str]
  (try
    (let [url (URL. str)]
      (and url
           (.getAuthority url)
           true))
    (catch Exception _
      nil)))

(s/def ::delete-token (s/and
                       string?
                       (max-len 100)))

(s/def ::filename string?)

(s/def ::upload-file-query (s/keys :opt-un [::filename]))

(s/def ::tag (s/and
              string?
              (s/spec #(not (empty? %)))))


(s/def ::delete-file-request
  (s/keys :req-in [::delete-token]))

(s/def ::redirect-to (s/and
                      string?
                      valid-url?))

(s/def ::register-url-request
  (s/keys :req-un [::redirect-to]))
