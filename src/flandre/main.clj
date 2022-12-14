(ns flandre.main
  (:require [flandre.system :as sys])
  (:gen-class))

(defn -main [& args]
  (sys/start))
