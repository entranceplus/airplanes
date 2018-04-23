(ns airplanes.boot
  (:require [boot.core :refer [deftask with-pre-wrap] :as boot]
            [clojure.java.io :as io]
            [airplanes.ubuntu :as u
             ]
            [airplanes.deploy :as d]))

(deftask deploy
  "Deploy the application to remote"
  [f file PATH str "The jar file to install."
   i ip IP str "Ip where to deploy"
   n name NAME str "Name of app"
   d dir DIR str "Directory to create"
   p pom PATH  str "The pom.xml file to use."
   m main MAIN str "clojure main"]
  (boot/with-pass-thru [fs]
    (let [jarfiles (or (and file [(io/file file)])
                       (->> (boot/output-files fs)
                            (boot/by-ext [".jar"])
                            (map boot/tmp-file)))]
      (when-not (seq jarfiles) (throw (Exception. "can't find jar file")))
      (doseq [jarfile jarfiles]
        (println "Installing %s...\n" (.getName jarfile))
        (d/copy-jar ip (.getAbsolutePath jarfile) name)
        (when (some? dir) (d/exec-cmd ip  (str "mkdir -p " (str "/root/" name "/" dir))))
        (d/setup-systemd ip {:working-directory (str "/root/" name)
                             :description (str name "-service")
                             :name name
                             :exec-start (str "/usr/bin/java -jar "
                                              (.getName jarfile)
                                              (str " -m " main))})))))

