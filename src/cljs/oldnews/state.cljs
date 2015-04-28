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
  (:require [reagent.core :as r]))

(defonce the-state (r/atom nil))

(defn set! [keys value]
  (swap! the-state assoc-in keys value))

(defn getval [keys]
  (get-in @the-state keys))


;; [TODO] sadd! and sremove! don't yet handle nested keys
(defn append! [key value]
  (swap! the-state
         (fn [root]
           (let [old (key root)]
             (assoc root key
                    (if (set? old)
                      (conj old value)
                      (hash-set value)))))))

(defn remove! [key value]
  (swap! the-state
         (fn [root]
           (assoc root key (disj (key root) value)))))


(defn cursor [keys]
  (r/cursor the-state keys))


(defn inc! [keys increment default minval maxval]
  (swap! the-state
         (fn [root]
           (let [old (get-in root keys)
                 new (->> (or old default) (+ increment) (min maxval) (max minval))]
             (assoc-in root keys new)))))
