(ns mino-site.content.symbol-accuracy-test
  "Every mino_* and MINO_* symbol referenced in the doc content files
  must exist somewhere in the mino C headers under src/. Catches the
  drift class where docs reference API functions that were renamed,
  removed, or never existed."
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.string :as str]
    [clojure.java.io :as io]))

(def mino-src
  (or (System/getenv "MINO_SRC") "mino/src"))

(defn header-symbols
  "Extract all mino_* and MINO_* identifiers from every .h file
  under the mino source tree."
  []
  (let [src-dir (io/file mino-src)]
    (when (.isDirectory src-dir)
      (->> (file-seq src-dir)
           (filter #(str/ends-with? (.getName %) ".h"))
           (mapcat (fn [f]
                     (let [text (slurp f)]
                       (concat
                         (map first (re-seq #"\b(mino_[a-z_]+)\b" text))
                         (map first (re-seq #"\b(MINO_[A-Z_]+)\b" text))))))
           (into #{})))))

(def ^:private false-positives
  "Patterns that match the regex but are not API symbols or are
  defined in .c files (env vars) rather than headers."
  #{"mino_lang" "mino_site" "mino_bench" "mino_lean"
    "mino_edn" "mino_state" "MINO_EDN"
    "mino_linux_" "mino_jni"            ; filename fragments
    "mino_tx_"                           ; wildcard prefix pattern
    "MINO_GC_" "MINO_UNSTABLE_"         ; wildcard family refs
    "MINO_GC_VERIFY" "MINO_GC_STRESS"   ; env vars in .c, not .h
    "MINO_GC_EVT"})

(defn content-symbols
  "Extract all mino_* and MINO_* identifiers referenced in a
  content file, excluding known false positives."
  [file]
  (let [text (slurp file)]
    (into #{}
          (remove false-positives)
          (concat
            (map first (re-seq #"\b(mino_[a-z_]+)\b" text))
            (map first (re-seq #"\b(MINO_[A-Z_]+)\b" text))))))

(deftest all-doc-symbols-exist-in-headers
  (testing "every mino_* and MINO_* in doc content exists in the C headers"
    (let [defined (header-symbols)]
      (if (nil? defined)
        (println "Skipping: mino source tree not found at" mino-src)
        (let [content-dir (io/file "src/mino_site/content")
              files (filter #(str/ends-with? (.getName %) ".clj")
                            (file-seq content-dir))
              missing (->> files
                           (map (fn [f]
                                  (let [refs (content-symbols f)
                                        bad (remove defined refs)]
                                    (when (seq bad)
                                      [(.getName f) (sort bad)]))))
                           (filter some?)
                           (into {}))]
          (when (seq missing)
            (doseq [[file syms] missing]
              (println (str "  " file ": " (str/join ", " syms)))))
          (is (empty? missing)
              (str (count (mapcat val missing))
                   " symbol(s) referenced in docs but not found in headers")))))))
