(ns flandre.limits)

(defn past-rate-limit? [cfg key query-fn]
  (let [rate-period (:rate-period cfg)
        rate-period-limit (:rate-period-limit cfg)]
    (if-not (= rate-period-limit -1)
      (< rate-period-limit
         (query-fn key rate-period)))))
