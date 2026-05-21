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
       ": code that does not reach for JVM interop or host-thread "
       "primitives runs on mino unchanged. "
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
        [:tr [:td [:code "defrecord"] " / " [:code "deftype"] " / "
              [:code "->RecordName"] " / " [:code "map->RecordName"]]
         [:td "Supported"]
         [:td "Real value types with field-slot storage, protocol "
          "dispatch, and " [:code "instance?"] ". "
          [:code "(= record map-with-same-content)"] " is "
          [:code "false"] " by design."]]
        [:tr [:td [:code "byte-array"] " / " [:code "bytes?"] " / "
              [:code "bitstring?"] " / "
              [:code "aget"] " on a bytes value / "
              [:code "alength"] " / "
              [:code "#bytes \"...\""] " reader literal"]
         [:td "Differs"]
         [:td [:code "byte-array"] " returns an immutable "
          [:code "MINO_BYTES"] " value, not a host-mutable Java "
          [:code "byte[]"] ". "
          [:code "aset"] " on it throws "
          [:code ":mino/state"]
          " (the persistent-value model excludes in-place writes). "
          "Other Java primitive-array constructors "
          "(" [:code "int-array"] ", " [:code "long-array"] ", "
          "etc.) stay as " [:code "MINO_HOST_ARRAY"] " for backward "
          "compat. See "
          [:a {:href "/documentation/bytes/"}
           "Bytes and Bit Syntax"] "."]]
        [:tr [:td "Bit syntax: " [:code "bits"] " / "
              [:code "bits-get"] " / " [:code "subbits"] " / "
              [:code "let-bits"]]
         [:td "Extension"]
         [:td "mino-only surface borrowed from Erlang's bit syntax. "
          "Builds and reads bit fields at arbitrary offsets / sizes / "
          "endians / types. JVM Clojure has no equivalent."]]
        [:tr [:td "Chunked-seq APIs ("
              [:code "chunked-seq?"] ", " [:code "chunk-first"]
              ", " [:code "chunk-rest"] ", " [:code "chunk-next"]
              ", " [:code "chunk-cons"] ", " [:code "chunk-buffer"]
              ", " [:code "chunk-append"] ", " [:code "chunk"] ")"]
         [:td "Supported"]
         [:td "Real " [:code "MINO_CHUNKED_CONS"] " value type with "
          "the canon API. " [:code "map"] ", " [:code "filter"] ", "
          [:code "take"] ", " [:code "keep"] ", " [:code "keep-indexed"]
          ", and " [:code "map-indexed"] " propagate chunkedness "
          "end-to-end. Sources auto-chunk since v0.98.3: "
          [:code "(seq vec)"] " emits 32-element chunks reusing the "
          "vector's leaves, and lazy " [:code "range"] " produces a "
          "fresh chunk on each force."]]]]

      ;; ----------------------------------------------------------------
      ;; Sequences

      [:h2 "Sequences"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "lazy-seq"] " / " [:code "cons"] " / "
              [:code "seq"] " / " [:code "first"] " / "
              [:code "next"] " / " [:code "rest"]]
         [:td "Supported"] [:td]]
        [:tr [:td [:code "(list)"] " / " [:code "'()"]]
         [:td "Supported"]
         [:td "Returns the canonical empty list. "
          [:code "(= '() nil)"] " is " [:code "false"] "; "
          [:code "(= '() [])"] " is " [:code "true"] "."]]
        [:tr [:td [:code "map"] " / " [:code "filter"] " / "
              [:code "remove"] " / " [:code "take"] " / "
              [:code "drop"] " / " [:code "take-while"] " / "
              [:code "drop-while"]]
         [:td "Supported"] [:td "Multi-collection " [:code "map"]
          " works."]]
        [:tr [:td [:code "range"] " / " [:code "iterate"] " / "
              [:code "repeat"] " / " [:code "cycle"] " / "
              [:code "iteration"]]
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
         [:td "Fork-join reducers are not provided. Transducers "
          "cover the throughput shape; "
          [:code "future"] " plus partitioning covers ad-hoc "
          "parallelism when threading is granted."]]]]

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
          [:code "MINO_BIGINT"] ". "
          [:code "with-precision"] " accepts every JVM rounding "
          "mode: " [:code ":down"] ", " [:code ":up"] ", "
          [:code ":floor"] ", " [:code ":ceiling"] ", "
          [:code ":half-up"] ", " [:code ":half-down"] ", "
          [:code ":half-even"] " (banker's), "
          [:code ":unnecessary"] " (throws when rounding would "
          "occur)."]]
        [:tr [:td "Plain " [:code "+"] " / " [:code "-"] " / "
              [:code "*"] " / " [:code "inc"] " / " [:code "dec"]]
         [:td "Differs"]
         [:td "Auto-promotes to bigint on long overflow rather than "
          "raising " [:code "ArithmeticException"] " as JVM Clojure "
          "does for the unprimed forms. Equivalent to Clojure's "
          [:code "+'"] " / " [:code "-'"] " / " [:code "*'"]
          " / " [:code "inc'"] " / " [:code "dec'"] ". Use the "
          [:code "unchecked-*"] " family for wraparound. "
          "Documented under "
          [:a {:href "/documentation/intentional-divergences/#auto-promote"}
           "auto-promoting math"] "."]]
        [:tr [:td [:code "unchecked-+"] " / " [:code "unchecked--"]
              " / " [:code "unchecked-*"] " / "
              [:code "unchecked-inc"] " / " [:code "unchecked-dec"]
              " / " [:code "unchecked-divide-int"]]
         [:td "Supported"]
         [:td "Fast int64 path with wraparound semantics, matching "
          "Clojure's " [:code "unchecked-*"] " family."]]
        [:tr [:td [:code "rational?"] " / " [:code "ratio?"] " / "
              [:code "decimal?"] " / " [:code "integer?"] " / "
              [:code "bigint?"] " / " [:code "number?"]]
         [:td "Supported"]
         [:td "Predicates point at the real numeric-tower types. "
          [:code "rational?"] " is true for int / bigint / ratio / "
          "bigdec; " [:code "integer?"] " is true for int and "
          "bigint; " [:code "bigint?"] " specifically tests the "
          "bigint tier. The narrow " [:code "pos-int?"] " / "
          [:code "neg-int?"] " / " [:code "nat-int?"] " predicates "
          "match only the long tier (per Clojure)."]]
        [:tr [:td [:code "mod"] " / " [:code "rem"] " / "
              [:code "quot"]]
         [:td "Supported"]
         [:td "Preserve operand tier: bigint operands produce "
          "bigint results, ratio operands produce a bigint quot "
          "and a ratio rem / mod (collapsing to bigint when integer-"
          "valued), bigdec operands produce bigdec results at the "
          "aligned scale."]]
        [:tr [:td [:code "<"] " / " [:code "<="] " / " [:code ">"]
              " / " [:code ">="]]
         [:td "Supported"]
         [:td "Operands must be numeric ("
          [:code "(< nil 1)"] " throws); any "
          [:code "##NaN"] " operand short-circuits the chain to "
          [:code "false"] "."]]
        [:tr [:td [:code "bit-and"] " / " [:code "bit-or"] " / "
              [:code "bit-xor"] " / " [:code "bit-not"] " / "
              [:code "bit-shift-left"] " / "
              [:code "bit-shift-right"]]
         [:td "Supported"] [:td "Long-only bit operations."]]
        [:tr [:td [:code "Math/abs"] " / " [:code "Math/sqrt"]
              " / " [:code "Math/sin"] " etc."]
         [:td "Absent"]
         [:td "JVM static-call shape. Of the Java Math surface, only "
          [:code "abs"] " ships as a plain function in "
          [:code "clojure.core"] " today. Other transcendental "
          "functions (" [:code "sqrt"] ", " [:code "sin"] ", etc.) "
          "are not provided; the embedder can register them by "
          "wrapping " [:code "<math.h>"] " through the host capability "
          "registry."]]]]

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
              " / " [:code "replace-first"] " / "
              [:code "split"] " / " [:code "split-lines"] " / "
              [:code "join"] " / " [:code "trim"] " / "
              [:code "triml"] " / " [:code "trimr"] " / "
              [:code "trim-newline"] " / " [:code "index-of"] " / "
              [:code "last-index-of"] " / "
              [:code "re-quote-replacement"]]
         [:td "Supported"]
         [:td [:code "split"] " accepts an optional 3rd "
          [:code "limit"] " argument matching canon "
          [:code "String.split(re, limit)"] ": positive caps the "
          "result, zero or negative keeps trailing empties."]]
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
          [:code "#\"...\""] " bypasses string-escape processing "
          "so " [:code "\\d"] " reaches the engine intact."]]]]

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
        [:tr [:td [:code "clojure.core.async"] " - "
              [:code "chan"] " / " [:code "go"] " / "
              [:code "go-loop"] " / " [:code "<!"] " / "
              [:code ">!"] " / " [:code "<!!"] " / "
              [:code ">!!"] " / " [:code "alts!"] " / "
              [:code "alts!!"] " / " [:code "timeout"] " / "
              [:code "pipe"] " / " [:code "merge"] " / "
              [:code "into"] " / " [:code "reduce"] " / "
              [:code "transduce"] " / " [:code "split"] " / "
              [:code "partition-by"] " / "
              [:code "mult"] " / " [:code "tap"] " / "
              [:code "pub"] " / " [:code "sub"] " / "
              [:code "mix"] " / " [:code "pipeline"] " / "
              [:code "pipeline-async"]]
         [:td "Supported"]
         [:td "Surface lives in the " [:code "clojure.core.async"]
          " namespace; " [:code "merge"] ", " [:code "into"] ", "
          [:code "reduce"] ", " [:code "transduce"] ", and "
          [:code "partition-by"] " shadow the "
          [:code "clojure.core"] " forms inside that ns. "
          "Cooperative scheduling for "
          [:code "go"] " blocks; "
          [:code "<!!"] " / " [:code ">!!"] " / "
          [:code "alts!!"] " park across OS threads when the host "
          "grants threading. Channels carry transducers and "
          "exception handlers. See "
          [:a {:href "/documentation/intentional-divergences/#host-threads"}
           "host-grant-gated host threads"] " for the embed contract."]]
        [:tr [:td [:code "alt!"] " macro"]
         [:td "Absent"]
         [:td "Use " [:code "alts!"] " (the function form)."]]
        [:tr [:td [:code "future"] " / " [:code "promise"] " / "
              [:code "deliver"] " / " [:code "thread"] " / "
              [:code "future-cancel"] " / " [:code "future-done?"]
              " / " [:code "future-cancelled?"] " / "
              [:code "realized?"] " / " [:code "future?"]]
         [:td "Same when host grants threads"]
         [:td "Real OS-thread futures and promises backed by "
          [:code "pthread_create"] " (CreateThread on Windows). "
          "Embedded states default to "
          [:code "thread_limit = 1"] " and throw "
          [:code ":mino/unsupported"] " until the host calls "
          [:code "mino_set_thread_limit"] "; standalone "
          [:code "./mino"] " grants " [:code "cpu_count"] " by "
          "default, so the REPL surface matches canonical Clojure "
          "out of the box. See "
          [:a {:href "/documentation/intentional-divergences/#host-threads"}
           "host-grant-gated host threads"] " for the embed contract "
          "and the multi-tenant pool surface."]]
        [:tr [:td [:code "pmap"]]
         [:td "Absent"]
         [:td "Not provided. When threading is granted, the same "
          "shape is reachable as a small composition of "
          [:code "future"] " and " [:code "deref"]
          " over a partitioned input."]]
        [:tr [:td [:code "ref"] " / " [:code "ref?"]]
         [:td "Supported"]
         [:td "STM ref construction and identity predicate. See "
          [:a {:href "/documentation/stm/"} "STM"] " for the design "
          "choices and deviations."]]
        [:tr [:td [:code "dosync"] " / " [:code "dosync*"]]
         [:td "Supported"]
         [:td "Transaction body. " [:code "dosync"] " is a macro "
          "expanding to " [:code "(dosync* (fn [] ...))"] "."]]
        [:tr [:td [:code "alter"] " / " [:code "commute"] " / "
              [:code "ref-set"] " / " [:code "ensure"]]
         [:td "Supported"]
         [:td "In-tx writers. " [:code "alter"] " participates in "
          "read-set validation; " [:code "commute"] " does not and "
          "is replayed at commit. " [:code "ref-set"] " / "
          [:code "alter"] " after " [:code "commute"]
          " on the same ref throws " [:code "\"Can't set after commute\""]
          " (JVM canon)."]]
        [:tr [:td [:code "io!"]]
         [:td "Supported"]
         [:td "Throws " [:code ":mino/state"] " MST003 when called "
          "inside a transaction; outside, runs body unchanged."]]
        [:tr [:td [:code "in-transaction?"]]
         [:td "Supported"]
         [:td "Returns " [:code "true"] " inside a "
          [:code "dosync"] " body, " [:code "false"] " otherwise."]]
        [:tr [:td [:code "add-watch"] " / " [:code "remove-watch"]
              " on refs"]
         [:td "Supported"]
         [:td "Watches fire after commit. Same surface as on atoms "
          "and vars."]]
        [:tr [:td [:code "add-watch"] " / " [:code "remove-watch"]
              " on vars"]
         [:td "Supported"]
         [:td "Fire on " [:code "alter-var-root"] " (and " [:code "def"]
          " with rebind), not on " [:code "binding"]
          " thread-local pushes. Same callback signature as on "
          "atoms / refs."]]
        [:tr [:td [:code "set-validator!"] " / "
              [:code "get-validator"] " on vars"]
         [:td "Supported"]
         [:td "Validator runs before " [:code "alter-var-root"]
          " publishes the new root; throw rejects without "
          "mutating the var."]]
        [:tr [:td [:code "set-validator!"] " / "
              [:code "get-validator"] " on refs"]
         [:td "Supported"]
         [:td "Validators run during commit before any write becomes "
          "visible. Same surface as on atoms."]]
        [:tr [:td [:code "ref-min-history"] " / "
              [:code "ref-max-history"] " / "
              [:code "ref-history-count"]]
         [:td "Stub"]
         [:td "Returns " [:code "0"] " / " [:code "10"] " / "
          [:code "0"] " unconditionally. mino uses single-version "
          "optimistic locking and has no MVCC history to introspect. "
          "See " [:a {:href "/documentation/stm/"} "STM"] "."]]
        [:tr [:td [:code "agent"] " / " [:code "agent?"] " / "
              [:code "send"] " / " [:code "send-off"] " / "
              [:code "await"] " / " [:code "await-for"] " / "
              [:code "agent-error"] " / " [:code "restart-agent"] " / "
              [:code "set-error-handler!"] " / "
              [:code "error-handler"] " / "
              [:code "set-error-mode!"] " / " [:code "error-mode"]]
         [:td "Supported"]
         [:td [:code "send"] " enqueues onto the agent's POOLED "
          "run-queue, " [:code "send-off"] " onto SOLO; both "
          "return the agent immediately, and each pool's worker "
          "drains its queue under " [:code "state_lock"] ". "
          [:code "await"] " / " [:code "await-for"] " block until "
          "every named agent's in-flight count reaches zero. Each "
          "worker counts against " [:code "thread_limit"]
          " (the embedder thread does not; default 1 in embedded "
          "use, bumped to "
          [:code "cpu_count"] " in standalone " [:code "./mino"]
          "); " [:code "send"] " throws " [:code "MTH001"]
          " if the host hasn't granted a thread budget. Action "
          "throws and watch throws are both captured into "
          [:code "agent-error"] ". Public C-API mirrors: "
          [:code "mino_send"] ", " [:code "mino_send_off"] ", "
          [:code "mino_await"] ", " [:code "mino_await_for"] ", "
          [:code "mino_agent_error"] ", "
          [:code "mino_restart_agent"] ". See "
          [:a {:href "/documentation/stm/"} "STM"]
          " for the deviations."]]
        [:tr [:td [:code "send-via"]]
         [:td "Absent"]
         [:td "Intentionally deferred -- no public Executor type "
          "is exposed yet. " [:code "send"] " and " [:code "send-off"]
          " each have their own per-state worker (POOLED and SOLO)."]]
        [:tr [:td [:code "shutdown-agents"]]
         [:td "Supported"]
         [:td "Quiesces both per-state workers (drains the queues, "
          "joins the pthreads) and seals the agent surface so "
          "subsequent " [:code "send"] " / " [:code "send-off"]
          " throw " [:code "MST008"] ". Idempotent. Throws "
          [:code "MST002"] " if called from inside an action body."]]
        [:tr [:td [:code "release-pending-sends"]]
         [:td "Supported"]
         [:td "Inside a " [:code "dosync"] ", returns the count of "
          "queued sends and clears them so they don't fire on "
          "commit. Outside a transaction returns 0."]]
        [:tr [:td [:code "add-watch"] " / " [:code "remove-watch"]
              " on agents"]
         [:td "Supported"]
         [:td "Same callback signature as on atoms / refs / vars. "
          "Watch throws are captured into " [:code "agent-error"]
          " (one watch's throw does not silence later watches on "
          "the same publish)."]]
        [:tr [:td [:code "locking"] " / " [:code "monitor-enter"]
              " / " [:code "monitor-exit"]]
         [:td "Absent"]
         [:td "Each runtime serializes mutator threads under a "
          "per-state lock; user code does not see preemption "
          "inside one runtime, so there is nothing to lock against "
          "from inside mino."]]
        [:tr [:td [:code "volatile!"] " / " [:code "vswap!"]
              " / " [:code "vreset!"] " / " [:code "volatile?"]]
         [:td "Supported"]
         [:td "Real " [:code "MINO_VOLATILE"] " value type with a "
          "single mutable slot. Stateful transducers ("
          [:code "take"] ", " [:code "drop"] ", " [:code "drop-while"]
          ", " [:code "take-nth"] ", " [:code "interpose"] ", "
          [:code "distinct"] ", " [:code "partition-by"] ", "
          [:code "partition-all"] ", " [:code "map-indexed"] ", "
          [:code "dedupe"] ") use volatiles for their per-step state."]]]]

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

      ;; ----------------------------------------------------------------
      ;; Testing

      [:h2 "Testing"]
      [:table
       [:thead [:tr [:th "Form"] [:th "Status"] [:th "Note"]]]
       [:tbody
        [:tr [:td [:code "clojure.test/deftest"] " / "
              [:code "is"] " / " [:code "are"] " / "
              [:code "testing"] " / " [:code "run-tests"]]
         [:td "Supported"]
         [:td "Standard test framework. " [:code "(is (= 1 1))"]
          ", " [:code "(is (thrown? ...))"] ", and "
          [:code "(testing \"description\" ...)"] " all work."]]
        [:tr [:td [:code "use-fixtures"] " / " [:code "compose-fixtures"]
              " / " [:code "join-fixtures"]]
         [:td "Supported"]
         [:td "Both " [:code ":once"] " and " [:code ":each"]
          " kinds. The " [:code "use-fixtures"] " form is a macro "
          "so the calling namespace is captured at expansion time -- "
          "mino's " [:code "*ns*"] " is the function's defining "
          "namespace, not a dynamic var."]]
        [:tr [:td [:code "clojure.test.check"] " - "
              [:code "quick-check"] " / generators / properties"]
         [:td "Supported"]
         [:td "Minimal port of the test.check API. Generators, "
          "properties, and " [:code "quick-check"] " ship; "
          "shrinking is deferred. Backs the "
          [:code "s/gen"] " and " [:code "s/exercise"]
          " hooks under " [:code "clojure.spec.alpha"] "."]]
        [:tr [:td [:code "special-symbol?"]]
         [:td "Supported"]
         [:td "Returns " [:code "true"] " for the Clojure-reserved "
          "special form names ("
          [:code "fn"] ", " [:code "fn*"] ", "
          [:code "let"] ", " [:code "let*"] ", "
          [:code "loop"] ", " [:code "loop*"] ", "
          [:code "if"] ", " [:code "do"] ", " [:code "quote"]
          ", " [:code "def"] ", " [:code "var"] ", "
          [:code "set!"] ", " [:code "throw"] ", " [:code "try"]
          ", " [:code "catch"] ", " [:code "finally"] ", "
          [:code "recur"] ", " [:code "new"] ", " [:code "."]
          ", " [:code "&"] ", " [:code "case*"] ", "
          [:code "deftype*"] ", " [:code "letfn*"]
          ", and the mino-only " [:code "ns"] ", "
          [:code "binding"] ", " [:code "lazy-seq"]
          ", " [:code "refer-clojure"] "). Unimplemented "
          "JVM-only specials are reserved as a portability "
          "courtesy."]]]]

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
          "shapes. User-defined tag dispatch via "
          [:code "*data-readers*"] " is honored; the "
          [:code "*default-data-reader-fn*"] " fallback applies "
          "for unknown tags."]]
        [:tr [:td [:code "#inst \"...\""] " literal / "
              [:code "inst?"] " / " [:code "inst-ms"] " / "
              [:code "clojure.instant"]]
         [:td "Supported"]
         [:td "The " [:code "#inst"]
          " reader literal parses into a component map carrying "
          [:code "{:mino/instant true}"] " metadata. "
          [:code "inst?"] " detects the marker; "
          [:code "inst-ms"] " decodes epoch millis from the
          component map. mino has no JVM "
          [:code "Date"] " / " [:code "Timestamp"]
          " class -- the map IS the representation."]]
        [:tr [:td [:code "#uuid \"...\""] " literal / "
              [:code "uuid?"] " / " [:code "random-uuid"] " / "
              [:code "parse-uuid"]]
         [:td "Supported"]
         [:td "Real " [:code "MINO_UUID"]
          " value type. " [:code "(str u)"]
          " emits the canonical 36-char form so it round-trips
          through " [:code "java.util.UUID/fromString"]
          " on JVM."]]
        [:tr [:td [:code "*clojure-version*"] " / "
              [:code "(clojure-version)"]]
         [:td "Differs"]
         [:td "Returns a four-key map and a "
          [:code "\"M.N.P\""]
          " string respectively. The version numbers reflect mino,
          not JVM Clojure -- the Clojure-shape is for code that
          reads the map's structure; the literal version string
          differs intentionally."]]]]

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
