;;; Copyright (c) 2015 David Goldfarb. All rights reserved.
;;; Contact info: deg@degel.com
;;;
;;; The use and distribution terms for this software are covered by the Eclipse
;;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
;;; be found in the file epl-v10.html at the root of this distribution.
;;; By using this software in any fashion, you are agreeing to be bound by the
;;; terms of this license.
;;;
;;; You must not remove this notice, or any other, from this software.



(ns oldnews.pages.home
  (:require [clojure.string :as str]
            [oldnews.state :refer [sget sset!]]
            [oldnews.ajax :refer [get-url]]))


(defn input-element
  "An input element which updates its value on change"
  [id name type]
  [:input {:id id
           :name name
           :class "form-control"
           :type type
           :required ""
           :value (sget id)
           :on-change #(sset! id (-> % .-target .-value))}])


(defn search-input [state-id]
  (input-element state-id "searchVal" "text"))


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


(defn handler [response]
  (let [{:keys [totalItems items]} response
        item-data (map #(-> %
                            (select-keys [:city :date :title :url :id])
                            (clojure.set/rename-keys {:id :key}))
                       items)]
    (sset! :num-results totalItems)
    (sset! :results item-data)))


(defn launch-search []
  (let [search-string (sget :text1)
        base "http://chroniclingamerica.loc.gov/search/pages/results/?"
        text (str "proxtext=" search-string)
        filter "&dateFilterType=yearRange&date1=1800&date2=1930&format=json"]
    (sset! :current-search-string search-string)
    (sset! :num-results 0)
    (sset! :results nil)
    (sset! :searching true)
    (get-url (str base text filter) handler :searching)))


(defn page []
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

     (let [search-string (sget :current-search-string)
           search-results (sget :results)]
       [:div
        (if (sget :searching)
          [:div {:id "searching"}
           [:em "Searching for " search-string]]
          [:div])
        (if (str/blank? search-results)
          [:div]
          (map (fn [row] [result-row row]) (sget :results)))])
     [:div [:a {:href "#/about"} "go to about page"]]
     [:div [:a {:href "#/debug"} "See internal app state"]]]))