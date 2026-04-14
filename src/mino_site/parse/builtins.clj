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
  [{:name "arithmetic"  :fns #{"+" "-" "*" "/"}}
   {:name "comparison"  :fns #{"=" "<" "<=" ">" ">=" "not="}}
   {:name "list"        :fns #{"car" "cdr" "cons" "list"}}
   {:name "collection"  :fns #{"count" "nth" "first" "rest" "vector" "hash-map"
                                "assoc" "get" "conj" "update" "keys" "vals"}}
   {:name "sets"        :fns #{"hash-set" "set?" "contains?" "disj"}}
   {:name "atoms"       :fns #{"atom" "deref" "reset!" "swap!" "atom?"}}
   {:name "sequences"   :fns #{"map" "filter" "reduce" "take" "drop" "range"
                                "repeat" "concat" "into" "apply" "reverse" "sort"}}
   {:name "predicates"  :fns #{"cons?" "nil?" "string?" "number?" "keyword?"
                                "symbol?" "vector?" "map?" "set?" "fn?" "empty?"}}
   {:name "utility"     :fns #{"not" "not=" "identity" "some" "every?"}}
   {:name "reflection"  :fns #{"type" "doc" "source" "apropos"}}
   {:name "strings"     :fns #{"str" "subs" "split" "join" "starts-with?"
                                "ends-with?" "includes?" "upper-case"
                                "lower-case" "trim"}}
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
  "Extract mino_env_set calls from mino_install_core in mino.c.
  Returns a seq of primitive names as strings."
  [c-text]
  (let [;; Find the mino_install_core function body
        install-re #"(?s)void mino_install_core\(mino_env_t \*env\)\s*\{(.+?)\n\}"
        m (re-find install-re c-text)]
    (when m
      (let [body (nth m 1)]
        (->> (re-seq #"mino_env_set\(env,\s*\"([^\"]+)\"" body)
             (mapv second))))))

;; --- I/O primitives ---

(defn- extract-io-primitives
  "Extract primitives registered in mino_install_io."
  [c-text]
  (let [io-re #"(?s)void mino_install_io\(mino_env_t \*env\)\s*\{(.+?)\n\}"
        m (re-find io-re c-text)]
    (when m
      (let [body (nth m 1)]
        (->> (re-seq #"mino_env_set\(env,\s*\"([^\"]+)\"" body)
             (mapv second))))))

;; --- Stdlib macros ---

(defn- read-stdlib-source
  "Read core.mino from the mino repo root (sibling of mino.c)."
  [mino-c-path]
  (let [dir  (.getParent (java.io.File. mino-c-path))
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
                          (recur (rest ls) (conj collected l) new-depth)))))]
              (recur (drop (count form-lines) lines)
                     (conj forms {:name name
                                  :kind :macro
                                  :source (str/join "\n" form-lines)})))

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
                          (recur (rest ls) (conj collected l) new-depth)))))]
              (recur (drop (count form-lines) lines)
                     (conj forms {:name name
                                  :kind :function
                                  :source (str/join "\n" form-lines)})))

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
  "Parse mino.c for built-in functions and return structured data.
  Returns:
    {:categories [{:name \"arithmetic\" :primitives [\"+ \" \"-\" ...]} ...]
     :stdlib [{:name \"when\" :kind :macro :source \"...\"} ...]
     :io-primitives [\"println\" \"prn\" \"slurp\"]
     :special-forms [\"quote\" \"def\" ...]}"
  [path]
  (let [c-text (slurp path)
        prims (extract-primitives c-text)
        io-prims (extract-io-primitives c-text)
        stdlib-src (read-stdlib-source path)
        stdlib-forms (when stdlib-src (parse-stdlib-forms stdlib-src))]
    {:categories
     (mapv (fn [{:keys [name fns]}]
             {:name name
              :primitives (filterv #(contains? fns %) prims)})
           category-order)

     :stdlib (or stdlib-forms [])

     :io-primitives (or io-prims [])

     :special-forms special-forms}))
