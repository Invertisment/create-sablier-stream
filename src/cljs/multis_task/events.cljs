(ns multis-task.events
  (:require
   [re-frame.core :as re-frame]
   [multis-task.db :as db]
   [multis-task.config :as config]
   [multis-task.metamask :as metamask]
   [clojure.spec.alpha :as s]
   ))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

;; now we create an interceptor using `after`
(def check-spec-interceptor (re-frame/after (partial check-and-throw ::db/db-spec)))

(def interceptors (if config/debug?
                    [check-spec-interceptor]
                    []))

(re-frame/reg-event-db
 ::initialize-db
 interceptors
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::change-loader-counter
 interceptors
 (fn [db [_ operation]]
   (update db :loader-counter #(operation (or % 0) 1))))

(re-frame/reg-event-fx
 ::request-metamask-accounts
 (cons (re-frame/inject-cofx ::metamask/load-api) interceptors)
 (fn [cofx [_]]
   (println :request-metamask-accounts)
   ;; Put accounts into the current state
   (println (keys cofx))
   {}))

(re-frame/reg-event-fx
 ::notify-error
 interceptors
 (fn [cofx [_ error]]
   (println "[DISPLAY USER ERROR]" error)))

(re-frame/reg-event-fx
 ::activate-metamask
 interceptors
 (fn [cofx [_ future]]
   {::metamask/try-activate {:on-success [::request-metamask-accounts]
                             :on-failure [::notify-error "Can't activate Metamask"]
                             :on-finally [::change-loader-counter -]}
    :dispatch [::change-loader-counter +]}))
