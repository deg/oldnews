(ns oldnews.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
    (:import goog.History))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Old News browser"]
   [:div [:p "Tomorrow's way to see yesterday today"]]
   [:div [:a {:href "#/about"} "go to about page"]]
   [:div [:a {:href "#/debug"} "See internal app state"]]])

(defn about-page []
  [:div [:h2 "About Old News"]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:div [:a {:href "#/debug"} "See internal app state"]]])

(defn debug-page []
  [:div [:h2 "App state"]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/debug" []
  (session/put! :current-page #'debug-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
