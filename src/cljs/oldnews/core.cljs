(ns oldnews.core
    (:require [clojure.string :as str]
              [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [ajax.core :refer [GET POST]])
    (:import goog.History))

;; -------------------------
;; Views

(defonce the-state (atom nil))

(defn set-state! [key value]
  (swap! the-state assoc key value))

(defn get-state [key]
  (key @the-state))

(defn input-element
  "An input element which updates its value on change"
  [id name type]
  [:input {:id id
           :name name
           :class "form-control"
           :type type
           :required ""
           :value (get-state id)
           :on-change #(set-state! id (-> % .-target .-value))}])


(defn search-input [state-id]
  (input-element state-id "searchVal" "text"))

(defn handler [response]
  (let [{:keys [totalItems items]} response
        item-data (map #(-> %
                            (select-keys [:city :date :title :url :id])
                            (clojure.set/rename-keys {:id :key}))
                       items)]
    (set-state! :num-results totalItems)
    (set-state! :results item-data)))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn finalize-search []
  (set-state! :searching false))

(defn launch-search []
  (let [search-string (get-state :text1)
        base "http://chroniclingamerica.loc.gov/search/pages/results/?"
        text (str "proxtext=" search-string)
        filter "&dateFilterType=yearRange&date1=1800&date2=1930&format=json"]
    (set-state! :current-search-string search-string)
    (set-state! :num-results 0)
    (set-state! :results nil)
    (set-state! :searching true)
    (GET (str base text filter)
        {:keywords? true
         :response-format :json
         :handler handler
         :error-handler error-handler
         :finally finalize-search})))

(defn format-loc-date [date-string]
  (let [all (str/trim date-string)
        year (subs all 0 4)
        month-num (subs all 4 6)
        month (["" "Jan" "Feb" "Mar" "Apr" "May" "Jun"
                "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"] month-num)
        date (subs all 6 8)]
    [:span month " " date ", " year]))

(defn result-row [{:keys [city date title url]}]
  [:div
   [:b (if (vector? city) (first city) city)]
   " "
   [:em (format-loc-date date)]
   " "
   [:a {:href url} title]])

(defn home-page []
  (fn []
    [:div [:h2 "Old News browser"]
     [:div [:p "Tomorrow's way to see yesterday today"]]
     ;; [TODO] Understand why form causes page refresh. Seems to be related
     ;; to it adding search string as a param to the page URL
     ;;[:form
      [:fieldset
       [:legend "Search for some good news"]
       [:label {:for :text1} "Enter text: "]
       [search-input :text1]
       [:br]
       [:button {:type "submit"
                 :class "btn btn-default"
                 :id :search
                 :on-click launch-search}
        "Search now"]]
     ;;]

     (let [search-string (get-state :current-search-string)
           search-results (get-state :results)]
       [:div
        (if (get-state :searching)
          [:div {:id "searching"}
           [:em "Searching for " search-string]]
          [:div])
        (if (str/blank? search-results)
          [:div]
          (map (fn [row] [result-row row]) (get-state :results)))])
     [:div [:a {:href "#/about"} "go to about page"]]
     [:div [:a {:href "#/debug"} "See internal app state"]]]))

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
