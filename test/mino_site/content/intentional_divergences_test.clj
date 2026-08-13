(ns mino-site.content.intentional-divergences-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [mino-site.parse.census :as census]
            [mino-site.content.intentional-divergences :as div]))

(def payload (census/load-payload))

(deftest page-renders-html-string
  (let [html (div/intentional-divergences-page payload)]
    (is (string? html))
    (is (seq html))))

(deftest page-contains-editorial-sections
  (let [html (div/intentional-divergences-page payload)]
    (is (.contains html "No JVM interop surface")
        "page must contain the editorial JVM interop section")
    (is (.contains html "Host-grant-gated host threads")
        "page must contain the editorial threading section")
    (is (.contains html "STM uses single-version optimistic locking")
        "page must contain the editorial STM section")))

(deftest page-contains-census-catalog-heading
  (let [html (div/intentional-divergences-page payload)]
    (is (.contains html "Full divergence catalog")
        "page must contain the census-driven catalog section")))

(deftest page-contains-first-divergence-title
  (let [html (div/intentional-divergences-page payload)
        first-title (-> payload :divergences first :title)]
    (is (.contains html first-title)
        "page must contain the first divergence title from the payload")))

(deftest page-contains-category-heading
  (let [html (div/intentional-divergences-page payload)
        first-cat (-> payload :categories first :title
                      (str/replace "&" "&amp;"))]
    (is (.contains html first-cat)
        "page must contain category headings from the payload")))

(deftest page-contains-coverage-percent
  (let [html (div/intentional-divergences-page payload)
        pct (census/coverage-percent payload)]
    (is (.contains html pct)
        "page must show the headline coverage percentage")))

(deftest page-contains-missing-counts
  (let [html (div/intentional-divergences-page payload)
        jvm-bound (-> payload :missing :count :jvm-bound str)]
    (is (.contains html jvm-bound))))
