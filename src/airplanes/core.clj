(ns airplanes.core
  (:require [clj-jgit.porcelain :as g]
            [clojure.java.io :as io]
            [snow.env :refer [profile]]
            [clojure.edn :as edn]
            [airplanes.deploy :as d]
            [clojure.pprint :as p]
            [timely.core :as timely]
            [clojure.java.shell :refer [sh]]))

#_(timely/start-scheduler)

(def build-folder (-> (profile) :build-folder))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def repo)

(def wd)

(defn read-deps [wd]
  (->  wd
      (io/file "deps.edn")
      slurp
      edn/read-string))

(defn gen-wd
  ([] (gen-wd "new-repo"))
  ([name] (io/file build-folder (uuid) name)))

(defn make-repo [wd git-url]
  (-> git-url (g/git-clone-full wd) :repo))

(defn get-latest-hash [git-url]
  (-> (gen-wd)
     (make-repo git-url)
     g/git-log
     first
     .getName))

(defn update-deps [deps]
  (->> deps 
     (map (fn [[dep-name {:keys [git/url sha] :as dep-data}]]
            {dep-name (if (some? url) 
                        (let [new-sha (get-latest-hash url)]                          
                          (cond-> dep-data
                            (not= sha new-sha) (merge {:sha new-sha})))
                        dep-data)}))
     (into {})))

(defn run-upgrade [dir]
  (let [{:keys [deps] :as deps-details} (-> dir read-deps)]
    (merge deps-details
           {:deps (update-deps deps)})))

;; TODO unable to write namespaced keys in edn
(defn write-upgrade [dir]
  (let [new-deps (run-upgrade dir)]
    (spit (io/file dir "deps.edn") 
          (-> new-deps p/pprint with-out-str))))

#_(write-upgrade wd)
#_(slurp (io/file wd "deps.edn"))

(defn run-build [{:keys [build wd]}]
  (let [{:keys [exit out err]} (sh build :dir wd)]
    (println out err)
    (when-not (= exit 0)
      (throw (ex-info "Build failed. Not proceeding further." {:directory wd})))))

(defn build-project [{:keys [name git-url update? build] :as p}]
  (io/make-parents build-folder)
  (let [wd  (-> name gen-wd .getAbsolutePath)
        repo (make-repo wd git-url)] 
    (println "Cloned succesfully")
    (when update?
      (set! *print-namespace-maps* false)
      (write-upgrade wd)
      (g/git-add repo "deps.edn")
      (g/git-commit repo "bump deps" {:name "airplane-bot"
                                      :email "shakdwipeea@gmail.com"})
      (run-build p)
      (g/git-push repo))
    (run-build p)))

#_(edn/read-string (pr-str (slurp (io/file wd "deps.edn"))))

#_(g/with-identity {:name "~/keys/bot-ssh-key"}
    (build-project "entrance-plus" {:git-url "git@github.com:entranceplus/ep-build.git"}))

#_(build-project "voidwalker" {:git-url "https://github.com/entranceplus/voidwalker"})

(defn build [{projects :projects}]
  (io/make-parents build-folder)
  (map build-project projects))

(def project (-> "deploy.edn"
                slurp
                edn/read-string
                :ep-build))

(defn read-config []
  (-> "deploy.edn"
     slurp
     edn/read-string))

(defn deploy-project [{:keys [working-directory name ip]}]
  (let [{:keys [working-directory name]} project] 
    (d/exec-cmd ip (str "cd " working-directory " && git pull"))
    (d/exec-cmd ip (str "systemctl restart " name))
    (d/exec-cmd ip (str "systemctl status " name))))

(defn build-and-deploy [project]
  (-> project
     build-project
     deploy-project))

(defn run-from-config []
  (println "Running deploy pipeline")
  (map (fn [[name proj]]
         (build-and-deploy proj)) (read-config)))

(defn -main [& args]
  (timely/scheduled-item (timely/every 6
                                       :hours)
                         (run-from-config)))
