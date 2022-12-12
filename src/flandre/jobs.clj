(ns flandre.jobs
  (:require [flandre.queries :as queries]
            [flandre.files :as files])
  (:import (java.lang Thread)))

(defn clean-up-files [cfg db]
  (future
    (let [files-root (get-in cfg [:files :root])
          cleanup-batch-size (:cleanup-batch-size cfg)
          cleanup-interval (* (:cleanup-interval cfg)
                              1000)]
      (while (not (Thread/interrupted))
        (let [infos (queries/get-expired-or-deleted-files db cleanup-batch-size)]
          (doseq [info infos]
            (files/delete-file (:files/tag info) files-root)
            (queries/delete-file-info db (:files/tag info))))
        (Thread/sleep cleanup-interval)))))
