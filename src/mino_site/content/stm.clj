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

      [:h2 "Agents"]
      [:p "mino ships agents with async dispatch: "
       [:code "agent"] ", " [:code "send"] ", " [:code "send-off"]
       ", " [:code "await"] ", " [:code "await-for"] ", "
       [:code "agent-error"] ", " [:code "restart-agent"] ", "
       [:code "shutdown-agents"] ", and the error-mode / "
       "error-handler surface. " [:code "send"] " enqueues the "
       "action onto a per-state run-queue and returns the agent "
       "immediately; a worker thread drains the queue and runs each "
       "action under " [:code "state_lock"] ". "
       [:code "await"] " and " [:code "await-for"] " block until "
       "every named agent's in-flight count reaches zero ("
       [:code "await-for"] " returns " [:code "false"] " on "
       "timeout)."]
      [:p [:strong "Thread budget."]
       " The worker counts against " [:code "thread_limit"]
       " (default 1 in embedded use; standalone " [:code "./mino"]
       " bumps to " [:code "cpu_count"] " after install), so "
       [:code "send"] " / " [:code "send-off"] " throw "
       [:code "MTH001"] " when the host hasn't granted a thread "
       "budget -- the same shape "
       [:code "future"] " / " [:code "promise"] " / "
       [:code "thread"] " already use. Each pool's worker exits "
       "when its run-queue drains so it doesn't keep "
       [:code "thread_count"] " > 0 indefinitely; the next "
       [:code "send"] " re-spawns. " [:code "send"] " routes onto "
       "the POOLED pool, " [:code "send-off"] " onto SOLO; the two "
       "queues are independent so a long-running send-off does not "
       "stall pending sends, and vice versa. mino's per-state eval "
       "lock still serializes one action at a time across both "
       "pools, so the user-visible behavior is identical to a single "
       "queue today; the split is the seam for a future "
       "SOLO-yields-eval-lock-during-blocking-IO design. Embedders "
       "that want both pools alive concurrently must raise the "
       "thread limit to at least 3 (embedder + POOLED + SOLO worker)."]
      [:p [:strong "Failure handling."]
       " Action throws and watch throws are both captured into "
       [:code "agent-error"] " via "
       [:code "mino_pcall"] " -- a thrown watch does not abort "
       "sibling watches or propagate to the caller of "
       [:code "send"] ", matching JVM canon. With an "
       [:code "error-handler"] " installed, the action throw "
       "routes through the handler and the agent stays clean (no "
       [:code "agent-error"] " latch). "
       [:code "restart-agent"] " accepts trailing "
       [:code ":clear-actions true"] " to drop every queued action "
       "for that agent. "
       [:code "send-via"] " is intentionally deferred (no public "
       "Executor type)."]
      [:p [:strong "Lifecycle."]
       " " [:code "shutdown-agents"] " flips an agents-shutdown "
       "flag, signals both pool workers to drain and exit, and "
       [:code "pthread_join"] "s each. Subsequent "
       [:code "send"] " / " [:code "send-off"] " throw "
       [:code "MST008"] ". Calling "
       [:code "shutdown-agents"] " from inside an action body "
       "(self-join) throws " [:code "MST002"] " instead of "
       "deadlocking. " [:code "mino_state_free"] " quiesces both "
       "pools before heap teardown so a worker can't run after "
       "free."]
      [:p [:strong "Embedder C-API."]
       " Host code can drive agents directly without going through "
       "the Clojure prim layer. "
       [:code "mino_send"] " / " [:code "mino_send_off"] " enqueue "
       "an action and return the agent immediately. "
       [:code "mino_await"] " / " [:code "mino_await_for"] " block "
       "until the named agents drain (NULL-terminated array). "
       [:code "mino_agent_error"] " reads the failure latch. "
       [:code "mino_restart_agent"] " clears it and resets the "
       "value, with optional clear-actions semantics. Each entry "
       "takes the same " [:code "mino_lock"] " perimeter "
       [:code "mino_call"] " uses, and the cross-state guard fires "
       "at the boundary -- passing an agent from another "
       [:code "mino_state_t"] " throws "
       [:code "MST007"] " and returns NULL."]

      [:h2 "What still doesn't work"]
      [:ul
       [:li [:code "send-via"]
        " (custom executor for sends) -- intentionally deferred. "
        "Use " [:code "send"] " or " [:code "send-off"]
        " through the per-state worker."]
       [:li "Reflection on " [:code "clojure.lang.Ref"] " (or any "
        "other JVM class). Cross-link: see "
        [:a {:href "/documentation/intentional-divergences/"}
         "intentional divergences"]
        " for the longer-form rationale."]])))


