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
            [clojure.set :as set]
            [cemerick.url :refer [url-encode]]
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
           :on-change #(sset! [id] (-> % .-target .-value))}])


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



(defn result-row [{:keys [search-string city date title pdf sequence]}]
  [:div
   [:b (format-loc-date date)]
   " "
   [:em (str/join ", " city)]
   " "
   (let [last-char (-> title count dec)
         clean-title (if (= (get title last-char) ".")
                       (subs title 0 last-char)
                       title)
         title-with-pagenum (str clean-title ", p" sequence)]
     (if pdf
       [:a {:href (str pdf "#search=" (url-encode search-string))
            :target "_blank"}
        title-with-pagenum]
       title-with-pagenum))])


(defn handle-page-data-results [key response]
  (sset! [:results key :pdf] (:pdf response)))


(defn handle-set-of-search-results [search-string response]
  (let [{:keys [totalItems items]} response
        item-data (map #(-> %
                            (select-keys [:id :title :sequence :date :city :url])
                            (set/rename-keys {:id :key})
                            (assoc :search-string search-string))
                       items)]
    (sset! [:num-results] totalItems)
    ;; Enable full-results when needing to explore the entire response
    ;; (sset! [:full-results] response)
    (dorun (map
            (fn [{:keys [key url] :as full-item}]
              (sset! [:results key] full-item)
              (get-url url
                       (fn [response]
                         (handle-page-data-results key response))
                       key))
            item-data))))


(defn launch-search []
  (let [search-string (sget :text1)
        base "http://chroniclingamerica.loc.gov/search/pages/results/?"
        text (str "proxtext=" search-string)
        filter "&dateFilterType=yearRange&date1=1800&date2=1930&format=json"]
    (sset! [:num-results] 0)
    (sset! [:results] nil)
    (sset! [:searching] true)
    (get-url (str base text filter)
             (fn [response]
               (handle-set-of-search-results search-string response))
             search-string)))

(defn button [text id on-click]
  [:button {:type "submit"
            :class "btn btn-default"
            :id id
            :on-click on-click}
   text])

(defn radio [name default values-and-texts]
  [:div (map (fn [[value text]]
               (let [props {:type "radio" :name name :value value}]
                 [:input
                  (if (= value default) (assoc props :checked "checked") props)
                  text]))
             (partition 2 values-and-texts))])

(defn select [name default values-and-texts]
  [:select {:name name}
   (map (fn [[value text]]
          (let [props {:value value}]
            [:option
             (if (= value default) (assoc props :selected "selected") props)
             text]))
        (partition 2 values-and-texts))])

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
       [button "Search now" :search launch-search]
       [:br]
       [button "Prev page" :search launch-search]
       [button "Next page" :search launch-search]
       [:br]
       [select "sort-order" :relevance
        [:date "Date"
         :relevance "Relevance"]]]
     ;;]

     (let [search-results (sget :results)]
       [:div
        (let [open-searches (sget :searching)]
          (if (empty? open-searches)
            [:div]
            [:div {:id "searching"}
             [:em "Searching: (" (count open-searches) " pending)"]]))
        (if (str/blank? search-results)
          [:div]
          (map (fn [[_ row]] [result-row row]) search-results))])
     [:div [:a {:href "#/about"} "go to about page"]]
     [:div [:a {:href "#/debug"} "See internal app state"]]]))
