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



;;; Manage mutable atom holding our virtual DOM state.

;;; [TODO] Control access to state and limit/document the set of valid keys.
;;; [TODO] Tools to inspect the state.

(ns oldnews.state
  (:require [reagent.core :refer [atom]]))

(defonce the-state (atom nil))

(defn sset! [keys value]
  (swap! the-state assoc-in keys value))

(defn sadd! [key value]
  (swap! the-state
         (fn [root]
           (let [old (key root)]
             (assoc root key
                    (if (set? old)
                      (conj old value)
                      (hash-set value)))))))

(defn sremove! [key value]
  (swap! the-state
         (fn [root]
           (assoc root key (disj (key root) value)))))


(defn sget [key]
  (key @the-state))
