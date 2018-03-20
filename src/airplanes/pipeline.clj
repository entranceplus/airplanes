(ns airplanes.pipeline
  (:use [lambdacd.steps.control-flow]
        [airplanes.steps :as steps])
  (:require [lambdacd-git.core :as git]
            [lambdacd.steps.manualtrigger :as manualtrigger]))

(def demo-pipeline-def
  `(
    manualtrigger/wait-for-manual-trigger
    some-step-that-does-nothing
    some-step-that-echos-bar
    manualtrigger/wait-for-manual-trigger
    some-failing-step))

(def other-pipeline-def
  `(
    manualtrigger/wait-for-manual-trigger
    some-step-that-does-nothing
    some-step-that-echos-foo))



(defn clone-repo [repo]
  (fn [args ctx]
    (git/clone ctx repo  (:revision args) (:cwd args))))

(defn wait-for-repo [repo]
  (fn [_ ctx]
    (git/wait-for-git ctx repo)))

(defn make-clj-pipeline [{:keys [repo env]}]
  `((either
      manualtrigger/wait-for-manual-trigger
      (wait-for-repo ~repo))
    (with-workspace
      (clone-repo ~repo)
      build-uberjar
      deploy-jar)))

(def kongauth-pipeline
  (make-clj-pipeline {:repo "https://github.com/entranceplus/kongauth"
                      :env {:dbuser "kong"
                            :password "functor"
                            :host "127.0.0.1"
                            :db "auth"
                            :http-port "8081"}}))
