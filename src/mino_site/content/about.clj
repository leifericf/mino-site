(ns mino-site.content.about
  "About page content."
  (:require
    [hiccup2.core :as h]))

(defn about-page
  "Generates the About page HTML body."
  []
  (str
    (h/html
      [:h1 "About mino"]

      [:h2 "What is mino?"]
      [:p "mino is a tiny, embeddable Lisp runtime library implemented in pure "
       "ANSI C. It is designed as a scripting and extension layer for native "
       "applications: the host links the library, creates a runtime instance, "
       "installs capabilities, and evaluates user code through a compact C API."]
      [:p "The language centers on immutable values, persistent collections with "
       "structural sharing, code-as-data, macros, and REPL-driven interactive "
       "development. It fills the space where an application needs a "
       "programmable extension layer with strong data semantics and a safer "
       "default than mutable state."]
      [:p "The standalone REPL is a convenience for development and testing. "
       "The center of the design is a host process embedding the language for "
       "scripting, configuration, automation, extension, and interactive "
       "inspection."]

      [:h2 "Inspirations"]
      [:p "mino draws from three languages that each got something deeply right:"]
      [:ul
       [:li [:strong "Clojure"] " showed that immutable values, persistent "
        "data structures, and a data-first programming model could be practical "
        "and productive, not just academic. mino preserves this value-oriented "
        "core: immutable collections with structural sharing, code-as-data, "
        "and REPL-driven development."]
       [:li [:strong "Lua"] " proved that a small, portable ANSI C "
        "implementation could become a world-class embeddable scripting "
        "language. mino follows the same discipline: library-first design, "
        "a compact C API, capability-based sandboxing, and a small footprint."]
       [:li [:strong "Fennel"] " demonstrated that Lisp syntax and macros "
        "can layer cleanly over a minimal runtime without sacrificing "
        "simplicity. mino takes a similar approach to keeping the language "
        "small while making it extensible through macros and code-as-data."]]
      [:p "The goal is not to replicate any of these languages, but to combine "
       "their best ideas into something new: a tiny embeddable runtime with "
       "Clojure's data model, Lua's embedding philosophy, and Fennel's "
       "pragmatic minimalism."]

      [:h2 "Why embed mino?"]
      [:ul
       [:li [:strong "Single-file C implementation."] " ~7,000 lines of "
        [:code "mino.c"] " plus a 350-line header. Copy two files into your "
        "project and compile with any C99 compiler."]
       [:li [:strong "No external dependencies."] " No VM, no JIT, no "
        "platform-specific runtime services. Pure ANSI C."]
       [:li [:strong "Persistent data structures."] " Vectors (32-way tries), "
        "maps (HAMT), and sets with structural sharing. Updates return new "
        "values; old versions remain accessible."]
       [:li [:strong "Sandboxed by default."] " A fresh runtime has no I/O "
        "capabilities. The host opts in to " [:code "println"] ", "
        [:code "slurp"] ", and file access by calling "
        [:code "mino_install_io()"] ". Untrusted code runs in a capability-free "
        "environment."]
       [:li [:strong "In-process REPL handle."] " "
        [:code "mino_repl_t"] " lets a host drive read-eval-print one line at "
        "a time with no thread required. Build interactive consoles, debuggers, "
        "and live inspection tools inside running applications."]
       [:li [:strong "Execution limits."] " Step counts and heap caps for "
        "untrusted code. When a limit is exceeded, the current eval returns "
        "cleanly with a descriptive error."]
       [:li [:strong "Macros and code-as-data."] " " [:code "defmacro"]
        ", quasiquote, and " [:code "gensym"] " for building domain-specific "
        "languages without growing the C evaluator."]
       [:li [:strong "Any platform."] " Compiles with gcc, clang, MSVC, and "
        "any other C99-compliant compiler. No platform-specific code."]]

      [:h2 "Use cases"]
      [:p "mino is built for domains where user scripts, host automation, and "
       "data-driven logic benefit from a safe, compositional value model."]
      [:ul
       [:li [:strong "Configuration files."] " Sandboxed evaluation of "
        "structured config with computed values, conditionals, and host queries."]
       [:li [:strong "Rules engines."] " Host state exposed to mino predicates "
        "for declarative business logic, validation, and policy."]
       [:li [:strong "Plugin and extension systems."] " Load user scripts "
        "with controlled capabilities and resource limits."]
       [:li [:strong "Interactive consoles."] " In-app REPLs for live "
        "inspection, debugging, and runtime configuration of running "
        "applications."]
       [:li [:strong "Data transformation pipelines."] " "
        [:code "map"] ", " [:code "filter"] ", " [:code "reduce"]
        " over persistent collections with structural sharing."]
       [:li [:strong "Game scripting."] " Embed a programmable console in "
        "game engines with sandboxing and step limits for player-authored code."]]

      [:h2 "Design principles"]
      [:ul
       [:li [:strong "Immutable by default."] " Values are facts that "
        "don't change. State is modeled as a succession of values, not "
        "in-place mutation."]
       [:li [:strong "Capability-based I/O."] " The runtime begins with a "
        "minimal core. Dangerous powers (I/O, networking, filesystem access) "
        "are installed explicitly by the host."]
       [:li [:strong "Library-first."] " The standalone executable is "
        "layered on top of the library. The embedding API is the primary "
        "product surface."]
       [:li [:strong "Small and coherent over feature-complete."] " A "
        "deliberately scoped language with a narrow but strong semantic core. "
        "Not a compatibility exercise."]])))
