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
       "These are preliminary results from an early profiling pass on "
       "pre-release code. Numbers were collected on an Apple M3 Pro "
       "laptop under normal desktop load (browser, editor, other "
       "processes running). Treat them as directional, not definitive. "
       "They will change as the runtime matures."]

      [:p "mino is a tree-walking interpreter. There is no bytecode "
       "compiler, no JIT, and no dispatch optimization beyond the C "
       "compiler's own work. The numbers below reflect that. They are "
       "included to set honest expectations and to show where the cost "
       "centers are, not to claim speed."]

      [:h2 "Core operations"]
      [:p "Per-operation cost for the fundamental building blocks. "
       "Lower is better."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Value construction (int, cons)"]
             [:td "0.1\u20130.2 \u00b5s"]
             [:td "malloc + linked list prepend"]]
        [:tr [:td "Symbol interning"]
             [:td "0.14 \u00b5s"]
             [:td "Hash lookup, returns cached pointer on hit"]]
        [:tr [:td "Simple eval " [:code "(+ 1 2)"]]
             [:td "0.8 \u00b5s"]
             [:td "Read + eval + result. The baseline."]]
        [:tr [:td "User function call"]
             [:td "2.1 \u00b5s"]
             [:td "Env child + parameter binding + body eval"]]
        [:tr [:td "Vector construction (5 elements)"]
             [:td "2.1 \u00b5s"]
             [:td "32-way trie allocation"]]
        [:tr [:td "Map construction (3 keys)"]
             [:td "4.3 \u00b5s"]
             [:td "HAMT insertion per key"]]
        [:tr [:td "Map lookup (3-key map)"]
             [:td "5.8 \u00b5s"]
             [:td "Hash + HAMT traversal"]]
        [:tr [:td "REPL feed " [:code "(+ 1 2)"]]
             [:td "0.9 \u00b5s"]
             [:td "Same as eval_string with line buffer"]]
        [:tr [:td "Reader (simple form)"]
             [:td "0.6 \u00b5s"]
             [:td "Tokenize + parse"]]
        [:tr [:td [:code "mino_ref"] " + deref + unref"]
             [:td "0.08 \u00b5s"]
             [:td "malloc/free of a small struct"]]]]

      [:h2 "Bulk operations"]
      [:p "Cost of working with collections at scale. These numbers "
       "show where the interpreter overhead compounds."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Per element"]]]
       [:tbody
        [:tr [:td [:code "(into [] (range 100))"]]
             [:td "728 \u00b5s"]
             [:td "7.3 \u00b5s"]]
        [:tr [:td [:code "(reduce + 0 (range 100))"]]
             [:td "728 \u00b5s"]
             [:td "7.3 \u00b5s"]]
        [:tr [:td [:code "(into [] (range 1000))"]]
             [:td "7.6 ms"]
             [:td "7.6 \u00b5s"]]
        [:tr [:td [:code "(reduce + 0 (range 1000))"]]
             [:td "8.1 ms"]
             [:td "8.1 \u00b5s"]]
        [:tr [:td "Build 100-key map"]
             [:td "1.1 ms"]
             [:td "10.8 \u00b5s"]]
        [:tr [:td [:code "loop/recur"] " 10,000 iterations"]
             [:td "12.4 ms"]
             [:td "1.24 \u00b5s"]]
        [:tr [:td [:code "(fib 20)"] " (recursive, ~21k calls)"]
             [:td "42.1 ms"]
             [:td "2.0 \u00b5s per call"]]]]

      [:h2 "Eager collection builders"]
      [:p "When lazy evaluation is not needed, the " [:code "rangev"]
       ", " [:code "mapv"] ", and " [:code "filterv"]
       " primitives produce vectors directly in C, bypassing thunk "
       "allocation entirely."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Per element"]
             [:th "vs. lazy"]]]
       [:tbody
        [:tr [:td [:code "(rangev 100)"]]
             [:td "11 \u00b5s"]
             [:td "0.11 \u00b5s"]
             [:td "63\u00d7 faster"]]
        [:tr [:td [:code "(rangev 1000)"]]
             [:td "110 \u00b5s"]
             [:td "0.11 \u00b5s"]
             [:td "69\u00d7 faster"]]
        [:tr [:td [:code "(reduce + 0 (rangev 1000))"]]
             [:td "311 \u00b5s"]
             [:td "0.31 \u00b5s"]
             [:td "26\u00d7 faster"]]
        [:tr [:td [:code "(mapv inc (rangev 1000))"]]
             [:td "7.0 ms"]
             [:td "7.0 \u00b5s"]
             [:td "~1\u00d7 (fn call dominates)"]]]]
      [:p "The speedup comes from eliminating thunk allocation and "
       "eval overhead per element. When the per-element work is "
       "dominated by a function call (as in " [:code "mapv"]
       "), the eager path provides little advantage. Use "
       [:code "rangev"] " for data generation and " [:code "reduce"]
       " over vectors for the biggest wins."]

      [:h2 "Cross-state cloning"]
      [:p "Cost of deep-copying data between runtime instances. "
       [:code "mino_clone"] " is the only cross-state primitive in C; "
       "hosts that manage more than one " [:code "mino_state_t"]
       " (for example, one state per OS thread) use it to move "
       "immutable data across. Actor mailboxes live entirely in mino "
       "and do not cross state boundaries."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Clone: 5-element vector"]
             [:td "0.24 \u00b5s"]
             [:td "Deep copy, allocates in destination state"]]
        [:tr [:td "Clone: 100-element vector"]
             [:td "3.6 \u00b5s"]
             [:td "Linear in element count"]]
        [:tr [:td "Clone: nested map"]
             [:td "1.2 \u00b5s"]
             [:td "Recursive traversal"]]
        [:tr [:td "Actor send! + receive (integer)"]
             [:td "~1.2 \u00b5s"]
             [:td "Atom swap! over a persistent vector mailbox"]]
        [:tr [:td "Actor send! with 5-element vector payload"]
             [:td "~1.4 \u00b5s"]
             [:td "Values are immutable; no deep copy across send!"]]]]

      [:h2 "Lifecycle"]
      [:p "Cost of creating and destroying runtime objects. These "
       "matter most for actor-heavy architectures where each actor "
       "gets its own runtime instance."]
      [:table
       [:thead
        [:tr [:th "Operation"] [:th "Cost"] [:th "Notes"]]]
       [:tbody
        [:tr [:td [:code "mino_state_new"] " + " [:code "mino_state_free"]]
             [:td "0.28 \u00b5s"]
             [:td "Bare state with no bindings"]]
        [:tr [:td [:code "mino_new"] " (state + core + I/O)"]
             [:td "524 \u00b5s"]
             [:td "Parses and evaluates ~800 lines of core.mino"]]
        [:tr [:td [:code "mino_env_clone"]]
             [:td "90 \u00b5s"]
             [:td "Copy all bindings, share values"]]]]

      [:h2 "Actor scaling"]
      [:p "Actors are mino values: an atom wrapping a mailbox vector "
       "plus a " [:code "*self*"] " dynamic binding. "
       [:code "spawn"] " does not create a new runtime instance; the "
       "body runs in the caller's environment and returns once. The "
       "table below shows wall-clock time for spawning N actors and "
       "sending one integer message to each."]
      [:table
       [:thead
        [:tr [:th "Actors"] [:th "Spawn"] [:th "Send (one per actor)"] [:th "Total"]]]
       [:tbody
        [:tr [:td "100"]    [:td "~0.9 ms"]  [:td "~0.15 ms"]  [:td "~1.1 ms"]]
        [:tr [:td "1,000"]  [:td "~9 ms"]    [:td "~1.5 ms"]   [:td "~11 ms"]]
        [:tr [:td "10,000"] [:td "~90 ms"]   [:td "~15 ms"]    [:td "~105 ms"]]]]
      [:p "Spawn cost is ~10 \u00b5s per actor. The previous "
       "C-backed actor allocated a fresh " [:code "mino_state_t"]
       " with a full core library and cost ~1.2 ms each; demoting "
       "the actor system into " [:code "lib/core/actor.mino"] " in "
       "v0.43.0 cut that by two orders of magnitude. Messaging is a "
       "single atom " [:code "swap!"] " per send."]

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
       "~51 ms under the default slice budget, with GC share between "
       "17 and 28 percent of wall clock. Tail-heavy workloads (deeply "
       "nested lazy pipelines, large transient vectors, deep recursion) "
       "were the headline target; the incremental major cut their "
       "max pause roughly in half versus the previous single-phase "
       "collector."]
      [:table
       [:thead
        [:tr [:th "Workload"] [:th "GC share"] [:th "Max pause"]]]
       [:tbody
        [:tr [:td "Small function calls (empty, identity, let)"]
             [:td "~9%"]
             [:td "< 1 ms"]]
        [:tr [:td "Tight loop 10,000 iters"]
             [:td "~11%"]
             [:td "~3 ms"]]
        [:tr [:td "Build 1,000-element collection"]
             [:td "~14-18%"]
             [:td "~5-10 ms"]]
        [:tr [:td "Build 10,000-element collection"]
             [:td "~17-22%"]
             [:td "~12-20 ms"]]
        [:tr [:td "map/filter/reduce over 50,000"]
             [:td "~27%"]
             [:td "~51 ms"]]
        [:tr [:td "Nested vectors 500x100"]
             [:td "~26%"]
             [:td "~51 ms"]]]]
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
        " (1.24 \u00b5s/iteration) is 6x faster than lazy reduce "
        "(7.3 \u00b5s/iteration). The eager variants "
        [:code "rangev"] ", " [:code "mapv"] ", and "
        [:code "filterv"] " eliminate thunk overhead entirely "
        "when laziness is not needed."]
       [:li [:strong "Core library initialization."]
        " Every new " [:code "mino_state_t"] " evaluates "
        [:code "core.mino"] " from source (~524 \u00b5s). Parsed "
        "forms are cached per state, so creating multiple "
        "environments within one state avoids re-parsing."]
       [:li [:strong "Cons-list argument passing."]
        " Every function call builds a linked list of cons cells "
        "for its arguments. The callee walks the list to bind "
        "parameters. A fixed-arity fast path would eliminate this "
        "for common cases."]
       [:li [:strong "Tree-walking eval."]
        " There is no intermediate representation. Each form is "
        "traversed, dispatched on type, and interpreted directly. "
        "A bytecode compiler would give a large constant-factor "
        "improvement across the board."]]

      [:h2 "Known issues"]
      [:p "Two performance characteristics are inherent to the current "
       "architecture. Both have mitigations."]
      [:ul
       [:li [:strong "Core library initialization (0.5 ms per runtime)."]
        " Every new runtime instance evaluates ~800 lines of "
        [:code "core.mino"] " from source text. Parsed forms are "
        "cached per state, so creating multiple environments within "
        "one state avoids re-parsing. Cross-state sharing is not "
        "possible because parsed forms contain state-specific "
        "interned pointers. A bytecode format would address this "
        "but does not exist yet."]
       [:li [:strong "Lazy sequence per-element overhead (7\u20138 \u00b5s)."]
        " Lazy-by-default sequences pay for a thunk allocation, an "
        "eval, and a cons cell on every element. The eager variants "
        [:code "rangev"] ", " [:code "mapv"] ", and "
        [:code "filterv"] " eliminate this overhead when laziness "
        "is not needed (see table above). For iteration without "
        "building a collection, " [:code "loop/recur"]
        " remains the fastest option at 1.24 \u00b5s/iteration."]]

      [:h2 "What this means in practice"]
      [:p "mino is fast enough for configuration evaluation, rules "
       "engines, interactive consoles, plugin systems, and data "
       "transformation on moderate-sized collections. It evaluates "
       "a simple expression in under a microsecond and processes "
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
       "link against the mino source. Compile and run with:"]
      [:pre [:code
       "cc -std=c99 -O2 -Isrc -o my_bench my_bench.c src/*.c -lm\n./my_bench"]])))
