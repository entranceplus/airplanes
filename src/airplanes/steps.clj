(ns airplanes.steps
  (:require [lambdacd-git.core :as lambdacd-git]
            [lambdacd.steps.shell :as shell]))

(defn run-some-tests [args ctx]
  (shell/bash ctx (:cwd args) "lein test"))

(defn build-uberjar [args ctx]
  (shell/bash ctx (:cwd args) "boot build"))

(defn env-props [env]
  (reduce-kv (fn [env key value]
               (str env (str " " "-D" (name key) "=" value)))
             "" env))

(defn gen-jar-cmd [jar-file env]
  (str "java -jar " jar-file (env-props env)))

(defn run-jar [env]
  (fn [args ctx]
    (shell/bash ctx (:cwd args) (gen-jar-cmd "target/kongauth-0.1.0-SNAPSHOT-standalone.jar" env))))

(defn deploy-jar [args ctx]
  (shell/bash ctx (:cwd args) "scp target/kongauth-0.1.0-SNAPSHOT-standalone.jar root@139.59.89.233:"))

(defn some-step-that-does-nothing [args ctx]
  {:status :success})

(defn some-step-that-echos-foo [args ctx]
  (println "Context is " ctx)
  (shell/bash ctx "/" (str "echo " ctx)))

(defn some-step-that-echos-bar [args ctx]
  (shell/bash ctx "/" "echo bar"))

(defn some-failing-step [args ctx]
  (shell/bash ctx "/" "echo \"i am going to fail now...\"" "exit 1"))
