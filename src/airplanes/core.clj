(ns airplanes.core
  (:gen-class)
  (:require
   [system.repl :refer [set-init! start]]
   [airplanes.systems :refer [prod-system base-system]]))

(defn -main
  "Start a production system."
  [& args]
  (let [system (or (first args) #'base-system)]
    (set-init! system)
    (start)))
