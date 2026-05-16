(ns mino-site.content.jit-matrix
  "Internals page: per-host CPJIT support matrix and the verification
  posture for each cell."
  (:require
    [hiccup2.core :as h]))

(defn jit-matrix-page
  "Generates the JIT support matrix page HTML body."
  []
  (str
    (h/html
      [:h1 "JIT support matrix"]

      [:p.banner
       "The copy-and-patch JIT (CPJIT) extracts pre-compiled opcode "
       "stencils into byte tables per host object format. At runtime "
       "those bytes are copied into executable memory and patched "
       "with operand-specific immediates. This page records which "
       "hosts have which pieces and what level of CI verification "
       "stands behind each cell."]

      [:p "Five host pipelines exist today. Three are continuously "
       "exercised end-to-end on every push; one is covered by a "
       "cross-compile parity check because the upstream Intel-Mac "
       "runner tier is being retired; one runs the smoke build but "
       "not the release-gate composite because the toolchain ships "
       "without a working libsanitizer."]

      [:h2 "Per-host pipeline stages"]
      [:p "Each stage is independent. " [:strong "extract"] " parses "
       "the object file produced by the host compiler. "
       [:strong "generate"] " emits the on-disk byte table the "
       "runtime patcher reads. " [:strong "build"] " is the full "
       "in-tree compile against that byte table. "
       [:strong "smoke"] " is the test suite running through the "
       "JIT pipeline. " [:strong "parity"] " is the four-way "
       [:code "auto/on/off/lean"] " byte-identical-stdout check."]

      [:table
       [:thead
        [:tr [:th "Target"]
             [:th "Format"]
             [:th "extract"]
             [:th "generate"]
             [:th "build"]
             [:th "smoke"]
             [:th "parity"]]]
       [:tbody
        [:tr [:td "ARM64 Darwin"]
             [:td "Mach-O 64"]
             [:td "green"] [:td "green"] [:td "green"]
             [:td "green"] [:td "green"]]
        [:tr [:td "ARM64 Linux"]
             [:td "ELF64"]
             [:td "green"] [:td "green"] [:td "green"]
             [:td "green"] [:td "green"]]
        [:tr [:td "x86_64 Linux"]
             [:td "ELF64"]
             [:td "green"] [:td "green"] [:td "green"]
             [:td "green"] [:td "green"]]
        [:tr [:td "x86_64 Darwin"]
             [:td "Mach-O 64"]
             [:td "green"] [:td "green"] [:td "partial"]
             [:td "partial"] [:td "partial"]]
        [:tr [:td "x86_64 Windows"]
             [:td "PE/COFF"]
             [:td "green"] [:td "green"] [:td "green"]
             [:td "green"] [:td "partial"]]]]

      [:h2 "Partial-cell notes"]
      [:ul
       [:li [:strong "x86_64 Darwin build/smoke/parity: "]
        "covered by the cross-compile parity job on macos-14 that "
        "regenerates every committed byte table and asserts "
        [:code "git diff --exit-code"] ". End-to-end execution on "
        "Intel macOS is not in the GHA matrix; GitHub has been "
        "retiring Intel-Mac runners and the project does not yet "
        "host a self-hosted Intel runner. The Mach-O 64 stencil "
        "parser and patcher share their data flow with the ARM64 "
        "Darwin path, which is the dev host and is exercised on "
        "every push."]
       [:li [:strong "x86_64 Windows parity: "]
        "the four-way parity sits inside the release-gate composite, "
        "which depends on a libsanitizer build that MinGW does not "
        "ship. The Windows runner instead runs the smoke build "
        "(make + " [:code "tests/run.clj"] " through the JIT) on "
        "every push, which exercises the same code paths the parity "
        "check would but stops short of the byte-identical-stdout "
        "assertion."]]

      [:h2 "How each cell is gated"]
      [:p "All five targets land their byte tables through the same "
       "extractor (" [:code "tools/stencil-extract"] "), so a "
       "regression in the format parser breaks " [:em "all"] " hosts "
       "that share that format. The synthetic-blob selftest in "
       [:code "tools/stencil_extract --selftest"] " catches parser "
       "regressions before any compile runs; the per-host generated "
       "byte table comparison catches drift introduced after the "
       "parser passes."]
      [:p "Three workflows produce the green cells:"]
      [:ul
       [:li [:strong "release-gate (every push, non-Windows): "]
        "produces extract / generate / build / smoke / parity for "
        "the three hosts where it runs."]
       [:li [:strong "cross-compile (every push, on macos-14): "]
        "regenerates every committed stencil byte table and asserts "
        "no diff. Covers x86_64 Darwin's first two columns and "
        "guards the byte-identical claim for the other four hosts."]
       [:li [:strong "ci-nightly (04:00 UTC): "]
        "re-runs release-gate plus extended suites (GC stress, "
        "fault injection, embedding stress) on the three non-Windows "
        "hosts. Surfaces toolchain drift on a daily cadence instead "
        "of waiting for the next PR to trip on it."]]

      [:h2 "Runtime control"]
      [:p "Each row in the table is a build claim. At runtime, every "
       "JIT-capable binary lets the host choose how the pipeline "
       "actually executes:"]
      [:pre [:code {:data-lang "c"}
"/* Embed-side. Returns 0 on success, -1 on bad mode. */
mino_state_set_jit_mode(state, MINO_JIT_MODE_AUTO);
mino_state_set_jit_mode(state, MINO_JIT_MODE_OFF);
mino_state_set_jit_mode(state, MINO_JIT_MODE_ON);

/* Tunable call-count threshold before a function compiles. */
mino_state_set_jit_hot_threshold(state, 10);

/* Capability discovery -- empty struct on JIT-less binaries. */
mino_jit_capability_t cap = mino_state_jit_capability(state);
printf(\"jit available=%d host=%s/%s threshold=%d\\n\",
       cap.available, cap.host_arch, cap.host_os, cap.threshold);"]]
      [:p "CLI flags " [:code "--jit=auto|off|on"] " and "
       [:code "--jit-threshold=N"] " route to the same internal "
       "setters. Env vars " [:code "MINO_JIT"] " and "
       [:code "MINO_JIT_HOT_THRESHOLD"] " set the per-state default "
       "at " [:code "mino_state_new"] " time before the host gets a "
       "handle. The parallel " [:code "mino-lean"] " binary compiles "
       "the entire CPJIT pipeline out at build time; "
       [:code "mino_state_jit_capability"] " returns "
       [:code "{.available=0, ...}"] " and the mode setter is a "
       "no-op that returns 0. Hosts that need the smaller binary "
       "footprint over the throughput speedup link against "
       [:code "mino-lean"] " directly; the runtime decision tree "
       "collapses to the tree-walker plus the bytecode VM with no "
       "patching code linked in."]

      [:h2 "JIT on/off A/B against realistic_bench"]
      [:p "Median of three runs per cell, captured on the dev host "
       "against the v0.250.0 binary (with the 4 MiB nursery default "
       "from v0.250 in place). All numbers in ms/op except the "
       "sub-ms row in µs/op."]
      [:table
       [:thead
        [:tr [:th "Row"]
             [:th "JIT on"]
             [:th "JIT off"]
             [:th "Ratio (off/on)"]
             [:th "Reading"]]]
       [:tbody
        [:tr [:td "build 5k int-map and sum"]
             [:td "10.35 ms"] [:td "10.06 ms"] [:td "0.97x"]
             [:td "within noise envelope"]]
        [:tr [:td "bump 5k int-map values"]
             [:td "15.46 ms"] [:td "15.41 ms"] [:td "1.00x"]
             [:td "within noise envelope"]]
        [:tr [:td "map/filter/map/reduce over 50k"]
             [:td "667 µs"] [:td "708 µs"] [:td "1.06x"]
             [:td "small JIT win"]]
        [:tr [:td "nested vectors 500x100"]
             [:td "16.02 ms"] [:td "16.08 ms"] [:td "1.00x"]
             [:td "within noise envelope"]]
        [:tr [:td "realize 10k of lazy range"]
             [:td "5.67 ms"] [:td "5.86 ms"] [:td "1.03x"]
             [:td "within noise envelope"]]
        [:tr [:td "fibonacci(25)"]
             [:td "5.99 ms"] [:td "8.19 ms"] [:td "1.37x"]
             [:td "meaningful JIT win"]]]]
      [:p "Five of six rows land within the +/- 7% noise envelope. "
       "Allocation- and GC-dominated workloads are not where the JIT "
       "lives; they are dominated by nursery sizing, write-barrier "
       "cost, and minor-cycle frequency. The JIT sits above the GC "
       "and cannot accelerate work the allocator and collector are "
       "already doing. The two rows that move are exactly the rows "
       "where the JIT's stencils target the inner loop: pure compute "
       "(" [:code "fibonacci(25)"] ") and a fused transducer reduce "
       "(" [:code "map/filter/map/reduce"] ")."]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/bytecode-vm/#cpjit"}
             "Bytecode and VM -> The CPJIT layer"]
        ": the architectural tour of the stencil substrate, "
        "ICache discipline, and what the runtime patcher does."]
       [:li [:a {:href "/documentation/jit-status/"} "JIT status"]
        ": the feature-complete declaration page with the live "
        "verification checklist."]
       [:li [:a {:href "/documentation/garbage-collection/"}
             "Garbage Collection"]
        ": where the floor lives for the allocation-heavy rows in "
        "the A/B table above."]])))
