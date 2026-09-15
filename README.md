> [!WARNING]
> **Discontinued — archived LLM experiment.** This project was an experiment to explore LLM-based ("agentic") software development. The resulting code is low-quality, unreliable, and unmaintainable, and it is no longer developed or supported. It is kept public and archived purely as a learning example. Do not use it in production.

# mino-site

Static documentation website for [mino](https://github.com/leifericf/mino).

## Build

```bash
clj -X:build
```

Output goes to `_site/`.

## Development

```bash
clj -M:dev
```

Starts a local dev server via Stasis + Ring Jetty. Edit source, refresh browser.

## Tech stack

- [Stasis](https://github.com/magnars/stasis) — static site generation
- [Hiccup](https://github.com/weavejester/hiccup) — HTML rendering
- [Garden](https://github.com/noprompt/garden) — CSS rendering