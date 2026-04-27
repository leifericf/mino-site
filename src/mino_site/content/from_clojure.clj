(ns mino-site.content.from-clojure
  "Coming from Clojure page content."
  (:require
    [hiccup2.core :as h]))

(defn from-clojure-page
  "Generates the Coming from Clojure page HTML body."
  []
  (str
    (h/html
      [:h1 "Coming from Clojure"]

      [:p "mino aspires to become a proper Clojure dialect. "
       "If you know Clojure, most mino code will look familiar. "
       "This page is the higher-level tour of where mino differs "
       "and why; for an item-by-item table of supported / differs / "
       "absent forms, see the "
       [:a {:href "/documentation/compatibility-matrix/"}
        "compatibility matrix"]
       ", and for the longer-form rationale behind each divergence, "
       "see "
       [:a {:href "/documentation/intentional-divergences/"}
        "intentional divergences"] "."]

      ;; --- Rules of thumb ---

      [:h2 "Rules of thumb"]
      [:ul
       [:li "Pure language syntax follows Clojure."]
       [:li "Host boundary operations use mino-native explicit forms."]
       [:li "Reader desugaring is preferred over evaluator special cases."]
       [:li "Macro and library implementations are preferred over new "
        "C runtime features."]
       [:li "Least-surprising behavior for REPL users."]
       [:li "Differences are intentional, documented, and tied to "
        "embeddability."]]

      ;; --- What works the same ---

      [:h2 "What works the same"]
      [:p "Most daily Clojure idioms work as expected:"]
      [:ul
       [:li "Persistent vectors, maps, sets, and lists with structural sharing"]
       [:li "Immutable values everywhere"]
       [:li [:code "fn"] ", " [:code "defn"] ", " [:code "let"] ", "
        [:code "loop"] "/" [:code "recur"] " with vector binding forms"]
       [:li "Destructuring in " [:code "let"] ", " [:code "fn"] ", "
        [:code "loop"] " (vector positional, map " [:code ":keys"]
        "/" [:code ":or"] "/" [:code ":as"] ", nested)"]
       [:li "Multi-arity functions: " [:code "(fn ([x] ...) ([x y] ...))"]]
       [:li "Named anonymous functions: " [:code "(fn name [x] ...)"]]
       [:li "Macros, quasiquote, " [:code "gensym"]]
       [:li "Lazy sequences (" [:code "lazy-seq"] ", " [:code "map"]
        ", " [:code "filter"] ", " [:code "take"] ", " [:code "range"]
        ", etc.)"]
       [:li "Atoms with " [:code "swap!"] " and " [:code "reset!"]]
       [:li "Threading macros (" [:code "->"] ", " [:code "->>"] ", "
        [:code "as->"] ", " [:code "cond->"] ", " [:code "cond->>"] ", "
        [:code "some->"] ", " [:code "some->>"] ")"]
       [:li [:code "try"] "/" [:code "catch"] "/" [:code "finally"]
        "/" [:code "throw"]]
       [:li [:code "with-open"] " for resource management"]
       [:li [:code "apply"] ", " [:code "partial"] ", " [:code "comp"]
        ", " [:code "complement"]]
       [:li "Regular expressions via " [:code "re-find"] ", "
        [:code "re-matches"] ", " [:code "re-seq"]]
       [:li "Full test framework: " [:code "deftest"] ", "
        [:code "is"] ", " [:code "testing"]]
       [:li "Callable keywords: " [:code "(:k m)"] " as map lookup"]
       [:li [:code "#()"] " anonymous function shorthand"]
       [:li [:code "#_"] " discard reader macro"]
       [:li [:code "ex-info"] ", " [:code "ex-data"] ", "
        [:code "ex-message"]]
       [:li [:code "reduced"] " for early termination in " [:code "reduce"]]
       [:li "Multi-collection " [:code "map"] ": "
        [:code "(map + [1 2] [3 4])"] " works"]
       [:li [:code "set"] " constructor: " [:code "(set coll)"]]
       [:li "Variadic " [:code "comp"] ": "
        [:code "(comp f g h ...)"]]
       [:li [:code "identical?"] " for pointer identity"]
       [:li "Value metadata: " [:code "meta"] ", "
        [:code "with-meta"] ", " [:code "vary-meta"] ", "
        [:code "alter-meta!"] ", "
        [:code "^{:key val}"] "/" [:code "^:key"] "/"
        [:code "^Type"] " reader syntax"]
       [:li "Protocols: " [:code "defprotocol"] ", "
        [:code "extend-type"] ", " [:code "extend-protocol"] ", "
        [:code "satisfies?"]]
       [:li "Multi-binding " [:code "for"] " and " [:code "doseq"]
        " with " [:code ":when"] ", " [:code ":while"]
        ", and " [:code ":let"]]
       [:li "Transducers: " [:code "transduce"] ", "
        [:code "into"] " with xform, " [:code "sequence"] ", "
        [:code "eduction"] ", " [:code "completing"] ", "
        [:code "cat"] ", " [:code "halt-when"] ", "
        [:code "ensure-reduced"]]
       [:li "Attribute maps in " [:code "defn"] " and "
        [:code "defmacro"] " are accepted and skipped"]
       [:li "Forward declarations: " [:code "declare"] " and "
        [:code "(def name)"] " without a value"]
       [:li "Bootstrap aliases: " [:code "fn*"] ", "
        [:code "let*"] ", " [:code "loop*"] " are accepted as "
        "their unstarred equivalents"]
       [:li [:code "update-vals"] " and " [:code "update-keys"]
        " for transforming map values or keys"]
       [:li [:code "min-key"] " and " [:code "max-key"]
        " for finding elements by keyed comparison"]
       [:li [:code "case"] " with literal matching, multi-match lists, "
        "and default clauses"]
       [:li [:code "comment"] " and " [:code "when-first"] " macros"]
       [:li [:code "clojure.set"] " namespace: " [:code "union"] ", "
        [:code "intersection"] ", " [:code "difference"] ", "
        [:code "select"] ", " [:code "project"] ", " [:code "rename"] ", "
        [:code "rename-keys"] ", " [:code "map-invert"] ", "
        [:code "join"] ", " [:code "index"] ", " [:code "subset?"] ", "
        [:code "superset?"]]
       [:li [:code "clojure.string"] " namespace: " [:code "lower-case"] ", "
        [:code "upper-case"] ", " [:code "capitalize"] ", "
        [:code "reverse"] ", " [:code "blank?"] ", "
        [:code "starts-with?"] ", " [:code "ends-with?"] ", "
        [:code "escape"] ", " [:code "replace"] ", " [:code "trim"] ", "
        [:code "triml"] ", " [:code "trimr"] ", " [:code "trim-newline"] ", "
        [:code "split-lines"] ", " [:code "join"] ", " [:code "includes?"]]
       [:li [:code "random-sample"] " for probabilistic filtering"]
       [:li [:code "bounded-count"] " for counting with an upper limit "
        "on lazy sequences"]
       [:li [:code "while"] " macro for imperative loops"]
       [:li [:code "distinct?"] " for checking argument uniqueness"]
       [:li "Type predicates: " [:code "sorted?"] ", "
        [:code "associative?"] ", " [:code "reversible?"] ", "
        [:code "counted?"] ", " [:code "any?"]]]

      ;; --- Namespaces ---

      [:h2 "Namespaces"]
      [:p "Namespaces are first-class. Each namespace owns its "
       "own root binding table, so " [:code "(ns a) (def x 1)"]
       " and " [:code "(ns b) (def x 2)"] " are independent. "
       [:code "clojure.core"] " is the bundled-core namespace; "
       "every other namespace's root env chains to it via a parent "
       "pointer, so unqualified " [:code "if"] ", " [:code "map"]
       ", " [:code "let"] " keep working without an explicit refer."]
      [:pre [:code
        "(ns myapp.core\n"
        "  (:require [clojure.string :as str]))\n"
        "\n"
        "(str/blank? \"\")  ;=> true"]]
      [:p "The full " [:code "ns"] " surface is here: "
       [:code ":require"] " (with " [:code ":as"] ", "
       [:code ":as-alias"] ", " [:code ":refer"] ", "
       [:code ":refer :all"] ", " [:code ":only"] ", "
       [:code ":exclude"] ", " [:code ":rename"] ", and prefix "
       "lists), " [:code ":use"] ", and " [:code ":refer-clojure"]
       ". Vars are first-class — " [:code "(def x 1)"] " returns "
       [:code "#'<ns>/x"] ", " [:code "intern"] ", "
       [:code "find-var"] ", " [:code "var-get"] ", "
       [:code "var-set"] ", " [:code "alter-var-root"] ", and "
       [:code "with-redefs"] " all work, and " [:code "^:private"]
       " is enforced on cross-namespace qualified access."]
      [:p "Module resolution uses a host-supplied resolver. The "
       "default standalone resolver searches " [:code ".cljc"]
       ", " [:code ".clj"] ", and " [:code ".cljs"] " in that "
       "order. A loaded file's first " [:code "(ns ...)"]
       " form must declare the requested module name (dash and "
       "underscore are equivalent), so accidental misnaming fails "
       "loud rather than silently. Isolation between runtimes "
       "still gives you full per-state isolation when you want it."]

      ;; --- Concurrency ---

      [:h2 "Concurrency"]
      [:p "mino provides two concurrency models:"]

      [:h3 "core.async"]
      [:p [:code "core.async"] " channels and go blocks work as expected:"]
      [:pre [:code
        "(require \"core/async\")\n"
        "\n"
        "(let [ch (chan 10)]\n"
        "  (go (>! ch 42))\n"
        "  (println (<!! ch)))  ;=> 42"]]
      [:p "Supported: " [:code "chan"] ", " [:code "put!"] ", "
       [:code "take!"] ", " [:code "close!"] ", " [:code "go"] ", "
       [:code "go-loop"] ", " [:code "<!"] ", " [:code ">!"] ", "
       [:code "<!!"] ", " [:code ">!!"] ", " [:code "alts!"] ", "
       [:code "alts!!"] ", " [:code "timeout"] ", " [:code "pipe"] ", "
       [:code "merge-chans"] ", " [:code "mult"] "/" [:code "tap"] ", "
       [:code "pub"] "/" [:code "sub"] ", " [:code "mix"] "/" [:code "admix"] ", "
       [:code "pipeline"] ", " [:code "pipeline-async"] ". "
       "Channels support transducers and exception handlers."]
      [:p "Differences from the JVM implementation:"]
      [:ul
       [:li "Single-threaded cooperative scheduling (no OS threads)"]
       [:li "No " [:code "thread"] " or " [:code "thread-call"] " (use "
        [:code "go"] " blocks instead)"]
       [:li [:code "alt!"] " macro is not implemented (use " [:code "alts!"] ")"]
       [:li "Parks in " [:code "catch"] "/" [:code "finally"] " bodies "
        "are not supported"]
       [:li "Nested parks in function call arguments require explicit "
        [:code "let"] " bindings"]]

      ;; --- Host interop ---

      [:h2 "Host interop"]
      [:p "mino uses the same " [:code ".method"] ", "
       [:code ".-field"] ", " [:code "new"] ", and "
       [:code "Type/static"] " syntax. The host registers "
       "capabilities through a type-oriented registry with a "
       "default-deny policy:"]
      [:pre [:code
        ";; Familiar syntax, capability-gated dispatch\n"
        "(def c (new Counter))\n"
        "(.inc c)\n"
        "(.-value c)          ;=> 1\n"
        "(Math/add 3 4)       ;=> 7\n"
        "\n"
        ";; Explicit forms also available\n"
        "(host/call c :inc)\n"
        "(host/get c :value)\n"
        "(host/static-call :Math :add 3 4)"]]
      [:p "The host decides which types and methods each runtime "
       "gets. There is no ambient access to system resources. See "
       "the " [:a {:href "/documentation/embedding/"} "Embedding Guide"]
       " for details."]

      ;; --- Reader syntax ---

      [:h2 "Reader syntax"]
      [:p "Most reader macros work as in Clojure. A few are absent:"]

      [:table
       [:thead [:tr [:th "Syntax"] [:th "Status"]]]
       [:tbody
        [:tr [:td [:code "#(inc %)"]]
         [:td "Same"]]
        [:tr [:td [:code "#'var"]]
         [:td "Same"]]
        [:tr [:td [:code "#_ form"]]
         [:td "Same"]]
        [:tr [:td [:code "^{:key val}"] " / " [:code "^:key"]
         " / " [:code "^Type"]]
         [:td "Same"]]
        [:tr [:td [:code "#?(:clj ... :default ...)"]]
         [:td "Same (active dialect keys are " [:code ":mino"]
          " and " [:code ":clj"] ")"]]
        [:tr [:td [:code "#?@(...)"]]
         [:td "Same (splice reader conditional)"]]
        [:tr [:td [:code "'()"] " / " [:code "`(~x ~@xs)"]
         " / " [:code "@atom"]]
         [:td "Same"]]
        [:tr [:td [:code "2r1010"] " / " [:code "0xFF"]
         " / " [:code "8r77"]]
         [:td "Same (radix and hex integer literals)"]]
        [:tr [:td [:code "#\"regex\""]]
         [:td "Read but routes through the string-escape path "
          "(use " [:code "\"\\\\d+\""] " for "
          [:code "\\d+"] ")"]]
        [:tr [:td [:code "::keyword"] " / "
              [:code "::alias/keyword"]]
         [:td "Same (auto-resolved at read time)"]]
        [:tr [:td [:code "#:foo{:b 1}"] " / "
              [:code "#::{:b 1}"] " / "
              [:code "#::alias{...}"]]
         [:td "Same (namespaced map literals)"]]]]

      ;; --- Data structures ---

      [:h2 "Data structures"]
      [:p "Core data structures match Clojure semantics:"]
      [:ul
       [:li "Vectors, maps, sets, and lists are persistent and immutable "
        "with structural sharing (Bagwell tries for vectors, HAMT for maps "
        "and sets)"]
       [:li "Cross-type sequential equality: "
        [:code "(= '(1 2) [1 2])"] " is " [:code "true"]]
       [:li [:code "conj"] ", " [:code "assoc"] ", " [:code "dissoc"]
        ", " [:code "get"] ", " [:code "nth"] ", " [:code "into"]
        " work as expected"]
       [:li "Sets support " [:code "contains?"] ", " [:code "conj"]
        ", " [:code "disj"]]
       [:li "Collections as callable functions: " [:code "({:a 1} :a)"]
        " returns " [:code "1"] ", " [:code "([1 2 3] 0)"] " returns "
        [:code "1"] ", " [:code "(#{:a :b} :a)"] " returns " [:code ":a"]
        ". Works in higher-order contexts like "
        [:code "(map :name coll)"] " and " [:code "(filter #{:a} coll)"]]
       [:li [:code "peek"] " and " [:code "pop"] " for stack abstraction "
        "on vectors (from end) and lists (from front)"]
       [:li [:code "find"] " returns " [:code "[key val]"]
        " or " [:code "nil"]]
       [:li [:code "empty"] " returns an empty collection of the same type"]
       [:li [:code "rseq"] " for reverse-order vector traversal"]
       [:li [:code "sorted-map"] " and " [:code "sorted-set"]
        " with persistent red-black tree (LLRB), maintaining key ordering "
        "with structural sharing"]]
      [:p "Differences:"]
      [:ul
       [:li [:code "array-map"] " is an alias for " [:code "hash-map"]
        " (HAMT is used at all sizes)"]]

      ;; --- Characters ---

      [:h2 "Characters"]
      [:p "Character literals (" [:code "\\A"] ", " [:code "\\space"]
       ", " [:code "\\uNNNN"] ", literal UTF-8 like " [:code "\\☃"]
       ") parse to a distinct character type holding a Unicode "
       "codepoint. " [:code "char?"] " returns " [:code "true"] " for "
       "chars and only for chars; " [:code "string?"] " returns "
       [:code "false"] ". " [:code "(int \\A)"] " is " [:code "65"]
       " and " [:code "(str \\A)"] " is " [:code "\"A\""] ". Chars "
       "hash and compare distinctly from single-character strings, "
       "so they can live cleanly as map keys or set members."]

      ;; --- Sequences ---

      [:h2 "Sequences"]
      [:p "Lazy sequences work the same way:"]
      [:pre [:code
        "(take 5 (map inc (range)))  ;=> (1 2 3 4 5)"]]
      [:p [:code "rest"] " on vectors, maps, sets, and strings returns "
       "a lazy cons chain (elements produced on demand), matching the "
       "expected behavior for large collections."]
      [:p "Transducers work as expected:"]
      [:pre [:code
        "(into [] (comp (map inc) (filter even?)) [1 2 3 4 5])\n"
        ";=> [2 4 6]\n"
        "\n"
        "(transduce (map inc) + 0 [1 2 3])\n"
        ";=> 9"]]
      [:p "Differences:"]
      [:ul
       [:li "No chunked sequences"]
       [:li [:code "rest"] " has " [:code "next"] " semantics: it returns "
        [:code "nil"] " when exhausted, not an empty list. mino's empty list "
        "is " [:code "nil"] ". This matches how most real-world code uses "
        [:code "next"] " for nil-punning."]]

      ;; --- Numeric tower ---

      [:h2 "Numbers"]
      [:p "mino has the full Clojure numeric tower: 64-bit "
       [:code "Long"] ", 64-bit IEEE 754 " [:code "Double"] ", "
       "arbitrary-precision " [:code "BigInt"] ", exact "
       [:code "Ratio"] ", and arbitrary-precision "
       [:code "BigDec"] ". The bignum tier is backed by vendored "
       "MIT-licensed " [:a {:href "https://github.com/creachadair/imath"}
        "imath"] "."]
      [:ul
       [:li "Ratio literals (" [:code "1/2"] ") parse to a real "
        [:code "Ratio"] " value. " [:code "ratio?"] " is "
        [:code "true"] " for non-integer ratios; an exact "
        "integer ratio reduces to its long or bigint value."]
       [:li "BigInt literals (" [:code "42N"] ") parse to "
        [:code "MINO_BIGINT"] ". Cross-tier equality matches "
        "Clojure: " [:code "(= 1 1N)"] " is " [:code "true"]
        ", " [:code "(= 1.0 1)"] " is " [:code "false"] "."]
       [:li "BigDec literals (" [:code "1.5M"] ") parse to "
        [:code "MINO_BIGDEC"] ". " [:code "(with-precision n)"]
        " controls division rounding."]
       [:li "Plain " [:code "+"] " / " [:code "-"] " / "
        [:code "*"] " / " [:code "inc"] " / " [:code "dec"]
        " throw " [:code ":eval/overflow"] " (code "
        [:code "MOV001"] ") on long overflow rather than "
        "auto-promoting. Use " [:code "+'"] " / " [:code "-'"]
        " / " [:code "*'"] " / " [:code "inc'"] " / "
        [:code "dec'"] " for Clojure's auto-promote semantics. "
        "Mixed-type tower dispatch (long × bigint, ratio × "
        "bigdec, etc.) follows the standard promotion order."]
       [:li "Float arithmetic follows IEEE 754."]]

      ;; --- Error handling ---

      [:h2 "Error handling"]
      [:p [:code "try"] "/" [:code "catch"] "/" [:code "throw"] " work "
       "as expected, but mino improves on the JVM approach: "
       [:code "throw"] " accepts any value, and " [:code "catch"]
       " always receives a structured diagnostic map with stable "
       "keys like " [:code ":mino/kind"] ", " [:code ":mino/code"]
       ", and " [:code ":mino/message"] ". The original thrown value "
       "is accessible via " [:code "ex-data"] ":"]
      [:pre [:code
        "(try\n"
        "  (count 42)\n"
        "  (catch e\n"
        "    (println (:mino/kind e))    ;; :eval/type\n"
        "    (println (:mino/code e))    ;; \"MTY001\"\n"
        "    (println (:mino/message e)) ;; \"count: expected a collection, got int\"\n"
        "    ))"]]
      [:p "Errors render with source snippets in the REPL, similar "
       "to Rust and Elm. Every error has a searchable code. See the "
       [:a {:href "/documentation/errors/"} "Error Diagnostics"]
       " guide for the full story."]
      [:p [:code "ex-info"] ", " [:code "ex-data"] ", and "
       [:code "ex-message"] " work transparently with both "
       "diagnostic maps and user-thrown values. "
       [:code "finally"] " and " [:code "with-open"]
       " work as expected:"]
      [:pre [:code
        "(with-open [f (open \"data.txt\")]\n"
        "  (read-all f))"]]

      ;; --- What is intentionally absent ---

      [:h2 "Intentionally absent"]
      [:p "These are design decisions, not missing features. See "
       [:a {:href "/documentation/intentional-divergences/"}
        "intentional divergences"] " for the full rationale "
       "behind each:"]
      [:ul
       [:li [:strong "JVM interop surface."]
        " " [:code "Class/forName"] ", " [:code "bean"] ", "
        [:code "gen-class"] ", " [:code ".."] ", "
        [:code "*warn-on-reflection*"] ", and host-array "
        "literals all assume a JVM. mino's host-method syntax "
        "(" [:code "(.next obj)"] ", " [:code "Type/static"]
        ") goes through a capability registry the embedder "
        "controls."]
       [:li [:strong "Records and types."]
        " " [:code "defrecord"] " / " [:code "deftype"]
        " / " [:code "reify"] " / " [:code "proxy"]
        " / " [:code "definterface"]
        " do not exist. Use maps and "
        [:code "defprotocol"] " + " [:code "extend-type"] "."]
       [:li [:strong "Host-thread primitives."]
        " " [:code "future"] " / " [:code "promise"]
        " / " [:code "pmap"] " / " [:code "thread"] " / "
        [:code "agent"] " require host OS threads. Use "
        [:code "core.async"] " inside a runtime or run "
        "multiple isolated runtimes on host threads."]
       [:li [:strong "Shared-memory STM."]
        " No refs, no " [:code "dosync"] ". Atoms cover "
        "mino's concurrency model."]
       [:li [:strong "Chunked sequences."]
        " mino's lazy seqs are element-at-a-time. Use "
        "transducers when throughput matters."]
       [:li [:strong "Distinct empty list."]
        " " [:code "(list)"] " returns " [:code "nil"] ", not an "
        "empty list object. " [:code "rest"] " has " [:code "next"]
        " semantics."]
       [:li [:strong "Plain-arithmetic auto-promote."]
        " Plain " [:code "+"] " / " [:code "-"] " / " [:code "*"]
        " throw on long overflow; use " [:code "+'"] " / "
        [:code "-'"] " / " [:code "*'"] " for Clojure's "
        "auto-promoting semantics. The numeric tower itself "
        "(BigInt, Ratio, BigDec) is complete."]]

      ;; --- Quick reference ---

      [:h2 "Quick reference"]
      [:table
       [:thead [:tr [:th "Feature"] [:th "Status"]]]
       [:tbody
        [:tr [:td [:code "(ns ...)"] " / " [:code "require"]]
         [:td "Same"]]
        [:tr [:td [:code "(:key map)"] " / " [:code "#(inc %)"]]
         [:td "Same"]]
        [:tr [:td [:code "(.method obj)"] " / "
         [:code "Type/static"]]
         [:td "Same (capability-gated)"]]
        [:tr [:td [:code "(ex-info ...)"] " / "
         [:code "try"] "/" [:code "catch"]]
         [:td "Same"]]
        [:tr [:td [:code "core.async"] " channels / " [:code "go"]]
         [:td "Same (single-threaded scheduling)"]]
        [:tr [:td [:code "(dosync ...)"] " / " [:code "(ref ...)"]]
         [:td "Not applicable (use atoms)"]]
        [:tr [:td [:code "(future ...)"]]
         [:td "Not implemented (use " [:code "go"] ")"]]
        [:tr [:td [:code "(thread ...)"]]
         [:td "Not implemented (use " [:code "go"] ")"]]
        [:tr [:td [:code "defmulti"] " / " [:code "defmethod"]]
         [:td "Supported"]]
        [:tr [:td [:code "defrecord"] " / " [:code "deftype"]]
         [:td "Not implemented (use maps)"]]
        [:tr [:td [:code "1/2"] " / " [:code "42N"] " / "
         [:code "1.5M"]]
         [:td "Real Ratio / BigInt / BigDec"]]
        [:tr [:td "Plain " [:code "+"] " / " [:code "-"] " / "
         [:code "*"] " on long overflow"]
         [:td "Throws (use " [:code "+'"] " / " [:code "-'"]
          " / " [:code "*'"] " to auto-promote)"]]]])))

