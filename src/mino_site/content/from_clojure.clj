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
       "It's a work in progress. If you know Clojure, most mino "
       "code will look familiar. This page documents where "
       "mino differs and why."]

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
      [:p "mino supports " [:code "ns"] " forms and "
       [:code "require"] " with " [:code ":as"] " and "
       [:code ":refer"] ". Namespace-qualified and aliased symbols "
       "resolve correctly across files."]
      [:pre [:code
        "(ns myapp.core\n"
        "  (:require [clojure.string :as str]))\n"
        "\n"
        "(str/blank? \"\")  ;=> true"]]
      [:p "Module resolution uses a host-supplied resolver. The "
       "default standalone resolver searches relative paths with "
       [:code ".mino"] " and " [:code ".cljc"] " extensions. "
       "All definitions go into a single global environment per "
       "runtime. Isolation between runtimes replaces isolation "
       "between namespaces."]

      ;; --- Concurrency ---

      [:h2 "Concurrency"]
      [:p "Clojure provides shared-memory concurrency via STM, refs, "
       "agents, and " [:code "core.async"] " channels within a single "
       "JVM. mino provides runtime-level isolation instead:"]
      [:pre [:code
        ";; Clojure: shared-memory coordination\n"
        "(def counter (ref 0))\n"
        "(dosync (alter counter inc))\n"
        "\n"
        ";; mino: isolated runtimes with message passing\n"
        "(def worker (spawn (receive msg (send (first msg) \"done\"))))\n"
        "(def reply (ask worker \"go\"))"]]
      [:p "Within a single runtime, atoms work as in Clojure. Between "
       "runtimes, " [:code "spawn"] " creates isolated actors and "
       [:code "send"] "/" [:code "ask"] " pass immutable messages. "
       "There are no refs, no STM, no agents in the Clojure sense."]

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
         [:td "Same (dialect key is " [:code ":mino"] ")"]]
        [:tr [:td [:code "#?@(...)"]]
         [:td "Same (splice reader conditional)"]]
        [:tr [:td [:code "'()"] " / " [:code "`(~x ~@xs)"]
         " / " [:code "@atom"]]
         [:td "Same"]]
        [:tr [:td [:code "2r1010"] " / " [:code "0xFF"]
         " / " [:code "8r77"]]
         [:td "Same (radix and hex integer literals)"]]
        [:tr [:td [:code "#\"regex\""]]
         [:td "Use " [:code "(re-pattern \"regex\")"]]]
        [:tr [:td [:code "::keyword"]]
         [:td "Not supported (no auto-resolved keywords)"]]]]

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
       [:li "No transient collections (all mutation is through atoms)"]
       [:li [:code "array-map"] " is an alias for " [:code "hash-map"]
        " (HAMT is used at all sizes)"]]

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
      [:p "mino has 64-bit integers and 64-bit IEEE 754 floats."]
      [:ul
       [:li "Ratio literals (" [:code "1/2"] ") parse but convert "
        "to int (when exact) or float. " [:code "ratio?"] " always "
        "returns false."]
       [:li "BigInt literals (" [:code "42N"] ") parse as regular "
        "integers. " [:code "decimal?"] " always returns false."]
       [:li "BigDec literals (" [:code "1.5M"] ") parse as regular "
        "floats."]
       [:li "Integer overflow wraps silently (C semantics). "
        [:code "(+ Long/MAX_VALUE 1)"] " wraps to a negative number "
        "rather than auto-promoting to BigInt."]
       [:li "Float arithmetic follows IEEE 754 without exact rational "
        "arithmetic."]]
      [:p "All standard arithmetic, comparison, and math functions work. "
       "The difference is in type predicates and overflow behavior, "
       "not in the operations themselves."]

      ;; --- Error handling ---

      [:h2 "Error handling"]
      [:p [:code "try"] "/" [:code "catch"] "/" [:code "throw"] " work "
       "as in Clojure, but " [:code "throw"] " accepts any value, "
       "not just exceptions:"]
      [:pre [:code
        "(try\n"
        "  (throw {:type :not-found :id 42})\n"
        "  (catch e\n"
        "    (println \"caught:\" e)))"]]
      [:p [:code "ex-info"] ", " [:code "ex-data"] ", and "
       [:code "ex-message"] " work as in Clojure. "
       [:code "finally"] " works as expected. " [:code "with-open"]
       " manages resources with automatic cleanup:"]
      [:pre [:code
        "(with-open [f (open \"data.txt\")]\n"
        "  (read-all f))"]]

      ;; --- What is intentionally absent ---

      [:h2 "Intentionally absent"]
      [:p "These are design decisions, not missing features:"]
      [:ul
       [:li [:strong "Arbitrary-precision numbers."]
        " No BigInt, BigDecimal, or exact ratio types. Ratio, N, "
        "and M literals parse but convert to int/float. This keeps "
        "the runtime small and avoids a library dependency."]
       [:li [:strong "Distinct character type."]
        " Character literals (" [:code "\\A"] ", " [:code "\\space"]
        ") are represented as single-character strings. "
        [:code "char?"] " returns false, " [:code "string?"]
        " returns true. This simplifies the value model at the "
        "cost of " [:code "char?"] "/" [:code "string?"] " predicate "
        "divergence."]
       [:li [:strong "Distinct empty list."]
        " " [:code "(list)"] " returns " [:code "nil"] ", not an "
        "empty list object. " [:code "rest"] " has " [:code "next"]
        " semantics. " [:code "(seq ())"] " and " [:code "(seq nil)"]
        " are both " [:code "nil"] "."]
       [:li [:strong "Multimethods."]
        " " [:code "defmulti"] "/" [:code "defmethod"] " are not "
        "implemented. Protocols cover the same dispatch patterns."]
       [:li [:strong "Records and types."]
        " " [:code "defrecord"] "/" [:code "deftype"] " do not exist. "
        "Maps are the universal data carrier."]
       [:li [:strong "Transient collections."]
        " All mutation goes through atoms. "
        [:code "transient"] "/" [:code "persistent!"] " are not "
        "implemented."]
       [:li [:strong "Integer overflow detection."]
        " Arithmetic wraps silently per C semantics rather than "
        "throwing or auto-promoting."]
       [:li [:strong "Shared-memory STM."]
        " No refs, no " [:code "dosync"] ". Runtime isolation and "
        "message passing replace shared-memory coordination."]
       [:li [:strong "Auto-resolved keywords."]
        " " [:code "::key"] " and " [:code "::alias/key"]
        " are not supported."]]

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
        [:tr [:td [:code "(dosync ...)"] " / " [:code "(ref ...)"]]
         [:td "Not applicable (use atoms)"]]
        [:tr [:td [:code "(future ...)"]]
         [:td [:code "(spawn ...)"]]]
        [:tr [:td [:code "defmulti"] " / " [:code "defrecord"]]
         [:td "Not implemented"]]
        [:tr [:td [:code "1/2"] " / " [:code "42N"] " / "
         [:code "1.5M"]]
         [:td "Parse but convert to int/float"]]]])))

