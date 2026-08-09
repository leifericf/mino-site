(ns mino-site.search.index
  "Build the static search index served at /search/index.json.

  The client fetches this JSON once on first focus and runs prefix
  matching in memory. Three categories: pages, C API symbols, and
  language forms."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [mino-site.search.tokens :as tokens]))

(def ^:private pages
  "Static index of top-level pages with descriptions."
  [{"title" "Get Started" "uri" "/get-started/"
    "desc" "Install, build, and run your first mino program."}
   {"title" "Documentation" "uri" "/documentation/"
    "desc" "Hub page for all mino documentation."}
   {"title" "Embedding Guide" "uri" "/documentation/embedding/"
    "desc" "State lifecycle, value ownership, sandboxing, handles, sessions, and threading rules."}
   {"title" "Embedding Cookbook" "uri" "/documentation/cookbook/"
    "desc" "Twelve worked embedding examples with full C source."}
   {"title" "Language Bindings" "uri" "/documentation/bindings/"
    "desc" "Embedding mino from C, C++, Java, Zig, Rust, C#, Go, and Swift."}
   {"title" "C API Reference" "uri" "/documentation/api/"
    "desc" "Every public function, type, enum, and macro in mino.h."}
   {"title" "Language Reference" "uri" "/documentation/language/"
    "desc" "Every built-in function, special form, and macro."}
   {"title" "Error Diagnostics" "uri" "/documentation/errors/"
    "desc" "Structured errors with stable codes, source snippets, and programmatic access."}
   {"title" "Testing" "uri" "/documentation/testing/"
    "desc" "Write and run tests with deftest, is, and testing."}
   {"title" "Coming from Clojure" "uri" "/documentation/coming-from-clojure/"
    "desc" "What works the same, what differs, and intentional divergences."}
   {"title" "Compatibility Matrix" "uri" "/documentation/compatibility-matrix/"
    "desc" "Clojure core functions: supported, differs, or absent in mino."}
   {"title" "Intentional Divergences" "uri" "/documentation/intentional-divergences/"
    "desc" "Where mino deliberately differs from Clojure and why."}
   {"title" "Bytes and Bit Syntax" "uri" "/documentation/bytes/"
    "desc" "Immutable binary data and Erlang-inspired bit-pattern matching."}
   {"title" "Task Runner" "uri" "/documentation/tasks/"
    "desc" "Define build tasks in mino.edn as ordinary functions."}
   {"title" "Tooling and Editors" "uri" "/documentation/tooling/"
    "desc" "tree-sitter grammar, LSP server, and nREPL server."}
   {"title" "Bytecode and VM" "uri" "/documentation/bytecode-vm/"
    "desc" "Register-based bytecode VM: opcodes, inline caches, fused loops."}
   {"title" "JIT" "uri" "/documentation/jit/"
    "desc" "Copy-and-patch JIT: stencils, parity, deopt, runtime control."}
   {"title" "Garbage Collection" "uri" "/documentation/garbage-collection/"
    "desc" "Two-generation tracing collector with incremental old-gen mark."}
   {"title" "Software Transactional Memory" "uri" "/documentation/stm/"
    "desc" "Refs, dosync, alter, commute, ensure, ref-set, watches."}
   {"title" "Performance" "uri" "/documentation/performance/"
    "desc" "Per-operation costs, collection throughput, and guidance."}
   {"title" "Platform Support" "uri" "/documentation/platforms/"
    "desc" "Operating systems, compilers, and language floors."}
   {"title" "Dependencies" "uri" "/documentation/dependencies/"
    "desc" "Module resolver: bundled stdlib, on-disk lib/, git-fetched repos."}
   {"title" "Zero dependencies, vendored first" "uri" "/documentation/vendored-first/"
    "desc" "Single-file amalgamation, no transitive build dependencies."}
   {"title" "Use Cases" "uri" "/use-cases/"
    "desc" "Worked examples sized as small applications."}
   {"title" "About" "uri" "/about/"
    "desc" "Design philosophy, trade-offs, and related projects."}])

(defn- first-sentence
  "Extract the first sentence from a doc string."
  [doc]
  (when doc
    (let [s (str/trim doc)]
      (if-let [i (str/index-of s ". ")]
        (subs s 0 (inc i))
        s))))

(defn- api-entries
  "Build search entries from parsed header data."
  [api-data]
  (for [section (:sections api-data)
        decl (:declarations section)
        :let [name (:name decl)
              kind (:kind decl)
              doc (:doc decl)]
        :when name]
    (let [type-label (case kind
                       :function "function"
                       :typedef-fn "typedef"
                       :typedef "typedef"
                       :struct "struct"
                       :enum "enum"
                       :define "macro"
                       "symbol")]
      {"name" name
       "uri" (str "/documentation/api/#" name)
       "type" type-label
       "desc" (or (first-sentence doc) "")
       "norm" (tokens/normalize (str name " " (first-sentence doc)))})))

(defn- unqualified
  "Strip namespace prefix from a symbol name: clojure.core/map -> map."
  [name]
  (if (str/includes? name "/")
    (subs name (inc (str/last-index-of name "/")))
    name))

(defn- lang-entries
  "Build search entries from parsed builtins data."
  [builtin-data]
  (let [prim-docs (:prim-docs builtin-data {})
        stdlib (:stdlib builtin-data [])
        special-forms (:special-forms builtin-data [])]
    (concat
      ;; C primitives and core.clj functions from categories
      (for [cat (:categories builtin-data [])
            name (:primitives cat)
            :let [doc (get prim-docs name)]]
        {"name" name
         "uri" (str "/documentation/language/#fn-" name)
         "type" "function"
         "desc" (or (first-sentence doc) "")
         "norm" (tokens/normalize (str name " " (first-sentence doc)))})
      ;; Stdlib macros and functions (qualified names)
      (for [form stdlib
            :let [full-name (:name form)
                  short (unqualified full-name)]]
        {"name" short
         "uri" (str "/documentation/language/#fn-" full-name)
         "type" (case (:kind form) :macro "macro" "function")
         "desc" (or (first-sentence (:doc form)) "")
         "norm" (tokens/normalize (str short " " (first-sentence (:doc form))))})
      ;; Special forms
      (for [name special-forms]
        {"name" name
         "uri" (str "/documentation/language/#fn-" name)
         "type" "special form"
         "desc" (or (first-sentence (get prim-docs name)) "")
         "norm" (tokens/normalize name)}))))

(defn build
  "Build the search index from parsed API and builtins data.
  Returns a plain map ready for JSON serialization."
  [api-data builtin-data]
  (let [page-entries (mapv #(assoc % "norm" (tokens/normalize (str (% "title") " " (% "desc"))))
                           pages)]
    {"pages" page-entries
     "api" (vec (api-entries api-data))
     "language" (vec (lang-entries builtin-data))}))

(defn ->json
  "Serialize the index to a JSON string."
  [index]
  (json/write-str index :escape-slash false))
