(ns mino-site.parse.smoke
  "Parse tests/smoke.sh for usage examples.

  Extracts run \"description\" 'input' 'expected' triples into
  structured example data, grouped by version/section comments."
  (:require
    [clojure.string :as str]))

(defn- parse-run-calls
  "Extract run and run_err calls from smoke.sh.
  Returns [{:desc \"...\" :input \"...\" :expected \"...\" :kind :run|:run-err}]."
  [text]
  (let [lines (str/split-lines text)]
    (loop [lines lines
           section nil
           examples []]
      (if (empty? lines)
        examples
        (let [line (first lines)
              rest-lines (rest lines)]
          (cond
            ;; Section comment: # v0.X — description
            (re-find #"^#\s*v[\d.]+" line)
            (let [m (re-find #"^#\s*(v[\d.]+)\s*(?:—|--|-)\s*(.+)" line)]
              (recur rest-lines
                     (if m
                       {:version (second m) :label (nth m 2)}
                       section)
                     examples))

            ;; Section comment: # description
            (and (str/starts-with? line "# ")
                 (not (str/starts-with? line "#!")))
            (recur rest-lines
                   {:version nil :label (str/trim (subs line 2))}
                   examples)

            ;; Single-line run call: run "desc" 'input' 'expected'
            (re-find #"^run\s+\"" line)
            (let [;; Collect continuation lines (ending with ')
                  all-lines (loop [ls (cons line rest-lines) collected []]
                              (let [l (first ls)]
                                (if (nil? l)
                                  collected
                                  (let [joined (str/join "\n" (conj collected l))]
                                    ;; Count unescaped single quotes
                                    (if (>= (count (re-seq #"'" joined)) 4)
                                      (conj collected l)
                                      (recur (rest ls) (conj collected l)))))))
                  full (str/join "\n" all-lines)
                  ;; Parse: run "desc" 'input' 'expected'
                  m (re-find #"(?s)^run\s+\"([^\"]+)\"\s+'([^']*)'\s+'([^']*)'" full)]
              (if m
                (recur (drop (dec (count all-lines)) rest-lines)
                       section
                       (conj examples
                             {:description (nth m 1)
                              :input (nth m 2)
                              :expected (nth m 3)
                              :kind :run
                              :section section}))
                ;; Try multi-line input/expected with shell line continuation
                (let [m2 (re-find #"(?s)^run\s+\"([^\"]+)\"\s+\"([^\"]*)\"\s+'([^']*)'" full)]
                  (recur (drop (dec (count all-lines)) rest-lines)
                         section
                         (if m2
                           (conj examples
                                 {:description (nth m2 1)
                                  :input (nth m2 2)
                                  :expected (nth m2 3)
                                  :kind :run
                                  :section section})
                           examples)))))

            ;; run_err call
            (re-find #"^run_err\s+\"" line)
            (let [all-lines (loop [ls (cons line rest-lines) collected []]
                              (let [l (first ls)]
                                (if (nil? l)
                                  collected
                                  (let [joined (str/join "\n" (conj collected l))]
                                    (if (>= (count (re-seq #"'" joined)) 4)
                                      (conj collected l)
                                      (recur (rest ls) (conj collected l)))))))
                  full (str/join "\n" all-lines)
                  m (re-find #"(?s)^run_err\s+\"([^\"]+)\"\s+'([^']*)'\s+'([^']*)'" full)]
              (recur (drop (dec (count all-lines)) rest-lines)
                     section
                     (if m
                       (conj examples
                             {:description (nth m 1)
                              :input (nth m 2)
                              :expected (nth m 3)
                              :kind :run-err
                              :section section})
                       examples)))

            ;; Anything else
            :else
            (recur rest-lines section examples)))))))

;; --- Public API ---

(defn parse
  "Parse tests/smoke.sh and return structured usage examples.
  Returns {:examples [{:description, :input, :expected, :kind, :section}]}."
  [path]
  {:examples (parse-run-calls (slurp path))})
