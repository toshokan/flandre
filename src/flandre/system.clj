(ns flandre.system
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [flandre.handler :as h]
            [flandre.jobs :as jobs]
            [ragtime.core :as rt]
            [ragtime.next-jdbc :as rj]
            [next.jdbc :as jdbc]))

(def config
  {:app/config {}
   :app/db {:config (ig/ref :app/config)}
   :app/handler {:db (ig/ref :app/db)
                 :config (ig/ref :app/config)}
   :app/adapter {:config (ig/ref :app/config)
                 :handler (ig/ref :app/handler)}
   :app/retention-job {:config (ig/ref :app/config)
                       :db (ig/ref :app/db)}})

(defmethod ig/init-key :app/config [_ _]
  (ig/read-string
   (slurp (or (System/getenv "FLANDRE_CONFIG")
              "config.edn"))))

(defmethod ig/init-key :app/db [_ {:keys [config]}]
  (let [db-spec (:db config)
        store (rj/sql-database db-spec)
        migrations (rj/load-resources "migrations")
        index (rt/into-index migrations)]
    (rt/migrate-all store index migrations)
    (jdbc/get-datasource db-spec)))

(defmethod ig/init-key :app/handler [_ {:keys [db config]}]
  (h/router db config))

(defmethod ig/init-key :app/adapter [_ {:keys [config handler]}]
  (jetty/run-jetty handler {:port (:port config)
                            :join? false
                            :send-server-version? false}))

(defmethod ig/halt-key! :app/adapter [_ adapter]
  (.stop adapter))

(defmethod ig/init-key :app/retention-job [_ {:keys [config db]}]
  (jobs/clean-up-files config db))

(defmethod ig/halt-key! :app/retention-job [_ job]
  (future-cancel job))

(def system
  (ig/init config))

(comment
  (ig/halt! system))
