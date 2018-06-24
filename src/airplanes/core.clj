(ns airplanes.core
  (:require [clj-jgit.porcelain :as g]
            [clojure.java.io :as io]
            [snow.env :refer [profile]]
            [clojure.edn :as edn]
            [clojure.pprint :as p]))

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

(defn build-project [name {:keys [git-url trigger build]}]
  (let [wd  (-> name gen-wd .getAbsolutePath)
        repo (make-repo wd git-url)]
    (println "Cloned succesfully")
    (set! *print-namespace-maps* false)
    (def wd wd)
    (def repo repo)
    (write-upgrade wd)
    (g/git-add repo "deps.edn")
    (g/git-commit repo "bump deps" {:name "airplane-bot"
                                    :email "shakdwipeea@gmail.com"})
    (g/git-push repo)))

#_(edn/read-string (pr-str (slurp (io/file wd "deps.edn"))))

#_(g/with-identity {:name "~/keys/bot-ssh-key"}
    (build-project "entrance-plus" {:git-url "git@github.com:entranceplus/ep-build.git"}))

#_(build-project "voidwalker" {:git-url "https://github.com/entranceplus/voidwalker"})

(defn build [{projects :projects}]
  (io/make-parents build-folder)
  (map build-project projects))
