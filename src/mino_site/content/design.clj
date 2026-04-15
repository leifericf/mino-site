(ns mino-site.content.design
  "Design page content."
  (:require
    [hiccup2.core :as h]))

(defn design-page
  "Generates the Design page HTML body."
  []
  (str
    (h/html
      [:h1 "Design"]

      [:h2 "What mino is"]
      [:p "mino is an isolated, embeddable, value-oriented runtime "
       "implemented in pure ANSI C. The host application links the "
       "library, creates one or more runtime instances, installs "
       "capabilities, and evaluates user code through a compact C API."]
      [:p "Each runtime instance is self-contained. It owns its own "
       "garbage collector, intern tables, module cache, and every "
       "object allocated within it. Multiple instances coexist in the "
       "same process without sharing mutable state."]

      [:h2 "Design goals"]
      [:ul
       [:li [:strong "Embeddable."] " A C library first, a standalone "
        "REPL second. The embedding API is the primary product surface."]
       [:li [:strong "Value-oriented."] " Immutable values and "
        "persistent data structures by default. State is modeled as "
        "a succession of values, not in-place mutation."]
       [:li [:strong "Isolated."] " Each runtime is a failure domain. "
        "A crash, resource leak, or runaway eval in one runtime does "
        "not affect another."]
       [:li [:strong "Predictable."] " No hidden threads, no implicit "
        "I/O, no ambient state. The host controls what happens and when."]
       [:li [:strong "Small."] " A narrow core that does a few things "
        "well. Complexity lives in mino code and host code, not in "
        "the runtime."]]

      [:h2 "Core design rules"]

      [:h3 "Values vs. resources"]
      [:p "User code works with values: numbers, strings, symbols, "
       "keywords, and persistent collections. Values are immutable, "
       "comparable, printable, and safe to pass around freely."]
      [:p "Resources (file handles, sockets, database connections) "
       "belong to the host. The host wraps them in opaque handles "
       "and exposes controlled operations to mino code. When user "
       "code needs to talk to the outside world, it goes through "
       "host-provided primitives."]

      [:h3 "No ambient state"]
      [:p "A fresh runtime has no I/O capabilities, no filesystem "
       "access, and no way to affect the outside world. The host "
       "opts in to capabilities explicitly. This is not a restriction "
       "layered on top; it is the default."]

      [:h3 "Explicit ownership"]
      [:p "Values returned by the runtime are borrowed by default: "
       "valid until the next allocation that may trigger garbage "
       "collection. The host retains values across GC boundaries by "
       "creating explicit references. This keeps the common case "
       "fast (no reference counting, no barriers) and makes the "
       "ownership contract visible in the code."]

      [:h3 "Runtime isolation"]
      [:p "Each " [:code "mino_state_t"] " is a complete, independent "
       "runtime. Two runtimes in the same process share nothing. "
       "There are no global variables, no shared caches, no implicit "
       "coordination. This makes runtimes safe to use from different "
       "threads (one thread per runtime) and safe to create and "
       "destroy at will."]

      [:h3 "Concurrency between runtimes, not within"]
      [:p "A single runtime is single-threaded. Concurrency happens "
       "between runtimes via message passing. The host creates "
       "runtimes, sends messages between them, and controls "
       "scheduling. This eliminates shared-state concurrency bugs "
       "by construction."]

      [:h3 "No cross-runtime sharing"]
      [:p "Values cannot be shared between runtimes. To move a value "
       "from one runtime to another, the host clones it. The clone "
       "is a deep copy allocated in the destination runtime's heap. "
       "Only data values (numbers, strings, collections) are "
       "transferable. Functions, environments, and mutable references "
       "cannot cross runtime boundaries."]

      [:h3 "Message purity"]
      [:p "Messages between runtimes are pure data. The serialization "
       "boundary at the mailbox enforces this: if a value contains a "
       "function or a mutable reference, the send fails. This "
       "guarantees that message passing cannot create hidden "
       "dependencies between runtimes."]

      [:h3 "Host controls orchestration"]
      [:p "The host decides when code runs, how long it runs, what "
       "capabilities it has, and when it stops. Runtimes do not "
       "spawn threads, open sockets, or perform I/O on their own. "
       "The host is always in control. This is essential for "
       "embedding: the host application has its own event loop, its "
       "own threading model, and its own resource constraints."]

      [:h3 "Small trusted core"]
      [:p "The C implementation provides the irreducible core: the "
       "reader, evaluator, printer, garbage collector, persistent "
       "data structures, and a small set of primitive operations. "
       "Everything else is built in mino code on top of these "
       "primitives. The standard library is a mino file loaded at "
       "startup, not compiled into the binary."]

      [:h2 "Trade-offs we chose"]
      [:ul
       [:li [:strong "Isolation over shared memory."] " Runtimes "
        "cannot share objects. This means copying data between "
        "runtimes, which costs time and memory. The benefit is that "
        "each runtime is simple, self-contained, and safe to use "
        "from any thread."]
       [:li [:strong "Explicit ownership over convenience."] " The "
        "host must ref values it wants to keep across GC boundaries. "
        "This is more work than garbage-collecting everything "
        "automatically, but it makes the ownership contract clear "
        "and avoids subtle retention bugs."]
       [:li [:strong "Copying over unsafe sharing."] " Message "
        "passing clones values at the boundary. This is slower than "
        "passing pointers, but it eliminates an entire class of "
        "concurrency bugs. For the message sizes typical in embedded "
        "scripting (configuration, commands, small data), the cost "
        "is negligible."]
       [:li [:strong "Host control over VM autonomy."] " The runtime "
        "never acts on its own. It does not spawn threads, schedule "
        "timers, or perform background work. This gives the host "
        "complete authority over resource usage and scheduling, "
        "which matters in resource-constrained environments like "
        "game engines and embedded systems."]]

      [:h2 "What we rejected and why"]
      [:ul
       [:li [:strong "Shared-memory concurrency."] " Locks, "
        "mutexes, and shared mutable state inside the language "
        "runtime create bugs that are hard to find and harder to "
        "fix. The isolation model avoids them entirely."]
       [:li [:strong "Software transactional memory."] " STM "
        "assumes a shared-memory threading model. In mino, there "
        "is no shared memory between runtimes, so STM has nothing "
        "to coordinate. Atoms provide single-runtime mutation; "
        "cross-runtime coordination uses message passing."]
       [:li [:strong "Implicit GC assumptions."] " Many runtimes "
        "assume the host will not hold pointers across allocations. "
        "mino makes the contract explicit: values are borrowed, "
        "refs are retained. The host decides what lives and what "
        "dies."]
       [:li [:strong "Ambient capabilities."] " Many scripting "
        "runtimes start with full access to the filesystem, network, "
        "and process environment. mino starts with nothing. The host "
        "must explicitly grant each capability. This is safer for "
        "plugin systems, rules engines, and any context where user "
        "code is not fully trusted."]]

      [:h2 "What mino is not"]
      [:ul
       [:li [:strong "Not a general-purpose application runtime."]
        " mino is designed to be embedded inside another program. "
        "The standalone REPL is a development convenience, not the "
        "primary use case."]
       [:li [:strong "Not designed for shared-memory parallelism."]
        " Concurrency happens between isolated runtimes. If you "
        "need threads operating on shared data structures, mino is "
        "not the right tool."]
       [:li [:strong "Not self-hosting."] " The implementation "
        "language is C through v1.0. Self-hosting would undermine "
        "the embedding pitch: hosts link a C library, not a "
        "bootstrapped compiler."]])))
