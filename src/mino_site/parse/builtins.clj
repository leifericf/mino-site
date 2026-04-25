(ns mino-site.parse.builtins
  "Build Language Reference data by booting mino and introspecting
  every binding via apropos / doc.

  The previous implementation regex-scraped src/prim.c. Phase A of
  the C-Core Refactor reorganized the C tree into per-subsystem
  subdirectories and split prim.c into prim/*.c, so the regex
  parser broke. Runtime introspection makes the live runtime the
  source of truth and works uniformly for C primitives and forms
  defined in core.mino.

  Curated metadata that is not derivable from runtime values
  (category grouping, special-form list, I/O primitive set) lives
  here as data.

  Source extraction for the \"show source\" panel still parses
  core.mino text — that's a different concern from naming the
  bindings."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str]))

;; --- Curated category map ---

(def ^:private category-order
  "Authoritative category ordering and membership. Each :fns is a
  vector to preserve curated display order."
  [{:name "arithmetic"  :fns ["+" "-" "*" "/" "mod" "rem" "quot"
                              "inc" "dec" "+'" "-'" "*'" "inc'" "dec'"]}
   {:name "comparison"  :fns ["=" "<" "<=" ">" ">=" "not=" "compare"]}
   {:name "math"        :fns ["math-floor" "math-ceil" "math-round" "math-sqrt"
                              "math-pow" "math-log" "math-exp" "math-sin"
                              "math-cos" "math-tan" "math-atan2"]}
   {:name "bitwise"     :fns ["bit-and" "bit-or" "bit-xor" "bit-not"
                              "bit-shift-left" "bit-shift-right"]}
   {:name "list"        :fns ["car" "cdr" "cons" "list"]}
   {:name "collection"  :fns ["count" "nth" "first" "rest" "vector" "hash-map"
                              "assoc" "dissoc" "get" "conj" "update" "keys" "vals"
                              "hash"]}
   {:name "sets"        :fns ["hash-set" "set?" "contains?" "disj"]}
   {:name "atoms"       :fns ["atom" "deref" "reset!" "swap!" "atom?"]}
   {:name "sequences"   :fns ["map" "filter" "reduce" "take" "drop" "range"
                              "repeat" "concat" "into" "apply" "reverse" "sort"]}
   {:name "predicates"  :fns ["cons?" "nil?" "string?" "number?" "keyword?"
                              "symbol?" "vector?" "map?" "set?" "fn?" "empty?"
                              "seq?"]}
   {:name "utility"     :fns ["not" "not=" "identity" "some" "every?"
                              "rand" "eval"]}
   {:name "reflection"  :fns ["type" "name" "symbol" "keyword" "doc" "source"
                              "apropos"]}
   {:name "strings"     :fns ["str" "pr-str" "format" "subs" "split" "join"
                              "starts-with?" "ends-with?" "includes?"
                              "upper-case" "lower-case" "trim" "char-at"
                              "read-string"]}
   {:name "regex"       :fns ["re-find" "re-matches"]}
   {:name "type coercion" :fns ["int" "float"]}
   {:name "exceptions"  :fns ["throw" "last-error" "error?"]}
   {:name "modules"     :fns ["require"]}
   {:name "macros"      :fns ["macroexpand" "macroexpand-1" "gensym"]}
   {:name "definitions" :fns ["defn"]}])

(def ^:private special-forms
  "Special forms recognized directly by the evaluator. Not real
  bindings, so apropos does not see them."
  ["quote" "quasiquote" "unquote" "unquote-splicing"
   "def" "defmacro" "if" "do" "let" "fn" "loop" "recur" "try"])

(def ^:private io-prim-names
  "Primitives installed by mino_install_io. Listed here so the
  page can render them in their own section, since the host may
  call mino_install_core without mino_install_io."
  #{"println" "prn" "print" "pr" "newline"
    "slurp" "spit" "exit" "time-ms" "nano-time" "file-seq"
    "getenv" "getcwd" "chdir" "gc-stats" "gc!"})

;; --- Runtime introspection ---

(defn- introspect-bindings
  "Run the introspection script against the mino binary at
  <mino-root>/mino. Returns a vector of {:name :kind :doc} maps
  read from the script's EDN output."
  [mino-root script-path]
  (let [bin (str mino-root "/mino")]
    (when-not (.exists (io/file bin))
      (throw (ex-info (str "mino binary not found at " bin
                           " — run the bootstrap compile in the "
                           "submodule first")
                      {:bin bin})))
    (let [{:keys [exit out err]} (shell/sh bin script-path)]
      (when-not (zero? exit)
        (throw (ex-info "mino introspection script failed"
                        {:exit exit :stderr err :stdout out})))
      (edn/read-string out))))

;; --- Stdlib source extraction (parses core.mino text for "show source") ---

(defn- read-stdlib-source
  "Read core.mino from the mino source tree."
  [mino-root]
  (let [path (str mino-root "/src/core.mino")]
    (when (.exists (io/file path))
      (slurp path))))

(defn- parse-stdlib-forms
  "Walk the unescaped stdlib source line-by-line. Returns
  [{:name :kind :doc :source} ...] for each top-level defmacro
  or def. Used to populate the \"show source\" panel."
  [source]
  (let [lines (str/split-lines source)]
    (loop [lines lines
           forms []]
      (if (empty? lines)
        forms
        (let [line (first lines)]
          (cond
            (str/starts-with? line "(defmacro ")
            (let [name-m (re-find #"\(defmacro\s+(\S+)" line)
                  nm    (when name-m (second name-m))
                  form-lines
                  (loop [ls lines collected [] depth 0]
                    (if (empty? ls)
                      collected
                      (let [l (first ls)
                            opens (count (filter #(= % \() l))
                            closes (count (filter #(= % \)) l))
                            new-depth (+ depth opens (- closes))]
                        (if (and (pos? (+ depth opens)) (<= new-depth 0))
                          (conj collected l)
                          (recur (rest ls) (conj collected l) new-depth)))))
                  source (str/join "\n" form-lines)
                  doc-m (re-find #"(?s)\(defmacro\s+\S+\s+\"([^\"]+)\"" source)
                  doc (when doc-m (second doc-m))]
              (recur (drop (count form-lines) lines)
                     (conj forms {:name nm
                                  :kind :macro
                                  :doc doc
                                  :source source})))

            (str/starts-with? line "(def ")
            (let [name-m (re-find #"\(def\s+(\S+)" line)
                  nm    (when name-m (second name-m))
                  form-lines
                  (loop [ls lines collected [] depth 0]
                    (if (empty? ls)
                      collected
                      (let [l (first ls)
                            opens (count (filter #(= % \() l))
                            closes (count (filter #(= % \)) l))
                            new-depth (+ depth opens (- closes))]
                        (if (and (pos? (+ depth opens)) (<= new-depth 0))
                          (conj collected l)
                          (recur (rest ls) (conj collected l) new-depth)))))
                  source (str/join "\n" form-lines)
                  doc-m (re-find #"(?s)\(def\s+\S+\s+\"([^\"]+)\"" source)
                  doc (when doc-m (second doc-m))]
              (recur (drop (count form-lines) lines)
                     (conj forms {:name nm
                                  :kind :function
                                  :doc doc
                                  :source source})))

            :else
            (recur (rest lines) forms)))))))

;; --- Public API ---

(defn introspect
  "Boot mino at mino-root, run script-path against it, and return
  Language Reference page data.

  Returns:
    {:categories     [{:name \"arithmetic\" :primitives [\"+\" \"-\" ...]} ...]
     :prim-docs      {\"+\" \"...\" ...}
     :stdlib         [{:name :kind :doc :source} ...]
     :io-primitives  [\"println\" \"prn\" ...]
     :special-forms  [\"quote\" \"def\" ...]}"
  [mino-root script-path]
  (let [bindings     (introspect-bindings mino-root script-path)
        binding-docs (into {} (map (juxt :name :doc)) bindings)
        stdlib-src   (read-stdlib-source mino-root)
        stdlib       (when stdlib-src (parse-stdlib-forms stdlib-src))
        stdlib-names (into #{} (map :name) stdlib)
        binding-names (into #{} (map :name) bindings)
        ;; Anything bound that is not defined in core.mino is a C primitive.
        prim-names   (into #{}
                           (remove stdlib-names)
                           binding-names)
        curated-cats (mapv (fn [{:keys [name fns]}]
                             {:name name
                              :primitives (filterv #(contains? prim-names %) fns)})
                           category-order)
        curated-set  (into #{} (mapcat :primitives) curated-cats)
        other-prims  (->> (sort prim-names)
                          (remove curated-set)
                          (remove io-prim-names)
                          vec)
        cats         (cond-> curated-cats
                       (seq other-prims)
                       (conj {:name "other" :primitives other-prims}))]
    {:categories cats

     :prim-docs binding-docs

     :stdlib (or stdlib [])

     :io-primitives (filterv io-prim-names (sort prim-names))

     :special-forms special-forms}))
