(ns airplanes.deploy
  (:require [clojure.core.async :refer [chan >!! <!!]]
            [snow.client :refer [get post delete]]
            [clj-ssh.ssh :as ssh]
            [selmer.parser :as selmer]
            [clojure.string :as str]
            [airplanes.ubuntu :as ubuntu]
            [clojure.java.io :as io]))


(def res-chan (chan 50))

(def metadata (atom {}))
;
; ;; (>!! res-chan "a")
; ;; (<!! res-chan)
;
; (shell/bash {:result-channel res-chan
;              :config {:name "Airplanes"
;                       :home-dir "builds"
;                       :max-builds 3}
;              :is-killed (atom false)
;              :build-metadata-atom metadata}
;            "/mnt/d/workspace"
;            "ls")
;
(def digital-ocean-token "")
(def linode-api-key "")

(def linode-base-url "https://api.linode.com/v4/")
(def digo-base-url "https://api.digitalocean.com/v2/")


(defn do-url [endpoint]
  (str linode-base-url endpoint))

(defn do-request [request-type url & {:keys [body]}]
  (let [headers {:authorization (str "Bearer " linode-api-key)}]
    (case request-type
      :get (get (do-url url)
                {:headers headers})
      :post (post (do-url url)
                  :body body
                  :headers headers)
      :delete (delete (do-url url)
                      :headers headers))))

;; (def regions (do-request :get "regions"))

;; (do-request :get "linode/types")

;; (do-request :get "images")

(defn create-droplet [& {:keys [label ssh-key]}]
  (do-request :post
              "linode/instances"
              :body {:label (str label "_run")
                     :region "ap-south-1a"
                     :type "g5-nanode-1"
                     :root_pass "worldcup0*&4C"
                     :image "linode/ubuntu16.04lts"
                     :authorized_keys [ssh-key]}))

(defn read-ssh-key [path]
  (-> path slurp clojure.string/trim-newline))

;; (read-ssh-key "D:\\.ssh\\id_rsa.pub")

; (:body (create-droplet :label "gamma"
;                        :ssh-key (read-ssh-key "D:\\.ssh\\id_rsa.pub")))

(defn get-linodes []
  (do-request :get
              "linode/instances"))

;; (get-linodes)

(defn delete-linode [id]
  (do-request :delete
              (str "linode/instances/" id)))

(defn reboot-linode [id]
  (do-request :post (str "linode/instances/" id "/reboot")
              :body {}))

;; (reboot-linode "6841403")

;; (delete-droplet "6826167")

;; this is private because for some reason assigning this value to a session does not
;;   work out so create a new session at the end of every))
(defn- create-session [ip ssh-path]
  (let [agent (ssh/ssh-agent {})]
    (ssh/add-identity agent {:private-key-path (str ssh-path "\\id_rsa")
                             :public-key-path (str ssh-path "\\id_rsa.pub")})
    (ssh/session agent
                 ip
                 {:strict-host-key-checking :no
                  :username "root"})))

(defn exec-cmd [ip cmd & {:keys [ssh-dir]}]
  (let [session (create-session ip (or ssh-dir
                                       "D:\\.ssh"))]
    (ssh/with-connection  session
          (ssh/ssh session {:cmd cmd}))))

(defn print-cmd [ip cmd]
  (println (:out (exec-cmd ip cmd))))

(defn copy-file [ip & {:keys [src dest ssh-dir]}]
  (let [session (create-session ip (or ssh-dir
                                       "D:\\.ssh"))]
    (ssh/with-connection session
      (let [channel (ssh/ssh-sftp session)]
        (ssh/with-channel-connection channel
          (ssh/sftp channel {} :put src dest))))))

(def ip "139.162.31.74")

;; Install postgres
;; (exec-cmd "139.162.31.74" (ubuntu/install-package "postgresql"))
;; (exec-cmd "139.162.31.74" (ubuntu/install-package "nginx"))
;; (exec-cmd "139.162.31.74" (ubuntu/service-status "postgresql"))

(defn read-nginx []
  (-> "nginx.conf"
      io/resource
      slurp))

; (defn reload-nginx [docker]
;   (exec-command docker '("nginx" "-s" "reload")
;                         (get-nginx-container docker)))
;
(defn gen-nginx [apps]
  (-> (read-nginx)
      (selmer/render {:apps (map (fn [{:keys [ip] :as app}]
                                   (cond-> app
                                     (nil? ip) (merge {:ip "localhost"})))
                                apps)})))

(def apps [{:host "content.entranceplus.in"
            :port 8000}
           {:host "entranceplus.in"
            :port 7000}])

(defn setup-nginx [ip apps]
  (let [temp-file "resources/sites.conf"]
   (do (spit temp-file (gen-nginx apps))
       (copy-file ip :src temp-file
                     :dest "/etc/nginx/conf.d/ep.conf")
       (exec-cmd ip "nginx -s reload"))))

;; (setup-nginx ip apps)

(defn copy-jar [ip]
  (do (:out (exec-cmd ip "mkdir ep"))
      (copy-file ip
                 :src "resources/entrance-plus-0.1.0-SNAPSHOT-standalone.jar"
                 :dest "/root/ep/")))

;; (copy-jar "139.162.31.74")
;; (println (:out (exec-cmd ip (ubuntu/install-java))))
;; (exec-cmd ip (ubuntu/check-java))

;; postgres conf path  /etc/postgresql/9.5/main/pg_hba.conf

(defn gen-systemd-config [config]
  (selmer/render (slurp (io/resource "systemd.service")) config))

(def app {:description "Ep container"
          :working-directory "/root/ep"
          :name "ep"
          :exec-start "/usr/bin/java -jar /root/ep/entrance-plus-0.1.0-SNAPSHOT-standalone.jar"})

(defn setup-systemd [ip {:keys [name] :as app}]
  (let [temp-file (str "resources/" name ".service")]
   (do (spit temp-file (gen-systemd-config app))
       (copy-file ip :src temp-file
                     :dest (str "/lib/systemd/system/" name ".service"))
       ;; create sth like exec-cmd(s)
       (exec-cmd ip "systemctl daemon-reload")
       (exec-cmd ip (ubuntu/enable-service name)))))

; (print-cmd ip (ubuntu/service-status "ep"))
;; (exec-cmd ip "certbot --help")
; (setup-systemd ip app)

;; Installing certbot
;; this works
;; (print-cmd ip (ubuntu/install-certbot))

;; probably does not work
;;(print-cmd ip (ubuntu/get-certificates ["entranceplus.in"]))
