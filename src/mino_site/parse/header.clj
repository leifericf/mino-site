(ns mino-site.parse.header
  "Parse mino.h into structured API reference data.

  Splits on section banners, then extracts function signatures, enums,
  structs, typedefs, defines, and preceding doc comments within each
  section.

  Warnings accumulate into *warnings* when bound; strict mode escalates
  them at the end."
  (:require
    [clojure.string :as str]))

;; --- Forward declarations (file is read top-down by the compiler) ---

(declare collapse-block-comments struct-fields)

;; --- Warning surface ---

(def ^:dynamic *warnings* nil)

(defn- warn!
  "Push a warning into the active accumulator, if one is bound."
  [m]
  (when *warnings* (swap! *warnings* conj m)))

;; --- Section splitting ---

(defn- split-sections
  "Splits the header text on /* ---...--- */ banner lines.
  Returns a seq of {:name \"Section Name\" :body \"...lines...\" :start-line N}.
  A banner opener with no matching name+closer pair within two lines emits
  a :banner-opener-without-name warning."
  [text]
  (let [all-lines (str/split-lines text)
        banner-re #"^/\*\s*-{10,}\s*\*/$"
        name-re   #"^/\*\s+(.+?)\s+\*/$"]
    (loop [lines     all-lines
           line-no   1
           sections  []
           current   nil]
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
                      new-section {:name name :body "" :start-line line-no}]
                  (recur (drop 2 rest-lines)
                         (+ line-no 3)
                         (if current
                           (conj sections (update current :body str/trim))
                           sections)
                         new-section))
                (do
                  (warn! {:line line-no
                          :reason :banner-opener-without-name
                          :snippet line})
                  (recur rest-lines (inc line-no) sections
                         (when current
                           (update current :body str "\n" line))))))

            ;; Regular line inside a section
            current
            (recur rest-lines (inc line-no) sections
                   (update current :body str (when (seq (:body current)) "\n") line))

            ;; Before any section
            :else
            (recur rest-lines (inc line-no) sections current)))))))

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
  "Parses a function pointer typedef. Accepts single or multi-line input
  joined on a single string. Trailing C arg lists with internal commas
  are preserved verbatim in :params."
  [line]
  (let [m (re-find #"typedef\s+(.+?)\(\*(\w+)\)\s*\((.*)\)\s*;" line)]
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

(defn- parse-typedef-struct-def
  "Parses a typedef struct definition, named or anonymous:
    typedef struct { ... } NAME_t;
    typedef struct foo { ... } NAME_t;
  Returns {:kind :struct :name NAME_t :struct-tag foo|nil :fields [...]}."
  [text]
  (let [m (re-find #"(?s)typedef\s+struct\s*(\w*)\s*\{(.+)\}\s*(\w+)\s*;" text)]
    (when m
      (let [struct-tag (let [t (nth m 1)] (when-not (str/blank? t) t))
            body       (nth m 2)
            name       (nth m 3)]
        (cond-> {:kind :struct
                 :name name
                 :fields (struct-fields body)}
          struct-tag (assoc :struct-tag struct-tag))))))

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

(defn- collapse-block-comments
  "Collapse each /* ... */ block in text into a single-line /* ... */.
  Multi-line comments with leading `*` decoration are flattened to one
  space-separated comment, so per-line scanning of the surrounding code
  sees each variant or field with its complete trailing comment intact."
  [text]
  (str/replace text
    #"(?s)/\*(.*?)\*/"
    (fn [match-vec]
      (let [inner (nth match-vec 1)
            flat  (-> inner
                      (str/replace #"\n\s*\*\s*" " ")
                      (str/replace #"\s+" " ")
                      str/trim)]
        (java.util.regex.Matcher/quoteReplacement
          (str "/* " flat " */"))))))

(defn- parse-enum
  "Parses an enum block into {:kind :enum :name ... :variants [...]}.

  Greedy body capture handles `}` inside comment text (e.g. `{fn, args}`
  on a trampoline-sentinel doc line) by anchoring on the trailing
  `} NAME ;` instead of the first `}`."
  [text]
  (let [collapsed (collapse-block-comments text)
        m (re-find #"(?s)typedef\s+enum\s*\{(.+)\}\s*(\w+)\s*;" collapsed)]
    (when m
      (let [body (nth m 1)
            name (nth m 2)
            variants
            (->> (str/split-lines body)
                 (map str/trim)
                 (remove str/blank?)
                 (mapv (fn [line]
                         ;; Require the line to begin with an enum-style
                         ;; identifier (uppercase or underscore-led), so
                         ;; comment-continuation fragments don't masquerade
                         ;; as variants.
                         (let [vm (re-find
                                    #"^([A-Z_][A-Z0-9_]*)(?:\s*,)?\s*(?:/\*\s*(.+?)\s*\*/)?"
                                    line)]
                           (when vm
                             {:name (nth vm 1)
                              :comment (nth vm 2)}))))
                 (filterv some?))]
        {:kind :enum
         :name name
         :variants variants}))))

(defn- struct-fields
  "Extract field rows from the body of a struct (or typedef struct).
  Lines that don't parse cleanly (e.g. function-pointer fields) are
  skipped; the renderer still shows the struct heading."
  [body]
  (let [collapsed (collapse-block-comments body)]
    (->> (str/split-lines collapsed)
         (map str/trim)
         (remove str/blank?)
         (remove #(str/starts-with? % "union"))
         (remove #(str/starts-with? % "struct"))
         (remove #(= % "{"))
         (remove #(re-matches #"\}.*" %))
         (mapv (fn [line]
                 (let [comment-m (re-find #"/\*\s*(.+?)\s*\*/" line)
                       clean (str/trim (str/replace line #"/\*.*?\*/" ""))
                       clean (str/replace clean #";$" "")
                       clean (str/trim clean)
                       fm (re-find #"^(.+)\s+(\*?\w+(?:\[\d+\])?)$" clean)]
                   (when fm
                     (let [raw-type (str/trim (nth fm 1))
                           raw-name (str/trim (nth fm 2))
                           [t n] (if (str/starts-with? raw-name "*")
                                   [(str raw-type " *") (subs raw-name 1)]
                                   [raw-type raw-name])]
                       {:type t
                        :name n
                        :comment (when comment-m (nth comment-m 1))})))))
         (filterv some?))))

(defn- parse-struct
  "Parses a bare struct definition into {:kind :struct :name ... :fields [...]}."
  [text]
  (let [m (re-find #"(?s)struct\s+(\w+)\s*\{(.+)\}" text)]
    (when m
      (let [name (nth m 1)
            body (nth m 2)]
        {:kind :struct
         :name name
         :fields (struct-fields body)}))))

;; --- Section body parser ---

(defn- parse-section-body
  "Parses a section body into a list of declarations with doc comments.
  section-name and start-line are used to locate warnings."
  [section-name start-line body]
  (let [lines (str/split-lines body)
        warn-skip (fn [reason snippet idx]
                    (warn! {:section section-name
                            :line (+ start-line idx)
                            :reason reason
                            :snippet snippet}))]
    (loop [idx 0
           preceding []
           declarations []]
      (if (>= idx (count lines))
        declarations
        (let [line (nth lines idx)
              trimmed (str/trim line)]
          (cond
            ;; Blank line resets preceding context
            (str/blank? trimmed)
            (recur (inc idx) [] declarations)

            ;; Comment lines accumulate
            (or (str/starts-with? trimmed "/*")
                (str/starts-with? trimmed "*")
                (str/starts-with? trimmed "//")
                (= trimmed "*/"))
            (recur (inc idx) (conj preceding trimmed) declarations)

            ;; #include — skip
            (str/starts-with? trimmed "#include")
            (recur (inc idx) [] declarations)

            ;; #ifdef / #endif / extern — skip
            (or (str/starts-with? trimmed "#ifdef")
                (str/starts-with? trimmed "#ifndef")
                (str/starts-with? trimmed "#endif")
                (str/starts-with? trimmed "extern"))
            (recur (inc idx) [] declarations)

            ;; #define
            (str/starts-with? trimmed "#define")
            (let [;; A backslash-continued macro spans multiple physical
                  ;; lines; consume them all so the continuation body is
                  ;; not reparsed as a stray declaration.
                  span (loop [j idx]
                         (if (and (str/ends-with? (str/trimr (nth lines j)) "\\")
                                  (< (inc j) (count lines)))
                           (recur (inc j))
                           (inc (- j idx))))
                  ;; Function-like macros -- #define NAME(args) ... -- and
                  ;; multi-line macros are implementation detail, not
                  ;; documented API entries; skip them silently. Only an
                  ;; object-like #define that genuinely fails to parse is
                  ;; worth a warning.
                  fn-like? (re-find #"^#define\s+\w+\(" trimmed)
                  decl (when (and (not fn-like?) (= span 1))
                         (parse-define trimmed))
                  doc (extract-doc-comment preceding)]
              (when (and (not decl) (not fn-like?) (= span 1))
                (warn-skip :unparseable-define trimmed idx))
              (recur (+ idx span) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef with function pointer (may span multiple lines)
            (and (str/starts-with? trimmed "typedef")
                 (str/includes? trimmed "(*"))
            (let [block-lines (loop [j idx collected []]
                                (if (>= j (count lines))
                                  collected
                                  (let [l (str/trim (nth lines j))]
                                    (if (str/ends-with? l ";")
                                      (conj collected l)
                                      (recur (inc j) (conj collected l))))))
                  full-line (str/join " " block-lines)
                  decl (parse-typedef-fn full-line)
                  doc (extract-doc-comment preceding)]
              (when-not decl
                (warn-skip :unparseable-typedef-fn full-line idx))
              (recur (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef enum — collect with brace-depth so `;` inside
            ;; comments (e.g. `construction); arithmetic`) is not
            ;; mistaken for the closing of the typedef.
            (and (str/starts-with? trimmed "typedef enum")
                 (str/includes? trimmed "{"))
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
                  decl (parse-enum block-text)
                  doc (extract-doc-comment preceding)]
              (when-not decl
                (warn-skip :unparseable-typedef-enum block-text idx))
              (recur (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef struct forward declaration (one-line opaque)
            (and (str/starts-with? trimmed "typedef struct")
                 (str/ends-with? trimmed ";"))
            (let [decl (parse-typedef-struct trimmed)
                  doc (extract-doc-comment preceding)]
              (when-not decl
                (warn-skip :unparseable-typedef-struct trimmed idx))
              (recur (inc idx) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; typedef struct definition (named or anonymous):
            ;;   typedef struct foo { ... } foo_t;
            ;;   typedef struct      { ... } foo_t;
            (and (str/starts-with? trimmed "typedef struct")
                 (str/includes? trimmed "{"))
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
                  decl (parse-typedef-struct-def block-text)
                  doc (extract-doc-comment preceding)]
              (when-not decl
                (warn-skip :unparseable-typedef-struct-def block-text idx))
              (recur (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; Forward-declared / opaque struct: `struct foo;` with no
            ;; body (a trailing /* ... */ comment is fine). Not a
            ;; documented API entry -- skip rather than run the brace
            ;; collector off the end of the section.
            (and (str/starts-with? trimmed "struct ")
                 (str/includes? trimmed ";")
                 (not (str/includes? trimmed "{")))
            (recur (inc idx) [] declarations)

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
              (when-not decl
                (warn-skip :unparseable-struct block-text idx))
              (recur (+ idx (count block-lines)) []
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
              (when-not decl
                (warn-skip :unparseable-function full-line idx))
              (recur (inc idx) []
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
              (when-not decl
                (warn-skip :unparseable-function full-line idx))
              (recur (+ idx (count block-lines)) []
                     (if decl
                       (conj declarations (assoc decl :doc doc))
                       declarations)))

            ;; Anything else — accumulate as context
            :else
            (recur (inc idx) (conj preceding trimmed) declarations)))))))

;; --- Public API ---

(defn- split-unstable-tag
  "Detect a trailing `[MINO_UNSTABLE_*]` marker on a section name.
  Returns [clean-name unstable?] where clean-name is the user-visible
  label with the marker stripped, and unstable? is truthy when the
  section is provisional.

  Section names that look like they contain an unstable tag but do not
  match the canonical regex emit a :malformed-unstable-tag warning."
  [name start-line]
  (let [m (re-find #"^(.*?)\s*\[MINO_UNSTABLE_[A-Z_]+\]\s*$" name)]
    (cond
      m
      [(str/trim (nth m 1)) true]

      (str/includes? name "[MINO_UNSTABLE")
      (do (warn! {:line start-line
                  :reason :malformed-unstable-tag
                  :snippet name})
          [name false])

      :else
      [name false])))

(defn parse
  "Parse a mino.h file and return structured API data.

  Returns
    {:sections [{:name \"...\" :declarations [...] :unstable bool}]
     :warnings [{:line N :reason kw :snippet \"...\"} ...]}.

  Options:
    :strict?  when true, throw ex-info on the first non-empty warning
              list instead of returning it. Defaults to false."
  ([path] (parse path nil))
  ([path {:keys [strict?]}]
   (let [warnings (atom [])
         result
         (binding [*warnings* warnings]
           (let [text (slurp path)
                 raw-sections (split-sections text)]
             {:sections
              (mapv (fn [{:keys [name body start-line]}]
                      (let [[clean unstable?] (split-unstable-tag name start-line)
                            decls (parse-section-body clean (inc start-line) body)
                            decls (if unstable?
                                    (mapv #(assoc % :unstable true) decls)
                                    decls)]
                        {:name clean
                         :unstable unstable?
                         :declarations decls}))
                    raw-sections)}))
         ws @warnings]
     (when (and strict? (seq ws))
       (throw (ex-info (str "Header parser produced " (count ws)
                            " warning(s) under :strict? true")
                       {:warnings ws})))
     (assoc result :warnings ws))))
