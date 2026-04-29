# Changelog

All notable changes to mino-site are documented here.

## Unreleased

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
