(ns mino-site.parse.smoke
  "Parse mino test files for usage examples.

  Extracts (is (= expected actual)) assertions from *_test.mino files
  into structured example data for the language reference page."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn- read-mino-forms
  "Read all top-level s-expressions from a mino source string.
  Uses Clojure's reader which can parse the common subset."
  [text]
  (let [;; Strip mino-specific syntax that breaks Clojure's reader:
        ;; - @expr (deref) is fine in Clojure
        ;; - #{ } (sets) are fine in Clojure
        ;; The main issue is (require "...") which is a valid form.
        rdr (java.io.PushbackReader. (java.io.StringReader. text))]
    (loop [forms []]
      (let [form (try (read {:eof ::eof :read-cond :allow} rdr)
                      (catch Exception _ ::skip))]
        (cond
          (= form ::eof) forms
          (= form ::skip) (recur forms)
          :else (recur (conj forms form)))))))

(defn- extract-assertions
  "Walk a form tree and extract (is (= expected actual)) assertions.
  Returns [{:input actual-str :expected expected-str :description desc}]."
  [forms test-name]
  (cond
    (not (sequential? forms)) []
    (and (seq forms) (= 'is (first forms)))
    (let [expr (second forms)]
      (if (and (sequential? expr) (= '= (first expr)) (>= (count expr) 3))
        (let [expected (pr-str (nth expr 1))
              actual   (pr-str (nth expr 2))]
          [{:description (str test-name)
            :input actual
            :expected expected
            :kind :run
            :section nil}])
        []))
    :else (mapcat #(extract-assertions % test-name) forms)))

(defn- parse-test-file
  "Parse a single *_test.mino file and return extracted examples."
  [text]
  (let [forms (read-mino-forms text)]
    (mapcat
      (fn [form]
        (when (and (sequential? form) (= 'deftest (first form)))
          (let [test-name (str (second form))
                body (drop 2 form)]
            (mapcat #(extract-assertions % test-name) body))))
      forms)))

;; --- Public API ---

(defn parse
  "Parse mino test files and return structured usage examples.
  `path` is the root of the mino project (directory containing tests/).
  Returns {:examples [{:description, :input, :expected, :kind, :section}]}."
  [path]
  (let [test-dir (io/file (str path "/tests"))
        test-files (when (.isDirectory test-dir)
                     (->> (.listFiles test-dir)
                          (filter #(str/ends-with? (.getName %) "_test.mino"))
                          (sort-by #(.getName %))))]
    {:examples (vec (mapcat #(parse-test-file (slurp %)) test-files))}))
