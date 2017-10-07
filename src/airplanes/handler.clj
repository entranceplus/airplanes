(ns airplanes.handler
  (:require [compojure.core :refer [context GET routes]]
            [hiccup.core :as h]
            ;; [lambdacd.ui.core :as reference-ui]
            [lambdaui.core :as lambdaui]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def app
  (wrap-defaults #'routes site-defaults))

(defn get-pipeline [name pipelines]
  (first (filter #(= (:name %)
                     (keyword name))
                 pipelines)))

(defn pipeline-view [pipelines bname]
  (lambdaui/ui-for   (get-pipeline bname pipelines)
                     :contextPath (str "/lambdaui/" bname)))

(defn ui-routes [{pipeline :pipelines}]
  (routes
   (context "/lambdaui/:bname" [bname]
            (pipeline-view (:pipelines pipeline)
                           bname))))
