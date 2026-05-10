(ns mino-site.content.performance
  "Performance characteristics page content."
  (:require
    [hiccup2.core :as h]))

(defn performance-page
  "Generates the Performance page HTML body."
  []
  (str
    (h/html
      [:h1 "Performance"]

      [:p.banner
       "Per-call cost numbers below were measured against mino v0.92.0 "
       "on an Apple M3 Pro (6 performance cores plus 6 efficiency "
       "cores) under normal desktop load. Treat them as directional. "
       "v0.104.0 closed a non-JIT performance cycle that cut "
       "per-op cost by an average of about 24 percent across the "
       "microbenchmark gate, and dropped a tight integer "
       [:code "loop/recur"] " bench from 941 ms to 375 ms (about a "
       "60 percent reduction). Allocation shape per op is unchanged; "
       "the gains come from cutting fixed eval-side overhead. The "
       "shape relationships in the tables still hold; a full rebench "
       "is queued."]

      [:p "mino is a tree-walking interpreter. There is no bytecode "
       "compiler, no JIT, and no dispatch optimization beyond the C "
       "compiler's own work. The numbers below reflect that. They are "
       "included to set honest expectations and to show where the cost "
       "centers are, not to claim speed."]

      [:h2 "Core operations"]
      [:p "Per-call cost for fundamental eval shapes, measured through "
       "the full read plus eval path. Numbers are v0.104.0 perf-gate "
       "baselines (median of three runs, " [:code "dotimes [_ N]"]
       " amortized inside one call to "
       [:code "(time ...)"] "). Lower is better."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Primitive call " [:code "(+ 1 2)"]]
             [:td "3.4 µs"]
             [:td "Inline call cache hit, int+int fast lane, "
              "no cons-spine build"]]
        [:tr [:td "User fn call (1 arg)"]
             [:td "3.0 µs"]
             [:td "Shape-pre-compiled binding, env child"]]
        [:tr [:td "User fn call (3 args)"]
             [:td "3.5 µs"]
             [:td "Cost grows about 0.2 µs per arg"]]
        [:tr [:td "Vector literal " [:code "[1 2 3]"]]
             [:td "3.2 µs"]
             [:td "32-way trie allocation"]]
        [:tr [:td "Map literal " [:code "{:a 1 :b 2}"]]
             [:td "3.2 µs"]
             [:td "HAMT insertion per key"]]
        [:tr [:td [:code "(get m k)"] " on 100-key map"]
             [:td "4.9 µs"]
             [:td "Hash plus HAMT traversal"]]
        [:tr [:td [:code "(read-string \"42\")"]]
             [:td "3.4 µs"]
             [:td "Tokenize plus parse"]]
        [:tr [:td [:code "(read-string \"(+ 1 2 3)\")"]]
             [:td "3.8 µs"]
             [:td "Cons-list construction during read"]]
        [:tr [:td "Symbol/keyword resolution"]
             [:td "3.0 µs"]
             [:td "Eval shell, intern hash hit, hashed env probe"]]
        [:tr [:td [:code "(re-find re s)"] " short string"]
             [:td "4.3 µs"]
             [:td "Compiled pattern, capture groups, "
              "backtracking matcher"]]]]

      [:h2 "Bulk operations"]
      [:p "Cost of working with collections at scale. These show where "
       "interpreter overhead compounds. v0.104.0 numbers, measured "
       "the same way the bench harness does (1000 calls amortized; "
       "median of three runs)."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Per element"]]]
       [:tbody
        [:tr [:td [:code "(into [] (range 100))"]]
             [:td "24 µs"]
             [:td "0.24 µs"]]
        [:tr [:td [:code "(into [] (range 1000))"]]
             [:td "386 µs"]
             [:td "0.39 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 100))"]]
             [:td "6 µs"]
             [:td "0.06 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 1000))"]]
             [:td "7 µs"]
             [:td "0.007 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 10000))"]]
             [:td "9 µs"]
             [:td "0.0009 µs"]]
        [:tr [:td "Build 100-key map via " [:code "loop/recur"]]
             [:td "174 µs"]
             [:td "1.74 µs/key"]]
        [:tr [:td [:code "loop/recur"] " 1,000 iterations"]
             [:td "212 µs"]
             [:td "0.21 µs"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "2.3 ms"]
             [:td "0.23 µs"]]
        [:tr [:td [:code "(fib 20)"] " (~21k recursive calls)"]
             [:td "9.0 ms"]
             [:td "0.43 µs/call"]]
        [:tr [:td [:code "(fib 25)"] " (~242k recursive calls)"]
             [:td "96 ms"]
             [:td "0.40 µs/call"]]]]
      [:p "The three " [:code "(reduce + 0 (range N))"] " rows are "
       "the same shape and almost the same cost because the int-range "
       "fast path that landed in v0.104.0 walks the integer range in "
       "C with overflow-aware arithmetic when the reducer is the "
       "canonical " [:code "+"] " primitive. The reduction never "
       "materializes thunks or cons cells; the result drops out of a "
       "tight C loop."]

      [:h2 "Eager collection builders"]
      [:p "When laziness is not needed, " [:code "rangev"] ", "
       [:code "mapv"] ", and " [:code "filterv"] " produce vectors "
       "directly in C, bypassing thunk allocation entirely."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Per element"]
             [:th "vs. lazy equivalent"]]]
       [:tbody
        [:tr [:td [:code "(rangev 100)"]]
             [:td "3.3 µs"]
             [:td "0.033 µs"]
             [:td "7× faster than " [:code "(into [] (range 100))"]]]
        [:tr [:td [:code "(rangev 1000)"]]
             [:td "55 µs"]
             [:td "0.055 µs"]
             [:td "7× faster than " [:code "(into [] (range 1000))"]]]
        [:tr [:td [:code "(reduce + 0 (rangev 1000))"]]
             [:td "160 µs"]
             [:td "0.16 µs"]
             [:td "Slower than " [:code "(reduce + 0 (range 1000))"]
              ", which now hits the int-range fast path"]]
        [:tr [:td [:code "(mapv inc (rangev 1000))"]]
             [:td "350 µs"]
             [:td "0.35 µs"]
             [:td "Eliminates thunk plus cons cell per element"]]]]
      [:p "The eager-builder speedup comes from skipping thunk "
       "allocation and eval overhead per element. The picture for "
       [:code "reduce + (range N)"] " specifically inverted in v0.104.0: "
       "the lazy form now hits an int-range fast path that walks the "
       "range directly in C with overflow-aware arithmetic, so it is "
       "the fastest path for that shape. " [:code "rangev"] " still "
       "wins when the result needs to live as a vector for further "
       "indexed work; " [:code "mapv"] " and " [:code "filterv"]
       " still win when laziness is not the goal."]

      [:h2 "Concurrency"]
      [:p "Standalone mino grants " [:code "cpu_count"] " worker threads "
       "at startup, so " [:code "future"] ", " [:code "promise"]
       ", " [:code "thread"] ", and the blocking channel ops "
       [:code "<!!"] "/" [:code ">!!"] "/" [:code "alts!!"] " resolve to "
       "real OS threads. Embedders start at one (single-threaded) and "
       "raise the limit via " [:code "mino_set_thread_limit"] " or one "
       "of the pool/factory grants. A runtime that has at least one "
       "live worker serializes script execution on a per-state "
       "recursive mutex; cross-state work runs fully concurrent and "
       "intra-state work is naturally race-free. Single-threaded states "
       "skip the mutex entirely and pay no lock cost."]
      [:p "On the M3 Pro the OS scheduler places mino's workers on the "
       "six performance cores first; the six efficiency cores absorb "
       "additional workers when the count exceeds six."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "(future ...)"] " spawn + deref roundtrip"]
             [:td "31 µs"]
             [:td "Spawn-per-future path; pthread_create + cv_wait "
              "handshake (pthread_join runs at quiesce, not per-future)"]]
        [:tr [:td [:code "swap!"] " on shared atom (1 worker)"]
             [:td "4.4 µs/op"]
             [:td "Single-thread CAS, no contention"]]
        [:tr [:td [:code "swap!"] " on shared atom (4 workers)"]
             [:td "4.5 µs/op"]
             [:td "Per-state lock keeps total throughput flat"]]
        [:tr [:td [:code "swap!"] " on shared atom (8 workers)"]
             [:td "4.5 µs/op"]
             [:td "Same total throughput, more wakeup overhead"]]
        [:tr [:td "Unbuffered channel ping-pong (" [:code "<!!"] "/" [:code ">!!"] ")"]
             [:td "218 µs/msg"]
             [:td "Two cross-thread wakeups per handshake"]]
        [:tr [:td "Spawn 8 futures, await all"]
             [:td "226 µs"]
             [:td "Amortizes per-future cost across a fan-out batch"]]]]
      [:p "The atom-CAS rows show the central performance pattern of "
       "the per-state GIL: shared-state work scales to one core's "
       "worth of throughput regardless of worker count. To get parallel "
       "speedup, distribute work across runtime instances and pass "
       "results back via the host or via " [:code "mino_clone"] ", "
       "rather than across workers in one runtime."]

      [:h2 "Footprint and startup"]
      [:p "What an embedder is shipping or evaluating up-front."]
      [:table
       [:thead
        [:tr [:th "Item"] [:th "Size"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Stripped " [:code "mino"] " binary (arm64)"]
             [:td "~770 KB"]
             [:td "Single self-contained executable; no dynamic deps"]]
        [:tr [:td "C source tree (" [:code "src/"] " minus vendor and "
              "bundled-source headers)"]
             [:td "~1.7 MB"]
             [:td "What an embedder pulls in for an in-tree build"]]
        [:tr [:td "Vendor (" [:code "imath"] " for BigInt)"]
             [:td "~89 KB"]
             [:td "Only used when arithmetic exceeds 64-bit range"]]
        [:tr [:td "Bundled stdlib source"]
             [:td "~190 KB"]
             [:td "Compiled into the binary; no on-disk sidecar"]]]]
      [:p "Cold startup (median of 20 invocations on the M3 Pro, "
       "warm filesystem cache):"]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Wall time"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "./mino -e '(+ 1 2)'"]]
             [:td "~9 ms"]
             [:td "Process spawn, state init, eval, exit"]]
        [:tr [:td [:code "./mino -e nil"]]
             [:td "~9 ms"]
             [:td "Same path, no eval work"]]
        [:tr [:td [:code "mino_state_new"] " plus "
              [:code "mino_install_all"] " (in-process)"]
             [:td "~3.5 ms"]
             [:td "Parses core.clj, installs primitives, registers "
              "lazy bundled libs"]]]]
      [:p "Roughly a third of the cold start is "
       [:code "mino_new"] " evaluating " [:code "core.clj"]
       "; the rest is OS process spawn, dynamic loader, and exit. "
       "Embedders that create one runtime up-front pay the "
       "3.5 ms once."]

      [:h2 "Cross-state cloning"]
      [:p "Cost of deep-copying data between runtime instances. "
       [:code "mino_clone"] " is the primitive for moving immutable "
       "values across states; hosts that manage more than one "
       [:code "mino_state_t"] " (for example, one state per OS thread "
       "for true parallelism) use it at the boundary."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Clone: 5-element vector"]
             [:td "0.19 µs"]
             [:td "Deep copy, allocates in destination state"]]
        [:tr [:td "Clone: 100-element vector"]
             [:td "2.2 µs"]
             [:td "Linear in element count"]]
        [:tr [:td "Clone: nested map (2 levels, ~6 keys)"]
             [:td "1.7 µs"]
             [:td "Recursive traversal"]]
        [:tr [:td "Clone: 100-key string-keyed map"]
             [:td "29 µs"]
             [:td "Re-interns keys in destination state"]]]]

      [:h2 "Lifecycle"]
      [:p "Cost of creating and destroying runtime objects. These "
       "matter most for hosts that create many short-lived runtimes."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "mino_state_new"] " + " [:code "mino_state_free"]]
             [:td "0.8 µs"]
             [:td "Bare state with no bindings"]]
        [:tr [:td [:code "mino_new"] " (state + core + I/O)"]
             [:td "~3.5 ms"]
             [:td "Parses and evaluates core.clj"]]
        [:tr [:td [:code "mino_env_clone"]]
             [:td "2.5 µs"]
             [:td "Thin clone; values shared with the parent env"]]]]

      [:h2 "Garbage collection"]
      [:p "mino uses a non-moving two-generation tracing collector. "
       "Short-lived values live in a young-gen nursery that is swept "
       "in bounded minor collections. Survivors are promoted to "
       "old-gen, which is marked incrementally, paced by the "
       "allocator. A write barrier records old-to-young pointers so "
       "minors stay proportional to young reachability. The collector "
       "is stop-the-world at slice boundaries; there are no collector "
       "threads."]
      [:p "On realistic multi-subsystem benches the max pause sits at "
       "~19 ms under the default slice budget, with GC share between "
       "15 and 50 percent of wall clock depending on the allocation "
       "rate of the workload. Tail-heavy workloads (deeply nested "
       "lazy pipelines, large transient vectors, deep recursion) were "
       "the headline target for the incremental-major cycle; minor "
       "and major are paced together so the worst case stays bounded."]
      [:table
       [:thead
        [:tr [:th "Workload"] [:th "GC share"] [:th "Max pause"]]]
       [:tbody
        [:tr [:td "Small function calls (empty, identity, let)"]
             [:td "~7%"]
             [:td "~2 ms"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "~15%"]
             [:td "~1.3 ms"]]
        [:tr [:td "Build 1,000-element vector via " [:code "conj"]]
             [:td "~25%"]
             [:td "~4 ms"]]
        [:tr [:td "Build 10,000-element vector via " [:code "conj"]]
             [:td "~26%"]
             [:td "~7 ms"]]
        [:tr [:td "map/filter/map/reduce over 50,000"]
             [:td "~46%"]
             [:td "~19 ms"]]
        [:tr [:td "Nested vectors 500x100"]
             [:td "~41%"]
             [:td "~19 ms"]]
        [:tr [:td [:code "(fib 25)"] " (recursive)"]
             [:td "~29%"]
             [:td "~19 ms"]]]]
      [:p "Five tuning knobs are exposed through "
       [:code "mino_gc_set_param"] ": nursery size, major growth "
       "multiplier, promotion age, incremental slice budget, and "
       "allocation quantum between slices. The defaults target "
       "interactive latency on a general workload; embedders with "
       "throughput-dominated batches or tighter pause budgets can "
       "shift the tradeoff without rebuilding."]

      [:h2 "Where the time goes"]
      [:p "The cost centers in order of impact:"]
      [:ul
       [:li [:strong "Lazy sequence realization."]
        " Each element in a lazy sequence allocates a thunk, "
        "evaluates it, and produces a cons cell. This is the "
        "per-element cost in " [:code "range"] ", " [:code "map"]
        ", " [:code "filter"] ", " [:code "take"] ", and "
        [:code "concat"] ". For tight loops, " [:code "loop/recur"]
        " (about 0.23 µs per iteration in v0.104.0) is several times "
        "faster than the lazy reduce equivalent. The eager variants "
        [:code "rangev"] ", " [:code "mapv"] ", and " [:code "filterv"]
        " eliminate thunk overhead entirely when laziness is not "
        "needed. Lazy combinators " [:code "recur"] " on skip rather "
        "than allocating a thunk for every dropped element, and the "
        "chunked-seq family ("
        [:code "chunk-buffer"] " / " [:code "chunk-cons"]
        ") amortizes thunk overhead in batches of 32 when the "
        "consumer explicitly constructs a chunked seq."]
       [:li [:strong "Core library initialization."]
        " Every new " [:code "mino_state_t"] " parses and evaluates "
        [:code "core.clj"] " at " [:code "mino_install_core"]
        " (~3.5 ms). Parsed forms are cached per state, so creating "
        "additional environments within one state avoids re-parsing. "
        "The other bundled namespaces (" [:code "clojure.string"] ", "
        [:code "clojure.set"] ", " [:code "clojure.walk"] ", "
        [:code "clojure.edn"] ", and so on) are registered at "
        [:code "mino_install_all"] " but not evaluated; they only pay "
        "when a script first " [:code "require"] "s them."]
       [:li [:strong "Cons-list argument passing (residual)."]
        " The original eval path built a linked list of cons cells "
        "for every function call's arguments and walked it on the "
        "callee side to bind parameters. As of v0.104.0 the hot "
        "fixed-arity primitives (" [:code "inc"] ", " [:code "dec"]
        ", " [:code "count"] ", " [:code "first"] ", " [:code "rest"]
        ", " [:code "cons"] ", and the type predicates) take their "
        "arguments through a flat C array instead, and user "
        "functions whose parameter vector is a list of plain "
        "interned symbols dispatch through a shape-pre-compiled "
        "binding path. The variadic " [:code "+"] " " [:code "-"]
        " " [:code "*"] " " [:code "/"] " " [:code "<"] " "
        [:code "<="] " " [:code ">"] " " [:code ">="]
        " primitives also take this path for calls with three or "
        "more arguments. The cons-spine cost remains for "
        "destructuring parameter shapes, rest arguments, and "
        "non-hot prims, but it is no longer the per-call default."]
       [:li [:strong "Per-state lock at every eval entry (when threaded)."]
        " Once a state is multi-threaded (host has granted workers, or "
        "the standalone has run " [:code "mino_install_all"] "), each "
        "script entry through " [:code "mino_eval_string"] " or a "
        "worker " [:code "mino_call"] " takes the per-state recursive "
        "mutex. The uncontested cost is ~10 ns per entry, far below "
        "per-eval cost; contended throughput is bound by the lock and "
        "is the topic of the Concurrency section. Single-threaded "
        "states skip the mutex entirely."]
       [:li [:strong "Tree-walking eval."]
        " There is no intermediate representation. Each form is "
        "traversed, dispatched on type, and interpreted directly. "
        "v0.104.0 added a per-state monomorphic inline call cache "
        "keyed on the call form pointer, so calls whose head is an "
        "unqualified symbol that resolves past every local frame "
        "skip symbol lookup and var dereference on subsequent hits. "
        "Cache invalidation is generation-counter based: "
        [:code "var-set"] ", " [:code "ns-unmap"] ", and "
        [:code "var-unintern"] " all bump the counter and the next "
        "call falls through. A binary numeric fast lane handles "
        [:code "(op a b)"] " for the canonical " [:code "+"] " "
        [:code "-"] " " [:code "*"] " " [:code "="] " " [:code "<"]
        " " [:code "<="] " " [:code ">"] " " [:code ">="]
        " primitives directly through the C-builtin overflow ops "
        "when both operands are integers, skipping the numeric "
        "tower entirely. A bytecode compiler would still give a "
        "large constant-factor improvement across the board, "
        "but the cache and fast lanes recover most of the inner-loop "
        "cost without one."]]

      [:h2 "Known issues"]
      [:p "Two performance characteristics are inherent to the current "
       "architecture. Both have mitigations."]
      [:ul
       [:li [:strong "core.clj initialization (~3.5 ms per runtime)."]
        " Every new runtime instance parses and evaluates "
        [:code "core.clj"] " from the embedded C string literal. "
        "Parsed forms are cached per state, so creating additional "
        "environments within one state avoids re-parsing. Cross-state "
        "sharing is not possible because parsed forms contain "
        "state-specific interned pointers. A bytecode format would "
        "let parsed cores ride along the binary, but it doesn't exist "
        "yet. The other bundled namespaces are lazy and don't add to "
        "this cost."]
       [:li [:strong "Lazy sequence per-element overhead."]
        " Lazy-by-default sequences pay for a thunk allocation, an "
        "eval, and a cons cell on every element. The eager variants "
        [:code "rangev"] ", " [:code "mapv"] ", and "
        [:code "filterv"] " eliminate this overhead when laziness "
        "is not needed (see table above). For iteration without "
        "building a collection, " [:code "loop/recur"]
        " remains the fastest option. The chunked-seq family ("
        [:code "chunk-buffer"] " / " [:code "chunk-cons"]
        ") amortizes thunk overhead in batches of 32, and as of "
        "v0.98.3 default sources (" [:code "(seq [...])"] ", lazy "
        [:code "range"] ") auto-chunk through the same pipeline."]]

      [:h2 "What this means in practice"]
      [:p "mino is fast enough for configuration evaluation, rules "
       "engines, interactive consoles, plugin systems, and data "
       "transformation on moderate-sized collections. It evaluates "
       "a simple expression in single-digit microseconds and processes "
       "hundreds of elements per millisecond."]
      [:p "It is not the right choice for tight numerical loops, "
       "large-scale data processing, or any workload where "
       "per-element overhead matters at the microsecond level. For "
       "those cases, do the heavy lifting in C and pass results to "
       "mino for composition and coordination."]
      [:p "The embedding model supports this naturally: the host "
       "does performance-sensitive work in native code and exposes "
       "results to mino as values. mino provides the glue, the "
       "logic, and the interactivity."]

      [:h2 "Benchmarking"]
      [:p "Write benchmarks as mino scripts or C programs that "
       "link against the mino source. Compile and run with the "
       "same per-subsystem flags the standalone build uses:"]
      [:pre [:code
"cc -std=c99 -O2 \\
  -Isrc -Isrc/public -Isrc/runtime -Isrc/gc -Isrc/eval \\
  -Isrc/collections -Isrc/prim -Isrc/async -Isrc/interop \\
  -Isrc/diag -Isrc/vendor/imath \\
  -o my_bench my_bench.c \\
  src/public/*.c src/runtime/*.c src/gc/*.c src/eval/*.c \\
  src/collections/*.c src/prim/*.c src/async/*.c src/interop/*.c \\
  src/regex/*.c src/diag/*.c src/vendor/imath/*.c \\
  -lm
./my_bench"]])))
