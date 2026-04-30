# Changelog

All notable changes to mino-site are documented here.

## Unreleased

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
