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

      [:p "mino is a Clojure dialect. If you know Clojure, you "
       "can read and write mino immediately. This page documents "
       "where mino differs and why."]

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
        [:code "as->"] ", " [:code "cond->"] ", " [:code "cond->>"] ")"]
       [:li [:code "try"] "/" [:code "catch"] "/" [:code "throw"]]
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
        [:code "ex-message"]]]

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
         [:td "N/A"]
         [:td "No value metadata yet"]]
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
       [:li "Vectors, maps, sets, and lists are persistent and immutable"]
       [:li [:code "conj"] ", " [:code "assoc"] ", " [:code "dissoc"]
        ", " [:code "get"] ", " [:code "nth"] ", " [:code "into"]
        " work as expected"]
       [:li "Sets support " [:code "contains?"] ", " [:code "conj"]
        ", " [:code "disj"]]]
      [:p "Differences:"]
      [:ul
       [:li "No transient collections (all mutation is through atoms)"]
       [:li "No metadata on values (planned)"]
       [:li "No sorted maps or sorted sets"]
       [:li "Keywords as functions (" [:code "(:k m)"] ") work for "
        "map lookup. Maps and sets are not callable."]]

      ;; --- Sequences ---

      [:h2 "Sequences"]
      [:p "Lazy sequences work the same way:"]
      [:pre [:code
        "(take 5 (map inc (range)))  ;=> (1 2 3 4 5)"]]
      [:p "Differences:"]
      [:ul
       [:li "No chunked sequences"]
       [:li "No transducers (planned)"]
       [:li [:code "map"] " currently takes one collection. "
        "Multi-collection " [:code "map"] " is planned."]]

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
       [:code "finally"] " is not yet supported."]

      ;; --- What is intentionally absent ---

      [:h2 "Intentionally absent"]
      [:p "These are design decisions, not missing features:"]
      [:ul
       [:li [:strong "Protocols and multimethods"] " are planned "
        "but not yet implemented."]
       [:li [:strong "Type hints and records"] " do not exist. "
        "Maps are the universal data carrier."]
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
