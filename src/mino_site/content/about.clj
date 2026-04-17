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
      [:p "mino is a tiny, embeddable Lisp runtime library implemented "
       "in pure ANSI C. The " [:em "host"] " (the C or C++ application "
       "embedding mino) links the library, creates "
       "one or more " [:em "runtimes"] " (isolated instances, each with "
       "its own garbage collector, bindings, and memory), installs "
       "capabilities, and evaluates user code through a compact C API."]
      [:p "The language centers on immutable values and persistent "
       "collections (data structures that preserve previous versions "
       "when updated, sharing unchanged parts in memory). The "
       "standalone REPL is a convenience for development and testing. "
       "The embedding API is the product."]

      [:h2 "Inspirations"]
      [:p "mino draws from four projects that each got something "
       "deeply right:"]
      [:ul
       [:li [:strong "Clojure"] " showed that immutable values, "
        "persistent data structures, and a data-first programming "
        "model could be practical and productive. mino preserves "
        "this core: immutable collections with structural sharing, "
        "code-as-data (programs are data structures that programs can "
        "inspect and transform), and REPL-driven development. "
        "See " [:a {:href "/documentation/coming-from-clojure/"}
        "Coming from Clojure"] " for a detailed comparison."]
       [:li [:strong "Lua"] " proved that a small, portable ANSI C "
        "implementation could become a world-class embeddable "
        "scripting language. mino follows the same discipline: "
        "library-first design, a compact C API, capability-based "
        "sandboxing, and a small footprint."]
       [:li [:strong "Fennel"] " demonstrated that Lisp syntax and "
        "macros can layer cleanly over a minimal runtime without "
        "sacrificing simplicity."]
       [:li [:strong "Erlang/BEAM"] " showed that isolated processes "
        "communicating via message passing create fault-tolerant "
        "concurrent systems without shared mutable state. mino's "
        "runtime isolation model draws from this: each runtime "
        "instance is a failure domain, concurrency happens between "
        "runtimes, and the host controls scheduling."]]
      [:p "Within a single runtime, mino provides atoms (mutable "
       "reference cells) and dynamic bindings for controlled state. "
       "Between runtimes, message passing replaces shared-memory "
       "coordination."]

      [:h2 "Why embed mino?"]
      [:ul
       [:li [:strong "Small C implementation."] " A handful of "
        "focused C files in " [:code "src/"] ". Copy the directory "
        "into your project and compile with any C99 compiler."]
       [:li [:strong "No external dependencies."] " No VM, no JIT, "
        "no platform-specific runtime services. Pure ANSI C."]
       [:li [:strong "Persistent data structures."] " Vectors, "
        "maps, and sets that preserve previous versions when "
        "updated. Unchanged parts are shared in memory, so updates "
        "are cheap and old versions remain accessible."]
       [:li [:strong "Sandboxed by default."] " A fresh runtime has "
        "no I/O capabilities. The host opts in to " [:code "println"]
        ", " [:code "slurp"] ", and file access by calling "
        [:code "mino_install_io()"] ". Untrusted code runs in a "
        "capability-free environment."]
       [:li [:strong "In-process REPL handle."] " "
        [:code "mino_repl_t"] " lets a host drive read-eval-print "
        "one line at a time. Build interactive consoles, debuggers, "
        "and live inspection tools inside running applications."]
       [:li [:strong "Execution limits."] " Step counts and heap "
        "caps for untrusted code. When a limit is exceeded, the "
        "current eval returns cleanly with a descriptive error."]
       [:li [:strong "Macros and code-as-data."] " "
        [:code "defmacro"] ", quasiquote, and " [:code "gensym"]
        " for building domain-specific languages without growing "
        "the C evaluator."]
       [:li [:strong "Proper tail calls."] " All function calls in "
        "tail position run in constant stack space. Self-recursion, "
        "mutual recursion, and general tail calls are optimized "
        "automatically with no special syntax required."]]

      [:h2 "Use cases"]
      [:ul
       [:li [:strong "Configuration files."] " Sandboxed evaluation "
        "of structured config with computed values, conditionals, "
        "and host queries."]
       [:li [:strong "Rules engines."] " Host state exposed to mino "
        "predicates for declarative business logic, validation, "
        "and policy."]
       [:li [:strong "Plugin and extension systems."] " Load user "
        "scripts with controlled capabilities and resource limits."]
       [:li [:strong "Interactive consoles."] " In-app REPLs for "
        "live inspection, debugging, and runtime configuration of "
        "running applications."]
       [:li [:strong "Data transformation pipelines."] " "
        [:code "map"] ", " [:code "filter"] ", " [:code "reduce"]
        " over persistent collections with structural sharing."]
       [:li [:strong "Game scripting."] " Embed a programmable "
        "console in game engines with sandboxing and step limits "
        "for player-authored code."]]

      ;; ----- Design -----

      [:h2 "Design"]

      [:h3 "Goals"]
      [:ul
       [:li [:strong "Embeddable."] " A C library first, a standalone "
        "REPL second. The embedding API is the primary product "
        "surface."]
       [:li [:strong "Value-oriented."] " Immutable values and "
        "persistent data structures by default. State is modeled "
        "as a succession of values, not in-place mutation."]
       [:li [:strong "Isolated."] " Each runtime is a failure "
        "domain. A crash, resource leak, or runaway eval in one "
        "runtime does not affect another."]
       [:li [:strong "Predictable."] " No hidden threads, no "
        "implicit I/O, no ambient state. The host controls what "
        "happens and when."]
       [:li [:strong "Small."] " A narrow core that does a few "
        "things well. Complexity lives in mino code and host code, "
        "not in the runtime."]]

      [:h3 "Core rules"]

      [:h4 "Values vs. resources"]
      [:p "User code works with values: numbers, strings, symbols, "
       "keywords, and persistent collections. Values are immutable, "
       "comparable, printable, and safe to pass around freely. "
       "Resources (file handles, sockets, database connections) "
       "belong to the host, which wraps them in opaque handles and "
       "exposes controlled operations to mino code."]

      [:h4 "No ambient state"]
      [:p "A fresh runtime has no I/O capabilities, no filesystem "
       "access, and no way to affect the outside world. The host "
       "opts in to capabilities explicitly. This is not a "
       "restriction layered on top; it is the default."]

      [:h4 "Explicit ownership"]
      [:p "Values returned by the runtime are borrowed by default: "
       "valid until the next garbage collection cycle. The host "
       "retains values across GC boundaries by creating explicit "
       "references. This keeps the common case fast and makes the "
       "ownership contract visible in the code."]

      [:h4 "Runtime isolation"]
      [:p "Each " [:code "mino_state_t"] " is a complete, "
       "independent runtime. Two runtimes in the same process share "
       "no mutable state that affects evaluation. This makes "
       "runtimes safe to use from different threads (one thread per "
       "runtime) and safe to create and destroy at will."]

      [:h4 "Concurrency between runtimes, not within"]
      [:p "A single runtime is single-threaded. Concurrency happens "
       "between runtimes via message passing. The host creates "
       "runtimes, sends messages between them, and controls "
       "scheduling."]

      [:h4 "Host controls orchestration"]
      [:p "The host decides when code runs, how long it runs, what "
       "capabilities it has, and when it stops. Runtimes do not "
       "spawn threads, open sockets, or perform I/O on their own."]

      [:h4 "Small trusted core"]
      [:p "The C implementation provides the irreducible core: the "
       "reader, evaluator, printer, garbage collector, persistent "
       "data structures, and a small set of primitive operations. "
       "Everything else is built in mino code on top of these "
       "primitives."]

      [:h3 "Trade-offs"]
      [:ul
       [:li [:strong "Isolation over shared memory."] " Runtimes "
        "cannot share objects. This means copying data between "
        "runtimes, which costs time and memory. The benefit is "
        "simplicity and thread safety."]
       [:li [:strong "Explicit ownership over convenience."] " The "
        "host must ref values it wants to keep across GC "
        "boundaries. This makes the ownership contract clear and "
        "avoids subtle retention bugs."]
       [:li [:strong "Copying over unsafe sharing."] " Message "
        "passing clones values at the boundary. Slower than passing "
        "pointers, but eliminates an entire class of concurrency "
        "bugs."]
       [:li [:strong "Host control over VM autonomy."] " The "
        "runtime never acts on its own. No background threads, no "
        "timers, no implicit work. The host has complete authority "
        "over resource usage and scheduling."]]

      [:h3 "What we rejected"]
      [:ul
       [:li [:strong "Shared-memory concurrency."] " Locks and "
        "shared mutable state create bugs that are hard to find. "
        "The isolation model avoids them entirely."]
       [:li [:strong "Software transactional memory."] " STM "
        "assumes shared-memory threads. Atoms provide single-runtime "
        "mutation; cross-runtime coordination uses message passing."]
       [:li [:strong "Ambient capabilities."] " Many scripting "
        "runtimes start with full access to the filesystem and "
        "network. mino starts with nothing. The host grants each "
        "capability explicitly."]]

      [:h3 "What mino is not"]
      [:ul
       [:li [:strong "Not a general-purpose application runtime."]
        " The standalone REPL is a development convenience, not "
        "the primary use case."]
       [:li [:strong "Not designed for shared-memory parallelism."]
        " If you need threads operating on shared data structures, "
        "mino is not the right tool."]
       [:li [:strong "Not self-hosting."] " The implementation "
        "language is C through v1.0. Self-hosting would undermine "
        "the embedding pitch."]]

      ;; ----- Proper tail calls -----

      [:h2 "Proper tail calls"]
      [:p "All function calls in tail position run in constant "
       "stack space. Self-recursion, mutual recursion, and general "
       "tail calls are optimized automatically."]

      [:p "A function call is in " [:em "tail position"] " when it "
       "is the last thing a function does before returning. mino "
       "recognizes tail position in " [:code "if"] " branches, "
       "the last expression in " [:code "do"] "/" [:code "let"]
       "/function bodies, and forms that expand to these ("
       [:code "when"] ", " [:code "cond"] ", " [:code "and"] ", "
       [:code "or"] ")."]

      [:pre [:code {:data-lang "mino"}
"(defn countdown (n)
  (if (= n 0) :done (countdown (- n 1))))

(countdown 1000000) ;; => :done (no stack overflow)"]]

      [:p "Mutual recursion works the same way:"]
      [:pre [:code {:data-lang "mino"}
"(defn is-even? (n)
  (if (= n 0) true (is-odd? (- n 1))))

(defn is-odd? (n)
  (if (= n 0) false (is-even? (- n 1))))

(is-even? 100000) ;; => true"]]

      [:p [:code "loop"] "/" [:code "recur"] " provide explicit "
       "iteration when that reads more clearly. With proper tail "
       "calls they are a stylistic choice rather than a necessity."]
      [:pre [:code {:data-lang "mino"}
"(loop [n 100 acc 0]
  (if (= n 0)
    acc
    (recur (- n 1) (+ acc n))))"]]

      [:p "The evaluator tracks tail position via an internal flag. "
       "Tail-position calls return a " [:code "MINO_TAIL_CALL"]
       " trampoline sentinel instead of recursing. The C stack "
       "stays flat regardless of recursion depth."]

      ;; ----- Performance -----

      [:h2 "Performance"]
      [:p "mino is a tree-walking interpreter with no bytecode "
       "compiler or JIT. A simple expression evaluates in under a "
       "microsecond. Bulk operations over lazy sequences cost 7-8 "
       "\u00b5s per element; the eager variants " [:code "rangev"]
       ", " [:code "mapv"] ", and " [:code "filterv"]
       " eliminate thunk overhead for tight loops. Creating a new "
       "runtime with the full standard library takes about 0.5 ms."]
      [:p "For detailed benchmarks covering core operations, "
       "collection throughput, cross-state cloning, actor scaling, "
       "and where the time goes, see the "
       [:a {:href "/documentation/performance/"} "Performance"]
       " page."]

      ;; ----- Related projects -----

      [:h2 "Related projects"]
      [:p "mino exists alongside other excellent projects in the "
       "same programming tradition. Each one occupies a distinct "
       "niche."]

      [:h3 [:a {:href "https://jank-lang.org" :target "_blank"
                :rel "noopener"} "jank"]]
      [:p "jank is a native Clojure dialect with an LLVM-based "
       "compiler, inline C++ interop, and full AOT and JIT "
       "compilation. If you want to write entire programs in a "
       "native Lisp, jank is a great choice."]

      [:h3 [:a {:href "https://babashka.org" :target "_blank"
                :rel "noopener"} "Babashka"]]
      [:p "Babashka is a fast-starting Clojure scripting runtime "
       "built on GraalVM native image. It starts in milliseconds "
       "and ships with built-in support for HTTP, JSON, CSV, and "
       "shell interop."]

      [:h3 [:a {:href "https://www.lua.org" :target "_blank"
                :rel "noopener"} "Lua"]]
      [:p "Lua is the gold standard for embeddable scripting. "
       "Small, fast, portable, and battle-tested in game engines "
       "and networking equipment. Its C API is a model of embedding "
       "design."]

      [:h3 [:a {:href "https://fennel-lang.org" :target "_blank"
                :rel "noopener"} "Fennel"]]
      [:p "Fennel is a Lisp that compiles to Lua. Macros, pattern "
       "matching, and Lisp syntax on top of the Lua VM with full "
       "access to the Lua ecosystem."]

      [:h3 [:a {:href "https://janet-lang.org" :target "_blank"
                :rel "noopener"} "Janet"]]
      [:p "Janet is a functional, embeddable Lisp written in C "
       "with a register-based VM, PEG pattern matching, a built-in "
       "event loop, and a package manager. A feature-rich embeddable "
       "Lisp with a broader standard library and an active ecosystem."]

      [:h3 [:a {:href "https://github.com/Zelex/jo_clojure" :target "_blank"
                :rel "noopener"} "JO Clojure"]]
      [:p "JO Clojure is a native C/C++ implementation of a "
       "Clojure-like language with very fast startup, persistent "
       "data structures, and an interactive REPL. It shares mino's "
       "interest in native embedding and small-footprint deployment, "
       "while aiming for broader Clojure compatibility over time."]

      [:h3 [:a {:href "https://github.com/rekola/nanoclj" :target "_blank"
                :rel "noopener"} "nanoclj"]]
      [:p "nanoclj is a tiny Clojure interpreter written in C, "
       "focused on compactness and embeddability. Like mino, it "
       "explores what a small native Lisp can do outside the JVM, "
       "but it makes explicit compatibility trade-offs and includes "
       "a distinct REPL-oriented graphics and data-visualization "
       "direction."]

      [:h3 [:a {:href "https://github.com/mll/clojure-rt" :target "_blank"
                :rel "noopener"} "clojure-rt"]]
      [:p "clojure-rt is an LLVM-based Clojure implementation "
       "focused on deterministic, high-performance execution. It is "
       "a compiler-and-runtime effort aimed at real-time constraints, "
       "in contrast to mino's deliberately small interpreter core "
       "and library-first embedding model."]

      [:h3 "What makes mino different"]
      [:p "None of the projects above fill a specific gap: an "
       "embeddable scripting runtime with an immutable-first data "
       "model and zero external dependencies."]
      [:ul
       [:li "jank and Babashka are standalone runtimes, not "
        "embeddable libraries."]
       [:li "Lua is embeddable but centers on mutable tables."]
       [:li "Fennel gives you Lisp over Lua, but inherits Lua's "
        "mutable data model."]
       [:li "Janet is embeddable and Lisp, but uses mutable arrays "
        "and tables as its core data structures, with a larger "
        "footprint (VM, bytecode compiler, event loop, package "
        "manager)."]
       [:li "JO Clojure and nanoclj are close in spirit as native "
        "C/C++ Clojure-like runtimes, but mino emphasizes a narrower "
        "embedding surface, capability-first sandboxing, and strict "
        "runtime isolation as the primary product shape."]
       [:li "clojure-rt targets deterministic high performance via "
        "LLVM/JIT compiler architecture; mino intentionally "
        "prioritizes a small interpreter core and host-controlled "
        "orchestration over compiler complexity."]
       [:li "Erlang/BEAM pioneered isolated processes with message "
        "passing, but it is a full runtime system, not an "
        "embeddable library."]]
      [:p "mino combines embeddability (like Lua), immutable "
       "persistent data (like Clojure), sandboxing by default, "
       "and zero dependencies in pure ANSI C."]

      [:h3 "When to choose something else"]
      [:ul
       [:li [:strong "Standalone application:"]
        " " [:a {:href "https://jank-lang.org" :target "_blank"
                 :rel "noopener"} "jank"]
        " for compiled native Lisp, or Clojure on the JVM."]
       [:li [:strong "Shell scripting:"]
        " " [:a {:href "https://babashka.org" :target "_blank"
                 :rel "noopener"} "Babashka"]
        " starts in milliseconds with built-in HTTP, JSON, and "
        "shell interop."]
       [:li [:strong "Battle-tested embeddable runtime:"]
        " " [:a {:href "https://www.lua.org" :target "_blank"
                 :rel "noopener"} "Lua"]
        " has decades of production use and thousands of libraries."]
       [:li [:strong "Lisp on Lua:"]
        " " [:a {:href "https://fennel-lang.org" :target "_blank"
                 :rel "noopener"} "Fennel"]
        " compiles to Lua with macros and pattern matching."]
       [:li [:strong "Feature-rich embeddable Lisp:"]
        " " [:a {:href "https://janet-lang.org" :target "_blank"
                 :rel "noopener"} "Janet"]
        " has PEG matching, an event loop, multithreading, and a "
        "package ecosystem."]
       [:li [:strong "Native C/C++ Clojure-like runtime experiments:"]
        " " [:a {:href "https://github.com/Zelex/jo_clojure" :target "_blank"
                 :rel "noopener"} "JO Clojure"]
        " and " [:a {:href "https://github.com/rekola/nanoclj" :target "_blank"
                     :rel "noopener"} "nanoclj"] "."]
       [:li [:strong "Compiler/JIT path for deterministic performance:"]
        " " [:a {:href "https://github.com/mll/clojure-rt" :target "_blank"
                 :rel "noopener"} "clojure-rt"] "."]])))
