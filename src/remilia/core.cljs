(ns ^:figwheel-hooks remilia.core
  (:require [re-frame.core :as r]
            [reagent.core :as re]
            [reagent.dom :as rd]))

(def app-db (re/atom {}))

(defn app []
  [:div.flex.justify-center.mt-8
   [:div.flex.flex-col.items-center
    [:h1.text-2xl "flandre"]]])

(defn ^:after-load render []
  (rd/render [app]
             (.getElementById js/document "app")))
