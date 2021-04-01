(ns multis-task.util.async
  (:require
   [re-frame.core :as re-frame]))

(defn promise->dispatch [promise {:keys [on-success on-fail on-finally] :as promise-args}]
  (cond-> promise
    on-success (.then #(re-frame/dispatch (conj on-success (js->clj %))))
    on-fail (.catch (fn [e]
                      (re-frame/dispatch (conj on-fail (.toString e)))))
    on-finally (.finally #(re-frame/dispatch on-finally))))

(defn eth-event->dispatch [eth-event-type dispatch-event]
  (.on (.-ethereum js/window)
       eth-event-type
       #(re-frame/dispatch (conj dispatch-event (js->clj %)))))
