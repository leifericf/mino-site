#!/usr/bin/env bb
;;
;; Sync the census site payload from a local clojure-census checkout
;; into resources/census/site_payload.edn. Run after the census
;; divergences, coverage, or missing-reasons data changes.
;;
;; Usage:
;;   bb scripts/sync-census-payload.bb
;;   CENSUS_DIR=../clojure-census bb scripts/sync-census-payload.bb
;;
;; Exits 0 if the payload was already up-to-date, 0 if synced, 1 on
;; error.

(require '[babashka.process :refer [shell]])
(require '[babashka.fs :as fs])
(require '[clojure.string :as str])

(def census-dir
  (or (System/getenv "CENSUS_DIR")
      (when (fs/exists? "../clojure-census")
        "../clojure-census")
      (when (fs/exists? "../../clojure-census")
        "../../clojure-census")
      nil))

(when (nil? census-dir)
  (binding [*out* *err*]
    (println "ERROR: clojure-census checkout not found.")
    (println "Set CENSUS_DIR or ensure ../clojure-census exists."))
  (System/exit 1))

(def payload-src (str census-dir "/output/mino/site_payload.edn"))
(def payload-dst "resources/census/site_payload.edn")

(println "Generating payload from census at" census-dir "...")
(shell {:dir census-dir} "clojure -M:run payload mino")

(when-not (fs/exists? payload-src)
  (binding [*out* *err*]
    (println "ERROR: payload was not generated at" payload-src))
  (System/exit 1))

(fs/create-dirs (fs/parent payload-dst))

(if (and (fs/exists? payload-dst)
         (= (slurp payload-src) (slurp payload-dst)))
  (do (println "Payload already up-to-date.")
      (System/exit 0))
  (do (fs/copy payload-src payload-dst {:replace-existing true})
      (println "Synced" payload-dst)
      (System/exit 0)))
