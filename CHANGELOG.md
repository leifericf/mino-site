# Changelog

All notable changes to mino-site are documented here.

## Unreleased

- Submodule pointer bumped to mino v0.157.1. Picks up the
  bc-frontiers cycle (v0.152.0 through v0.157.1, six minor
  releases plus a patch): write-side fast-lane opcodes
  (`OP_CONJ_VEC`, `OP_ASSOC`), small-prim inlining for vector
  operands (`OP_FIRST_VEC` / `OP_COUNT_VEC` / `OP_EMPTY_VEC`),
  record fast path plus `(:kw coll)` keyword-as-fn inlining inside
  `OP_GET_KW_MAP`, inline-cached call sites (`OP_CALL_CACHED`
  fuses global-symbol head resolution and dispatch), generic get
  + arity-2 dissoc fast lanes, transducer fusion in `prim_reduce`
  via thunk-pointer inspection on `map` / `filter` / `take`
  chains, and the `MINO_BC_OP_COUNTS=1` build flag for VM perf
  work. Headline benchmark deltas: pipeline-sum 93 µs → 21 µs
  (-77%), get-str-map -81%, dissoc-map -21%, get-kw-record -93%,
  fib-30 -13%, conj-vec -34%, count-vec -94%.

- Bytecode VM page's "Recently picked up" section now lists all
  six shipped frontiers with their benchmark deltas; "Still open"
  sharpened on the dispatch-shape rework with the 2026-05
  trivial-fix spike outcome (compile-flag and attribute
  workarounds tested, none fired on the current switch
  dispatcher).

- Submodule pointer bumped to mino v0.151.1. Picks up the v0.150.0
  and v0.151.0 embedding-API revamp (opaque `struct mino_val`,
  single `mino_install(S, env, caps)` bitmask entry point, full
  predicate grid, `_ex` matrix with structured error access,
  collection builders, unified iterator, host-syntax routing
  through the BC tree-walker, namespace-resolved `host/new`,
  `mino_int` auto-promote to bigint with `MINO_CAP_BIGNUM`) plus
  the v0.151.1 embedding hardening (NULL-arg guards on
  `mino_eval_string` and `mino_read`, sorted-map / sorted-set
  iter walks via in-order RB traversal, `_ex` family delivers the
  raw thrown payload through `out_ex`, `mino_to_int` accepts
  bigints to close the auto-promote round-trip).

- Submodule pointer bumped to mino v0.149.1. Covers five minor
  cycles since v0.144.5 plus the v0.149.1 bug-fix roll-up: hash
  contract for sequential and sorted collections, sorted-collection
  dissoc count, `ex-info` 3-arity cause, catch metadata
  preservation, `fmt_ensure` / `(sh ...)` OOM cleanup, and the
  `pclose` `-1` sentinel.

- Submodule pointer bumped to mino v0.144.5 (closes the
  bytecode-VM cycle on remote; covers four release-pipeline
  follow-ups: a GC fix for tracing the compiled-bytecode record
  through the remembered set, two build-environment patches that
  silence gcc's `-Wclobbered` heuristic without changing
  generated code, and a real correctness fix in `OP_PUSHCATCH`
  for nested-try re-throw on stricter compilers).
- Tracking mino v0.105.0 through v0.144.1 (Bytecode-VM Cycle: a
  lazily-compiled register-based bytecode VM now handles user
  fns by default, with the tree-walker remaining as a fallback
  for declined forms and top-level evaluation. The cycle landed
  pointer-tagged values for small integers / booleans / nil /
  char, inline-cache slots for global symbol resolution,
  immediate-operand fast lanes for the canonical arithmetic and
  comparison primitives, fused counted-loop opcodes for the
  `(recur (dec ...))` and `(recur (inc ...))` shapes, an argv
  calling convention that skips the cons-spine build on hot
  paths, compile-time literal-arg folding for pure primitives,
  n-arity arithmetic expansion to chained binary ops, a flatmap
  representation for persistent maps under 8 entries, a cached
  hash on immutable vector / map / set headers with a same-type-
  and-both-cached `=` short-circuit, and a tree-walker
  destructure fix for `:strs` / `:syms` plus a forcing path for
  map / set value equality with lazy contents. Cross-runtime
  numbers shift substantially: a tight integer counter loop now
  runs ~4× faster than Lua 5.5 on the same shape; an arithmetic
  chain bench drops 19× from v0.128.0 baseline; cond-branch
  drops 50× and beats Janet 2.6×. Site updates: `:strs` and
  `:syms` listed in the Coming-from-Clojure destructure summary;
  Performance page banner and "Where the time goes" architecture
  description refreshed for the bytecode VM (microbench numbers
  in the body left intact with a "full rebench queued" note);
  landing-page embedding card replaces "No VM, no JIT, no
  daemon" with "No external runtime, no JIT, no daemon" so the
  intended contrast (against JVM / CLR / Node) survives the
  arrival of an internal bytecode interpreter).
- Tracking mino v0.104.0 (Eval-Floor Performance Cycle: a non-JIT
  pass that lands the inline call cache, argv/argc calling
  convention for the hot fixed-arity prims plus the variadic
  arithmetic and comparison ops, closure-shape pre-compile,
  binary numeric int+int fast lane, reduce/range fast paths,
  multi-arity recur env reuse, cached symbol hash, inline
  truthiness, and a fix for `ns-unmap` missing the inline call
  cache invalidation. Cumulative: per-op cost down about 24
  percent across 15 microbench-gate entries; a tight integer
  `loop/recur` bench dropped from 941 ms to 375 ms. Allocation
  per op is unchanged. The Performance page is refreshed with
  the new tables and updated cost-center prose; no other site
  changes are required because the cycle preserves the public
  C and Clojure-canon surfaces).
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
