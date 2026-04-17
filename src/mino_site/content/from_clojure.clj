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
      [:p "mino uses a flat namespace model. There is no " [:code "ns"]
       " form, no " [:code ":require"] "/" [:code ":as"]
       "/" [:code ":refer"] " within " [:code "ns"] "."]
      [:pre [:code
        ";; Clojure\n"
        "(ns myapp.core\n"
        "  (:require [clojure.string :as str]))\n"
        "\n"
        ";; mino\n"
        "(require \"path/to/module\")"]]
      [:p [:code "require"] " loads a file once by path. All definitions "
       "go into a single global environment per runtime. Isolation "
       "between runtimes replaces isolation between namespaces."]

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
      [:p "Clojure interops with the JVM through " [:code ".method"]
       " and " [:code "Class/staticMethod"] " syntax. mino interops "
       "with C through opaque handles and capability functions:"]
      [:pre [:code
        ";; Clojure: Java interop\n"
        "(.toLowerCase \"HELLO\")\n"
        "(System/getProperty \"os.name\")\n"
        "\n"
        ";; mino: handle-based C interop\n"
        "(def db (db-open \"test.db\"))  ; host installs db-open\n"
        "(db-query db \"SELECT 1\")      ; host installs db-query"]]
      [:p "The host decides which capabilities each runtime gets. "
       "There is no ambient access to system resources. See the "
       [:a {:href "/documentation/embedding/"} "Embedding Guide"]
       " for details."]

      ;; --- Reader syntax ---

      [:h2 "Reader syntax"]
      [:p "Most reader macros work as in Clojure. A few are absent:"]

      [:table
       [:thead [:tr [:th "Clojure"] [:th "mino"] [:th "Status"]]]
       [:tbody
        [:tr [:td [:code "#(inc %)"]]
         [:td [:code "#(inc %)"]]
         [:td "Same"]]
        [:tr [:td [:code "#'var"]]
         [:td "N/A"]
         [:td "No var indirection"]]
        [:tr [:td [:code "#_ form"]]
         [:td [:code "#_ form"]]
         [:td "Same"]]
        [:tr [:td [:code "^{:key val}"]]
         [:td [:code "^{:key val}"]]
         [:td "Same (attached at read time)"]]
        [:tr [:td [:code "^:key"]]
         [:td [:code "^:key"]]
         [:td "Same"]]
        [:tr [:td [:code "^Type"]]
         [:td [:code "^Type"]]
         [:td "Same (becomes " [:code "{:tag Type}"] ")"]]
        [:tr [:td [:code "#\"regex\""]]
         [:td [:code "(re-pattern \"regex\")"]]
         [:td "Use function form"]]
        [:tr [:td [:code "::keyword"]]
         [:td "N/A"]
         [:td "No auto-resolved keywords"]]
        [:tr [:td [:code "'()"]]
         [:td [:code "'()"]]
         [:td "Same"]]
        [:tr [:td [:code "`(~x ~@xs)"]]
         [:td [:code "`(~x ~@xs)"]]
         [:td "Same"]]
        [:tr [:td [:code "@atom"]]
         [:td [:code "@atom"]]
         [:td "Same"]]]]

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
       [:li "No array maps (small maps use HAMT directly)"]]

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
      [:p "mino has 64-bit integers and 64-bit IEEE 754 floats. "
       "There are no ratios, BigIntegers, or BigDecimals. Integer "
       "overflow wraps silently (C semantics)."]

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
       [:li [:strong "Multimethods"] " are not implemented. "
        "Protocols cover the same dispatch pattern."]
       [:li [:strong "Records and deftypes"] " do not exist. "
        "Maps are the universal data carrier. Type hints ("
        [:code "^String x"] ") parse as metadata but are not "
        "enforced at runtime."]
       [:li [:strong "JVM classpath, deps, and jar resolution"]
        " do not apply. mino is embedded via C source files."]
       [:li [:strong "Shared-memory STM"] " is replaced by "
        "runtime isolation and message passing."]
       [:li [:strong "Java interop syntax"] " is replaced by "
        "capability-based C handle APIs."]
       [:li [:strong "Full numeric tower"] " (ratios, BigInt, BigDec) "
        "is excluded to keep the runtime small."]]

      ;; --- Quick reference ---

      [:h2 "Quick reference"]
      [:table
       [:thead [:tr [:th "Clojure"] [:th "mino equivalent"]]]
       [:tbody
        [:tr [:td [:code "(ns ...)"]]
         [:td [:code "(require \"path\")"]]]
        [:tr [:td [:code "(:key map)"]]
         [:td [:code "(:key map)"]]]
        [:tr [:td [:code "#(inc %)"]]
         [:td [:code "#(inc %)"]]]
        [:tr [:td [:code "(dosync ...)"]]
         [:td "Not applicable"]]
        [:tr [:td [:code "(ref ...)"]]
         [:td [:code "(atom ...)"]]]
        [:tr [:td [:code "(.method obj)"]]
         [:td "Host-installed functions"]]
        [:tr [:td [:code "(ex-info ...)"]]
         [:td [:code "(ex-info ...)"]]]
        [:tr [:td [:code "(future ...)"]]
         [:td [:code "(spawn ...)"]]]]]))
)
