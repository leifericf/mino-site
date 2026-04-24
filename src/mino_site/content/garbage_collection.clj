(ns mino-site.content.garbage-collection
  "Garbage collection reference page content."
  (:require
    [hiccup2.core :as h]))

(defn garbage-collection-page
  "Generates the Garbage Collection page HTML body."
  []
  (str
    (h/html
      [:h1 "Garbage collection"]
      [:p "The collector is a non-moving generational tracing "
       "collector with an incremental old-gen mark phase. Short-lived "
       "allocations live in a young-generation nursery; values that "
       "survive a minor collection are promoted to old-gen, which is "
       "marked in paced slices between mutator allocations. A write "
       "barrier tracks old-to-young pointers so minor collections "
       "stay proportional to young reachability. Any mino function "
       "that allocates may advance the collector, which is why "
       "borrowed values can become invalid after the next call."]
      [:p "Objects survive collection if they are reachable from a root: "
       "registered environments, host refs, the intern tables, the "
       "module cache, or the C stack (via conservative scanning). See "
       "the "
       [:a {:href "/documentation/embedding/#value-retention"} "Value retention"]
       " section of the Embedding Guide for how to keep host-held "
       "values reachable across collection."]

      [:h2 "Design"]
      [:p "Four choices shape the collector:"]
      [:ul
       [:li [:strong "Two generations. "]
        "Most allocations in a running program die young: intermediate "
        "sequence results, destructured bindings, closure arguments, "
        "temporary strings. A nursery collection walks only the young "
        "reachability set, which stays small even when the total heap "
        "is large. Promotion to old-gen happens once a value has "
        "survived a configurable number of minor cycles."]
       [:li [:strong "Non-moving. "]
        "Host code holds raw " [:code "mino_val_t *"] " pointers. A "
        "copying collector would have to update every live reference "
        "at every collection, which means either a read barrier on "
        "the host side or extremely careful pinning. Non-moving keeps "
        "addresses stable, which keeps the embedding API simple and "
        "the FFI story honest."]
       [:li [:strong "Incremental major. "]
        "Old-gen tracing is paced across many slices interleaved with "
        "the mutator, with a tunable work budget per slice. Pause "
        "time under a growing old-gen stays bounded instead of "
        "scaling with total heap size. The final sweep is one short "
        "stop-the-world phase."]
       [:li [:strong "Two barriers, always armed. "]
        "A remembered-set barrier tracks old-to-young pointer stores, "
        "so a minor cycle does not have to scan the whole old-gen to "
        "find young reachability. A snapshot-at-the-beginning (SATB) "
        "barrier captures slot values overwritten during major mark, "
        "so values that were reachable at cycle start stay marked "
        "even if the mutator unlinks them mid-cycle. Both reduce to "
        "a dirty-bit check per store in the common case."]]

      [:h2 "Phases"]
      [:p "The collector runs in one of four phases, reported as the "
       [:code ":phase"] " key of " [:code "(gc-stats)"] " and the "
       [:code "phase"] " field of " [:code "mino_gc_stats_t"] ":"]
      [:ul
       [:li [:strong "idle"] ": no cycle is in flight. Most calls to "
        [:code "mino_gc_collect"] " start here."]
       [:li [:strong "minor"] ": young-only mark-and-sweep. Short, "
        "fully stop-the-world. Scales with young reachability via "
        "the remembered set plus conservative stack scan."]
       [:li [:strong "major-mark"] ": old-gen tracing in progress. "
        "Paced across many slices interleaved with mutator progress. "
        "The SATB (snapshot-at-the-beginning) barrier is armed so "
        "overwritten slot values stay live for the cycle."]
       [:li [:strong "major-sweep"] ": one-shot stop-the-world sweep "
        "of dead old-gen objects. Runs immediately after the final "
        "mark-stack drain."]]
      [:p "Both barriers -- the remembered-set barrier that tracks "
       "old-to-young edges, and the SATB barrier that captures "
       "overwritten values during major mark -- are always armed. "
       "Their cost is one dirty-bit check per store in the common case."]

      [:p "Transitions between phases:"]
      [:pre [:code {:data-lang "text"}
"                    nursery full
                   or explicit MINOR
             +------------------------+
             |                        v
          [idle] <-----------+    [minor]
             |               |        |
             |               +--------+ sweep done
             |
             | threshold reached
             | or explicit MAJOR/FULL
             v
       [major-mark] <--+
             |         |
             | slice   | more work
             +---------+
             |
             | mark drained
             v
       [major-sweep]
             |
             | sweep done
             v
          [idle]"]]
      [:p "A minor cycle can nest safely inside major-mark: when the "
       "nursery fills during an in-flight major, minor runs to "
       "completion and returns to major-mark without disturbing the "
       "mark stack or the SATB snapshot. "
       [:code "MINO_GC_FULL"] " from the idle state runs a minor, "
       "then a complete major cycle back-to-back."]

      [:h2 "Host-driven collection"]
      [:p "The host can trigger collection at quiescent points -- between "
       "REPL turns, after bulk import, or before long-idle periods -- "
       "through " [:code "mino_gc_collect"] ":"]
      [:pre [:code {:data-lang "c"}
"mino_gc_collect(S, MINO_GC_MINOR);  /* nursery sweep only */
mino_gc_collect(S, MINO_GC_MAJOR);  /* drain or run a major cycle */
mino_gc_collect(S, MINO_GC_FULL);   /* minor + full STW major */"]]

      [:h2 "Tuning"]
      [:p "Five knobs tune the collector. Defaults work for most "
       "embedders; adjust only with a measurement in hand."]
      [:pre [:code {:data-lang "c"}
"mino_gc_set_param(S, MINO_GC_NURSERY_BYTES,       2 * 1024 * 1024);
mino_gc_set_param(S, MINO_GC_MAJOR_GROWTH_TENTHS, 15);  /* 1.5x */
mino_gc_set_param(S, MINO_GC_PROMOTION_AGE,        1);
mino_gc_set_param(S, MINO_GC_INCREMENTAL_BUDGET,   4096);
mino_gc_set_param(S, MINO_GC_STEP_ALLOC_BYTES,     16 * 1024);"]]
      [:p "Each setter returns 0 on success and -1 on a bad parameter "
       "or out-of-range value. Accepted ranges:"]
      [:table
       [:thead
        [:tr [:th "Parameter"] [:th "Default"] [:th "Min"] [:th "Max"] [:th "Effect"]]]
       [:tbody
        [:tr [:td [:code "NURSERY_BYTES"]]       [:td "1 MiB"]    [:td "64 KiB"]   [:td "256 MiB"]
         [:td "Larger = fewer minor cycles, more work per cycle, higher peak pause."]]
        [:tr [:td [:code "MAJOR_GROWTH_TENTHS"]] [:td "15 (1.5x)"] [:td "11 (1.1x)"] [:td "40 (4.0x)"]
         [:td "Old-gen growth above baseline before the next major fires."]]
        [:tr [:td [:code "PROMOTION_AGE"]]       [:td "1"]        [:td "1"]        [:td "8"]
         [:td "Number of minor survivals before a young object promotes to old."]]
        [:tr [:td [:code "INCREMENTAL_BUDGET"]]  [:td "4096"]     [:td "64"]       [:td "65536"]
         [:td "Headers popped from the mark stack per major slice. Higher = longer slice, fewer slices."]]
        [:tr [:td [:code "STEP_ALLOC_BYTES"]]    [:td "16 KiB"]   [:td "1024"]     [:td "16 MiB"]
         [:td "Bytes allocated between automatic major slices. Lower = more frequent slicing."]]]]

      [:h2 "Stats"]
      [:p "Query collector counters via a plain out-struct. No "
       "allocation is performed."]
      [:pre [:code {:data-lang "c"}
"mino_gc_stats_t st;
mino_gc_stats(S, &st);
printf(\"live=%zu minor=%zu major=%zu max_pause_ns=%zu\\n\",
       st.bytes_live, st.collections_minor,
       st.collections_major, st.max_gc_ns);"]]
      [:p "The same data is available from mino via "
       [:code "(gc-stats)"] ", which returns a map of keyword keys. "
       "Full field list:"]
      [:ul
       [:li [:code ":collections-minor"] " / " [:code ":collections-major"]
        " -- lifetime cycle counters."]
       [:li [:code ":bytes-live"] " / " [:code ":bytes-young"] " / "
        [:code ":bytes-old"] " -- current heap breakdown."]
       [:li [:code ":bytes-alloc"] " / " [:code ":bytes-freed"]
        " -- lifetime totals."]
       [:li [:code ":total-gc-ns"] " / " [:code ":max-gc-ns"]
        " -- cumulative and worst-case collection wall time."]
       [:li [:code ":nursery-bytes"] " -- configured nursery size, "
        "reflecting any "
        [:code "MINO_GC_NURSERY_BYTES"] " override."]
       [:li [:code ":remset-entries"] " / " [:code ":remset-cap"] " / "
        [:code ":remset-high-water"]
        " -- current size, current capacity, and peak size of the "
        "old-to-young remembered set. High-water helps size workloads "
        "whose burst remset differs from steady state."]
       [:li [:code ":mark-stack-cap"] " / "
        [:code ":mark-stack-high-water"]
        " -- current capacity and peak depth of the mark stack."]
       [:li [:code ":phase"] " -- one of "
        [:code ":idle"] ", "
        [:code ":minor"] ", "
        [:code ":major-mark"] ", "
        [:code ":major-sweep"] "."]
       [:li [:code ":threshold"]
        " -- heuristic threshold controlling the next major trigger."]]

      [:h2 "Environment variables"]
      [:p "Four environment variables configure the collector at state "
       "init without touching source:"]
      [:ul
       [:li [:code "MINO_GC_NURSERY_BYTES"]
        " -- override the 1 MiB default nursery size. Same lower bound "
        "as " [:code "mino_gc_set_param"] " (64 KiB)."]
       [:li [:code "MINO_GC_STRESS=1"]
        " -- force a full collection on every allocation. Slow, but "
        "catches any code path that holds an unrooted pointer across an "
        "allocation boundary. Use during development."]
       [:li [:code "MINO_GC_VERIFY=1"]
        " -- run a reachability classifier pass during major sweep to "
        "surface bookkeeping bugs (for example, a remembered-set miss). "
        "Slow; test-suite use only."]
       [:li [:code "MINO_GC_EVT=1"]
        " -- enable a fixed-size in-process event ring that records "
        "barrier, remset, promotion, and sweep events. Dumped to stderr "
        "on a verify abort. Zero cost when unset."]]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/embedding/"} "Embedding Guide"]
        ": state lifecycle, value ownership, and the other pieces of "
        "the C embedding model the collector assumes."]
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        ": every public function, type, and enum in " [:code "mino.h"] "."]
       [:li [:a {:href "/documentation/performance/"} "Performance"]
        ": collector throughput numbers and workload profiles."]])))
