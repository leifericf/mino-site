(ns mino-site.parse.builtins
  "Parse mino.c for built-in functions, stdlib macros, and I/O primitives.

  Extracts:
  - Primitive registrations from mino_install_core (with section comments)
  - Stdlib macro source from stdlib_mino_src
  - I/O primitives from mino_install_io
  - Categorization from the mino_install_core docstring in mino.h"
  (:require
    [clojure.string :as str]))

;; --- Category map from mino.h docstring ---

(def ^:private category-order
  "Authoritative category ordering and membership from the
  mino_install_core docstring in mino.h (lines 196-217)."
  [{:name "arithmetic"  :fns #{"+" "-" "*" "/" "mod" "rem" "quot"}}
   {:name "comparison"  :fns #{"=" "<" "<=" ">" ">=" "not=" "compare"}}
   {:name "math"        :fns #{"math-floor" "math-ceil" "math-round" "math-sqrt"
                                "math-pow" "math-log" "math-exp" "math-sin"
                                "math-cos" "math-tan" "math-atan2"}}
   {:name "bitwise"     :fns #{"bit-and" "bit-or" "bit-xor" "bit-not"
                                "bit-shift-left" "bit-shift-right"}}
   {:name "list"        :fns #{"car" "cdr" "cons" "list"}}
   {:name "collection"  :fns #{"count" "nth" "first" "rest" "vector" "hash-map"
                                "assoc" "dissoc" "get" "conj" "update" "keys" "vals"
                                "hash"}}
   {:name "sets"        :fns #{"hash-set" "set?" "contains?" "disj"}}
   {:name "atoms"       :fns #{"atom" "deref" "reset!" "swap!" "atom?"}}
   {:name "sequences"   :fns #{"map" "filter" "reduce" "take" "drop" "range"
                                "repeat" "concat" "into" "apply" "reverse" "sort"}}
   {:name "predicates"  :fns #{"cons?" "nil?" "string?" "number?" "keyword?"
                                "symbol?" "vector?" "map?" "set?" "fn?" "empty?"
                                "seq?"}}
   {:name "utility"     :fns #{"not" "not=" "identity" "some" "every?"
                                "rand" "eval"}}
   {:name "reflection"  :fns #{"type" "name" "symbol" "keyword" "doc" "source"
                                "apropos"}}
   {:name "strings"     :fns #{"str" "pr-str" "format" "subs" "split" "join"
                                "starts-with?" "ends-with?" "includes?"
                                "upper-case" "lower-case" "trim" "char-at"
                                "read-string"}}
   {:name "regex"       :fns #{"re-find" "re-matches"}}
   {:name "type coercion" :fns #{"int" "float"}}
   {:name "exceptions"  :fns #{"throw"}}
   {:name "modules"     :fns #{"require"}}
   {:name "macros"      :fns #{"macroexpand" "macroexpand-1" "gensym"}}
   {:name "definitions" :fns #{"defn"}}])

(def ^:private special-forms
  "Special forms recognized directly by the evaluator."
  ["quote" "quasiquote" "unquote" "unquote-splicing"
   "def" "defmacro" "if" "do" "let" "fn" "loop" "recur" "try"])

;; --- Primitive extraction from mino_install_core ---

(defn- extract-primitives
  "Extract DEF_PRIM calls from mino_install_core.
  Returns a seq of {:name \"...\" :doc \"...\"}."
  [c-text]
  (let [install-re #"(?s)void mino_install_core\(mino_state_t \*S, mino_env_t \*env\)\s*\{(.+?)\n\}"
        m (re-find install-re c-text)]
    (when m
      (let [body (nth m 1)]
        (->> (re-seq #"DEF_PRIM\(env,\s*\"([^\"]+)\"[^\"]*\"([^\"]+)\"\)" body)
             (mapv (fn [[_ name doc]] {:name name :doc doc})))))))

;; --- I/O primitives ---

(defn- extract-io-primitives
  "Extract DEF_PRIM calls from mino_install_io."
  [c-text]
  (let [io-re #"(?s)void mino_install_io\(mino_state_t \*S, mino_env_t \*env\)\s*\{(.+?)\n\}"
        m (re-find io-re c-text)]
    (when m
      (let [body (nth m 1)]
        (->> (re-seq #"DEF_PRIM\(env,\s*\"([^\"]+)\"[^\"]*\"([^\"]+)\"\)" body)
             (mapv (fn [[_ name doc]] {:name name :doc doc})))))))

;; --- Stdlib macros ---

(defn- read-stdlib-source
  "Read core.mino from the src/ directory (sibling of prim.c)."
  [prim-c-path]
  (let [dir  (.getParent (java.io.File. prim-c-path))
        path (str dir "/core.mino")]
    (when (.exists (java.io.File. path))
      (slurp path))))

(defn- parse-stdlib-forms
  "Parse the unescaped stdlib source into individual form definitions.
  Returns [{:name \"when\" :kind :macro :source \"(defmacro when ...)\"}]."
  [source]
  (let [lines (str/split-lines source)]
    (loop [lines lines
           forms []]
      (if (empty? lines)
        forms
        (let [line (first lines)]
          (cond
            ;; defmacro
            (str/starts-with? line "(defmacro ")
            (let [name-m (re-find #"\(defmacro\s+(\S+)" line)
                  name (when name-m (second name-m))
                  ;; Collect until balanced parens
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
                     (conj forms {:name name
                                  :kind :macro
                                  :doc doc
                                  :source source})))

            ;; def (for comp, partial, complement)
            (str/starts-with? line "(def ")
            (let [name-m (re-find #"\(def\s+(\S+)" line)
                  name (when name-m (second name-m))
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
                     (conj forms {:name name
                                  :kind :function
                                  :doc doc
                                  :source source})))

            ;; Blank or other
            :else
            (recur (rest lines) forms)))))))

;; --- Categorize primitives ---

(defn- categorize-primitive
  "Returns the category name for a given primitive, or \"uncategorized\"."
  [prim-name]
  (or (some (fn [{:keys [name fns]}]
              (when (contains? fns prim-name) name))
            category-order)
      "uncategorized"))

;; --- Public API ---

(defn parse
  "Parse prim.c for built-in functions and return structured data.
  Takes the path to prim.c (which contains mino_install_core/io).
  Returns:
    {:categories [{:name \"arithmetic\" :primitives [\"+ \" \"-\" ...]} ...]
     :stdlib [{:name \"when\" :kind :macro :source \"...\"} ...]
     :io-primitives [\"println\" \"prn\" \"slurp\"]
     :special-forms [\"quote\" \"def\" ...]}"
  [path]
  (let [c-text (slurp path)
        prims (or (extract-primitives c-text) [])
        io-prims (or (extract-io-primitives c-text) [])
        prim-names (mapv :name prims)
        prim-docs (into {} (map (fn [{:keys [name doc]}] [name doc]) prims))
        io-docs (into {} (map (fn [{:keys [name doc]}] [name doc]) io-prims))
        all-docs (merge prim-docs io-docs)
        stdlib-src (read-stdlib-source path)
        stdlib-forms (when stdlib-src (parse-stdlib-forms stdlib-src))]
    {:categories
     (mapv (fn [{:keys [name fns]}]
             {:name name
              :primitives (filterv #(contains? fns %) prim-names)})
           category-order)

     :prim-docs all-docs

     :stdlib (or stdlib-forms [])

     :io-primitives (mapv :name io-prims)

     :special-forms special-forms}))
