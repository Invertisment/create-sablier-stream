(ns multis-task.util.async
  (:require
   [re-frame.core :as re-frame]))

(defn promise->dispatch [promise {:keys [on-success on-fail on-finally] :as promise-args}]
  (cond-> promise
    on-success (.then #(re-frame/dispatch (vec (concat on-success [(js->clj %)]))))
    on-fail (.catch (fn [e]
                      (re-frame/dispatch (concat on-fail [e]))))
    on-finally (.finally #(re-frame/dispatch on-finally))))
