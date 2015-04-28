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
            [cemerick.url :refer [url url-encode]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [oldnews.state :as state]
            [oldnews.ajax :refer [get-url]]))


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
  (state/set! [:results key :pdf] (:pdf response)))


(defn handle-set-of-search-results [search-string response]
  (let [{:keys [totalItems endIndex startIndex itemsPerPage items]} response
        item-data (map #(-> %
                            (select-keys [:id :title :sequence :date :city :url])
                            (set/rename-keys {:id :key})
                            (assoc :search-string search-string))
                       items)]
    (state/set! [:num-results] totalItems)
    (state/set! [:start-index] startIndex)
    (state/set! [:end-index] endIndex)
    ;; Enable full-results when needing to explore the entire response
    ;; (state/set! [:full-results] response)
    (dorun (map
            (fn [{:keys [key url] :as full-item}]
              (state/set! [:results key] full-item)
              (get-url url
                       (fn [response]
                         (handle-page-data-results key response))
                       key))
            item-data))))

(defn clear-search-results []
  (state/set! [:num-results] 0)
  (state/set! [:results] nil)
  (state/set! [:start-index] nil)
  (state/set! [:end-index] nil))



(defn launch-search []
  (let [search-string (state/getval [:forms :search :search-string])
        ;; Work around https://github.com/cemerick/url/issues/9
        fix-endslash #(assoc % :path (str (:path %) "/"))
        search-url (-> (url "http://chroniclingamerica.loc.gov/search/pages/results/")
                       (assoc :query {:proxtext search-string
                                      :page (or (state/getval [:page-number]) 1)
                                      :dateFilterType "yearRange"
                                      :date1 "1800"
                                      :date2 "1930"
                                      :format "json"})
                       fix-endslash
                       str)]
    (clear-search-results)
    (state/set! [:searching] true)
    (get-url search-url (fn [response]
                          (handle-set-of-search-results search-string response))
             search-string)))

(defn goto-page [n]
  (state/set! [:page-number] n)
  (launch-search))

(defn prev-page []
  (state/inc! [:page-number] -1 1 1 (-> [:num-results] state/getval (/ 20) Math.ceil))
  (launch-search))


(defn next-page []
  (state/inc! [:page-number] 1 1 1 (-> [:num-results] state/getval (/ 20) Math.ceil))
  (launch-search))


(defn button [bootstrap-class text id on-click]
  [:button {:type "submit"
            :class (str "btn " bootstrap-class)
            :id id
            :on-click on-click}
   text])


(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn input [label type id]
  (row label [:input.form-control {:field type :id id}]))


;; [TODO] convert to use reagent.forms
(defn radio [name default values-and-texts]
  [:div (map (fn [[value text]]
               (let [props {:type "radio" :name name :value value}]
                 [:input
                  (if (= value default) (assoc props :checked "checked") props)
                  text]))
             (partition 2 values-and-texts))])

;; [TODO] convert to use reagent.forms
(defn select [name default values-and-texts]
  [:select {:name name :value default}
   (map (fn [[value text]]
          [:option {:value value :key value} text])
        (partition 2 values-and-texts))])


;; [TODO] Merge with radio, above
(defn xradio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(def form-template
  [:div
   [:fieldset
    [:legend "Search for some good news"]
    (input "Search for" :text :search-string)
    [button "btn-primary" "Search now" :search (fn [] (goto-page 1))]
    [:br]
    [button "btn-default" "Prev page" :search prev-page]
    [button "btn-default" "Next page" :search next-page]
    [:br]
    [select "sort-order" :relevance
     [:date "Date"
      :relevance "Relevance"]]]])

(defn page []
  (let [search-form-cursor (state/cursor [:forms :search])]
    (fn []
      [:div [:h2 "Old News browser"]
       [:div [:h3 "Tomorrow's way to see yesterday today"]]
       ;; [TODO] Understand why form causes page refresh. Seems to be related
       ;; to it adding search string as a param to the page URL
       ;;[:form
       [:div [bind-fields form-template search-form-cursor]]
       ;;]

       (let [search-results (state/getval [:results])]
         [:div
          (let [open-searches (state/getval [:searching])]
            (when-not (empty? open-searches)
              [:div {:id "searching"}
               [:em "Searching: (" (count open-searches) " pending)"]]))
          (when-not (str/blank? search-results)
            [:div
             [:h5 "Results "
              (state/getval [:start-index])
              "-"
              (state/getval [:end-index])
              " of "
              (state/getval [:num-results])
              ]
             (map (fn [[_ row]] [result-row row]) search-results)])])
       [:div [:a {:href "#/about"} "go to about page"]]
       [:div [:a {:href "#/debug"} "See internal app state"]]])))
