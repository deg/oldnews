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



(ns oldnews.ajax
  (:require [ajax.core :refer [GET POST]]
            [oldnews.state :refer [sget sset!]]))


(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))


(defn get-url [url handler searching-flag]
  (sset! searching-flag true)
  (GET url {:response-format :json
            :keywords? true
            :handler handler
            :error-handler error-handler
            :finally #(sset! searching-flag false)
            }))
