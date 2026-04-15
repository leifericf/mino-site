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

      [:h2 "Cross-state operations"]
      [:p "Cost of moving data between runtime instances and the "
       "message-passing primitives used for actor communication."]
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
        [:tr [:td "Mailbox send + recv (integer)"]
             [:td "0.11 \u00b5s"]
             [:td "Serialize to buffer, deserialize on recv"]]
        [:tr [:td "Mailbox send + recv (5-element vector)"]
             [:td "3.0 \u00b5s"]
             [:td "Text serialization + full parse on recv"]]]]

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
      [:p "Actors are heavyweight: each one owns a full runtime "
       "instance with its own GC, intern tables, and a fresh "
       "evaluation of the standard library. The table below shows "
       "wall-clock time for creating N actors, sending one integer "
       "message to each, receiving and verifying, then freeing all "
       "of them."]
      [:table
       [:thead
        [:tr [:th "Actors"] [:th "Create"] [:th "Send + recv"] [:th "Free"] [:th "Total"]]]
       [:tbody
        [:tr [:td "100"]    [:td "49 ms"]    [:td "< 1 ms"]  [:td "10 ms"]    [:td "60 ms"]]
        [:tr [:td "1,000"]  [:td "490 ms"]   [:td "< 1 ms"]  [:td "100 ms"]   [:td "590 ms"]]
        [:tr [:td "10,000"] [:td "5.0 s"]    [:td "7 ms"]    [:td "1.0 s"]    [:td "6.0 s"]]
        [:tr [:td "50,000"] [:td "27.5 s"]   [:td "156 ms"]  [:td "15.6 s"]   [:td "43.3 s"]]]]
      [:p "Creation cost is ~0.5 ms per actor (dominated by "
       "core.mino evaluation). Messaging cost is negligible. "
       "Destruction cost grows with accumulated GC heap size. "
       "No failures were observed at any scale tested."]
      [:p "For comparison, creating 10,000 actors where each one "
       "receives a message, evaluates " [:code "(* x x)"]
       ", and clones the result back to the host takes 4.8 seconds "
       "end to end."]

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
        " Every new runtime evaluates " [:code "core.mino"]
        " from source. This is why actor creation costs 0.5 ms "
        "each. Parsed forms are cached per state, so creating "
        "multiple environments within one state avoids re-parsing."]
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
      [:p "The benchmark program lives at " [:code "bench/perf_profile.c"]
       " in the mino repository. Build and run it with:"]
      [:pre [:code
       "cc -std=c99 -O2 -Isrc -o bench/perf_profile \\\n  bench/perf_profile.c src/*.o -lm\n./bench/perf_profile"]]
      [:p "The actor scaling benchmark is at "
       [:code "examples/actor_scale_test.c"] "."])))
