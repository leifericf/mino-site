(ns mino-site.parse.header
  "Parse mino.h into structured API reference data.

  Splits on section banners, then extracts function signatures, enums,
  structs, typedefs, defines, and preceding doc comments within each
  section."
  (:require
    [clojure.string :as str]))

;; --- Section splitting ---

(defn- split-sections
  "Splits the header text on /* ---...--- */ banner lines.
  Returns a seq of {:name \"Section Name\" :body \"...lines...\"}."
  [text]
  (let [lines (str/split-lines text)
        banner-re #"^/\*\s*-{10,}\s*\*/$"
        name-re   #"^/\*\s+(.+?)\s+\*/$"]
    (loop [lines lines
           sections []
           current nil]
      (if (empty? lines)
        (if current
          (conj sections (update current :body str/trim))
          sections)
        (let [line (first lines)
              rest-lines (rest lines)]
          (cond
            ;; Banner delimiter: check if next line is a section name
            (re-matches banner-re line)
            (let [maybe-name (first rest-lines)
                  maybe-close (second rest-lines)]
              (if (and maybe-name
                       (re-matches name-re maybe-name)
                       maybe-close
                       (re-matches banner-re maybe-close))
                (let [name (second (re-matches name-re maybe-name))
                      new-section {:name name :body ""}]
                  (recur (drop 2 rest-lines)
                         (if current
                           (conj sections (update current :body str/trim))
                           sections)
                         new-section))
                (recur rest-lines sections
                       (when current
                         (update current :body str "\n" line)))))

            ;; Regular line inside a section
            current
            (recur rest-lines sections
                   (update current :body str (when (seq (:body current)) "\n") line))

            ;; Before any section
            :else
            (recur rest-lines sections current)))))))

;; --- Doc comment extraction ---

(defn- extract-doc-comment
  "Given lines preceding a declaration, extracts the doc comment block.
  Returns the comment text with leading /* * */ stripped, or nil."
  [preceding-lines]
  (let [reversed (reverse preceding-lines)]
    (loop [lines reversed
           comment-lines []]
      (if (empty? lines)
        (when (seq comment-lines)
          (str/trim (str/join "\n" comment-lines)))
        (let [line (str/trim (first lines))]
          (cond
            ;; Single-line /* ... */ comment
            (and (str/starts-with? line "/*")
                 (str/ends-with? line "*/")
                 (not (re-matches #"/\*\s*-{5,}.*" line)))
            (let [text (-> line
                           (subs 2 (- (count line) 2))
                           str/trim)]
              (str/trim (str/join "\n" (cons text comment-lines))))

            ;; End of block comment */
            (= line "*/")
            (recur (rest lines) comment-lines)

            ;; Middle of block comment * ... (may end with */)
            (str/starts-with? line "*")
            (let [raw (subs line 1)
                  text (str/trim (if (str/ends-with? raw "*/")
                                   (subs raw 0 (- (count raw) 2))
                                   raw))]
              (recur (rest lines) (cons text comment-lines)))

            ;; Start of block comment /* ...
            (str/starts-with? line "/*")
            (let [text (-> line (subs 2) str/trim)]
              (str/trim (str/join "\n" (cons text comment-lines))))

            ;; Not part of a comment
            :else
            (when (seq comment-lines)
              (str/trim (str/join "\n" comment-lines)))))))))

;; --- Declaration parsers ---

(defn- parse-function
  "Parses a C function declaration line into {:kind :function ...}."
  [line]
  (let [;; Match: return_type function_name(params);
        m (re-find #"^(.+?)\b(\w+)\s*\(([^)]*)\)\s*;" line)]
    (when m
      (let [ret  (str/trim (nth m 1))
            name (nth m 2)
            params (str/trim (nth m 3))]
        {:kind :function
         :name name
         :return-type ret
         :params params
         :signature (str/trim line)}))))

(defn- parse-typedef-fn
  "Parses a function pointer typedef."
  [line]
  (let [m (re-find #"typedef\s+(.+?)\(\*(\w+)\)\s*\(([^)]*)\)\s*;" line)]
    (when m
      {:kind :typedef-fn
       :name (nth m 2)
       :return-type (str/trim (nth m 1))
       :params (str/trim (nth m 3))
       :signature (str/trim line)})))

(defn- parse-typedef-struct
  "Parses a typedef struct forward declaration."
  [line]
  (let [m (re-find #"typedef\s+struct\s+(\w+)\s+(\w+)\s*;" line)]
    (when m
      {:kind :typedef
       :name (nth m 2)
       :struct-name (nth m 1)
       :signature (str/trim line)})))

(defn- parse-define
  "Parses a #define line."
  [line]
  (let [m (re-find #"^#define\s+(\w+)\s+(.+?)(?:\s*/\*.*\*/)?\s*$" line)]
    (when m
      (let [comment-m (re-find #"/\*\s*(.+?)\s*\*/" line)]
        {:kind :define
         :name (nth m 1)
         :value (str/trim (nth m 2))
         :inline-comment (when comment-m (nth comment-m 1))
         :signature (str/trim line)}))))

(defn- parse-enum
  "Parses an enum block into {:kind :enum :name ... :variants [...]}."
  [text]
  (let [m (re-find #"(?s)typedef\s+enum\s*\{([^}]+)\}\s*(\w+)\s*;" text)]
    (when m
      (let [body (nth m 1)
            name (nth m 2)
            variants
            (->> (str/split-lines body)
                 (map str/trim)
                 (remove str/blank?)
                 (mapv (fn [line]
                         (let [vm (re-find #"(\w+)(?:\s*,)?\s*(?:/\*\s*(.+?)\s*\*/)?" line)]
                           (when vm
                             {:name (nth vm 1)
                              :comment (nth vm 2)}))))
                 (filterv some?))]
        {:kind :enum
         :name name
         :variants variants}))))

(defn- parse-struct
  "Parses a struct definition into {:kind :struct :name ... :fields [...]}."
  [text]
  (let [m (re-find #"(?s)struct\s+(\w+)\s*\{(.+)\}" text)]
    (when m
      (let [name (nth m 1)
            body (nth m 2)
            ;; Extract top-level fields and union members
            fields
            (->> (str/split-lines body)
                 (map str/trim)
                 (remove str/blank?)
                 (remove #(str/starts-with? % "union"))
                 (remove #(str/starts-with? % "struct {"))
                 (remove #(= % "{"))
                 (remove #(= % "}"))
                 (remove #(str/starts-with? % "} as;"))
                 (remove #(str/starts-with? % "};"))
                 (mapv (fn [line]
                         (let [comment-m (re-find #"/\*\s*(.+?)\s*\*/" line)
                               clean (str/trim (str/replace line #"/\*.*?\*/" ""))
                               fm (re-find #"^(.+?)\s+(\*?\w+(?:\[\d+\])?)\s*;?" clean)]
                           (when fm
                             {:type (str/trim (nth fm 1))
                              :name (str/trim (nth fm 2))
                              :comment (when comment-m (nth comment-m 1))}))))
                 (filterv some?))]
        {:kind :struct
         :name name
         :fields fields}))))

;; --- Section body parser ---

(defn- parse-section-body
  "Parses a section body into a list of declarations with doc comments."
  [body]
  (let [lines (str/split-lines body)]
    (loop [lines lines
           idx 0
           preceding []
           declarations []]
      (if (>= idx (count lines))
        declarations
        (let [line (nth lines idx)
              trimmed (str/trim line)]
          (cond
            ;; Blank line resets preceding context
            (str/blank? trimmed)
            (recur lines (inc idx) [] declarations)

            ;; Comment lines accumulate
            (or (str/starts-with? trimmed "/*")
                (str/starts-with? trimmed "*")
                (str/starts-with? trimmed "//")
                (= trimmed "*/"))
            (recur lines (inc idx) (conj preceding trimmed) declarations)

            ;; #include — skip
            (str/starts-with? trimmed "#include")
            (recur lines (inc idx) [] declarations)

            ;; #ifdef / #endif / extern — skip
            (or (str/starts-with? trimmed "#ifdef")
                (str/starts-with? trimmed "#ifndef")
                (str/starts-with? trimmed "#endif")
                (str/starts-with? trimmed "extern"))
            (recur lines (inc idx) [] declarations)

            ;; #define
            (str/starts-with? trimmed "#define")
            (let [decl (parse-define trimmed)
                  doc (extract-doc-comment preceding)]
              (recur lines (inc idx) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef with function pointer
            (and (str/starts-with? trimmed "typedef")
                 (str/includes? trimmed "(*"))
            (let [decl (parse-typedef-fn trimmed)
                  doc (extract-doc-comment preceding)]
              (recur lines (inc idx) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef enum — collect until closing ;
            (and (str/starts-with? trimmed "typedef enum")
                 (str/includes? trimmed "{"))
            (let [block-lines (loop [j idx collected []]
                                (if (>= j (count lines))
                                  collected
                                  (let [l (nth lines j)]
                                    (if (str/includes? l ";")
                                      (conj collected l)
                                      (recur (inc j) (conj collected l))))))
                  block-text (str/join "\n" block-lines)
                  decl (parse-enum block-text)
                  doc (extract-doc-comment preceding)]
              (recur lines (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef struct forward declaration
            (and (str/starts-with? trimmed "typedef struct")
                 (str/ends-with? trimmed ";"))
            (let [decl (parse-typedef-struct trimmed)
                  doc (extract-doc-comment preceding)]
              (recur lines (inc idx) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; struct definition — collect until closing };
            (str/starts-with? trimmed "struct ")
            (let [block-lines (loop [j idx collected [] depth 0]
                                (if (>= j (count lines))
                                  collected
                                  (let [l (nth lines j)
                                        opens (count (re-seq #"\{" l))
                                        closes (count (re-seq #"\}" l))
                                        new-depth (+ depth opens (- closes))]
                                    (if (and (pos? (+ depth opens))
                                             (<= new-depth 0))
                                      (conj collected l)
                                      (recur (inc j) (conj collected l)
                                             new-depth)))))
                  block-text (str/join "\n" block-lines)
                  decl (parse-struct block-text)
                  doc (extract-doc-comment preceding)]
              (recur lines (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; Function declaration
            (and (str/ends-with? trimmed ";")
                 (str/includes? trimmed "("))
            (let [;; Handle multi-line declarations
                  full-line (if (str/ends-with? trimmed ";")
                              trimmed
                              (str/join " "
                                (loop [j idx collected []]
                                  (let [l (str/trim (nth lines j))]
                                    (if (str/ends-with? l ";")
                                      (conj collected l)
                                      (recur (inc j) (conj collected l)))))))
                  decl (parse-function full-line)
                  doc (extract-doc-comment preceding)]
              (recur lines (inc idx) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; Multi-line function declaration (no ; on this line)
            (and (str/includes? trimmed "(")
                 (not (str/ends-with? trimmed ";"))
                 (not (str/starts-with? trimmed "/*")))
            (let [block-lines (loop [j idx collected []]
                                (if (>= j (count lines))
                                  collected
                                  (let [l (nth lines j)]
                                    (if (str/includes? l ";")
                                      (conj collected (str/trim l))
                                      (recur (inc j) (conj collected (str/trim l)))))))
                  full-line (str/join " " block-lines)
                  decl (parse-function full-line)
                  doc (extract-doc-comment preceding)]
              (recur lines (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; Anything else — accumulate as context
            :else
            (recur lines (inc idx) (conj preceding trimmed) declarations)))))))

;; --- Public API ---

(defn parse
  "Parse a mino.h file and return structured API data.
  Returns {:sections [{:name \"...\" :declarations [...]}]}."
  [path]
  (let [text (slurp path)
        raw-sections (split-sections text)]
    {:sections
     (mapv (fn [{:keys [name body]}]
             {:name name
              :declarations (parse-section-body body)})
           raw-sections)}))
