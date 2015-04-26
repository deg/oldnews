(ns oldnews.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [oldnews.state :refer [sget sset!]])
    (:import goog.History))


(defn current-page []
  [:div [(session/get :current-page)]])

;; Routes
(secretary/set-config! :prefix "#")
(secretary/defroute "/"      [] (session/put! :current-page #'oldnews.pages.home/page))
(secretary/defroute "/about" [] (session/put! :current-page #'oldnews.pages.about/page))
(secretary/defroute "/debug" [] (session/put! :current-page #'oldnews.pages.debug/page))


;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
