(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'flandre/flandre)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "target/flandre-standalone.jar")

(defn uber [_]
  (b/delete {:path class-dir})
  (b/delete {:path uber-file})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'flandre.main}))
