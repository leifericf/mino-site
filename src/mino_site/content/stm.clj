(ns mino-site.content.stm
  "Software Transactional Memory reference page content."
  (:require
    [hiccup2.core :as h]))

(defn stm-page
  "Generates the STM page HTML body."
  []
  (str
    (h/html
      [:h1 "Software Transactional Memory"]
      [:p "mino has refs, " [:code "dosync"] ", " [:code "alter"] ", "
       [:code "commute"] ", " [:code "ensure"] ", " [:code "ref-set"]
       ", " [:code "io!"] ", and watches plus validators on refs. The "
       "Clojure-level surface matches canon for any program that does "
       "not depend on the items below. Underneath, mino uses single-"
       "version optimistic locking with a global commit lock, not "
       "MVCC with per-ref read/write locks. The trade-off is "
       "deliberate: mino's typical workload is a small ref set and a "
       "handful of worker threads, often single-threaded. The simpler "
       "machinery costs nothing on the single-threaded fast path and "
       "stays comprehensible at a glance."]

      ;; --- What works the same ---

      [:h2 "What works the same"]
      [:p "If you know JVM Clojure, the surface is identical for "
       "everyday code:"]
      [:ul
       [:li [:code "(ref initial-value)"] " constructs a ref."]
       [:li [:code "(deref r)"] " or " [:code "@r"] " reads it."]
       [:li [:code "(ref-set r val)"] ", " [:code "(alter r f & args)"]
        ", " [:code "(commute r f & args)"]
        " mutate inside a " [:code "dosync"] "."]
       [:li [:code "(ensure r)"] " pins a ref against concurrent "
        "writes for the duration of the transaction."]
       [:li [:code "(io! body...)"] " throws if executed inside a "
        "transaction; outside, it just runs the body."]
       [:li [:code "(in-transaction?)"] " returns true within a "
        [:code "dosync"] " body."]
       [:li [:code "add-watch"] ", " [:code "remove-watch"] ", "
        [:code "set-validator!"] ", and " [:code "get-validator"]
        " all work on refs the same way they work on atoms. "
        "Watches fire after commit; validators run during commit "
        "before any write becomes visible."]
       [:li [:code "(ref? x)"] " is the identity predicate."]]
      [:pre [:code
        "(let [counter (ref 0)\n"
        "      total   (ref 0)]\n"
        "  (dotimes [_ 5]\n"
        "    (dosync\n"
        "      (alter counter inc)\n"
        "      (alter total + @counter)))\n"
        "  [@counter @total])\n"
        ";=> [5 15]"]]

      ;; --- How mino's STM differs underneath ---

      [:h2 "How mino's STM differs underneath"]
      [:p "These are documented deviations from JVM Clojure's "
       [:code "clojure.lang.LockingTransaction"] ". They are listed "
       "in one place at the top of " [:code "src/prim/stm.c"]
       " in the mino source tree."]

      [:h3 "Single-version optimistic locking"]
      [:p "mino keeps one committed value per ref, not an MVCC "
       "history ring. Long-running readers competing with sustained "
       "writer pressure may exhaust the retry cap (10000) where JVM "
       "would serve an older snapshot from history. "
       [:code "ref-min-history"] ", " [:code "ref-max-history"]
       ", and " [:code "ref-history-count"] " exist as stubs "
       "returning " [:code "0"] " / " [:code "10"] " / "
       [:code "0"] " for source compatibility, but there is no "
       "history to introspect."]

      [:h3 "Global commit lock"]
      [:p "mino serializes all commits behind one mutex "
       "(" [:code "S->stm_commit_lock"]
       "). Coarser than JVM's per-ref read/write locks but simpler, "
       "and on a single thread the lock is skipped entirely. Reads "
       "outside a transaction are an atomic load; reads inside a "
       "transaction touch only per-thread state."]

      [:h3 "No barging"]
      [:p "JVM's older-tx-bumps-younger-tx mechanism is intentionally "
       "absent. Every retry restarts the body from scratch, so the "
       "retry cap is the only bound."]

      [:h3 "No mid-body retry"]
      [:p "JVM detects conflicts as soon as a read observes a stale "
       "version. mino only checks the read set at commit time. "
       "Wasted work is bounded by the retry cap."]

      [:h3 "Print form"]
      [:p [:code "(pr-str r)"] " produces "
       [:code "#ref[ID VAL]"] " where "
       [:code "ID"] " is a monotonic per-state counter. JVM prints "
       [:code "#object[clojure.lang.Ref 0x... {:status :ready, :val ...}]"]
       ", a JVM-specific shape. mino's form is deliberately simpler "
       "and not pretending to be a JVM class."]

      ;; --- Embedded use ---

      [:h2 "Embedded use"]
      [:p "STM is opt-in via "
       [:code "mino_install_stm(S, env)"]
       ". The standalone "
       [:code "./mino"] " binary calls "
       [:code "mino_install_all"] ", which installs it; embedders "
       "calling only " [:code "mino_new"]
       " stay opt-out. Anything a Clojure programmer can do, a C "
       "host can do via the mirroring "
       [:code "mino_tx_*"] " API:"]
      [:ul
       [:li [:code "mino_tx_ref(S, val)"] ", "
        [:code "mino_is_tx_ref(v)"]]
       [:li [:code "mino_tx_ref_deref(S, r)"] ", "
        [:code "mino_tx_ref_set(S, r, val)"]]
       [:li [:code "mino_tx_alter_c(S, r, fn, user, env)"] ", "
        [:code "mino_tx_commute_c(S, r, fn, user, env)"]]
       [:li [:code "mino_tx_ensure(S, r, env)"]]
       [:li [:code "mino_tx_run(S, body, user, env)"]
        " -- the host-level "
        [:code "dosync"] "."]]
      [:p "The C entries share their core implementation with the "
       "Clojure-side primitives via internal "
       [:code "tx_*_core"] " helpers, so the two surfaces cannot "
       "drift. A nested "
       [:code "mino_tx_run"] " or "
       [:code "dosync"] " inside an outer transaction is absorbed "
       "into the outer's " [:code "tx_state_t"]
       "; only the outermost runner owns the setjmp / retry frame. "
       "See the "
       [:a {:href "/documentation/embedding/"} "Embedding Guide"]
       " for how to compose this with watches, futures, and the "
       "host's own thread pool."]

      [:h3 "Cross-state ref defense"]
      [:p "JVM Clojure has one global transactional surface. mino "
       "supports many " [:code "mino_state_t"]
       " in a single host process, so a host that accidentally "
       "passes a ref allocated in one state to another state's "
       [:code "mino_tx_*"] " entries would silently mutate the "
       "foreign heap. To prevent that, every ref records its "
       "allocating state at construction time, and every public C "
       "entry checks it; a mismatch throws "
       [:code "eval/state"] " MST007 (\"ref from foreign state\")."]

      ;; --- What still doesn't work ---

      [:h2 "What still doesn't work"]
      [:ul
       [:li [:code "add-watch"] " on " [:strong "vars"]
        " (atoms and refs work)."]
       [:li [:code "agent"] " / " [:code "send"] " / "
        [:code "send-off"] " / " [:code "await"]
        " stay unimplemented. Refs cover shared-memory coordination; "
        "fire-and-forget agent semantics would need a runtime-owned "
        "dispatcher mino does not ship today."]
       [:li "Reflection on " [:code "clojure.lang.Ref"] " (or any "
        "other JVM class). Cross-link: see "
        [:a {:href "/documentation/intentional-divergences/"}
         "intentional divergences"]
        " for the longer-form rationale."]])))


