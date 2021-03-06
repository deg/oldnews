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



(ns oldnews.pages.debug
  (:require [clojure.string :as str]
            [json-html.core :refer [edn->html edn->hiccup]]
            [oldnews.state :as state]))


(defn render-all-state [root]
  (-> @root edn->hiccup))


(defn page []
  [:div [:h2 "App state"]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:div [:a {:href "#/about"} "go to about page"]]
   (render-all-state state/the-state)])
