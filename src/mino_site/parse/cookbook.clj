(ns mino-site.parse.cookbook
  "Parse cookbook/*.c files for embedding examples.

  Each cookbook file has a header comment with:
  - Title (first line after /*)
  - Description (second line)
  - Demonstrates: list
  - Build command
  Then the full C source."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn- parse-header-comment
  "Extract the structured header comment from a cookbook C file.
  Returns {:title, :description, :demonstrates, :build}."
  [text]
  (let [;; Extract the first block comment
        m (re-find #"(?s)/\*\s*\n(.+?)\*/" text)]
    (when m
      (let [body (nth m 1)
            ;; Strip leading " * " from each line, keep blanks as separators
            raw-lines (->> (str/split-lines body)
                           (map #(str/replace % #"^\s*\*\s?" "")))
            ;; Join continuation lines: a non-blank line that doesn't start
            ;; with a keyword (Demonstrates:, Build:, Run:) continues the
            ;; previous logical line.
            merged (reduce
                     (fn [acc line]
                       (let [trimmed (str/trim line)]
                         (cond
                           (str/blank? trimmed)
                           (conj acc "")

                           (or (re-find #"^(Demonstrates|Build|Run):" trimmed)
                               (re-find #"^\S+\.c\s+(—|--|-)" trimmed)
                               (empty? acc)
                               (str/blank? (peek acc)))
                           (conj acc trimmed)

                           :else
                           (update acc (dec (count acc)) str " " trimmed))))
                     []
                     raw-lines)
            lines (remove str/blank? merged)
            ;; First line: "name.c — title"
            title-line (first lines)
            title (when title-line
                    (if-let [tm (re-find #"^\S+\.c\s+(?:—|--|-)\s+(.+)" title-line)]
                      (-> (second tm)
                          (str/replace #"\.\s*$" "")
                          str/capitalize)
                      title-line))
            ;; Description: lines between title and "Demonstrates:"
            desc-lines (take-while #(not (str/starts-with? % "Demonstrates:"))
                                   (rest lines))
            description (when (seq desc-lines)
                          (str/join " " desc-lines))
            ;; Demonstrates: line(s) — may span multiple lines, now merged
            demo-line (->> lines
                          (filter #(str/starts-with? % "Demonstrates:"))
                          first)
            demonstrates (when demo-line
                           (let [content (str/trim (subs demo-line (count "Demonstrates:")))]
                             (->> (str/split content #",")
                                  (mapv str/trim)
                                  (filterv seq))))
            ;; Build: line
            build-line (->> lines
                           (filter #(str/starts-with? % "Build:"))
                           first)
            build (when build-line
                    (str/trim (subs build-line (count "Build:"))))]
        {:title title
         :description description
         :demonstrates (or demonstrates [])
         :build build}))))

(defn- parse-cookbook-file
  "Parse a single cookbook C file."
  [file]
  (let [text (slurp file)
        filename (.getName file)
        header (parse-header-comment text)]
    (merge
      {:filename filename
       :source text}
      header)))

;; --- Public API ---

(defn parse
  "Parse all cookbook/*.c files under the given mino root.
  Returns a vector of cookbook entries sorted by filename."
  [mino-root]
  (let [dir (io/file mino-root "cookbook")
        files (when (.isDirectory dir)
                (->> (.listFiles dir)
                     (filter #(str/ends-with? (.getName %) ".c"))
                     (sort-by #(.getName %))))]
    (mapv parse-cookbook-file files)))
