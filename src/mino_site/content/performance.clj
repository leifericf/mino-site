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
       "Numbers below were measured against mino v0.148.0 on x86_64 "
       "Linux (WSL2, kernel 6.6) under normal desktop load. Treat them "
       "as directional; different hardware will shift absolute numbers "
       "but the ratios between rows hold. The bc-frontiers cycle "
       "(v0.152.0 – v0.154.0) landed targeted fast lanes that moved "
       "the write-side, small-prim, and record-access rows by "
       "30–95%; those rows are not yet re-tabled here. The full "
       "bench suite lives in "
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
       (src-link "tests/min_embed.c")
       (src-link "tests/min_embed_floor.c")]
      [:p "Three binary footprints worth knowing about — the Floor "
       "tier that an embedder commits to, the Standard tier with the "
       "canonical Clojure surface, and the Standalone ceiling that "
       "ships from Homebrew. All linked with "
       [:code "-ffunction-sections -Wl,--gc-sections"]
       " so unreferenced subsystems drop out at link time, and "
       "stripped with " [:code "strip --strip-all"] "."]
      [:table
       [:thead
        [:tr [:th "Build"] [:th "Stripped size"] [:th "What's in it"]]]
       [:tbody
        [:tr [:td "Floor (" [:code "install_minimal"] " only)"]
             [:td "~613 KB"]
             [:td [:code "mino_state_new"] " + "
                  [:code "mino_install_minimal"] " + "
                  [:code "mino_eval_string"] ". Reader, evaluator, GC, "
                  "persistent collections, numeric ops, foundational "
                  "macros. No " [:code "core.clj"] " evaluation, no "
                  "regex / bignum / multimethods / protocols / "
                  "transducers, no I/O. Capability-gated names like "
                  [:code "re-find"] " or " [:code "slurp"]
                  " raise the MNS002 capability-disabled diagnostic if "
                  "user code references them."]]
        [:tr [:td "Standard (" [:code "install_core"] " back-compat alias)"]
             [:td "~728 KB"]
             [:td "Floor plus regex, bignum, multimethods, protocols, "
                  "transducers — every name a Clojure scripter expects. "
                  "Still no I/O, FS, processes, STM, agents, async, "
                  "no bundled " [:code "clojure.*"] " stdlib."]]
        [:tr [:td "Standalone (" [:code "install_all"] " + REPL)"]
             [:td "~987 KB"]
             [:td "Standard plus I/O, FS, " [:code "subprocess"]
                  ", STM, agents, async, host-interop, all bundled "
                  [:code "clojure.*"]
                  " namespaces, the project resolver, the task/deps "
                  "machinery, and the REPL crash handler. The "
                  "released " [:code "mino"]
                  " binary an end user receives from Homebrew (or "
                  "downloads from a GitHub release; the release "
                  "tarball is ~360 KB gzip-compressed)."]]]]
      [:p "Source-side numbers for what an in-tree embedder pulls in:"]
      [:table
       [:thead
        [:tr [:th "Item"] [:th "Size"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "C source tree (" [:code "src/"] " minus vendor)"]
             [:td "~1.6 MB"]
             [:td "C source plus generated bundled-source headers"]]
        [:tr [:td "Vendor (" [:code "imath"] " for BigInt)"]
             [:td "~150 KB"]
             [:td "Only loaded when arithmetic exceeds 64-bit range"]]
        [:tr [:td "Bundled stdlib source (" [:code "clojure.*"] " "
              "headers compiled into the binary)"]
             [:td "~147 KB"]
             [:td "Lazy-installed; the minimum-embed build drops these"]]
        [:tr [:td [:code "core.clj"] " source"]
             [:td "~113 KB"]
             [:td "Embedded as a C string literal; evaluated the first "
                  "time " [:code "mino_install"]
                  " brings in a non-floor capability"]]]]

      [:h2 "Cold startup"
       (src-link "tests/coldstart_compare.clj")
       (src-link "tests/refresh_perf.clj")]
      [:p "Wall time from " [:code "fork+exec"] " to process exit. Each "
       "row is the median of 50 invocations after three warmup runs to "
       "prime the OS page cache. Lua 5.5, Janet 1.41, and Babashka 1.12 "
       "are shown from the same host (x86_64 Linux) for reference; the "
       "Floor tier is what an embedder pays today."]
      [:table
       [:thead
        [:tr [:th "Interpreter / tier"] [:th "Wall time (median)"]
         [:th "Footprint"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Lua 5.5"]
             [:td "0.78 ms"]
             [:td "33 KB"]
             [:td "Reference. Different language, far smaller surface."]]
        [:tr [:td "mino Floor (" [:code "install_minimal"] ")"]
             [:td "1.04 ms"]
             [:td "598 KB"]
             [:td "Process spawn + " [:code "mino_state_new"] " + "
                  [:code "mino_install_minimal"] " + eval + exit. No "
                  [:code "core.clj"] " parse / eval. Within 30% of Lua "
                  "on cold start."]]
        [:tr [:td "Janet 1.41"]
             [:td "1.95 ms"]
             [:td "888 KB"]
             [:td "Reference."]]
        [:tr [:td "Babashka 1.12"]
             [:td "3.58 ms"]
             [:td "67 MB"]
             [:td "Reference. GraalVM AOT, Clojure dialect."]]
        [:tr [:td "mino Standard ("
              [:code "install_sandbox"] ")"]
             [:td "6.77 ms"]
             [:td "710 KB"]
             [:td "Floor + regex + bignum + multimethods + protocols "
                  "+ transducers + the safe bundled libs. Parses and "
                  "evaluates " [:code "core.clj"] " at install."]]
        [:tr [:td "mino Standalone (" [:code "./mino -e ..."] ")"]
             [:td "7.27 ms"]
             [:td "899 KB"]
             [:td "Standard plus I/O, FS, processes, STM, agents, async, "
                  "bundled " [:code "clojure.*"] ". The Homebrew binary."]]]]
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
             [:td "0.23 ms"]
             [:td "Floor tier. No " [:code "core.clj"] " parse / eval."]]
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_sandbox"]
              " + " [:code "mino_state_free"]]
             [:td "5.25 ms"]
             [:td "Standard tier. Equivalent to "
                  [:code "mino_install(S, env, MINO_CAP_DEFAULT)"]
                  ". Parses and evaluates "
                  [:code "core.clj"] " with regex, bignum, "
                  "multimethods, protocols, transducers, and the safe "
                  "bundled libs enabled."]]
        [:tr [:td [:code "mino_state_new"] " + "
              [:code "mino_install_all"] " + " [:code "mino_state_free"]]
             [:td "5.48 ms"]
             [:td "Adds I/O, FS, STM, agents, bundled "
                  [:code "clojure.*"]
                  " registration (lazy; not evaluated until "
                  [:code "require"] "d). The standalone CLI path."]]]]
      [:p "The Floor tier saves ~5 ms on every cold start by skipping "
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
        " A " [:code "mino_state_t"] " on the Standard or Standalone "
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
