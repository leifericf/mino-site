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
       "Numbers below were measured against mino v0.145.0 on x86_64 "
       "Linux (WSL2, kernel 6.6) under normal desktop load. Treat them "
       "as directional; different hardware will shift absolute numbers "
       "but the ratios between rows hold. The full bench suite lives "
       "in "
       [:a {:href "https://github.com/leifericf/mino-bench/tree/main/benchmarks"}
        "mino-bench/benchmarks/"]
       " and the in-process / cold-start / footprint harnesses live in "
       [:a {:href "https://github.com/leifericf/mino-bench/tree/main/tests"}
        "mino-bench/tests/"]
       ". Every table on this page links to the bench file the row was "
       "measured against — click the section heading's source link to "
       "see the actual code."]

      [:p "mino's evaluator is now a layered system. The tree-walker "
       "remains as the ground-truth interpreter; on top of it sits a "
       "small register-based bytecode VM that compiles function bodies "
       "lazily on first call. The compiler bails to the tree-walker on "
       "any unsupported form, so behavior stays identical and the VM is "
       "additive. The numbers below reflect this layered shape, not a "
       "single-path rewrite."]

      [:h2 "Footprint"
       (src-link "tests/min_embed.c")]
      [:p "Two binary footprints worth knowing about — the floor that "
       "an embedder commits to, and the ceiling that the standalone CLI "
       "ships with."]
      [:table
       [:thead
        [:tr [:th "Build"] [:th "Stripped size"] [:th "What's in it"]]]
       [:tbody
        [:tr [:td "Minimum embedded (" [:code "install_core"] " only)"]
             [:td "~666 KB"]
             [:td [:code "mino_state_new"] " + "
                  [:code "mino_install_core"] " + "
                  [:code "mino_eval_string"] ". No I/O, FS, processes, "
                  "STM, agents, no bundled stdlib. Linked with "
                  [:code "-ffunction-sections -Wl,--gc-sections"]
                  " so unreferenced subsystems drop out at link time."]]
        [:tr [:td "Full standalone (" [:code "install_all"] " + REPL)"]
             [:td "~898 KB"]
             [:td "Everything above plus I/O, FS, " [:code "subprocess"]
                  ", STM, agents, all bundled " [:code "clojure.*"]
                  " namespaces, the project resolver, the task/deps "
                  "machinery, and the REPL crash handler. This is "
                  "the released " [:code "mino"]
                  " binary an end user receives from Homebrew (or "
                  "downloads from a GitHub release; the release "
                  "tarball is ~360 KB gzip-compressed). "
                  "Stripped with " [:code "strip --strip-all"] "."]]]]
      [:p "Source-side numbers for what an in-tree embedder pulls in:"]
      [:table
       [:thead
        [:tr [:th "Item"] [:th "Size"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "C source tree (" [:code "src/"] " minus vendor)"]
             [:td "~1.6 MB"]
             [:td "Hand-written C plus generated bundled-source headers"]]
        [:tr [:td "Vendor (" [:code "imath"] " for BigInt)"]
             [:td "~150 KB"]
             [:td "Only loaded when arithmetic exceeds 64-bit range"]]
        [:tr [:td "Bundled stdlib source (" [:code "clojure.*"] " "
              "headers compiled into the binary)"]
             [:td "~147 KB"]
             [:td "Lazy-installed; the minimum-embed build drops these"]]
        [:tr [:td [:code "core.clj"] " source"]
             [:td "~113 KB"]
             [:td "Embedded as a C string literal; evaluated at "
                  [:code "mino_install_core"]]]]]

      [:h2 "Cold startup"
       (src-link "tests/refresh_perf.clj")]
      [:p "Wall time from " [:code "fork+exec"] " to process exit. Each "
       "row is the median of 50 invocations after three warmup runs to "
       "prime the OS page cache."]
      [:table
       [:thead
        [:tr [:th "Invocation"] [:th "Wall time"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Full standalone " [:code "./mino -e '(+ 1 2)'"]]
             [:td "~7.5 ms (p90 7.9 ms)"]
             [:td "Process spawn + state init + " [:code "install_all"]
                  " + eval + exit"]]
        [:tr [:td "Full standalone " [:code "./mino -e nil"]]
             [:td "~7.5 ms (p90 7.9 ms)"]
             [:td "Same path, no eval work — confirms the floor is "
                  "init, not eval"]]
        [:tr [:td "Minimum embedded " [:code "./min_embed '(+ 1 2)'"]]
             [:td "~7.0 ms (p90 7.2 ms)"]
             [:td "Process spawn + state init + " [:code "install_core"]
                  " + eval + exit; ~0.5 ms saved by skipping the "
                  "batteries-installation surface"]]]]
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
              [:code "mino_install_core"] " + " [:code "mino_state_free"]]
             [:td "~5.4 ms"]
             [:td "Parses " [:code "core.clj"]
                  ", installs primitives. The floor for any embed."]]
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_all"] " + " [:code "mino_state_free"]]
             [:td "~5.5 ms"]
             [:td "Adds I/O, FS, STM, agents, bundled "
                  [:code "clojure.*"]
                  " registration (lazy; not evaluated until "
                  [:code "require"] "d)"]]]]
      [:p "About 5.4 ms of the cold-start wall time is "
       [:code "core.clj"] " evaluation; the rest is OS process spawn, "
       "dynamic loader, and process exit. The min-embed path saves "
       "about 0.5 ms on cold start by deferring batteries until the "
       "host opts in."]

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
             [:td "1.7 µs"]
             [:td "Fused int-add fast lane, no boxing"]]
        [:tr [:td "User fn call (1 arg)"]
             [:td "1.9 µs"]
             [:td "Compiled to bytecode; register-window entry"]]
        [:tr [:td "User fn call (3 args)"]
             [:td "2.1 µs"]
             [:td "Cost grows ~0.07 µs per arg in the bc path"]]
        [:tr [:td "Vector literal " [:code "[1 2 3]"]]
             [:td "1.7 µs"]
             [:td "32-way trie allocation"]]
        [:tr [:td "Map literal " [:code "{:a 1}"]]
             [:td "1.8 µs"]
             [:td "HAMT insertion per key"]]
        [:tr [:td [:code "(get m k)"] " on 100-key map"]
             [:td "2.1 µs"]
             [:td "Hash + HAMT traversal"]]
        [:tr [:td [:code "(read-string \"42\")"]]
             [:td "2.6 µs"]
             [:td "Tokenize + parse"]]
        [:tr [:td [:code "(read-string \"(+ 1 2 3)\")"]]
             [:td "3.0 µs"]
             [:td "Cons-list construction during read"]]
        [:tr [:td "Symbol resolution (local " [:code "let"] ")"]
             [:td "1.6 µs"]
             [:td "Register read; no env walk"]]
        [:tr [:td "Symbol resolution (global var)"]
             [:td "1.6 µs"]
             [:td "Inline-cache hit on the bc " [:code "GETGLOBAL"]
                  " slot"]]]]

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
             [:td "29 µs"]
             [:td "0.29 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 100))"]]
             [:td "6.7 µs"]
             [:td "0.067 µs"]]
        [:tr [:td [:code "(reduce + 0 (range 1000))"]]
             [:td "9.4 µs"]
             [:td "0.009 µs"]]
        [:tr [:td [:code "loop/recur"] " 1,000 iterations"]
             [:td "13.2 µs"]
             [:td "0.013 µs"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "133 µs"]
             [:td "0.013 µs"]]
        [:tr [:td "Build 100-key map (" [:code "assoc"] " loop)"]
             [:td "246 µs"]
             [:td "2.46 µs/key"]]
        [:tr [:td [:code "(fib 20)"] " recursive (~21k calls)"]
             [:td "639 µs"]
             [:td "0.030 µs/call"]]
        [:tr [:td [:code "(fib 25)"] " recursive (~242k calls)"]
             [:td "7.0 ms"]
             [:td "0.029 µs/call"]]]]
      [:p "Tight integer loops run at near-native speed when the "
       "compiler can prove the iteration is int-typed. "
       [:code "loop/recur"] " over 10,000 iterations and recursive "
       "Fibonacci both clock in around 13 ns per step because the "
       "fused-loop opcode collapses the test/dec/back-jump into a "
       "single dispatch with two tagged-int checks."]

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
       "raise the limit via " [:code "mino_set_thread_limit"] " or one "
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
             [:td "~10%"]
             [:td "~0.5 ms"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "~3%"]
             [:td "~0.4 ms"]]
        [:tr [:td "Build 1,000-element vector via " [:code "conj"]]
             [:td "~28%"]
             [:td "~1.6 ms"]]
        [:tr [:td "Build 10,000-element vector via " [:code "conj"]]
             [:td "~29%"]
             [:td "~1.6 ms"]]
        [:tr [:td "Build 5k int-map and sum"]
             [:td "~29%"]
             [:td "~1.5 ms"]]
        [:tr [:td "map/filter/map/reduce over 50,000"]
             [:td "~34%"]
             [:td "~2.2 ms"]]
        [:tr [:td "Nested vectors 500x100"]
             [:td "~34%"]
             [:td "~2.2 ms"]]
        [:tr [:td "Realize 10k of lazy range"]
             [:td "~38%"]
             [:td "~3.8 ms"]]]]
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
        " Every new " [:code "mino_state_t"] " parses and evaluates "
        [:code "core.clj"] " (~5.4 ms here, smaller on faster "
        "hardware). Parsed forms are cached per state; additional "
        "envs in one state avoid re-parsing. Bundled "
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
  -Isrc/collections -Isrc/prim -Isrc/async -Isrc/interop \\
  -Isrc/diag -Isrc/vendor/imath \\
  -o my_bench my_bench.c \\
  src/public/*.c src/runtime/*.c src/gc/*.c src/eval/*.c \\
  src/eval/bc/*.c src/collections/*.c src/prim/*.c \\
  src/async/*.c src/interop/*.c src/regex/*.c \\
  src/diag/*.c src/vendor/imath/*.c \\
  -lm -lpthread
./my_bench"]]
      [:p "For minimum-footprint embed measurements, add "
       [:code "-ffunction-sections -fdata-sections"]
       " to the compile flags and "
       [:code "-Wl,--gc-sections"]
       " to the link flags so unreferenced subsystems drop out."])))
