# CLAUDE.md — mino-site Development Guidelines

## What this is

Static documentation website for the mino embeddable Lisp runtime. Built with
Clojure (Stasis + Hiccup + Garden), deployed to GitHub Pages.

## Build and run

```bash
# Production build — generates _site/
clj -X:build

# Development — REPL with dev server
clj -M:dev
# Then in the REPL: (start!)
```

## Project structure

```
src/mino_site/
  build.clj              Orchestrator (Stasis export-pages)
  render.clj             Shared page chrome (Hiccup)
  styles.clj             Garden CSS (green phosphor theme)
  highlight.clj          Inline JS for syntax highlighting
  parse/
    header.clj           mino.h → API reference data
    builtins.clj         mino.c → language reference data
    cookbook.clj          cookbook/*.c → cookbook data
    changelog.clj        CHANGELOG.md → changelog data
    smoke.clj            smoke.sh → example data
  content/
    landing.clj          Homepage
    about.clj            About page
    get_started.clj      Get Started guide
    download.clj         Download page
dev/
  user.clj               REPL setup + Stasis dev server
mino/                    Git submodule → leifericf/mino
```

## Key conventions

- **deps.edn** only, no Leiningen.
- **REPL-driven development** is the primary workflow.
- **Functional core, imperative shell**: parsers and renderers are pure
  functions; `build.clj` is the imperative shell that reads files and writes
  output.
- **Stasis page map**: the site is defined as `{"/path/" (fn [ctx] html-str)}`.
  Use `stasis/export-pages` for builds, `stasis/serve-pages` for dev.
- **Hiccup** for HTML, **Garden** for CSS. All CSS is inlined in `<style>`.
- **No Replicant, no Powerpack** — keep the stack minimal.

## Content rules

- **Never mention competing/related languages** (Lua, Fennel, Clojure,
  Babashka, SCI, etc.) in any site content, commits, or comments. Describe
  mino's features in their own terms.
- All auto-generated content comes from parsing the mino submodule source at
  build time. The parsers work with existing source structure — no annotations
  or metadata should be added to mino itself.

## Clojure style

- 2-space indentation, no tabs.
- 80-char line limit (120 max).
- `:require :as` over `:refer`. Idiomatic aliases: `str`, `io`, `set`.
- Predicates end in `?`, side-effecting fns end in `!`.
- Prefer `map`/`filter`/`reduce` over manual `loop/recur`.
- Maps over records. Data over objects.
- Keep `ns` forms clean: `:require` sorted alphabetically.
