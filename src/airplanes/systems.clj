(ns airplanes.systems
  (:require [system.core :refer [defsystem]]
            [lambdacd.steps.shell :as shell]
            [lambdacd.steps.manualtrigger :as manualtrigger]
            [com.stuartsierra.component :as component]
            [airplanes.pipeline :as pipelines]
            (system.components
             [immutant-web :refer [new-immutant-web]]
             [repl-server :refer [new-repl-server]]
             [lambdacd :refer [new-lambdacd-pipeline]]
             [middleware :refer [new-middleware]]
             [http-kit :refer [new-web-server]]
             [handler :refer [new-handler]]
             [endpoint :refer [new-endpoint]])
            [environ.core :refer [env]]
            [lambdacd.core :as lambdacd]
            [airplanes.handler :refer [ui-routes app]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [lambdacd.runners :as runners]))

(defn base-system []
  (component/system-map
   :pipelines (new-lambdacd-pipeline {:kongauth pipelines/kongauth-pipeline
                                      :other pipelines/other-pipeline-def}
                                     {:name "Airplanes"
                                      :home-dir "builds"
                                      :max-builds 3
                                      :ui-config {:expand-active-default true
                                                  :expand-failures-default true}})
   :routes (component/using (new-endpoint ui-routes)
                            [:pipelines])
   :middleware (new-middleware {:middleware [wrap-restful-format
                                             [wrap-defaults site-defaults]]})
   :handler (component/using
             (new-handler)
             [:routes])
   :http (component/using
          (new-web-server (Integer. (env :http-port)))
          [:handler])))

(defsystem prod-system
  [:web (new-immutant-web :port (Integer. (env :http-port))
                          :handler app)])

(defsystem dev-system
  [:web (new-immutant-web :port (Integer. (env :http-port))
                          :handler app)])
