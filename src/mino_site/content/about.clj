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
      [:p "mino draws from four projects that each got something deeply right:"]
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
        "small while making it extensible through macros and code-as-data."]
       [:li [:strong "Erlang/BEAM"] " showed that isolated processes "
        "communicating via message passing create fault-tolerant, concurrent "
        "systems without shared mutable state. mino's runtime isolation "
        "model draws from this: each runtime instance is a failure domain, "
        "concurrency happens between runtimes via explicit message passing, "
        "and the host controls scheduling."]]
      [:p "The goal is not to replicate any of these languages, but to combine "
       "their best ideas into something new: a tiny embeddable runtime with "
       "Clojure's data model, Lua's embedding philosophy, Fennel's "
       "pragmatic minimalism, and Erlang's isolation-first concurrency."]

      [:p "Where Clojure's concurrency model maps honestly to an embedded "
       "context, mino provides familiar abstractions: atoms for single-runtime "
       "mutation, agents backed by the actor model, and dynamic binding scoped "
       "to a runtime instance. Where Clojure's model assumes shared-memory "
       "threads (STM, refs, dosync), mino intentionally diverges: the embedding "
       "context demands isolation, and message passing between runtimes replaces "
       "coordinated in-process mutation."]

      [:h2 "Why embed mino?"]
      [:ul
       [:li [:strong "Small C implementation."] " A handful of focused C "
        "files in " [:code "src/"] ". Copy the directory into your project "
        "and compile with any C99 compiler."]
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

      [:h2 "Related projects"]
      [:p "mino exists alongside other excellent projects in the same "
       "programming tradition. Each one occupies a distinct niche."]

      [:h3 [:a {:href "https://jank-lang.org" :target "_blank"
                :rel "noopener"} "jank"]]
      [:p "jank is a native Clojure dialect with an LLVM-based compiler, "
       "inline C++ interop, and full AOT and JIT compilation. It brings "
       "the power of Clojure to native applications with strong "
       "performance and world-class error reporting. If you want to "
       "write entire programs in a native Lisp, jank is a great choice."]

      [:h3 [:a {:href "https://babashka.org" :target "_blank"
                :rel "noopener"} "Babashka"]]
      [:p "Babashka is a fast-starting Clojure scripting runtime built "
       "on GraalVM native image. It starts in milliseconds, ships as a "
       "single binary, and gives you access to a rich set of libraries "
       "for shell scripting, HTTP, JSON, and more. If you want to "
       "replace bash scripts with Clojure, Babashka is the tool."]

      [:h3 [:a {:href "https://www.lua.org" :target "_blank"
                :rel "noopener"} "Lua"]]
      [:p "Lua is the gold standard for embeddable scripting. It is "
       "small, fast, portable, and battle-tested in game engines, "
       "networking equipment, and countless other systems. Its C API "
       "is a model of embedding design. If you need a proven, mature "
       "scripting runtime with a vast ecosystem, Lua is hard to beat."]

      [:h3 [:a {:href "https://fennel-lang.org" :target "_blank"
                :rel "noopener"} "Fennel"]]
      [:p "Fennel is a Lisp that compiles to Lua. It gives you macros, "
       "pattern matching, and Lisp syntax while running on the Lua VM "
       "with full access to the Lua ecosystem. If you want Lisp "
       "expressiveness on top of Lua's runtime, Fennel is a beautiful "
       "way to get it."]

      [:h3 "What makes mino different"]
      [:p "None of the projects above fill a specific gap: an "
       "embeddable scripting runtime with an immutable-first data "
       "model and zero external dependencies."]
      [:ul
       [:li "jank and Babashka are standalone runtimes. They are meant "
        "to run your program, not to be embedded inside it."]
       [:li "Lua is embeddable but centers on mutable tables. When "
        "scripts can mutate shared state, the host must carefully "
        "manage what gets passed in and out."]
       [:li "Fennel gives you Lisp over Lua, but it still inherits "
        "Lua's mutable data model underneath."]]
      [:p "mino exists because no one else offers this particular "
       "combination:"]
      [:ul
       [:li [:strong "Embeddable like Lua."] " A C library you link "
        "into your program. No separate process, no VM, no build "
        "toolchain. Copy " [:code "src/"] ", any C99 compiler."]
       [:li [:strong "Immutable like Clojure."] " Persistent vectors, "
        "hash-array mapped tries, structural sharing. Values are "
        "safe to pass between host and guest without defensive "
        "copying."]
       [:li [:strong "Sandboxed by default."] " A fresh runtime has "
        "no I/O capabilities. The host decides what the script can "
        "do. Untrusted code cannot reach the filesystem, network, "
        "or anything else unless the host explicitly permits it."]
       [:li [:strong "Zero dependencies."] " Pure ANSI C. No "
        "runtime library, no platform-specific code, no transitive "
        "dependencies to audit or maintain."]]
      [:p "If you have a C program and want to give it a "
       "programmable extension layer where user scripts work with "
       "safe, immutable values in a sandboxed environment, mino is "
       "built for that."]
      [:h3 "When to choose something else"]
      [:p "mino is not the right tool for every situation. Here is "
       "when we would point you elsewhere:"]
      [:ul
       [:li [:strong "You are writing a standalone application."]
        " Use " [:a {:href "https://jank-lang.org" :target "_blank"
                     :rel "noopener"} "jank"]
        " for a compiled native Lisp, or Clojure on the JVM for the "
        "full ecosystem."]
       [:li [:strong "You need fast shell scripting and automation."]
        " Use " [:a {:href "https://babashka.org" :target "_blank"
                     :rel "noopener"} "Babashka"]
        ". It starts in milliseconds and has built-in support for "
        "HTTP, JSON, CSV, and shell interop."]
       [:li [:strong "You need a battle-tested embeddable runtime "
        "with a large ecosystem."]
        " Use " [:a {:href "https://www.lua.org" :target "_blank"
                     :rel "noopener"} "Lua"]
        ". It has decades of production use, thousands of libraries, "
        "and proven performance."]
       [:li [:strong "You want Lisp expressiveness on top of Lua."]
        " Use " [:a {:href "https://fennel-lang.org" :target "_blank"
                     :rel "noopener"} "Fennel"]
        ". It compiles to Lua and gives you full access to the Lua "
        "ecosystem with macros and pattern matching."]]
      [:p "All of these projects are excellent. They are complementary "
       "rather than competing, and we encourage you to explore each "
       "one."]

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
