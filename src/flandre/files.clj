(ns flandre.files
  (:import (java.io File)
           (java.nio.file Files CopyOption)))

(defn- get-file-path [tag base]
  (let [file (File. base tag)
        parent (.getParentFile file)]
    (.mkdirs parent)
    (.toPath file)))

(defn upload-file [tag content root]
  (let [path (get-file-path tag root)]
    (Files/copy content
                path
                (into-array CopyOption []))))

(defn get-file [tag root]
  (let [path (get-file-path tag root)
        file (.toFile path)]
    {:contents file
     :length (.length file)}))

(defn delete-file [tag root]
  (let [path (get-file-path tag root)
        file (.toFile path)]
    (if (.exists file)
      (.delete file))))
