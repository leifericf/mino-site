(ns mino-site.content.performance
  "Performance characteristics page content."
  (:require
    [hiccup2.core :as h]))

(def ^:private bench-base
  "https://github.com/leifericf/mino-bench/blob/main/")

(defn- src-link
  "Render a small \"(source)\" pointer to a bench file path."
  [path]
  [:a.src-link {:href (str bench-base path)
                :style "font-size: 0.85em; margin-left: 0.5em; font-weight: normal;"}
   "(source)"])

(defn performance-page
  "Generates the Performance page HTML body."
  []
  (str
    (h/html
      [:h1 "Performance"]

      [:p.banner
       "Numbers below were measured on Apple Silicon (arm64-darwin) "
       "under normal desktop load. Different hardware will shift "
       "absolute numbers; the ratios between rows are the useful "
       "signal. The full bench suite lives in "
       [:a {:href "https://github.com/leifericf/mino-bench/tree/main/benchmarks"}
        "mino-bench/benchmarks/"]
       " and the in-process / cold-start / footprint harnesses live in "
       [:a {:href "https://github.com/leifericf/mino-bench/tree/main/tests"}
        "mino-bench/tests/"]
       ". Every table on this page links to the bench file the row was "
       "measured against."]

      [:p "mino's evaluator is now a layered system. The tree-walker "
       "remains as the ground-truth interpreter; on top of it sits a "
       "small register-based bytecode VM that compiles function bodies "
       "lazily on first call. The compiler bails to the tree-walker on "
       "any unsupported form, so behavior stays identical and the VM is "
       "additive. The numbers below reflect this layered shape, not a "
       "single-path rewrite."]

      [:h2 "Footprint"
       (src-link "tests/min_embed.c")
       (src-link "tests/min_embed_floor.c")]
      [:p "Three binary footprints worth knowing about: the Floor "
       "tier that an embedder commits to, the Sandbox tier with the "
       "canonical Clojure surface, and the Standalone ceiling that "
       "ships from Homebrew. All linked with "
       [:code "-ffunction-sections -Wl,--gc-sections"]
       " (Mach-O: " [:code "-Wl,-dead_strip"]
       ") so unreferenced subsystems drop out at link time, and "
       "stripped with " [:code "strip --strip-all"] "."]
      [:p "Each tier ships in two flavours: the JIT-free build "
       "(matches the parallel " [:code "mino-lean"] " binary; embed "
       "with " [:code "MINO_CPJIT"] " undefined) and the JIT-included "
       "build (the default Homebrew bottle). Both columns are "
       "stripped, dead-section-eliminated builds against the same "
       "C source tree."]
      [:table
       [:thead
        [:tr [:th "Build"] [:th "No JIT"] [:th "+ JIT"] [:th "JIT cost"]
         [:th "What's in it"]]]
       [:tbody
        [:tr [:td "Floor (" [:code "install_minimal"] " only)"]
             [:td "~601 KB"] [:td "~651 KB"] [:td "+50 KB (8%)"]
             [:td [:code "mino_state_new"] " + "
                  [:code "mino_install_minimal"] " + "
                  [:code "mino_eval_string"] ". Reader, evaluator, GC, "
                  "persistent collections, numeric ops, foundational "
                  "macros. No " [:code "core.clj"] " evaluation, no "
                  "regex / bignum / multimethods / protocols / "
                  "transducers, no I/O."]]
         [:tr [:td "Sandbox (" [:code "install_sandbox"] ")"]
             [:td "~909 KB"] [:td "~943 KB"] [:td "+34 KB (4%)"]
             [:td "Floor plus regex, bignum, multimethods, protocols, "
                  "transducers, and the safe bundled libs: every name "
                  "a Clojure scripter expects. Still no I/O, FS, "
                  "processes, STM, agents, async."]]
        [:tr [:td "Standalone (" [:code "install_all"] " + REPL)"]
             [:td "~962 KB"] [:td "~996 KB"] [:td "+34 KB (4%)"]
             [:td "Sandbox plus I/O, FS, " [:code "subprocess"]
                  ", STM, agents, async, host-interop, all bundled "
                  [:code "clojure.*"]
                  " namespaces, the project resolver, the task/deps "
                  "machinery, and the REPL crash handler. The "
                  "released " [:code "mino"]
                  " binary an end user receives from Homebrew. The "
                  "no-JIT Standalone column is the " [:code "mino-lean"]
                  " sibling binary."]]]]
      [:p "JIT adds 34-50 KB across the tiers: under one percent of "
       "any modern device's disk budget, and well under 1 ms of "
       "additional disk-load time on a cold launch. The JIT-included "
       "build pays back 1.8-6.5x on compute-bound hot code (see the "
       [:a {:href "/documentation/jit/"} "JIT page"]
       " for the workload table). Embedders running one-shot scripts "
       "on Floor get marginal value from JIT (no hot loops to "
       "amortize against); embedders on Sandbox or Standalone with "
       "any sustained execution should keep it on."]
      [:p "Source-side numbers for what an in-tree embedder pulls in:"]
      [:table
       [:thead
        [:tr [:th "Item"] [:th "Size"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "C source tree (" [:code "src/"] " minus vendor "
              "and the generated bundled-source headers)"]
             [:td "~3.4 MB"]
             [:td "Ordinary runtime, VM, JIT, prims, GC, collections"]]
        [:tr [:td "Vendor (" [:code "imath"] " for BigInt)"]
             [:td "~158 KB"]
             [:td "Only loaded when arithmetic exceeds 64-bit range"]]
        [:tr [:td "Bundled stdlib source (" [:code "clojure.*"] " "
              "headers compiled into the binary)"]
             [:td "~244 KB"]
             [:td "Lazy-installed; the minimum-embed build drops these"]]
        [:tr [:td [:code "core.clj"] " source"]
             [:td "~121 KB"]
             [:td "Embedded as a C string literal; evaluated the first "
                  "time " [:code "mino_install"]
                  " brings in a non-floor capability"]]]]

      [:h2 "Cold startup"
       (src-link "tests/refresh_perf.clj")]
      [:p "Wall time from " [:code "fork+exec"] " to process exit. Each "
       "row is the median of 50 invocations after three warmup runs to "
       "prime the OS page cache. Both columns evaluate the same "
       [:code "(+ 1 2)"]
       " expression; the difference between the two is the cost of "
       "linking the JIT pipeline in (a small mmap + symbol-table init "
       "at " [:code "mino_state_new"] " time, with no actual "
       "compilation triggered by the one-shot expression)."]
      [:table
       [:thead
        [:tr [:th "Tier"]
         [:th "No JIT"] [:th "+ JIT"] [:th "JIT cost"]
         [:th "Notes"]]]
       [:tbody
        [:tr [:td "Floor (" [:code "install_minimal"] ")"]
             [:td "3.98 ms"] [:td "4.01 ms"] [:td "~0"]
             [:td "Process spawn + " [:code "mino_state_new"] " + "
                  [:code "mino_install_minimal"] " + eval + exit. No "
                  [:code "core.clj"] " parse / eval."]]
        [:tr [:td "Sandbox ("
              [:code "install_sandbox"] ")"]
             [:td "6.99 ms"] [:td "6.99 ms"] [:td "0"]
             [:td "Floor + regex + bignum + multimethods + protocols "
                  "+ transducers + the safe bundled libs. Parses and "
                  "evaluates " [:code "core.clj"] " at install."]]
        [:tr [:td "Standalone (" [:code "./mino -e ..."] ")"]
             [:td "8.09 ms"] [:td "8.04 ms"] [:td "~0"]
             [:td "Sandbox plus I/O, FS, processes, STM, agents, async, "
                  "bundled " [:code "clojure.*"] ". The Homebrew binary. "
                  "No-JIT column is the " [:code "mino-lean"] " sibling."]]]]
      [:p "JIT contributes essentially zero to cold start. The "
       "pipeline initializes an mmap'd page at "
       [:code "mino_state_new"] " (sub-millisecond), and the hot-call "
       "threshold means " [:em "nothing compiles"] " until user code "
       "calls a function past " [:code "MINO_JIT_THRESHOLD"]
       " (default 100) times. A one-shot script that fits in a single "
       "expression therefore pays the same wall-time on JIT and "
       "no-JIT builds."]
      [:p "Per-process initialization cost, measured in-process over 50 "
       "init/teardown cycles inside one binary (no fork/exec overhead). "
       "An embedder that creates one runtime up-front pays this once; "
       "an embedder that spins one runtime per request sees it on "
       "every call."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Median"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_minimal"] " + "
              [:code "mino_state_free"]]
             [:td "0.18 ms"]
             [:td "Floor tier. No " [:code "core.clj"] " parse / eval."]]
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_sandbox"]
              " + " [:code "mino_state_free"]]
             [:td "2.65 ms"]
             [:td "Sandbox tier. Equivalent to "
                  [:code "mino_install(S, env, MINO_CAP_DEFAULT)"]
                  ". Parses and evaluates "
                  [:code "core.clj"] " with regex, bignum, "
                  "multimethods, protocols, transducers, and the safe "
                  "bundled libs enabled."]]
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_all"] " + " [:code "mino_state_free"]]
             [:td "2.78 ms"]
             [:td "Adds I/O, FS, STM, agents, bundled "
                  [:code "clojure.*"]
                  " registration (lazy; not evaluated until "
                  [:code "require"] "d). The standalone CLI path."]]]]
      [:p "The Floor tier saves ~2.5 ms on every cold start by skipping "
       [:code "core.clj"] " evaluation. The cost is that "
       "capability-gated names (e.g. " [:code "re-find"] ", "
       [:code "defmulti"] ", " [:code "slurp"]
       ") are not bound; user code calling them raises an MNS002 "
       "capability-disabled diagnostic until the host installs the "
       "corresponding capability."]

      [:h2 "Core operations"
       (src-link "benchmarks/micro_bench.clj")
       (src-link "benchmarks/eval_bench.clj")]
      [:p "Per-call cost for fundamental eval shapes, measured through "
       "the full read + eval path via the bytecode VM (mean of 100,000 "
       "iterations, dominant fast-path). Lower is better."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Primitive call " [:code "(+ 1 2)"]]
             [:td "5.5 µs"]
             [:td "Fused int-add fast lane, no boxing"]]
        [:tr [:td "User fn call (1 arg)"]
             [:td "5.5 µs"]
             [:td "Compiled to bytecode; register-window entry"]]
        [:tr [:td "User fn call (3 args)"]
             [:td "5.8 µs"]
             [:td "Cost grows ~0.1 µs per arg in the bc path"]]
        [:tr [:td "Vector literal " [:code "[1 2 3]"]]
             [:td "5.0 µs"]
             [:td "32-way trie allocation"]]
        [:tr [:td "Map literal " [:code "{:a 1}"]]
             [:td "5.1 µs"]
             [:td "HAMT insertion per key"]]
        [:tr [:td [:code "(get m k)"] " on 100-key map"]
             [:td "5.8 µs"]
             [:td "Hash + HAMT traversal"]]
        [:tr [:td "Symbol resolution (local " [:code "let"] ")"]
             [:td "4.7 µs"]
             [:td "Register read; no env walk"]]
        [:tr [:td "Symbol resolution (global var)"]
             [:td "4.9 µs"]
             [:td "Inline-cache hit on the bc " [:code "GETGLOBAL"]
                  " slot"]]
        [:tr [:td "Closure capture (1 var)"]
             [:td "4.9 µs"]
             [:td "Env-chain extend + restore"]]
        [:tr [:td "Closure capture (5 vars)"]
             [:td "5.0 µs"]
             [:td "Captures scale below noise"]]]]

      [:h2 "Bulk operations"
       (src-link "benchmarks/vec_bench.clj")
       (src-link "benchmarks/map_bench.clj")
       (src-link "benchmarks/micro_bench.clj")]
      [:p "Cost of working with collections at scale. The fused "
       "counted-loop opcodes (" [:code "OP_LOOP_INT_DEC"] " et al.) "
       "and the int-fast-lane opcodes are responsible for most of the "
       "movement here since the last bench."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Per element"]]]
       [:tbody
        [:tr [:td [:code "(into [] (range 100))"]]
             [:td "29.8 µs"]
             [:td "0.30 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 100))"]]
             [:td "12.6 µs"]
             [:td "0.13 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 1000))"]]
             [:td "13.9 µs"]
             [:td "0.014 µs"]]
        [:tr [:td [:code "loop/recur"] " 1,000 iterations"]
             [:td "5.5 µs"]
             [:td "0.006 µs"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "73 µs"]
             [:td "0.007 µs"]]
        [:tr [:td "Build 100-key map (" [:code "assoc"] " loop)"]
             [:td "282 µs"]
             [:td "2.82 µs/key"]]
        [:tr [:td [:code "conj"] " 1,000-element vector"]
             [:td "183 µs"]
             [:td "0.18 µs/elt"]]
        [:tr [:td [:code "conj"] " 10,000-element vector"]
             [:td "2.29 ms"]
             [:td "0.23 µs/elt"]]
        [:tr [:td [:code "nth"] " random on 1,000-vec"]
             [:td "5.5 µs"]
             [:td "n/a"]]
        [:tr [:td [:code "(get m k)"] " on 1,000-key map"]
             [:td "5.4 µs"]
             [:td "n/a"]]
        [:tr [:td [:code "(fib 25)"] " recursive (~242k calls)"]
             [:td "6.65 ms"]
             [:td "0.027 µs/call"]]]]
      [:p "Tight integer loops run at near-native speed when the "
       "compiler can prove the iteration is int-typed. "
       [:code "loop/recur"] " over 10,000 iterations clocks in "
       "around 7 ns per step because the fused-loop opcode "
       "collapses the test/dec/back-jump into a single dispatch "
       "with two tagged-int checks. Recursive Fibonacci sees the "
       "JIT compile the recursion hot path and reaches roughly 27 "
       "ns per call."]

      [:h2 "Eager collection builders"
       (src-link "benchmarks/micro_bench.clj")
       (src-link "benchmarks/lazy_bench.clj")]
      [:p "When laziness is not needed, " [:code "rangev"] ", "
       [:code "mapv"] ", and " [:code "filterv"] " produce vectors "
       "directly in C, bypassing thunk allocation entirely. The bc "
       "compiler also recognizes " [:code "reduce"] " over a vector "
       "and dispatches straight to the C primitive walker."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"]
             [:th "vs. lazy equivalent"]]]
       [:tbody
        [:tr [:td [:code "(rangev 100)"]]
             [:td "2.8 µs"]
             [:td "10× faster than " [:code "(into [] (range 100))"]]]
        [:tr [:td [:code "(mapv inc (rangev 100))"]]
             [:td "15.2 µs"]
             [:td "Eliminates per-element thunk + cons"]]
        [:tr [:td [:code "(filterv odd? (rangev 100))"]]
             [:td "13.8 µs"]
             [:td "Same shape as " [:code "mapv"]]]]]
      [:p "Use " [:code "rangev"] " for data generation and "
       [:code "reduce"] " over vectors for the biggest wins. The "
       "speedup over lazy comes from skipping thunk allocation and "
       "eval overhead per element; once per-element work is dominated "
       "by a user fn the gap narrows."]

      [:h2 "Concurrency"
       (src-link "benchmarks/async_bench.clj")]
      [:p "Standalone mino grants " [:code "cpu_count"] " worker threads "
       "at startup, so " [:code "future"] ", " [:code "promise"]
       ", " [:code "thread"] ", and the blocking channel ops "
       [:code "<!!"] "/" [:code ">!!"] "/" [:code "alts!!"] " resolve to "
       "real OS threads. Embedders start at one (single-threaded) and "
       "raise the limit via " [:code "mino_set_option(S, MINO_OPT_THREAD_LIMIT, n)"] " or one "
       "of the pool/factory grants. A runtime that has at least one "
       "live worker serializes script execution on a per-state "
       "recursive mutex; cross-state work runs fully concurrent and "
       "intra-state work is naturally race-free. Single-threaded states "
       "skip the mutex entirely and pay no lock cost."]
      [:p "core.async numbers from the current bench run:"]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "offer!"] "/" [:code "poll!"] " on "
              [:code "(chan 1024)"] " (no scheduler)"]
             [:td "281 µs/op"]
             [:td "Buffer + offer/poll data-structure cost only"]]
        [:tr [:td [:code "offer!"] " on full buffer returns false"]
             [:td "117 µs/op"]
             [:td "Hot rejection path"]]
        [:tr [:td [:code "poll!"] " on empty buffer returns nil"]
             [:td "67 µs/op"]
             [:td "Hot empty-buffer path"]]
        [:tr [:td [:code "put!"] "/" [:code "take!"] " on "
              [:code "(chan 1)"] " + " [:code "drain!"]]
             [:td "363 µs/op"]
             [:td "Callback path through the scheduler"]]
        [:tr [:td [:code "go"] " block " [:code "(<! ch)"]
              " with pending put + " [:code "drain!"]]
             [:td "1.84 ms/op"]
             [:td "IOC state machine + park/unpark roundtrip"]]
        [:tr [:td [:code "go"] " producer/consumer hand-shake pair"]
             [:td "3.67 ms/op"]
             [:td "Two park/unpark cycles end-to-end"]]
        [:tr [:td [:code "alts!"] " over 1 ready channel"]
             [:td "405 µs/op"]
             [:td "Arbitration on a single ready candidate"]]
        [:tr [:td [:code "alts!"] " over 8 channels, last ready"]
             [:td "1.03 ms/op"]
             [:td "Linear walk through " [:code ":priority"] " order"]]
        [:tr [:td [:code "alts!"] " with " [:code ":default"]]
             [:td "159 µs/op"]
             [:td "Fast non-block path"]]
        [:tr [:td [:code "(timeout 0)"] " + " [:code "take!"]
              " + drain"]
             [:td "692 µs/op"]
             [:td "Timer-chan path through the scheduler"]]]]
      [:p "Shared-state work scales to one core's worth of throughput "
       "regardless of worker count because the per-state recursive "
       "mutex serializes script execution. To get parallel speedup, "
       "distribute work across runtime instances and pass results back "
       "via the host or via " [:code "mino_clone"] ", not across "
       "workers in one runtime."]

      [:h2 "Garbage collection"
       (src-link "benchmarks/realistic_bench.clj")]
      [:p "mino uses a non-moving two-generation tracing collector. "
       "Short-lived values live in a young-gen nursery that is swept "
       "in bounded minor collections. Survivors are promoted to "
       "old-gen, which is marked incrementally, paced by the "
       "allocator. A write barrier records old-to-young pointers so "
       "minors stay proportional to young reachability. The collector "
       "is stop-the-world at slice boundaries; there are no collector "
       "threads."]
      [:p "GC share is a function of allocation pressure, not "
       "absolute speed. The bytecode VM cut the constant-factor cost "
       "of computation but left allocation rates largely unchanged, so "
       "GC share rose proportionally on the same workloads:"]
      [:table
       [:thead
        [:tr [:th "Workload"] [:th "GC share"] [:th "Max pause"]]]
       [:tbody
        [:tr [:td "Small function calls (empty, identity, let)"]
             [:td "~12%"]
             [:td "~1.4 ms"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "~0%"]
             [:td "n/a"]]
        [:tr [:td "Build 1,000-element vector via " [:code "conj"]]
             [:td "~19%"]
             [:td "~1.4 ms"]]
        [:tr [:td "Build 10,000-element vector via " [:code "conj"]]
             [:td "~21%"]
             [:td "~7.8 ms"]]
        [:tr [:td "Build 5k int-map and sum"]
             [:td "~16%"]
             [:td "~1.8 ms"]]
        [:tr [:td "map/filter/map/reduce over 50,000 (fused transducers)"]
             [:td "~0%"]
             [:td "n/a"]]
        [:tr [:td "Nested vectors 500x100"]
             [:td "~17%"]
             [:td "~2.0 ms"]]
        [:tr [:td "Realize 10k of lazy range"]
             [:td "~33%"]
             [:td "~4.0 ms"]]]]
      [:p "Five tuning knobs are exposed through "
       [:code "mino_gc_set_param"] ": nursery size, major growth "
       "multiplier, promotion age, incremental slice budget, and "
       "allocation quantum between slices. The defaults target "
       "interactive latency on a general workload; embedders with "
       "throughput-dominated batches or tighter pause budgets can "
       "shift the tradeoff without rebuilding."]
      [:p "The default nursery size is 8 MiB. Allocation-heavy "
       "workloads benefit from the larger young gen because each "
       "minor cycle sweeps more bytes before promotion pressure "
       "builds, so total GC wall time falls even though each minor "
       "pass walks more. The realistic_bench rows where nursery size "
       "matters most are allocation-heavy pipelines:"
       [:a {:href "https://github.com/leifericf/mino-bench/blob/main/benchmarks/realistic_bench.clj"}
        "realistic_bench"]
       " carries the per-row methodology. Each VM state holds more "
       "young-gen residency before the first major GC; embedders "
       "running many concurrent VM states under tight memory budgets "
       "override via "
       [:code "MINO_GC_NURSERY_BYTES"] " or "
       [:code "mino_gc_set_param(S, MINO_GC_NURSERY_BYTES, n)"] "."]

      [:h2 "Where the time goes"]
      [:p "The cost centers in order of impact:"]
      [:ul
       [:li [:strong "Allocation pressure."]
        " Persistent collections, cons cells, and intermediate seqs "
        "are the dominant source of work in any realistic pipeline. "
        "Recent " [:code "reduce"] " and "
        [:code "assoc"] "/identity short-circuits cut redundant "
        "allocation, but laziness still pays a thunk + cons-cell per "
        "element. Use " [:code "rangev"] "/" [:code "mapv"] "/"
        [:code "filterv"] " when laziness is not needed; use "
        [:code "loop/recur"] " when iterating without building a "
        "collection."]
        [:li [:strong "Core library initialization."]
         " A " [:code "mino_state"] " on the Sandbox or Standalone "
         "tier parses and evaluates " [:code "core.clj"]
        " (~5.2 ms here, smaller on faster hardware). The Floor tier "
        "(" [:code "mino_install_minimal"] ") skips this entirely "
        "and pays only ~0.22 ms. Parsed forms are cached per state, "
        "so additional envs in one state avoid re-parsing. Bundled "
        [:code "clojure.*"] " namespaces are registered but not "
        "evaluated until " [:code "require"] "d."]
       [:li [:strong "Bytecode bailouts."]
        " Forms the bc compiler doesn't yet handle bail to the "
        "tree-walker on first call and are remembered as declined; "
        "subsequent calls skip recompilation but still pay "
        "tree-walker per-call cost. Compiler coverage is the lever "
        "here, not VM speed."]
       [:li [:strong "Per-state lock at every eval entry "
              "(when threaded)."]
        " Once a state is multi-threaded (host has granted workers, "
        "or the standalone has run " [:code "mino_install_all"] "), "
        "each script entry through " [:code "mino_eval_string"] " or "
        "a worker " [:code "mino_call"] " takes the per-state "
        "recursive mutex. Uncontested cost is ~10 ns per entry; "
        "single-threaded states skip the mutex entirely."]]

      [:h2 "What this means in practice"]
      [:p "mino is fast enough for configuration evaluation, rules "
       "engines, interactive consoles, plugin systems, scripting "
       "automation, and data transformation on moderate-sized "
       "collections. It evaluates a simple expression in low "
       "microseconds and processes hundreds of thousands of elements "
       "per millisecond on tight integer loops."]
      [:p "It is still not the right choice for tight numerical loops "
       "in the hot inner cycle of a server, large-scale data "
       "processing, or any workload where per-element overhead "
       "matters at the nanosecond level. For those cases, do the "
       "heavy lifting in C and pass results to mino for composition "
       "and coordination. The embedding model supports this naturally."]

      [:h2 "Benchmarking"]
      [:p "Write benchmarks as mino scripts or C programs that "
       "link against the mino source. Compile and run with the "
       "same per-subsystem flags the standalone build uses:"]
      [:pre [:code
"cc -std=c99 -O2 \\
  -Isrc -Isrc/public -Isrc/runtime -Isrc/gc -Isrc/eval \\
  -Isrc/values -Isrc/collections -Isrc/prim \\
  -Isrc/async -Isrc/interop \\
  -Isrc/diag -Isrc/vendor/imath \\
  -o my_bench my_bench.c \\
  src/public/*.c src/runtime/*.c src/gc/*.c src/eval/*.c \\
  src/eval/bc/*.c src/eval/bc/jit/*.c \\
  src/values/*.c src/collections/*.c src/prim/*.c \\
  src/async/*.c src/interop/*.c src/regex/*.c \\
  src/diag/*.c src/vendor/imath/*.c \\
  -lm -lpthread
./my_bench"]]
      [:p "For minimum-footprint embed measurements, add "
       [:code "-ffunction-sections -fdata-sections"]
       " to the compile flags and "
       [:code "-Wl,--gc-sections"]
       " to the link flags so unreferenced subsystems drop out."])))
