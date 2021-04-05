(ns multis-task.util.form-utils)

(defn full-db-path [form-id path]
  (cons form-id path))

(defn db-get [db form-id path]
  (get-in db (full-db-path form-id path)))
