(ns airplanes.steps
  (:require [lambdacd-git.core :as lambdacd-git]
            [lambdacd.steps.shell :as shell]))

(defn run-some-tests [args ctx]
  (shell/bash ctx (:cwd args) "lein test"))

(defn build-uberjar [args ctx]
  (shell/bash ctx (:cwd args) "lein uberjar"))

(defn deploy-jar [args ctx]
  (shell/bash ctx (:cwd args) "cp builds/airplanes/lambdacd/target/uberjar/server.jar /tmp/"))

(defn some-step-that-does-nothing [args ctx]
  {:status :success})

(defn some-step-that-echos-foo [args ctx]
  (shell/bash ctx "/" "echo foo"))

(defn some-step-that-echos-bar [args ctx]
  (shell/bash ctx "/" "echo bar"))

(defn some-failing-step [args ctx]
  (shell/bash ctx "/" "echo \"i am going to fail now...\"" "exit 1"))
