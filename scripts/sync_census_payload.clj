(ns scripts.sync-census-payload)

;; Sync the census site payload from a local clojure-census checkout
;; into resources/census/site_payload.edn. Run after the census
;; divergences, coverage, or missing-reasons data changes.
;;
;; Usage:
;;   ./mino/mino scripts/sync_census_payload.clj
;;   CENSUS_DIR=../clojure-census ./mino/mino scripts/sync_census_payload.clj
;;
;; Exits 0 if the payload was already up-to-date, 0 if synced, 1 on
;; error.

(require '[clojure.string :as str])

(def census-dir
  (or (getenv "CENSUS_DIR")
      (when (file-exists? "../clojure-census") "../clojure-census")
      (when (file-exists? "../../clojure-census") "../../clojure-census")
      nil))

(when (nil? census-dir)
  (println "ERROR: clojure-census checkout not found.")
  (println "Set CENSUS_DIR or ensure ../clojure-census exists.")
  (exit 1))

(def payload-src (str census-dir "/output/mino/site_payload.edn"))
(def payload-dst "resources/census/site_payload.edn")

(println "Generating payload from census at" census-dir "...")
(sh! "sh" "-c" (str "cd " census-dir " && clojure -M:run payload mino"))

(when-not (file-exists? payload-src)
  (println "ERROR: payload was not generated at" payload-src)
  (exit 1))

(mkdir-p "resources/census")

(if (and (file-exists? payload-dst)
         (= (slurp payload-src) (slurp payload-dst)))
  (do (println "Payload already up-to-date.")
      (exit 0))
  (do (spit payload-dst (slurp payload-src))
      (println "Synced" payload-dst)
      (exit 0)))
