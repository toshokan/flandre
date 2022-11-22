(ns flandre.core
  (:import (java.util.concurrent ThreadLocalRandom)
           (java.util Base64 UUID)
           (java.nio ByteBuffer)
           (java.security MessageDigest)))

(defn- base64-encode [bytes]
  (-> (Base64/getUrlEncoder)
      .withoutPadding
      (.encodeToString bytes)))

(defn- sha256-digest [string]
  (-> (MessageDigest/getInstance "SHA-256")
      (.digest (.getBytes string))))

(defn- random-data [n]
  (let [rng (ThreadLocalRandom/current)
        arr (byte-array n)]
    (.nextBytes rng arr)
    (base64-encode arr)))

(defn- get-hash [msg]
  (-> msg
      sha256-digest
      base64-encode))

(defn- create-random-token [n]
  (let [data (random-data n)
        hash (get-hash data)]
    {:token data
     :digest hash}))

(defn get-token-digest [token]
  (get-hash token))

(defn create-delete-token []
  (create-random-token 10))
  
(defn create-tag []
  (let [buf (ByteBuffer/allocate (* 2 (Long/BYTES)))
        uid (UUID/randomUUID)]
    (.putLong buf (.getMostSignificantBits uid))
    (.putLong buf (.getLeastSignificantBits uid))
    (base64-encode (.array buf))))
