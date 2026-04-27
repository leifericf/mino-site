(ns mino-site.content.compatibility-matrix
  "Clojure / mino compatibility matrix page content."
  (:require
    [hiccup2.core :as h]))

(defn compatibility-matrix-page
  "Generates the Clojure compatibility matrix HTML body."
  []
  (str
    (h/html
      [:h1 "Clojure compatibility matrix"]
      [:p "What Clojure code runs on mino, what runs with small "
       "differences, and what is intentionally absent. The bar for "
       "this page is " [:em "the Clojure dialect at embedded scale"]
       ": code that does not reach for JVM interop, chunked-seq "
       "throughput, or host-thread primitives runs on mino unchanged. "
       "Companion pages: "
       [:a {:href "/documentation/intentional-divergences/"}
        "intentional divergences"]
       " spells out why mino diverges where it does, and "
       [:a {:href "/documentation/coming-from-clojure/"}
        "Coming from Clojure"]
       " gives a higher-level tour."]

      [:p "Coverage tracks the test suites under "
       [:code "tests/clj_*_test.clj"] " and "
       [:code "tests/compat_test.clj"] ". If a function or macro "
       "is listed as supported, the test suite exercises it."]

      [:h2 "Reading the table"]
      [:ul
       [:li [:strong "Supported"] " - the function or macro behaves "
        "the way Clojure does for the inputs mino accepts."]
       [:li [:strong "Differs"] " - the name exists and is callable, "
        "but the behavior diverges deliberately. The note column "
        "explains the divergence and links to "
        [:a {:href "/documentation/intentional-divergences/"}
         "intentional divergences"] " where appropriate."]
       [:li [:strong "Absent"] " - the name is not provided. "
        "Calling it raises a resolution error. The note explains "
        "the replacement (atoms instead of refs, protocols instead "
        "of " [:code "reify"] ", etc.) or links the divergence."]]

      ;; ----------------------------------------------------------------
      ;; Core language

      [:h2 "Core language"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "def"] " / " [:code "defn"] " / "
              [:code "defn-"] " / " [:code "defmacro"]]
         [:td "Supported"] [:td "Including " [:code "^:private"]
          " metadata and attribute maps."]]
        [:tr [:td [:code "fn"] " / " [:code "let"] " / "
              [:code "letfn"]]
         [:td "Supported"] [:td "Multi-arity, named, variadic; "
          "vector binding forms with destructuring."]]
        [:tr [:td [:code "loop"] " / " [:code "recur"]]
         [:td "Supported"] [:td "Tail position only inside "
          [:code "loop"] " and " [:code "fn"] "."]]
        [:tr [:td [:code "if"] " / " [:code "when"] " / "
              [:code "if-not"] " / " [:code "when-not"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "if-let"] " / " [:code "when-let"] " / "
              [:code "if-some"] " / " [:code "when-some"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "cond"] " / " [:code "condp"] " / "
              [:code "case"]]
         [:td "Supported"] [:td [:code "case"] " covers literal "
          "matching, multi-match lists, and a default clause."]]
        [:tr [:td [:code "do"] " / " [:code "comment"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "and"] " / " [:code "or"] " / "
              [:code "not"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "quote"] " / " [:code "var"] " / "
              [:code "#'sym"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Reader conditionals "
              [:code "#?"] " / " [:code "#?@"]]
         [:td "Supported"]
         [:td "Active dialect keys are " [:code ":mino"] " and "
          [:code ":clj"] " (so " [:code ":clj"] "/"
          [:code ":cljs"] "-only libraries read on the "
          [:code ":clj"] " branch); "
          [:code ":default"] " is honored. "
          [:code "read-string"] " accepts an opts map "
          "with " [:code ":read-cond :preserve"] " to keep the "
          "form unevaluated."]]
        [:tr [:td [:code "::keyword"] " / "
              [:code "::alias/keyword"]]
         [:td "Supported"]
         [:td "Auto-resolved keywords resolve at read time using "
          "the current namespace and alias table."]]
        [:tr [:td "Namespaced map literals "
              [:code "#:foo{:b 1}"] " / " [:code "#::{:b 1}"]
              " / " [:code "#::alias{...}"]]
         [:td "Supported"]
         [:td "Bare keys are qualified with the named, current, or "
          "aliased namespace; " [:code ":_/x"] " strips the prefix "
          "leaving a bare key. Duplicate keys after qualification "
          "error at read time."]]]]

      ;; ----------------------------------------------------------------
      ;; Destructuring & bindings

      [:h2 "Destructuring"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td "Vector positional destructuring"]
         [:td "Supported"] [:td "Including " [:code "& rest"]
          " and " [:code ":as"] "."]]
        [:tr [:td "Map " [:code ":keys"] " / " [:code ":strs"] " / "
              [:code ":syms"] " / " [:code ":or"] " / "
              [:code ":as"]]
         [:td "Supported"] [:td "Including nested destructuring."]]
        [:tr [:td "Namespaced map destructuring "
              [:code "{:keys [::ns/x]}"]]
         [:td "Supported"]
         [:td "Auto-resolved keywords resolve at read time, so "
          "namespaced " [:code ":keys"] " entries work."]]
        [:tr [:td [:code "destructure"] " function"]
         [:td "Supported"]
         [:td "Returns the flat " [:code "[name init ...]"]
          " vector for a binding pairs vector, ready to feed to "
          [:code "let"] "."]]]]

      ;; ----------------------------------------------------------------
      ;; Collections

      [:h2 "Collections"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td "Persistent " [:code "vector"] " / "
              [:code "hash-map"] " / " [:code "hash-set"] " / "
              [:code "list"]]
         [:td "Supported"] [:td "Bagwell tries for vectors, HAMT "
          "for maps and sets, structural sharing throughout."]]
        [:tr [:td [:code "sorted-map"] " / " [:code "sorted-set"]
              " / " [:code "sorted-map-by"] " / "
              [:code "sorted-set-by"]]
         [:td "Supported"] [:td "Persistent left-leaning red-black "
          "tree; custom comparator via the " [:code "-by"]
          " variants."]]
        [:tr [:td [:code "subseq"] " / " [:code "rsubseq"]]
         [:td "Supported"] [:td "Bounded in-order walks against the "
          "rbtree."]]
        [:tr [:td [:code "array-map"]]
         [:td "Differs"]
         [:td "Alias for " [:code "hash-map"] "; HAMT is used at "
          "every size."]]
        [:tr [:td [:code "conj"] " / " [:code "disj"] " / "
              [:code "assoc"] " / " [:code "dissoc"] " / "
              [:code "get"] " / " [:code "nth"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "into"] " / " [:code "merge"] " / "
              [:code "merge-with"] " / " [:code "zipmap"]]
         [:td "Supported"] [:td [:code "into"] " accepts a "
          "transducer xform."]]
        [:tr [:td [:code "update"] " / " [:code "update-in"] " / "
              [:code "assoc-in"] " / " [:code "get-in"] " / "
              [:code "select-keys"] " / " [:code "find"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "keys"] " / " [:code "vals"] " / "
              [:code "contains?"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "update-keys"] " / " [:code "update-vals"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "peek"] " / " [:code "pop"] " / "
              [:code "empty"] " / " [:code "rseq"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Transients: " [:code "transient"] " / "
              [:code "persistent!"] " / " [:code "assoc!"] " / "
              [:code "conj!"] " / " [:code "dissoc!"] " / "
              [:code "disj!"] " / " [:code "pop!"]]
         [:td "Supported"] [:td "Vector / map / set transients. "
          "Sorted-map and sorted-set transients are not provided."]]
        [:tr [:td [:code "defrecord"] " / " [:code "deftype"]]
         [:td "Absent"]
         [:td "Maps cover the use case. See "
          [:a {:href "/documentation/intentional-divergences/#records"}
           "records and types"] "."]]
        [:tr [:td "Chunked-seq APIs ("
              [:code "chunked-seq?"] ", " [:code "chunk-first"]
              ", " [:code "chunk-rest"] ")"]
         [:td "Absent"]
         [:td "mino sequences are element-at-a-time. See "
          [:a {:href "/documentation/intentional-divergences/#chunked-seqs"}
           "no chunked sequences"] "."]]]]

      ;; ----------------------------------------------------------------
      ;; Sequences

      [:h2 "Sequences"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "lazy-seq"] " / " [:code "cons"] " / "
              [:code "seq"] " / " [:code "first"] " / "
              [:code "next"] " / " [:code "rest"]]
         [:td "Supported"]
         [:td [:code "rest"] " has " [:code "next"] " semantics - "
          "it returns " [:code "nil"] " when exhausted."]]
        [:tr [:td [:code "(list)"]]
         [:td "Differs"]
         [:td "Returns " [:code "nil"] ", not an empty list. "
          [:code "(seq nil)"] " is " [:code "nil"] "; "
          [:code "(seq ())"] " is " [:code "nil"] "."]]
        [:tr [:td [:code "map"] " / " [:code "filter"] " / "
              [:code "remove"] " / " [:code "take"] " / "
              [:code "drop"] " / " [:code "take-while"] " / "
              [:code "drop-while"]]
         [:td "Supported"] [:td "Multi-collection " [:code "map"]
          " works."]]
        [:tr [:td [:code "range"] " / " [:code "iterate"] " / "
              [:code "repeat"] " / " [:code "cycle"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "concat"] " / " [:code "interleave"] " / "
              [:code "interpose"] " / " [:code "partition"] " / "
              [:code "partition-all"] " / " [:code "partition-by"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "reduce"] " / " [:code "reduce-kv"] " / "
              [:code "reductions"] " / " [:code "reduced"] " / "
              [:code "reduced?"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Transducers: " [:code "transduce"] " / "
              [:code "into"] " w/ xform / " [:code "sequence"]
              " / " [:code "eduction"] " / " [:code "completing"]
              " / " [:code "cat"] " / " [:code "halt-when"]
              " / " [:code "ensure-reduced"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "sort"] " / " [:code "sort-by"] " / "
              [:code "frequencies"] " / " [:code "group-by"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "some"] " / " [:code "every?"] " / "
              [:code "not-any?"] " / " [:code "not-every?"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "for"] " / " [:code "doseq"]]
         [:td "Supported"] [:td "Multi-binding with "
          [:code ":when"] ", " [:code ":while"] ", "
          [:code ":let"] "."]]
        [:tr [:td [:code "doall"] " / " [:code "dorun"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "min-key"] " / " [:code "max-key"] " / "
              [:code "random-sample"] " / " [:code "bounded-count"]
              " / " [:code "distinct?"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "clojure.core.reducers"]]
         [:td "Absent"]
         [:td "Fork-join reducers depend on host threads. See "
          [:a {:href "/documentation/intentional-divergences/#host-threads"}
           "no host threads"] "."]]]]

      ;; ----------------------------------------------------------------
      ;; Higher-order

      [:h2 "Higher-order"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "apply"] " / " [:code "partial"] " / "
              [:code "comp"] " / " [:code "complement"]]
         [:td "Supported"] [:td "Variadic " [:code "comp"] "."]]
        [:tr [:td [:code "juxt"] " / " [:code "every-pred"] " / "
              [:code "some-fn"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Threading: " [:code "->"] " / " [:code "->>"]
              " / " [:code "as->"] " / " [:code "cond->"] " / "
              [:code "cond->>"] " / " [:code "some->"] " / "
              [:code "some->>"] " / " [:code "doto"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Callable collections "
              [:code "({:a 1} :a)"] " and keywords "
              [:code "(:k m)"]]
         [:td "Supported"] [:td "Including in higher-order "
          "contexts: " [:code "(map :name coll)"] ", "
          [:code "(filter #{:a} coll)"] "."]]]]

      ;; ----------------------------------------------------------------
      ;; Numbers

      [:h2 "Numbers"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td "64-bit integer (" [:code "Long"]
              "), 64-bit float (" [:code "Double"] ")"]
         [:td "Supported"] [:td]]
        [:tr [:td "BigInt (" [:code "1N"] " literal, "
              [:code "(bigint x)"] ")"]
         [:td "Supported"]
         [:td "Backed by vendored MIT-licensed imath. "
          [:code "MINO_BIGINT"] " is a distinct type tag."]]
        [:tr [:td "Ratio (" [:code "1/2"] " literal, "
              [:code "(numerator r)"] ", "
              [:code "(denominator r)"] ", "
              [:code "(rationalize x)"] ")"]
         [:td "Supported"]
         [:td "Always gcd-reduced; an exact integer ratio reduces "
          "to its " [:code "MINO_INT"] " or "
          [:code "MINO_BIGINT"] " value."]]
        [:tr [:td "BigDec (" [:code "1.5M"] " literal, "
              [:code "(bigdec x)"] ", "
              [:code "(with-precision n)"] ")"]
         [:td "Supported"]
         [:td "Stored as " [:code "{unscaled scale}"] " over "
          [:code "MINO_BIGINT"] "."]]
        [:tr [:td "Auto-promoting " [:code "+'"] " / " [:code "-'"]
              " / " [:code "*'"] " / " [:code "inc'"] " / "
              [:code "dec'"]]
         [:td "Supported"]
         [:td "Long-overflow promotes to bigint; bigint arithmetic "
          "stays in bigint."]]
        [:tr [:td "Plain " [:code "+"] " / " [:code "-"] " / "
              [:code "*"] " / " [:code "inc"] " / " [:code "dec"]]
         [:td "Differs"]
         [:td "Throw on long overflow with code "
          [:code "MOV001"] ". Mino does not silently wrap. Use "
          [:code "+'"] " / " [:code "-'"] " / " [:code "*'"]
          " to promote. See "
          [:a {:href "/documentation/intentional-divergences/#overflow"}
           "overflow throws"] "."]]
        [:tr [:td [:code "rational?"] " / " [:code "ratio?"] " / "
              [:code "decimal?"] " / " [:code "integer?"] " / "
              [:code "number?"]]
         [:td "Supported"]
         [:td "Predicates point at the real numeric-tower types."]]
        [:tr [:td [:code "bit-and"] " / " [:code "bit-or"] " / "
              [:code "bit-xor"] " / " [:code "bit-not"] " / "
              [:code "bit-shift-left"] " / "
              [:code "bit-shift-right"]]
         [:td "Supported"] [:td "Long-only bit operations."]]
        [:tr [:td [:code "Math/abs"] " / " [:code "Math/sqrt"]
              " / " [:code "Math/sin"] " etc."]
         [:td "Differs"]
         [:td "mino exposes math via plain " [:code "abs"] ", "
          [:code "sqrt"] ", " [:code "sin"] ", etc., not the "
          [:code "Math/"] " static-call shape."]]]]

      ;; ----------------------------------------------------------------
      ;; Characters & strings

      [:h2 "Characters and strings"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td "Character literals "
              [:code "\\A"] " / " [:code "\\space"] " / "
              [:code "\\uNNNN"] " / " [:code "\\o77"]]
         [:td "Supported"]
         [:td "Distinct character type. "
          [:code "(char? \\A)"] " is true; "
          [:code "(string? \\A)"] " is false."]]
        [:tr [:td [:code "char?"] " / " [:code "char"] " / "
              [:code "int"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "clojure.string"] ": "
              [:code "lower-case"] " / " [:code "upper-case"]
              " / " [:code "capitalize"] " / "
              [:code "reverse"] " / " [:code "blank?"]
              " / " [:code "starts-with?"] " / "
              [:code "ends-with?"] " / " [:code "includes?"]
              " / " [:code "escape"] " / " [:code "replace"]
              " / " [:code "trim"] " / " [:code "triml"]
              " / " [:code "trimr"] " / " [:code "trim-newline"]
              " / " [:code "split-lines"] " / " [:code "join"]]
         [:td "Supported"] [:td]]
        [:tr [:td "Regex: " [:code "re-find"] " / "
              [:code "re-matches"] " / " [:code "re-seq"]
              " / " [:code "re-pattern"] " / "
              [:code "re-matcher"] " / " [:code "re-groups"]]
         [:td "Supported"]
         [:td [:code "re-find"] " and " [:code "re-matches"]
          " return " [:code "[whole g1 g2 ...]"] " for grouped "
          "patterns and the matched substring otherwise. "
          [:code "re-matcher"] " returns a stateful iterator that "
          [:code "re-find"] " advances; " [:code "re-groups"]
          " reads the last match. Reader literal "
          [:code "#\"...\""] " is read but routes patterns through "
          "the same string-escape path, so " [:code "\\d"] " loses "
          "its backslash before the engine sees it; pass patterns "
          "as strings (" [:code "\"\\\\d+\""] ") until a regex-aware "
          "reader escape mode lands."]]]]

      ;; ----------------------------------------------------------------
      ;; Concurrency

      [:h2 "Concurrency"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "atom"] " / " [:code "swap!"] " / "
              [:code "reset!"] " / " [:code "compare-and-set!"]
              " / " [:code "deref"] " / " [:code "@"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "core.async"] " - "
              [:code "chan"] " / " [:code "go"] " / "
              [:code "go-loop"] " / " [:code "<!"] " / "
              [:code ">!"] " / " [:code "<!!"] " / "
              [:code ">!!"] " / " [:code "alts!"] " / "
              [:code "alts!!"] " / " [:code "timeout"] " / "
              [:code "pipe"] " / " [:code "merge"] " / "
              [:code "mult"] " / " [:code "tap"] " / "
              [:code "pub"] " / " [:code "sub"] " / "
              [:code "mix"] " / " [:code "pipeline"] " / "
              [:code "pipeline-async"]]
         [:td "Supported"]
         [:td "Single-threaded cooperative scheduling. Channels "
          "carry transducers and exception handlers. See "
          [:a {:href "/documentation/intentional-divergences/#host-threads"}
           "no host threads"] " for what changes from the JVM "
          "implementation."]]
        [:tr [:td [:code "alt!"] " macro"]
         [:td "Absent"]
         [:td "Use " [:code "alts!"] " (the function form)."]]
        [:tr [:td [:code "future"] " / " [:code "promise"] " / "
              [:code "deliver"] " / " [:code "pmap"] " / "
              [:code "thread"]]
         [:td "Absent"]
         [:td "All require host OS threads. See "
          [:a {:href "/documentation/intentional-divergences/#host-threads"}
           "no host threads"] "."]]
        [:tr [:td [:code "ref"] " / " [:code "ref-set"] " / "
              [:code "alter"] " / " [:code "commute"] " / "
              [:code "dosync"]]
         [:td "Absent"]
         [:td "STM is not provided. Atoms cover mino's concurrency "
          "model. See "
          [:a {:href "/documentation/intentional-divergences/#stm"}
           "no STM"] "."]]
        [:tr [:td [:code "agent"] " / " [:code "send"] " / "
              [:code "send-off"] " / " [:code "await"]]
         [:td "Absent"]
         [:td "Same reason - agents are host-thread backed."]]
        [:tr [:td [:code "locking"] " / " [:code "monitor-enter"]
              " / " [:code "monitor-exit"]]
         [:td "Absent"]
         [:td "Each runtime is single-threaded; there is nothing "
          "to lock against from inside mino."]]
        [:tr [:td [:code "volatile!"] " / " [:code "vswap!"]
              " / " [:code "vreset!"]]
         [:td "Absent"]
         [:td "Without preemptive threads, atoms are equivalent. "
          "See "
          [:a {:href "/documentation/intentional-divergences/#volatile"}
           "no volatile!"] "."]]]]

      ;; ----------------------------------------------------------------
      ;; Polymorphism

      [:h2 "Multimethods, hierarchies, protocols"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "defmulti"] " / " [:code "defmethod"]
              " / " [:code "remove-method"] " / "
              [:code "remove-all-methods"] " / "
              [:code "methods"] " / " [:code "get-method"]]
         [:td "Supported"]
         [:td "Dispatch cache invalidates on hierarchy mutation."]]
        [:tr [:td [:code "prefer-method"] " / " [:code "prefers"]]
         [:td "Supported"]
         [:td "Transitive prefer through hierarchy parents matches "
          "Clojure semantics."]]
        [:tr [:td [:code "defmulti"] " " [:code ":hierarchy"]
              " option"]
         [:td "Differs"]
         [:td "mino dispatches against the global hierarchy only. "
          "See "
          [:a {:href "/documentation/intentional-divergences/#multimethod-hierarchy"}
           "global hierarchy only"] "."]]
        [:tr [:td [:code "make-hierarchy"] " / " [:code "derive"]
              " / " [:code "underive"] " / " [:code "parents"]
              " / " [:code "ancestors"] " / "
              [:code "descendants"] " / " [:code "isa?"]]
         [:td "Supported"]
         [:td "Both 2-arity (global) and 3-arity (explicit "
          "hierarchy) forms."]]
        [:tr [:td [:code "defprotocol"] " / " [:code "extend-type"]
              " / " [:code "extend-protocol"] " / "
              [:code "satisfies?"] " / " [:code "extend"]
              " / " [:code "extends?"]]
         [:td "Supported"]
         [:td "Per-method dispatch atoms, late-bound."]]
        [:tr [:td [:code "definterface"]]
         [:td "Absent"]
         [:td "Throws an informative error. Use "
          [:code "defprotocol"] "."]]
        [:tr [:td [:code "reify"] " / " [:code "proxy"]]
         [:td "Absent"]
         [:td "JVM-interop shapes. Use " [:code "defprotocol"]
          " + " [:code "extend-type"] ". See "
          [:a {:href "/documentation/intentional-divergences/#reify-proxy"}
           "no reify / proxy"] "."]]
        [:tr [:td [:code ":extend-via-metadata"] " on protocols"]
         [:td "Differs"]
         [:td "Not honored. Method-table extension only."]]]]

      ;; ----------------------------------------------------------------
      ;; Errors

      [:h2 "Errors"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "try"] " / " [:code "catch"] " / "
              [:code "finally"] " / " [:code "throw"]]
         [:td "Supported"]
         [:td [:code "throw"] " accepts any value; "
          [:code "catch"] " always receives a structured "
          "diagnostic map."]]
        [:tr [:td [:code "ex-info"] " / " [:code "ex-data"]
              " / " [:code "ex-message"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "with-open"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "assert"]]
         [:td "Supported"] [:td]]]]

      ;; ----------------------------------------------------------------
      ;; Metadata & vars

      [:h2 "Metadata and vars"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "meta"] " / " [:code "with-meta"]
              " / " [:code "vary-meta"] " / "
              [:code "alter-meta!"] " / " [:code "reset-meta!"]]
         [:td "Supported"]
         [:td "Including " [:code "^{:k v}"] ", " [:code "^:k"]
          ", " [:code "^Type"] " reader syntax."]]
        [:tr [:td [:code "var?"] " / " [:code "var-get"]
              " / " [:code "var-set"] " / "
              [:code "alter-var-root"] " / "
              [:code "with-redefs"] " / " [:code "with-redefs-fn"]
              " / " [:code "with-local-vars"] " / "
              [:code "intern"] " / " [:code "find-var"] " / "
              [:code "bound?"]]
         [:td "Supported"]
         [:td "Vars are first-class: " [:code "(def x 1)"]
          " returns the var, " [:code "(meta #'foo)"] " returns "
          [:code "{:ns ... :name ... :private ... :dynamic ...}"]
          ", and " [:code "(def x)"] " creates an unbound var that "
          [:code "bound?"] " reports as " [:code "false"] "."]]
        [:tr [:td [:code "bound-fn"] " / " [:code "bound-fn*"]
              " / " [:code "get-thread-bindings"] " / "
              [:code "with-bindings*"]]
         [:td "Supported"]
         [:td "Capture and replay dynamic bindings around a thunk."]]]]

      ;; ----------------------------------------------------------------
      ;; I/O & system

      [:h2 "I/O and system"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "print"] " / " [:code "println"]
              " / " [:code "pr"] " / " [:code "prn"]
              " / " [:code "newline"] " / " [:code "pr-str"]
              " / " [:code "println-str"]]
         [:td "Supported"]
         [:td "Routed through the " [:code "print-method"]
          " multimethod; user types extend printing with "
          [:code "(defmethod print-method MyType ...)"] "."]]
        [:tr [:td [:code "slurp"] " / " [:code "spit"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "read"] " / " [:code "read-string"]
              " / " [:code "clojure.edn/read"] " / "
              [:code "clojure.edn/read-string"]]
         [:td "Supported"]
         [:td "Both accept an opts map with " [:code ":read-cond"]
          " (" [:code ":allow"] " default, " [:code ":preserve"]
          ", " [:code ":disallow"] "). " [:code "clojure.edn/*"]
          " forces " [:code ":preserve"] " so untrusted text never "
          "auto-evaluates a reader conditional. Stream-shaped "
          [:code "java.io.PushbackReader"] " arguments are not "
          "applicable; mino accepts strings only."]]
        [:tr [:td [:code "tagged-literal"] " / "
              [:code "tagged-literal?"] " / "
              [:code "reader-conditional"] " / "
              [:code "reader-conditional?"]]
         [:td "Supported"]
         [:td "Constructors and predicates for the reader-record "
          "shapes. " [:code "*data-readers*"] " for user-defined "
          "tag dispatch is not yet provided."]]]]

      ;; ----------------------------------------------------------------
      ;; Namespaces & host

      [:h2 "Namespaces and host"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "ns"] " / " [:code "require"]
              " / " [:code ":as"] " / " [:code ":as-alias"]
              " / " [:code ":refer"] " / " [:code ":use"]
              " / " [:code ":refer-clojure"]]
         [:td "Supported"]
         [:td "Each namespace owns a root binding table; "
          "unqualified lookup walks lexical, then current-ns, then "
          [:code "clojure.core"] " parent. Modifiers: "
          [:code ":only"] ", " [:code ":exclude"] ", "
          [:code ":rename"] ", " [:code ":refer :all"] " (skips "
          "privates). Prefix lists work in " [:code ":require"]
          ". A loaded file's first " [:code "(ns ...)"] " must "
          "declare the requested module name."]]
        [:tr [:td [:code "in-ns"] " / " [:code "find-ns"]
              " / " [:code "the-ns"] " / " [:code "create-ns"]
              " / " [:code "remove-ns"] " / " [:code "ns-name"]
              " / " [:code "ns-publics"] " / " [:code "ns-interns"]
              " / " [:code "ns-refers"] " / " [:code "ns-aliases"]
              " / " [:code "ns-map"] " / " [:code "ns-unmap"]
              " / " [:code "ns-unalias"] " / " [:code "alias"]
              " / " [:code "all-ns"] " / " [:code "loaded-libs"]
              " / " [:code "ns-resolve"] " / "
              [:code "requiring-resolve"] " / " [:code "*ns*"]]
         [:td "Supported"]
         [:td "Full first-class namespace registry. "
          [:code "*ns*"] " is interned as a dynamic var that "
          "tracks user-visible namespace switches. Namespaces "
          "carry metadata (docstring, attribute map). Privacy is "
          "enforced on cross-namespace qualified access."]]
        [:tr [:td "Java interop "
              [:code "(.method obj)"] " / "
              [:code "(.-field obj)"] " / "
              [:code "(Type/static)"] " / "
              [:code "(new Type ...)"]]
         [:td "Differs"]
         [:td "Same syntax, but dispatched through the host "
          "capability registry. The host opts in to each method "
          "and getter. See the "
          [:a {:href "/documentation/embedding/"}
           "Embedding Guide"] "."]]
        [:tr [:td [:code "Class/forName"] " / " [:code "bean"]
              " / " [:code "proxy"] " / " [:code "gen-class"]
              " / " [:code ".."] " / " [:code "set!"]]
         [:td "Absent"]
         [:td "JVM reflection / interop surface. See "
          [:a {:href "/documentation/intentional-divergences/#jvm-interop"}
           "no JVM interop"] "."]]
        [:tr [:td [:code "*warn-on-reflection*"]]
         [:td "Absent"]
         [:td "There is no reflection in mino."]]]]

      [:p {:style "margin-top:2.5rem;font-size:0.9em;color:#666"}
       "Items marked " [:em "supported"] " round-trip through "
       "the test suite; an entry's existence here is a claim that "
       [:code "tests/clj_*_test.clj"] " or "
       [:code "tests/compat_test.clj"] " exercises it. If you "
       "find a divergence not documented above, file an issue at "
       [:a {:href "https://github.com/leifericf/mino/issues"}
        "github.com/leifericf/mino/issues"] "."])))
