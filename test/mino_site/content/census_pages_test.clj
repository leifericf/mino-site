(ns mino-site.content.census-pages-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [mino-site.parse.census :as census]
            [mino-site.content.compatibility-matrix :as matrix]
            [mino-site.content.from-clojure :as from-clojure]))

(def payload (census/load-payload))

;; ===== P0-T5: compatibility matrix ================================

(deftest matrix-renders-coverage-section
  (let [html (matrix/compatibility-matrix-page payload)]
    (is (string? html))
    (is (.contains html "Surface coverage"))
    (is (.contains html (census/coverage-percent payload))
        "matrix must show the headline coverage percentage")))

(deftest matrix-renders-namespace-breakdown
  (let [html (matrix/compatibility-matrix-page payload)]
    (is (.contains html "clojure.core")
        "matrix must list clojure.core in the namespace table")))

(deftest matrix-renders-missing-split
  (let [html (matrix/compatibility-matrix-page payload)
        gap-count (-> payload :missing :count :gap str)]
    (is (.contains html gap-count)
        "matrix must show the genuine-gap count")))

(deftest matrix-renders-surface-audit
  (let [html (matrix/compatibility-matrix-page payload)]
    (is (.contains html "Surface audit from census")
        "matrix must contain the surface audit section")
    (is (.contains html "jvm-bound")
        "matrix must list jvm-bound verdicts")))

(deftest matrix-renders-jvm-bound-var
  (let [html (matrix/compatibility-matrix-page payload)
        first-jvm-var (-> payload :missing :jvm-bound first :var str)]
    (is (.contains html first-jvm-var)
        "matrix must list actual missing var names")))

;; ===== P0-T6: from-clojure anchors =================================

(deftest from-clojure-renders-html
  (let [html (from-clojure/from-clojure-page payload)]
    (is (string? html))
    (is (seq html))))

(deftest every-doc-link-anchor-resolves
  (let [html (from-clojure/from-clojure-page payload)
        fragments (->> (:divergences payload)
                       (map :doc-link)
                       (keep identity)
                       (map #(last (str/split % #"#")))
                       (into #{}))]
    (is (seq fragments) "payload must have doc-link fragments")
    (doseq [frag fragments]
      (is (or (.contains html (str "id=\"" frag "\""))
              (.contains html (str "id='" frag "'")))
          (str "from-clojure page must have anchor for census doc-link #" frag)))))
