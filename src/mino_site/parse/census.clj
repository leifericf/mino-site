(ns mino-site.parse.census
  "Load and validate the census site payload EDN.

  The payload is emitted by clojure-census at
  output/<dialect>/site_payload.edn and vendored into
  resources/census/site_payload.edn. It is the single source of
  truth for divergence entries, coverage stats, and the missing-
  surface split."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]))

(def default-path
  "Default location of the vendored census payload."
  "resources/census/site_payload.edn")

(defn load-payload
  "Read and return the census site payload EDN from `path`.
  Throws if the file is missing or malformed."
  ([]
   (load-payload default-path))
  ([path]
   (let [f (io/file path)]
     (when-not (.exists f)
       (throw (ex-info (str "Census payload not found: " path)
                       {:path path})))
     (-> path slurp edn/read-string))))

(defn divergences-by-category
  "Group payload divergences into a vector of
  {:category cat :divergences [d ...]} blocks, ordered by the
  payload's :categories declaration order. Categories with zero
  divergences are omitted."
  [payload]
  (let [cat-order  (into {} (map-indexed (fn [i c] [(:id c) i])
                                         (:categories payload)))
        groups     (group-by #(get-in % [:category :id])
                             (:divergences payload))]
    (->> groups
         (sort-by (fn [[cat-id _]] (cat-order cat-id 999)))
         (mapv (fn [[cat-id divs]]
                 {:category   (get-in divs [0 :category])
                  :divergences (vec divs)})))))

(defn coverage-percent
  "Return the headline coverage percent as a string like '90.9%'."
  [payload]
  (format "%.1f%%" (* 100 (double (get-in payload
                                          [:coverage :headline :percent])))))
