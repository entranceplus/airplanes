(ns airplanes.ubuntu
  (:require [clojure.string :as str]
            [clojure.java.shell :as s]))

;; #!/usr/bin/env boot

;;(set-env! :dependencies '[])
;; (require '[clojure.java.shell :as s])
;; thoughts-> we can also build up all the commands
;; and then execute as a batch job


;;todo update all this to use aptly

;; what follows is just for fun
;; this could be flawed concept/code

(def xampp-binary "xampp-linux-x64-5.6.30-0-installer.run")

(def xampp-install-location "/opt/lampp/")

;;;;;;;;;;;;;;
;; packages ;;
;;;;;;;;;;;;;;

(defn cmd-str [& cmds]
  (str/join " " cmds))

(defn cmd-chain [& cmds]
  (str/join " && " (map (fn [cmd]
                          (cond->> cmd
                            (sequential? cmd) (apply cmd-str)))
                    cmds)))

(defn update-packages []
  (cmd-str "apt-get" "update" "-y"))

(defn install-package [package]
  (cmd-chain ["apt" "update" "-y"]
             ["apt" "install" package "-y"]))

(defn install-packages [packages]
  (cmd-chain ["apt" "update" "-y"]
             ["apt" "install" (str/join " " packages) "-y"]))

;; (install-packages ["nginx" "ubuntu"])
;;;;;;;;;;;;;;;;;;;;;
;; ubuntu services ;;
;;;;;;;;;;;;;;;;;;;;;

(defn enable-service [service]
  (cmd-chain ["systemctl" "enable" service]
             ["systemctl" "restart" service]))

(defn service-status [service]
  (cmd-str "systemctl" "status" service))

;;;;;;;;;;;;;;;;;;;;;
;; letsencrypt      ;;
;;;;;;;;;;;;;;;;;;;;;

(defn install-certbot []
  (cmd-chain (install-packages ["software-properties-common"
                                "python-software-properties"])
             ["add-apt-repository"  "ppa:certbot/certbot" "-y"]
             (install-package "python-certbot-nginx")))

; (install-certbot)

(defn get-certificates [domains]
  (let [domains-str (map #(cmd-str "-d" %) domains)]
    (apply cmd-str (concat ["certbot" "--nginx"] domains-str))))

;; (get-certificates ["example.com" "www.example.com"])
;;;;;;;;;;;
;; mysql ;;
;;;;;;;;;;;

(defn mysql-present? []
  (try  (s/sh "/opt/lampp/bin/mysql")
        (catch Exception _)))

(defn install-xampp! []
  (s/sh "wget" (str "https://www.apachefriends.org/xampp-files/5.6.30/"
                  xampp-binary))
  (s/sh "chmod" "a+x" xampp-binary)
  (s/sh (str "./" xampp-binary) "--mode" "unattended"))


(defn create-mysqldb! [db]
  (s/sh (str xampp-install-location "bin/mysql")
        "-u" "root" "-e" (s/sh (str "create database if not exists " db))))


(defn start-mysql! [database]
  (s/sh update-packages)
;; todo add if condition to check
  (when-not (mysql-present?)
    (install-xampp!))
  (s/sh (str xampp-install-location "lampp") "startmysql")
  (create-mysqldb!))


;;;;;;;;;;
;; java ;;
;;;;;;;;;;

(defn install-java []
  (install-package "default-jdk"))

(defn check-java []
  (cmd-str "java" "-version"))

(def install-clojure 
  "curl -O https://download.clojure.org/install/linux-install-1.9.0.381.sh && chmod +x linux-install-1.9.0.381.sh && sudo ./linux-install-1.9.0.381.sh")

(defn set-env [env-vars]
  (map (fn [k v]
         (str "echo  \" as\" + \"NVM_DIR=/usr/local/nvm\" >> ~/.bashrc"))))



;;;;;;;;;;;
;; nginx ;;
;;;;;;;;;;;

(defn configure-nginx []
  (s/sh (str xampp-install-location "lampp" "stopapache"))
  (s/sh install-package "nginx")
  (s/sh "cp" "-ru" "default" "/etc/nginx/sites-available/default")
  (enable-service "nginx"))

;;;;;;;;;;;;;;;;
;; app config ;;
;;;;;;;;;;;;;;;;

                                        ; (defn app-config []
                                        ;   (s/sh "cp" "./voidwalker.service" "/lib/systemd/system/voidwalker.service")
                                        ;   (set-env! :database-url "jdbc:mysql://localhost/voidwalker?user=root&password=")
                                        ;   (s/sh "java" "-jar" "voidwalker.jar" "migrate")
                                        ;   (s/sh "systemctl" "daemon-reload")
                                        ;   (enable-service "voidwalker"))
                                        ;
                                        ;
                                        ; (defn -main [& args]
                                        ;   (println "Starting setup")
                                        ;   (do (println (:out (s/sh "ls")))
                                        ;       (s/sh "cd" "voidwalker")
                                        ;       (start-mysql!)
                                        ;       (app-config)
                                        ;       (configure-nginx))
                                        ;   (println "Setup complete"))
