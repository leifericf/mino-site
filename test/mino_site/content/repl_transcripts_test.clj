(ns mino-site.content.repl-transcripts-test
  "REPL transcript blocks in the docs must stay faithful to the real
  binary: the prompt shape must be `mino=>` and the input forms, fed
  back to the mino REPL in order, must evaluate without error. Catches
  the drift class the Get Started page shipped with (stale prompt,
  `(doc 'map)` instead of `(doc map)`, unbound symbols). Uses
  $MINO_BIN (defaults to ../mino/mino); skips when no binary is on
  deck so the suite still runs without a mino build."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [clojure.java.shell :as shell]
    [mino-site.content.get-started :as get-started]
    [mino-site.content.testing :as testing]))

(def mino-bin
  (or (System/getenv "MINO_BIN") "../mino/mino"))

(defn mino-available? []
  (and mino-bin (.canExecute (io/file mino-bin))))

(defn unescape [s]
  (-> s
      (str/replace "&amp;" "&")
      (str/replace "&lt;" "<")
      (str/replace "&gt;" ">")
      (str/replace "&quot;" "\"")
      (str/replace "&#39;" "'")
      (str/replace "&apos;" "'")))

(defn transcript-blocks [html]
  (->> (re-seq #"<code data-lang=\"mino\">([\s\S]*?)</code>" html)
       (map #(-> % second unescape))
       (filter #(re-find #"(?m)^mino=>" %))))

(defn input-forms [block]
  ;; A prompt line is `mino=> <form>`; everything else is banner,
  ;; output, or comment and is not fed back.
  (->> (str/split-lines block)
       (keep #(some-> (re-find #"^mino=> (.*)$" %) second not-empty))))

(defn prompt-lines [block]
  (str/split-lines block))

(defn eval-in-repl [forms]
  ;; Run from the mino repo root so repo-relative requires in the
  ;; transcripts (e.g. (require "tests/test")) resolve.
  (let [input (str/join "\n" forms)
        dir   (.getParent (io/file mino-bin))]
    (shell/sh mino-bin :in input :dir dir)))

(defn has-error? [result]
  (or (not (zero? (:exit result)))
      (re-find #"error\[" (str (:out result) (:err result)))))

(deftest repl-transcripts-match-the-real-binary
  (if-not (mino-available?)
    (testing "minobin"
      (is true "MINO_BIN not set or not executable; transcript replay skipped"))
    (doseq [[label html] [["get-started" (get-started/get-started-page)]
                          ["testing" (testing/testing-page)]]
            block (transcript-blocks html)]
      (testing (str label " transcript")
        (doseq [line (prompt-lines block)]
          (when (re-find #"^mino>" line)
            (is (re-find #"^mino=>" line)
                (str "prompt must be `mino=>`, got: " (pr-str line)))))
        (let [forms (input-forms block)]
          (is (seq forms) "transcript yielded no input forms to replay")
          (let [result (eval-in-repl forms)]
            (is (not (has-error? result))
                (str "transcript inputs errored in the real REPL.\n"
                     "inputs: " (pr-str forms) "\n"
                     "out: " (:out result) "\n"
                     "err: " (:err result)))))))))
