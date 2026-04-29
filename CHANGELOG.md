# Changelog

All notable changes to mino-site are documented here.

## Unreleased

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
