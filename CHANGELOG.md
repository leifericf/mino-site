# Changelog

All notable changes to mino-site are documented here.

## Unreleased

- Tracking mino v0.103.0 (Worker-List Lock Split: separates the
  brief worker-bookkeeping lock (`worker_ctxs_head` linked list +
  `thread_count` counter) from the heavy recursive `state_lock`.
  Closes the only NEEDS-DESIGN finding from the v0.102.0
  adversarial pass: a tight embedder loop holding state_lock can
  no longer starve future / agent worker entry-link or
  exit-detach. Lock order: state_lock outer, worker_list_lock
  inner; workers at entry/exit acquire alone. GC root scan takes
  the new lock briefly across the three `worker_ctxs_head`
  walks. The public C API surface is unchanged; embedders that
  rebuild against the new submodule pick up the fix
  transparently. No site-side changes: pages reference the
  general embedding contract, not the specific lock split, so
  the existing prose stays accurate).
- Tracking mino v0.102.1 (Agents adversarial-test pass + qa-arch
  hygiene: doc accuracy fix to the thread-budget message in
  `agent_worker_ensure` and the matching site copy on the STM
  page, the Compatibility Matrix, the Intentional Divergences
  page, and the Coming-from-Clojure page; 11 `abort()`
  rationale comments + TU-size allowlist updates).
- Tracking mino v0.102.0 (Agents finish MVP: per-state agent
  workers + run-queues replace the synchronous-on-the-calling-thread
  fallback, with a separate POOLED / SOLO split for `send` /
  `send-off` and a public C-API perimeter for embedders. `send` /
  `send-off` enqueue and return the agent immediately; each pool's
  worker drains its queue under `state_lock` and signals `agent_cv`
  when each agent's in-flight count reaches zero. `await` and
  `await-for` now block until every named agent's queued actions
  complete; `await-for` returns `false` on timeout. Each worker
  counts against `thread_limit`, so `send` throws `MTH001` if the
  host hasn't granted a thread budget -- same shape as `future` /
  `promise` / `thread`. Each worker exits when its run-queue
  drains so it doesn't keep `thread_count` above zero
  indefinitely; the next `send` into that pool re-spawns.
  `shutdown-agents` joins both workers and seals the state's agent
  surface (subsequent sends throw `MST008`); self-call from inside
  an action body throws `MST002`. `restart-agent` accepts
  `:clear-actions true` to drop every queued action for that agent
  across both pools. The `dosync` post-commit drain enqueues onto
  the POOLED worker instead of running synchronously. Public C-API
  entries `mino_send`, `mino_send_off`, `mino_await`,
  `mino_await_for`, `mino_agent_error`, `mino_restart_agent` let
  embedder code drive the agent subsystem without going through
  the Clojure prim layer; the cross-state guard fires at the C
  boundary. Latent fix: `mino_pcall` now restores `lock_depth`
  after a longjmp from a throwing body; the agent worker's
  yield/resume cycle made the imbalance observable). The
  Compatibility Matrix agent rows flip from "Supported (MVP)" to
  "Supported"; Intentional Divergences rewrites the agent
  paragraph from "synchronous (MVP)" to "async via per-state
  workers (POOLED + SOLO)"; the STM page agents section gains the
  thread-budget contract, pool-split paragraph, embedder C-API
  paragraph, failure-handling details, and lifecycle paragraph;
  Coming from Clojure flips the synchronous-agents bullet to
  async-agents with the `MTH001` thread-budget caveat.
- Tracking mino v0.101.1 (STM and agent hardening pass: 19 commits
  closing real or latent bugs across the STM and agent surfaces;
  no new features, all alignment with JVM canon and internal
  consistency. Two-pass atomic commit; `tx_state_t.in_commit` to
  reject re-entered mutators; `mino_pcall`-wrapped commute-replay
  and watch dispatch for both refs and agents; in-tx `send` /
  `send-off` deferred to post-commit with `release-pending-sends`
  wired to count and clear; cross-state defense for agents and
  tightened for refs (moved into shared cores so the Clojure path
  is covered); constructor option parsing for `:validator`,
  `:error-handler`, `:error-mode`, `:meta` with unknown keys
  throwing; `error-handler` actually invoked on action and
  validator failure; `restart-agent` validates new state;
  `*agent*` bound during action / validator / watch dispatch;
  `shutdown-agents` flips a state-level flag and `send-via`
  throws MST008 instead of being unbound; `with-meta` /
  `vary-meta` on stateful types throw; agent print form now
  carries identity).
- Tracking mino v0.98.5 (Hygiene + Closure cycle: macro hygiene
  fix in `qq_qualify_symbol` so syntax-quoted bare symbols inside a
  macro body qualify against the macro's defining namespace not the
  consumer's `*ns*` (closes the silent
  `with-out-str`-after-`:refer :all` miscompile and the
  `unbound symbol: chan*` failure for `(a/go ...)` called from
  outside `clojure.core.async`); `compare` gains the canon
  cross-type total order
  `nil < false < true < numbers < strings < symbols < keywords`;
  `clojure.string/split` gains the 3-arg `limit` arity; vector seqs
  and lazy `range` auto-chunk into 32-element chunks so
  `(chunked-seq? (seq [1 2 3]))` is `true` and
  `(reduce + (map inc (filter odd? (range 1e6))))`-style pipelines
  run end-to-end chunked; `array-map` insertion-order semantics
  verified to already match canon; `random-seed!` primitive plus a
  minimal `clojure.test.check` port (generators, properties,
  `quick-check`; shrinking deferred) backing
  `clojure.spec.alpha/gen` and `clojure.spec.alpha/exercise`).
  Compatibility Matrix flips the chunked-seq row note to mention
  source-side auto-chunking, expands the `clojure.string` row with
  the new arity, and the `clojure.string/split` 3-arg note.
  Intentional Divergences gains the cross-type `compare` order, the
  source-auto-chunking note, and the `clojure.test.check` line.
  From-Clojure flips the chunked-sequences caveat from "sources do
  not auto-chunk yet" to "vector seqs and lazy `range` auto-chunk
  into 32-element chunks". Performance flips the same caveat in the
  pipeline notes.
- Tracking mino v0.97.5 (Kwargs + Audit + Hygiene cycle: kwargs
  destructuring matches Clojure 1.11 (inline pairs, trailing map,
  mixed; `:or` defaults eval correctly inside the C-level binder);
  `iteration` rewritten to canon `& {:keys [...]}` shape; `sort-by`
  and `reductions` gain multi-arity; `src/core.clj` 80-char wrap
  with no behavioral churn; `defn` lifted so six bootstrap
  `def + fn` forms become `defn`; `clojure.core.async` gains canon
  `reduce` / `transduce` / `split` / `partition-by` (and excludes
  them from `clojure.core`); `clojure.spec.alpha` gains `abbrev` /
  `describe`). Compatibility Matrix flips the `iteration` row to
  drop the kwargs-as-map divergence note and adds the four new
  async combinators to the `clojure.core.async` row. Intentional
  Divergences drops the "No user tagged literals" h2: end-to-end
  verification confirms `*data-readers*` and the
  `tagged-literal` record fallback both fire from `read-string`.
- Tracking mino v0.96.8 (Canon-Parity cycle: real `MINO_VOLATILE`
  primitive backing `volatile!`/`vswap!`/`vreset!`; stateful transducers
  use volatiles; lazy-seq combinators `recur` on skip; transients in
  `frequencies`/`group-by`; comp/partial/some-fn/every-pred unrolled
  fast paths; `into` 0/1-arg arities; `unchecked-divide-int`;
  `iteration` (Clojure 1.11); `clojure.core.async` namespace wrap with
  `merge` and `into` under their canon names; `:refer :all` no longer
  drags transitive refers; chunked-seq family (`MINO_CHUNK` +
  `MINO_CHUNKED_CONS`, eight primitives, C-level map/filter/take and
  mino-level keep/keep-indexed/map-indexed propagating chunkedness).
  Compatibility Matrix rows for `volatile!`, chunked-seq APIs,
  `iteration`, `unchecked-divide-int`, and `clojure.core.async`
  flipped or expanded; Intentional Divergences drops the
  no-chunked-seqs and volatile-as-atom entries; Coming from Clojure
  rewrites the chunked-seq disclaimer and the async require example;
  Performance banner notes the phase-level changes pending a fresh
  bench run; the language-reference async parser reads from the new
  single-file `lib/clojure/core/async.clj` location; minor lowercase
  and version-string fixes across About/Get Started/Testing.
- Tracking mino v0.95.5 (Clojure-side hygiene pass: `clojure.data/diff`
  rebuilt around `reduce`; `clojure.test` state moved to dynamic vars
  and `run-tests` returns a summary map; `clojure.instant/parse-timestamp`
  decomposed into per-segment helpers; `core.async` canon parity with
  `onto-chan!`/`to-chan!` renames, `pipeline` ex-handler arity, and
  `alts!` kwargs surface; `mino.tasks.builtin` and `clojure.string`
  hygiene; `src/core.clj` standardisation dropping the trailing-`_`
  private convention and converting `def`-with-fn-body to `defn`.
  No user-visible site copy needed updating — the changelog page picks
  up the new entries automatically and the Coming-from-Clojure /
  async / test pages do not name the renamed internal helpers.
- Tracking mino v0.94.0 (empty-list canon parity: `()` is now a real
  value type, distinct from nil; cross-type seq equality includes
  empty-list and excludes nil; print form is `()`). Removed the
  `(list)` is `nil` divergence from the Intentional Divergences and
  Coming from Clojure pages and flipped the compatibility matrix row.
- Tracking mino v0.93.0 (C refactoring pass; bundled `mino deps` and
  `mino task` so brew installs work without a sibling `lib/`; bootstrap
  Makefile). Get Started and Tasks pages now show `make` as the
  bootstrap step, with `./mino task ...` for everything beyond.
- Tracking mino v0.42.0: generational + incremental garbage collector,
  new public GC control API (`mino_gc_collect`, `mino_gc_set_param`,
  `mino_gc_stats`), five tuning env vars, literal-builder barrier fix.
  Embedding and performance pages refreshed.
- Tracking mino v0.39.1 (task runner, `str-replace` primitive,
  `file-mtime` primitive, Windows CI)
- Added mino-examples submodule for use-case pages (restores 8 pages
  broken when examples moved to a separate repo).
- Updated Coming from Clojure page with new conformance features.
- Added EMBEDDING.md, NREPL.md, CONFORMANCE.md source docs.
