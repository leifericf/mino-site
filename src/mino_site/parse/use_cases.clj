(ns mino-site.parse.use-cases
  "Parse examples/use-cases/*.cpp files for the use case pages.

  Each file has three marked sections (Embed, Expose, Script) and
  a header comment with title and description. The script section
  is also extracted from the embedded C string literal."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn- extract-header
  "Extract title and description from the file header comment."
  [text]
  (when-let [m (re-find #"(?s)/\*\s*\n(.+?)\*/" text)]
    (let [body (nth m 1)
          lines (->> (str/split-lines body)
                     (map #(str/replace % #"^\s*\*\s?" ""))
                     (map str/trim))
          ;; First non-blank line: "name.cpp — title"
          title-line (first (remove str/blank? lines))
          title (when title-line
                  (if-let [tm (re-find #"^\S+\.cpp\s+(?:—|--|-)\s+(.+?)\.\s*$" title-line)]
                    (second tm)
                    (str/replace title-line #"\.\s*$" "")))
          ;; Description: lines between title and Build:
          desc-start (inc (.indexOf lines title-line))
          desc-lines (->> (drop desc-start lines)
                          (take-while #(not (str/starts-with? % "Build")))
                          (remove str/blank?))]
      {:title title
       :description (when (seq desc-lines) (str/join " " desc-lines))})))

(defn- extract-sections
  "Extract the three code sections by the ── markers.
  Returns a map of {:embed str, :expose str, :script-code str}
  where each value is the C++ source between the section marker
  and the next marker (or EOF)."
  [text]
  (let [;; Split on section markers
        parts (str/split text #"/\*\s*──\s*")
        find-section (fn [label]
                       (some (fn [part]
                               (when (str/starts-with? part label)
                                 ;; Strip the marker line and trailing */
                                 (let [after-marker (str/replace part
                                                      (re-pattern (str "^" label "\\s*─*\\s*\\*/\\s*\n?"))
                                                      "")]
                                   (str/trim after-marker))))
                             parts))]
    {:embed  (find-section "Embed")
     :expose (find-section "Expose")
     :script-section (find-section "Script")}))

(defn- extract-mino-script
  "Extract the embedded mino script from the C string literal.
  Looks for static const char *script = ... and unescapes the C strings."
  [text]
  (when-let [m (re-find #"(?s)static const char \*(?:script|prelude)\s*=\s*\n(.*?);\s*\n" text)]
    (let [raw (nth m 1)]
      (->> (str/split-lines raw)
           (map #(str/trim %))
           (map #(str/replace % #"^\"" ""))
           (map #(str/replace % #"\"$" ""))
           (map #(str/replace % #"\\n$" ""))
           (map #(str/replace % #"\\\\n" "\n"))
           (map #(str/replace % #"\\\\t" "\t"))
           (map #(str/replace % #"\\\\\"" "\""))
           (map #(str/replace % #"\\\\" "\\"))
           (str/join "\n")
           str/trim))))

(defn- strip-comment-stars
  "Strip leading * from C block comment lines and join into prose."
  [body]
  (->> (str/split-lines body)
       (map #(str/replace % #"^\s*\*\s?" ""))
       (map str/trim)
       (remove str/blank?)
       (str/join " ")))

(defn- extract-expose-comment
  "Extract the prose comment from the Expose section."
  [section-text]
  (when section-text
    (when-let [m (re-find #"(?s)/\*\s*(.+?)\s*\*/" section-text)]
      (strip-comment-stars (nth m 1)))))

(defn- extract-script-comment
  "Extract the prose comment from the Script section."
  [section-text]
  (when section-text
    (when-let [m (re-find #"(?s)/\*\s*(.+?)\s*\*/" section-text)]
      (strip-comment-stars (nth m 1)))))

(defn- parse-use-case-file
  "Parse a single use case C++ file."
  [file]
  (let [text (slurp file)
        filename (.getName file)
        slug (str/replace filename #"\.cpp$" "")
        header (extract-header text)
        sections (extract-sections text)
        mino-script (extract-mino-script text)]
    (merge
      {:filename filename
       :slug slug
       :source text
       :mino-script mino-script
       :expose-prose (extract-expose-comment (:expose sections))
       :script-prose (extract-script-comment (:script-section sections))}
      header)))

;; --- Public API ---

(defn parse
  "Parse all examples/use-cases/*.cpp files under the given mino root.
  Returns a vector of use case entries sorted by filename."
  [mino-root]
  (let [dir (io/file mino-root "examples" "use-cases")
        files (when (.isDirectory dir)
                (->> (.listFiles dir)
                     (filter #(str/ends-with? (.getName %) ".cpp"))
                     (sort-by #(.getName %))))]
    (mapv parse-use-case-file files)))
