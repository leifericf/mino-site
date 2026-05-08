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

      [:h2 {:id "host-threads"} "Host-grant-gated host threads"]
      [:p "Threading is a per-state runtime " [:em "capability"]
       " the host grants, not a build-time feature. Each "
       [:code "mino_state_t"] " starts at "
       [:code "thread_limit = 1"] " (single-threaded). Embedders "
       "raise the limit via " [:code "mino_set_thread_limit(S, n)"]
       "; while the limit is " [:code "<= 1"]
       ", " [:code "future"] ", " [:code "promise"] ", "
       [:code "deliver"] ", " [:code "thread"] ", and the blocking "
       [:code "<!!"] " / " [:code ">!!"] " / " [:code "alts!!"]
       " ops throw " [:code ":mino/unsupported"] " with a message "
       "naming the policy."]
      [:p "Standalone " [:code "./mino"] " grants "
       [:code "cpu_count"] " right after " [:code "mino_install_all"]
       ", so REPL/script users see the canon surface working out of "
       "the box. Embedders that want sandboxed scripts withhold the "
       "grant; embedders that want canon parity make the same call "
       "the standalone binary does."]
      [:p [:strong "Status."] " The full surface ships: real OS-thread "
       [:code "future"] " / " [:code "promise"] " / "
       [:code "thread"] " backed by " [:code "pthread_create"]
       " (CreateThread on Windows); " [:code "deref"]
       " parks via " [:code "pthread_cond_wait"] "; "
       [:code "future-cancel"] ", " [:code "future-done?"]
       ", " [:code "future-cancelled?"] ", "
       [:code "realized?"] ", " [:code "future?"] " round it out; "
       "blocking " [:code "<!!"] " / " [:code ">!!"] " / "
       [:code "alts!!"] " park across OS threads. "
       "The " [:code "(mino-thread-limit)"] " primitive exposes "
       "the current limit so library code can branch on it. "
       "ASan + UBSan + TSan-clean across the test suite."]
      [:p [:strong "Embed-distinctive value-add."] " "
       [:code "mino_set_thread_pool"] " lets the host hand mino an "
       "existing pool (Tokio runtime, libuv worker pool, ASIO io_context, "
       "custom pthread pool); workers from that pool service "
       [:code "future"] " spawns. The work item carries the state pointer, "
       "not the thread, so the same N-worker pool can service an "
       "unbounded number of isolated " [:code "mino_state_t"]
       " runtimes - multi-tenant by construction. "
       [:code "mino_set_thread_factory"] " hooks per-worker naming, "
       "affinity, priority for the spawn-per-future path; "
       [:code "mino_set_thread_stack_size"] " tunes RSS for tight "
       "embedders. JVM Clojure cannot offer this because the JVM "
       "forces one global heap; mino's per-state isolation makes "
       "it natural. See "
       [:code "examples/embed_multi_tenant_threads.c"]
       " for a worked end-to-end demo."]
      [:p [:strong "Cooperative concurrency without threading."] " "
       [:code "core.async"] " channels and " [:code "go"]
       " blocks remain the inside-one-runtime story. " [:code "go"]
       " parking, channel composition, transducer-carrying channels, "
       [:code "alts!"] ", " [:code "timeout"] ", "
       [:code "mult"] " / " [:code "tap"] ", "
       [:code "pub"] " / " [:code "sub"] ", and "
       [:code "pipeline"] " all work without threads. Inside a "
       [:code "go"] " block " [:code "<!"] " / " [:code ">!"]
       " park the fiber; outside, the blocking variants pump the "
       "scheduler. The grant gates only the OS-thread shape, not "
       "the cooperative shape."]

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

      [:h2 {:id "reify-proxy"} "No proxy, definterface"]
      [:p [:code "defrecord"] ", " [:code "deftype"] ", "
       [:code "reify"] ", and " [:code "instance?"] " all ship as "
       "real value types. See the "
       [:a {:href "/documentation/coming-from-clojure/"}
        "Coming from Clojure"]
       " page for the canonical surface and the embed-distinctive "
       "C-side construction API."]
      [:p [:code "proxy"] " materializes an anonymous JVM object "
       "implementing host interfaces; " [:code "definterface"] " "
       "declares one. Both are JVM shapes that don't translate to "
       "mino's runtime."]
      [:p [:strong "Use defprotocol + extend-type."]
       " For one-off polymorphic values, mino has real "
       [:code "reify"] ". For static interface declaration, "
       [:code "defprotocol"] " is the analogue. "
       [:code "definterface"] " throws an informative error pointing "
       "at " [:code "defprotocol"] "."]

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

      [:h2 {:id "auto-promote"} "Auto-promoting math operators"]
      [:p "Plain " [:code "+"] " / " [:code "-"] " / " [:code "*"]
       " / " [:code "inc"] " / " [:code "dec"] " auto-promote to "
       [:code "MINO_BIGINT"] " on long overflow. JVM Clojure raises "
       [:code "ArithmeticException"] " for the unprimed forms and "
       "reserves the prime variants (" [:code "+'"] " / "
       [:code "-'"] " / etc.) for the auto-promoting behaviour; "
       "mino keeps only one form and chooses the safe one. "
       [:code "(* Long/MIN_VALUE -1)"] " on the JVM throws; on mino "
       "it returns " [:code "9223372036854775808N"] "."]
      [:p [:strong "Why."] " mino's stated goal is correctness over "
       "throughput. Long overflow is a frequent source of subtle "
       "production bugs in JVM Clojure and forces every numeric "
       "library to choose between throwing and primed-everywhere. "
       "Auto-promotion makes the obvious form mathematically right; "
       "the embedded use case rarely needs the wraparound shape, and "
       "when it does the " [:code "unchecked-*"] " family "
       "(" [:code "unchecked-+"] ", " [:code "unchecked--"] ", "
       [:code "unchecked-*"] ", " [:code "unchecked-inc"] ", "
       [:code "unchecked-dec"] ", " [:code "unchecked-divide-int"]
       ") is the explicit opt-in. Same surface as Clojure, sharper "
       "default."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "float-double"} "One float tier (double)"]
      [:p "mino has a single 64-bit IEEE 754 float tier ("
       [:code "MINO_FLOAT"] "). " [:code "(float 0.1)"] " and "
       [:code "(double 0.1)"] " return the same value, "
       [:code "(= (float 0.5) (double 0.5))"] " is "
       [:code "true"] ", and " [:code "float?"] " / "
       [:code "double?"] " are aliases. JVM Clojure exposes both "
       [:code "java.lang.Float"] " and " [:code "java.lang.Double"]
       " as distinct types; cross-type equality is "
       [:code "false"] " there even when the values are numerically "
       "equal."]
      [:p [:strong "Why."] " A 32-bit float tier exists in JVM "
       "Clojure mostly because Java's primitive set forces it; the "
       "values exist on the heap as boxed " [:code "java.lang.Float"]
       " objects, and cross-tier comparison is a frequent source of "
       "bugs (" [:code "(= 0.1 (float 0.1))"] " is "
       [:code "false"] " on the JVM). mino picks one float tier and "
       "uses it consistently. The C-level embedding API exposes "
       [:code "double"] " for both reading and writing, so there's "
       "no representational gap to span."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "regex-strings"} "Regex patterns are strings"]
      [:p "mino's regex literal " [:code "#\"...\""]
       " parses to a " [:code "MINO_STRING"] " whose contents are "
       "the pattern source. " [:code "re-find"] ", "
       [:code "re-matches"] ", " [:code "re-seq"] ", "
       [:code "re-pattern"] ", and the " [:code "clojure.string"]
       " regex consumers compile from that string at the call site. "
       "There is no separate " [:code "java.util.regex.Pattern"]
       "-equivalent value type."]
      [:p [:strong "Consequences."] " Two regex values produced from "
       "the same source string compare equal under "
       [:code "="] " (they're the same string). On the JVM, "
       [:code "(= #\"x\" #\"x\")"] " is " [:code "false"] " because "
       "Pattern instances rely on identity. The mino behaviour is "
       "convenient (regex patterns can be used as map keys, "
       "deduplicated in sets) but does diverge from Clojure's "
       "Pattern semantics."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "fn-arity"} "Permissive function arity"]
      [:p "Calling a fixed-arity function with too few or too many "
       "arguments does not throw in mino. Missing positional "
       "parameters bind to " [:code "nil"] ", and trailing "
       "arguments are silently ignored: "
       [:code "((fn [x] x) 1 2 3)"] " returns " [:code "1"]
       ", and " [:code "((fn [x y] [x y]))"] " returns "
       [:code "[nil nil]"] ". JVM Clojure raises "
       [:code "clojure.lang.ArityException"] " in both cases."]
      [:p [:strong "Why."] " The embedded use case favours "
       "robustness over strictness; permissive arity makes "
       "host-supplied callbacks easier to slot in without exact "
       "shape negotiation. Variadic " [:code "& rest"] " parameters "
       "are still respected, and a future strict mode is on the "
       "long-term roadmap."]

      ;; ----------------------------------------------------------------

      [:h2 "What is in scope for future versions"]
      [:p "One queued item remains on the roadmap:"]
      [:ul
       [:li [:strong "ABI freeze"] " at v1.0. Until then "
        [:code "src/mino.h"] " is labelled evolving and the "
        "numeric-tower type tags (" [:code "MINO_BIGINT"]
        ", " [:code "MINO_RATIO"] ", " [:code "MINO_BIGDEC"]
        ") sit under the same evolving-API umbrella."]]
      [:p "Items that shipped recently: regex literal escapes; the "
       [:code "*out*"] " / " [:code "*err*"] " / " [:code "*in*"]
       " print pipeline; REPL specials and "
       [:code "clojure.repl"] " / " [:code "clojure.stacktrace"]
       "; " [:code "clojure.core.protocols"] " and "
       [:code "clojure.datafy"] "; auto-promoting "
       [:code "+"] " / " [:code "-"] " / " [:code "*"] " / "
       [:code "inc"] " / " [:code "dec"] " plus the "
       [:code "unchecked-*"] " family; real "
       [:code "defrecord"] " / " [:code "deftype"] " / "
       [:code "reify"] " / " [:code "instance?"] "; "
       "bundled stdlib + per-group install hooks; "
       [:code "clojure.template"] " + " [:code "clojure.instant"]
       "; " [:code "*data-readers*"] " reader hook; "
       [:code "clojure.spec.alpha"] " + "
       [:code "clojure.core.specs.alpha"] "; the "
       "host-thread capability and metadata surface; real OS-thread "
       [:code "future"] " / " [:code "promise"] " / "
       [:code "thread"] " backed by " [:code "pthread_create"]
       "; blocking " [:code "<!!"] " / " [:code ">!!"] " / "
       [:code "alts!!"] " parking across threads; "
       "the embed-distinctive thread pool, factory, and "
       "stack-size surface; real "
       [:code "MINO_VOLATILE"] " backing "
       [:code "volatile!"] " / " [:code "vswap!"] " / "
       [:code "vreset!"] " for stateful transducers; "
       [:code "iteration"] " (Clojure 1.11); the "
       [:code "clojure.core.async"] " namespace wrap with "
       [:code "merge"] " and " [:code "into"]
       " under their canon names; the chunked-seq family ("
       [:code "MINO_CHUNKED_CONS"] " value type, "
       [:code "chunked-seq?"] ", " [:code "chunk-first"] ", "
       [:code "chunk-rest"] ", " [:code "chunk-next"] ", "
       [:code "chunk-cons"] ", " [:code "chunk-buffer"] ", "
       [:code "chunk-append"] ", " [:code "chunk"] ") with "
       [:code "map"] "/" [:code "filter"] "/" [:code "take"]
       "/" [:code "keep"] "/" [:code "keep-indexed"] "/"
       [:code "map-indexed"] " propagating chunkedness end-to-end "
       "and source-side auto-chunking on " [:code "(seq vec)"]
       " and lazy " [:code "range"] "; cross-type "
       [:code "compare"] " over the canon order "
       "(" [:code "nil"] " < " [:code "false"] " < "
       [:code "true"] " < numbers < strings < symbols < keywords"
       "); and a minimal "
       [:code "clojure.test.check"] " port (generators, properties, "
       [:code "quick-check"] "; shrinking deferred) backing "
       [:code "s/gen"] " and " [:code "s/exercise"] "."]
      [:p "The remaining items above (no JVM interop, no STM, no "
       "proxy / definterface) are stable design choices, not "
       "deferrals."])))
