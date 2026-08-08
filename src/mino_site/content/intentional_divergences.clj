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
       [:code "(new Type ...)"] " all work, but they dispatch "
       "through a capability registry the embedder controls. "
       "Each method, getter, and constructor is opted in by "
       "the host. No ambient access to system resources, "
       "and no reflection at all. See the "
       [:a {:href "/documentation/embedding/"} "Embedding Guide"]
       " for the full host contract."]

      ;; ----------------------------------------------------------------

      [:h2 {:id "host-threads"} "Host-grant-gated host threads"]
      [:p "Threading is a per-state runtime " [:em "capability"]
       " the host grants, not a build-time feature. Each "
       [:code "mino_state"] " starts at "
       [:code "thread_limit = 1"] " (single-threaded). Embedders "
       "raise the limit via "        [:code "mino_set_option(S, MINO_OPT_THREAD_LIMIT, n)"]
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
       "unbounded number of isolated " [:code "mino_state"]
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

      [:h2 {:id "stm"} "STM uses single-version optimistic locking"]
      [:p [:code "ref"] ", " [:code "dosync"] ", "
       [:code "alter"] ", " [:code "commute"] ", "
       [:code "ensure"] ", " [:code "ref-set"] ", and "
       [:code "io!"] " all work as in Clojure, plus watches and "
       "validators on refs. The Clojure surface matches canon for "
       "any program that does not depend on the items below."]
      [:p [:strong "Underneath, mino is simpler than JVM Clojure."]
       " mino keeps one committed value per ref instead of the "
       "JVM MVCC history ring; "
       [:code "ref-min-history"] ", " [:code "ref-max-history"]
       ", and " [:code "ref-history-count"] " are stubs returning "
       [:code "0"] " / " [:code "10"] " / " [:code "0"]
       ". A single global commit lock serializes commits in place "
       "of per-ref read/write locks, and no barging or "
       "mid-body retry. Long readers under sustained writer "
       "pressure may exhaust the 10000-retry cap rather than serve "
       "an older snapshot from history."]
      [:p [:strong "The trade-off is deliberate."]
       " mino's typical workload is a small ref set and a handful "
       "of worker threads, often single-threaded. The simpler "
       "machinery costs nothing on the single-threaded fast path "
       "and stays comprehensible at a glance. See the "
       [:a {:href "/documentation/stm/"} "STM page"]
       " for the full enumeration of deviations and the C API "
       "mirror."]
      [:p [:strong "Agents dispatch asynchronously through "
                    "per-state worker threads (POOLED + SOLO)."]
       " " [:code "agent"] ", " [:code "send"] ", "
       [:code "send-off"] ", " [:code "await"] ", "
       [:code "await-for"] ", " [:code "agent-error"] ", "
       [:code "restart-agent"] ", and " [:code "shutdown-agents"]
       " all ship. " [:code "send"] " enqueues onto the POOLED "
       "run-queue, " [:code "send-off"] " onto SOLO; each pool has "
       "its own worker thread but the per-state eval lock still "
       "serializes one action at a time across both pools, so "
       "multi-agent dispatch is still serialized within one state. "
       "Each worker counts against "
       [:code "thread_limit"] " (the embedder thread does not), so "
       [:code "send"] " throws "
       [:code "MTH001"] " if the host hasn't granted a thread "
       "budget; embedders that want both pools alive concurrently "
       "must raise the limit to >= 2. " [:code "send-via"]
       " is intentionally deferred (no public Executor type). One "
       "small pool-routing deviation: " [:code "send-off"]
       " inside a " [:code "dosync"] " posts onto POOLED for the "
       "post-commit drain (JVM canon would dispatch via the action's "
       "original pool). This is invisible while both pools run under "
       "the per-state eval lock; the deviation will matter once SOLO "
       "yields the lock for blocking IO. See "
       [:a {:href "/documentation/stm/"} "STM"]
       " for the full surface, including the public C-API perimeter "
       "(" [:code "mino_send"] ", " [:code "mino_send_off"] ", "
       [:code "mino_await"] ", " [:code "mino_await_for"] ", "
       [:code "mino_agent_error"] ", "
       [:code "mino_restart_agent"] ")."]

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

      [:h2 "What is in scope for future versions"]
      [:p "One queued item remains on the roadmap:"]
      [:ul
       [:li [:strong "ABI freeze"] " at v1.0. Until then "
        [:code "src/mino.h"] " is labelled evolving and the "
        "numeric-tower type tags (" [:code "MINO_BIGINT"]
        ", " [:code "MINO_RATIO"] ", " [:code "MINO_BIGDEC"]
        ") sit under the same evolving-API umbrella."]]
      [:p "The remaining items above (no JVM interop, simpler STM "
       "underneath, no proxy / definterface) are stable design "
       "choices, not deferrals."])))
