(ns mino-site.content.intentional-divergences
  "Intentional divergences from Clojure page content."
  (:require
    [hiccup2.core :as h]))

(defn intentional-divergences-page
  "Generates the Intentional divergences page HTML body."
  []
  (str
    (h/html
      [:h1 "Intentional divergences from Clojure"]
      [:p "mino aims to be the Clojure dialect at embedded scale. "
       "Every divergence on this page is a deliberate design "
       "decision, not a missing feature waiting for a contributor. "
       "Each entry names what is different, why, and what mino "
       "offers in its place."]
      [:p "For an item-by-item rundown of which Clojure functions "
       "and macros are supported, differ, or are absent, see the "
       [:a {:href "/documentation/compatibility-matrix/"}
        "compatibility matrix"] "."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "jvm-interop"} "No JVM interop surface"]
      [:p "mino is written in ANSI C, not on the JVM. "
       [:code "Class/forName"] ", " [:code "bean"] ", "
       [:code "gen-class"] ", " [:code ".."] ", "
       [:code "set!"] " on instance fields, host-array literals "
       "(" [:code "int-array"] ", " [:code "to-array"] ", etc.), "
       "Java class type hints, and " [:code "*warn-on-reflection*"]
       " all assume a JVM that mino does not have."]
      [:p "What mino keeps: the surface syntax for calling host "
       "methods. " [:code "(.next obj)"] ", "
       [:code "(.-field obj)"] ", "
       [:code "(Type/static-call ...)"] ", and "
       [:code "(new Type ...)"] " all work - but they dispatch "
       "through a capability registry the embedder controls. "
       "Each method, getter, and constructor is opted in by "
       "the host. There is no ambient access to system resources, "
       "and there is no reflection at all. See the "
       [:a {:href "/documentation/embedding/"} "Embedding Guide"]
       " for the full host contract."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "host-threads"} "No host threads"]
      [:p [:code "future"] ", " [:code "promise"] ", "
       [:code "deliver"] ", " [:code "pmap"] ", "
       [:code "thread"] ", " [:code "agent"] ", "
       [:code "send"] ", and " [:code "send-off"] " all "
       "depend on a thread pool that schedules host OS threads "
       "preemptively. Each mino runtime (" [:code "mino_state_t"]
       ") is single-threaded by contract: one mutator at a time, "
       "no shared mutable state across runtimes, no implicit "
       "thread pool. The embedder runs as many runtimes as it "
       "wants on as many host threads as it wants - but that "
       "shape is decided by the host, not by mino."]
      [:p "What mino offers in their place:"]
      [:ul
       [:li [:strong "core.async"] " channels and " [:code "go"]
        " blocks for cooperative concurrency inside a runtime. "
        [:code "go"] " parking, channel composition, transducer-"
        "carrying channels, " [:code "alts!"] ", "
        [:code "timeout"] ", " [:code "mult"] " / " [:code "tap"]
        ", " [:code "pub"] " / " [:code "sub"] ", and "
        [:code "pipeline"] " all work as expected."]
       [:li "Multiple isolated runtimes, each driven by its own "
        "host thread. The embedder picks the threading model. "
        "Cross-runtime communication uses message passing through "
        "host channels, not shared memory."]]
      [:p [:code "<!!"] " and " [:code ">!!"] " exist but block "
       "the calling " [:em "fiber"] ", not a host thread; they are "
       "compile-time errors only inside a " [:code "go"] " block "
       "(use " [:code "<!"] " / " [:code ">!"] " there). Outside "
       "a " [:code "go"] " they pump the scheduler until the "
       "operation completes."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "stm"} "No STM (refs / dosync)"]
      [:p [:code "ref"] ", " [:code "ref-set"] ", "
       [:code "alter"] ", " [:code "commute"] ", and "
       [:code "dosync"] " coordinate writes across multiple "
       "host threads. mino has one mutator per runtime, so the "
       "problem STM solves does not exist inside a runtime, and "
       "across runtimes the answer is message passing, not shared "
       "memory."]
      [:p [:strong "Use atoms instead."] " "
       [:code "atom"] ", " [:code "swap!"] ", " [:code "reset!"]
       ", and " [:code "compare-and-set!"] " cover the same "
       "uniform-update pattern that single-ref dosync handles in "
       "Clojure code, with simpler semantics."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "chunked-seqs"} "No chunked sequences"]
      [:p "Clojure's sequences pull elements 32 at a time off "
       "vectors and other indexed sources to amortize per-element "
       "overhead. The chunk machinery (" [:code "chunked-seq?"]
       ", " [:code "chunk-first"] ", " [:code "chunk-rest"]
       ", " [:code "chunk-cons"] ", " [:code "chunk-buffer"] ") "
       "is part of the public seq surface."]
      [:p "mino's sequences are element-at-a-time. The chunk "
       "throughput win matters most when a JVM's per-call overhead "
       "is high relative to per-element work - which is a "
       "reasonable tradeoff for JVM Clojure but not load-bearing "
       "for embedded-scale workloads where mino lives. mino's "
       "lazy seqs are GC-traceable, allocate cheaply, and "
       "interoperate cleanly with transducers."]
      [:p [:strong "Use transducers for throughput."] " A "
       [:code "(transduce (comp (map ...) (filter ...)) ...)"]
       " avoids intermediate-seq allocation entirely and beats "
       "chunked-seq throughput in most cases."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "records"} "No defrecord / deftype"]
      [:p "Records and types compile to JVM classes implementing "
       "host interfaces, with field-access protocol bridging and "
       "implicit constructors. None of that translates to mino's "
       "runtime."]
      [:p [:strong "Use maps and protocols."] " mino's "
       [:code "defprotocol"] " + " [:code "extend-type"] " covers "
       "polymorphic dispatch on user data; maps cover record-shaped "
       "data carriers. " [:code "extend-type"] " takes a type tag "
       "(" [:code "::Point"] " keyword by convention, or one of the "
       "built-in keys like " [:code "::map"] " / " [:code "::vector"]
       ") and registers method implementations."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "reify-proxy"} "No reify, proxy, definterface"]
      [:p [:code "reify"] " and " [:code "proxy"] " materialize "
       "anonymous JVM objects implementing host interfaces. "
       [:code "definterface"] " declares a host interface. All "
       "three are JVM shapes."]
      [:p [:strong "Use defprotocol + extend-type."] " For the "
       "reify case where you want a one-off polymorphic value, "
       "mino's idiom is to " [:code "extend-type"] " a fresh "
       "type tag with the methods you need. " [:code "definterface"]
       " in particular throws an informative error pointing at "
       [:code "defprotocol"] "."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "overflow"} "Plain + / - / * throw on overflow"]
      [:p "Clojure's plain " [:code "+"] " on Long values silently "
       "promotes to BigInt. mino's plain " [:code "+"] ", "
       [:code "-"] ", " [:code "*"] ", " [:code "inc"] ", and "
       [:code "dec"] " throw an " [:code ":eval/overflow"]
       " exception with code " [:code "MOV001"] " when the result "
       "exceeds " [:code "Long/MAX_VALUE"] " or "
       [:code "Long/MIN_VALUE"] " - like Clojure's "
       [:code "unchecked-*"] " family but with a diagnostic "
       "instead of silent wraparound."]
      [:p [:strong "Use " [:code "+'"] " / " [:code "-'"] " / "
        [:code "*'"] " / " [:code "inc'"] " / " [:code "dec'"]
        " to auto-promote."] " These behave like Clojure's plain "
       "arithmetic: long-overflow promotes to BigInt, BigInt math "
       "stays in BigInt, and the rest of the numeric tower (Ratio, "
       "BigDec) participates in dispatch."]
      [:p "The throw-by-default rule keeps every-day integer "
       "arithmetic on the fast int64 path; promotion is opt-in "
       "via the prime variants. The numeric tower itself is "
       "complete (BigInt, Ratio, BigDec all real types backed "
       "by vendored MIT-licensed imath)."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "auto-resolved-keywords"} "No auto-resolved keywords"]
      [:p [:code "::name"] " and " [:code "::alias/name"] " are "
       "not supported. The reader does not maintain a per-namespace "
       "alias map for keyword resolution because mino has a single "
       "global definition env per runtime - there is no "
       "\"current namespace\" for the keyword to resolve relative "
       "to."]
      [:p [:strong "Use the fully spelled keyword."] " "
       [:code ":myapp.core/name"] " is unambiguous and works "
       "everywhere a Clojure auto-resolved keyword would. "
       "Namespaced map shorthand (" [:code "{::ns/x 1}"] ") is "
       "not supported either; spell it out."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "list-empty"} "(list) is nil"]
      [:p "Clojure's " [:code "(list)"] " is an empty "
       [:code "PersistentList$EmptyList"] " instance. mino's "
       [:code "(list)"] " is " [:code "nil"] ", and "
       [:code "rest"] " has " [:code "next"] " semantics - it "
       "returns " [:code "nil"] " when exhausted, not an empty "
       "list. " [:code "(seq nil)"] " and " [:code "(seq ())"]
       " are both " [:code "nil"] "."]
      [:p "This matches how the bulk of real-world Clojure code "
       "uses sequences: code that calls " [:code "next"]
       " for nil-punning is unaffected; code that compares an "
       "exhausted seq against " [:code "()"] " explicitly needs "
       "to switch to a nil check. The simpler invariant "
       "(\"empty seq is nil\") removes a class of bugs around "
       "distinguishing empty list from nil sequence."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "multimethod-hierarchy"} "Multimethods use the global hierarchy only"]
      [:p [:code "defmulti"] " accepts a " [:code ":hierarchy"]
       " option in Clojure to dispatch against an explicit "
       "user-supplied hierarchy. mino's " [:code "defmulti"]
       " always dispatches through the global hierarchy."]
      [:p "Hierarchy-as-data still works: " [:code "make-hierarchy"]
       ", 3-arity " [:code "derive"] " / " [:code "underive"]
       ", and " [:code "isa?"] " against an explicit hierarchy "
       "all behave as in Clojure. What does not exist is binding "
       "such a hierarchy to a particular multimethod."]
      [:p "If user code needs scoped dispatch, the workaround is "
       "to keep the hierarchy keys disjoint between subsystems "
       "(prefix with namespace) so the global hierarchy stays "
       "uncontested."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "volatile"} "No volatile!"]
      [:p [:code "volatile!"] ", " [:code "vswap!"] ", and "
       [:code "vreset!"] " exist in Clojure to give transducers "
       "a fast unsynchronized mutable cell that does not pay the "
       "atomic-CAS cost. The win is real on the JVM where "
       [:code "atom"] "'s CAS is not free."]
      [:p "Each mino runtime is single-threaded; there is no "
       "preemption inside a transducer, and " [:code "atom"]
       " is itself a non-CAS swap on a mutable cell. Volatile "
       "would be a synonym for atom with a different name, so it "
       "is not provided. Stateful transducers use plain "
       [:code "atom"] "."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "tagged-literals"} "No user tagged literals"]
      [:p "Clojure's " [:code "*data-readers*"] " and "
       [:code "tagged-literal"] " let user code register a reader "
       "function for " [:code "#tag value"] " literals. mino's "
       "reader does not consult a user table; the only tagged-"
       "literal forms accepted are the built-in numeric ones "
       "(" [:code "1N"] ", " [:code "1M"] ", " [:code "1/2"]
       ") and reader-conditional " [:code "#?"] " / "
       [:code "#?@"] "."]
      [:p "This is a soft divergence - the same end (custom "
       "data carriers from text) is reachable via "
       [:code "read-string"] " plus a normalizing " [:code "fn"]
       ". A future cycle may add a small "
       [:code "*data-readers*"] "-style hook."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "namespace-registry"} "No first-class namespace registry"]
      [:p [:code "in-ns"] ", " [:code "create-ns"] ", "
       [:code "all-ns"] ", " [:code "ns-publics"] ", "
       [:code "ns-interns"] ", " [:code "ns-resolve"] ", and the "
       [:code "*ns*"] " dynamic var are not provided. "
       [:code "ns"] " forms parse, " [:code "require"]
       " loads code, and " [:code ":as"] " / " [:code ":refer"]
       " resolve symbols at read time - but there is no first-"
       "class namespace object you can introspect at runtime."]
      [:p "Each runtime has a single global definition env. "
       "Isolation between subsystems happens at the runtime "
       "boundary (one " [:code "mino_state_t"] " per concern), "
       "not at the namespace boundary. This is the same trade "
       "Lua makes - and it keeps the embedding contract simple."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "regex-reader-literal"} "No #\"...\" regex literal"]
      [:p "The reader does not provide a " [:code "#\"...\""]
       " shorthand for regular expressions. Use "
       [:code "(re-pattern \"...\")"] " - the engine itself "
       "(" [:code "re-find"] ", " [:code "re-matches"] ", "
       [:code "re-seq"] ") behaves the same as Clojure for the "
       "POSIX feature set mino's regex compiler covers."]

      ;; ----------------------------------------------------------------

      [:h2 "What is in scope for future cycles"]
      [:p "Several deferrals on this page are queued - not "
       "rejected - for later cycles:"]
      [:ul
       [:li [:strong "Host-thread extension"] " - an opt-in "
        "OS-thread runtime that surfaces " [:code "future"]
        ", " [:code "promise"] ", " [:code "thread"] ", and "
        [:code "<!!"] " backed by real thread pools. Coordinated "
        "with the embedding contract, not bolted on."]
       [:li [:strong "ABI freeze"] " at v1.0. Until then "
        [:code "src/mino.h"] " is labelled evolving and the "
        "numeric-tower type tags (" [:code "MINO_BIGINT"]
        ", " [:code "MINO_RATIO"] ", " [:code "MINO_BIGDEC"]
        ") sit under the same evolving-API umbrella."]
       [:li [:strong "Tagged-literal hook"] " - a small "
        [:code "*data-readers*"] "-style table for user-extensible "
        "reader tags."]]
      [:p "The remaining items above (no JVM interop, no STM, no "
       "chunked seqs, no records / reify / proxy, "
       [:code "(list)"] " is " [:code "nil"] ", auto-resolved "
       "keywords, plain " [:code "+"] " throws on overflow) are "
       "stable design choices, not deferrals."])))
