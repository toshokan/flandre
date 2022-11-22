(ns flandre.queries
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]))

(defn count-files-in-last [db uploader seconds]
  (let [time-shift (str "-" seconds " seconds")
        query {:select [[:%count.* :count]]
               :from :files
               :where [:and
                       [:= :files/uploader_ip uploader]
                       [:>
                        :files/uploaded_at
                        [:datetime "now" time-shift]]]}]
    (:count (jdbc/execute-one! db (sql/format query)))))

(defn insert-file-info [db tag name ip expires-in delete-token-digest]
  (let [expiry-shift (str "+" expires-in " seconds")
        query {:insert-into :files
               :values [{:tag tag
                         :original_name name
                         :uploader_ip ip
                         :uploaded_at [:datetime]
                         :expires_at [:datetime "now" expiry-shift]
                         :delete_token_digest delete-token-digest}]
               :returning :*}]
    (:files/expires_at
     (jdbc/execute-one! db (sql/format query)))))

(defn get-file-info [db tag]
  (let [query {:select :*
               :from [:files]
               :where [:and
                       [:= :files.tag tag]
                       [:> :files.expires_at
                        [:datetime]]
                       [:= :files.deleted false]]}]
    (jdbc/execute-one! db (sql/format query))))

(defn mark-file-deleted [db tag delete-token-digest]
  (let [query {:update :files
               :set {:files/deleted true}
               :where [:and
                       [:= :files/tag tag]
                       [:=
                        :files/delete_token_digest
                        delete-token-digest]]}]
    (jdbc/execute! db (sql/format query))))

(defn get-expired-or-deleted-files [db batch-size]
  (let [query {:select :*
               :from :files
               :where [:or
                       [:= :files/deleted true]
                       [:<
                        :files/expires_at
                        [:datetime]]]
               :limit batch-size}]
    (jdbc/execute! db (sql/format query))))

(defn delete-file-info [db tag]
  (let [query {:delete-from :files
               :where [:= :files/tag tag]}]
    (jdbc/execute! db (sql/format query))))
