(ns mino-site.content.jit
  "Internals page: single canonical CPJIT overview. Covers the
  stencil substrate, per-host support matrix, runtime control,
  side-exit deopt path, loop cancellability, perf evidence, and
  the verification posture behind each cell."
  (:require
    [hiccup2.core :as h]))

(defn jit-page
  "Generates the unified JIT documentation page HTML body."
  []
  (str
    (h/html
      [:h1 "JIT"]

      [:p.banner
       "The copy-and-patch JIT (CPJIT) is feature-complete on five "
       "host pipelines. It extracts pre-compiled opcode stencils into "
       "byte tables per host object format; the runtime patcher copies "
       "those bytes into executable memory and rewrites the operand "
       "immediates per call site. The full "
       [:code "mino"] " binary links the JIT in by default; the "
       "parallel " [:code "mino-lean"] " build compiles the CPJIT "
       "machinery out entirely for hosts that want the smaller "
       "footprint over the throughput speedup."]

      [:h2 "What ships today"]
      [:ul
       [:li [:strong "58 opcode stencils"] " covering arithmetic ("
        [:code "OP_ADD_II"] ", " [:code "OP_SUB_II"] ", "
        [:code "OP_MUL_II"] ", k-immediate variants), comparison ("
        [:code "OP_EQ_II"] " through " [:code "OP_GE_II"] " plus "
        [:code "*_IK"] " forms), bitwise ("
        [:code "OP_BAND_II"] ", " [:code "OP_BOR_II"] ", "
        [:code "OP_BXOR_II"] ", " [:code "OP_SHL_II"] ", "
        [:code "OP_SHR_II"] ", " [:code "OP_USHR_II"] ", "
        [:code "OP_BNOT_I"] "), unary predicates ("
        [:code "OP_ZERO_INT_P"] ", " [:code "OP_POS_P_I"] ", "
        [:code "OP_EVEN_P_I"] ", " [:code "OP_ODD_P_I"] "), unary "
        "increment/decrement ("
        [:code "OP_INC_I"] ", " [:code "OP_DEC_I"] "), fused loop "
        "steps (" [:code "OP_LOOP_INT_LT"] ", "
        [:code "OP_LOOP_INT_DEC"] ", " [:code "OP_LOOP_INT_LT_INC"]
        ", " [:code "OP_LOOP_INT_DEC_INC"] "), data-structure fast "
        "lanes (" [:code "OP_NTH_VEC"] ", "
        [:code "OP_FIRST_VEC"] ", " [:code "OP_COUNT_VEC"] ", "
        [:code "OP_EMPTY_VEC"] ", " [:code "OP_CONJ_VEC"] ", "
        [:code "OP_GET_KW_MAP"] ", "
        [:code "OP_ASSOC"] ", " [:code "OP_DISSOC"] "), dispatch ("
        [:code "OP_CALL"] ", " [:code "OP_CALL_CACHED"] ", "
        [:code "OP_TAILCALL"] ", "
        [:code "OP_PROTOCOL_CALL_CACHED"] ", "
        [:code "OP_PROTOCOL_TAILCALL_CACHED"] ", "
        [:code "OP_GETGLOBAL_CACHED"] ", "
        [:code "OP_CLOSURE"] ", " [:code "OP_MAKE_LAZY"] "), env "
        "management (" [:code "OP_PUSH_ENV"] ", "
        [:code "OP_POP_ENV"] ", " [:code "OP_ENV_BIND"] "), leaf "
        "shapes (" [:code "OP_LOAD_K"] ", " [:code "OP_MOVE"] ", "
        [:code "OP_RETURN"] ", and the fused "
        [:code "OP_LOAD_K_RETURN"] " superinstruction), and one "
        "synthetic " [:code "OP_DEOPT_TO_INTERP"] " stencil that "
        "the compile path inserts to bail back to the interpreter."]

       [:li [:strong "5 host arches with on-disk byte tables."]
        " Each generated header carries the stencil bytes plus the "
        "symbol and relocation tables the runtime patcher consumes:"
        [:ul
         [:li [:code "stencils_arm64_darwin.h"] " (101,201 bytes)"]
         [:li [:code "stencils_arm64_linux.h"] " (101,314 bytes)"]
         [:li [:code "stencils_x86_64_darwin.h"] " (94,893 bytes)"]
         [:li [:code "stencils_x86_64_linux.h"] " (95,317 bytes)"]
         [:li [:code "stencils_x86_64_windows.h"] " (97,856 bytes)"]]]

       [:li [:strong "Side-exit deopt path."]
        " Fns whose first unstenciled op sits past PC 0 compile "
        "to a native prefix plus a deopt stencil. When execution "
        "reaches the deopt instruction the native code returns "
        "into " [:code "mino_bc_run_resume"] " which drives the "
        "interpreter over the same regs window from the recorded "
        "PC. Round-trip cost is roughly 100 ns per deopt on Apple "
        "Silicon; the path amortises against running the prefix "
        "through the interpreter around the 30-op prefix length. "
        "After this landed, the " [:code "realistic_bench"] " and "
        [:code "real_workloads"] " corpora both show 100% native "
        "eligibility with zero hard rejections."]

       [:li [:strong "Cancellable JIT'd loops."]
        " Every native loop back-edge polls the safepoint on a "
        "256-iteration downcounter: fused-loop stencils keep the "
        "counter in a register, and every other loop shape gets a "
        "safepoint stencil planted before its backward jump. A "
        "spinning JIT'd loop responds to "
        [:code "(future-cancel f)"] " within bounded wall time "
        "even when the body is entirely native, and the poll keeps "
        "the runtime's lock auto-yield at the same cadence the "
        "interpreter produces, so a native spin cannot starve "
        "sibling workers. Per-iteration cost is one decrement plus "
        "one branch on the fused hot path."]

       [:li [:strong "Adaptive tiering."]
        " In " [:code "AUTO"] " mode any callee invoked from "
        "inside a JIT-compiled region picks up a threshold of 1, "
        "so warm-up gaps on short-lived scripts collapse to the "
        "first call after the JIT'd caller fires. Tunable via "
        [:code "mino_state_set_jit_hot_threshold"] " for embedders "
        "that want different cold-call counts."]

       [:li [:strong "Dual-binary build."]
        " The full " [:code "mino"] " binary builds with "
        [:code "-DMINO_CPJIT=1"] " and links the entire JIT "
        "pipeline. The parallel " [:code "mino-lean"] " binary "
        "builds the same source tree with " [:code "MINO_CPJIT"]
        " undefined; the patcher, emitter, and stencil entry "
        "layers compile to no-ops and the runtime decision tree "
        "collapses to the tree-walker plus the bytecode VM. "
        [:code "mino_state_jit_capability"] " returns "
        [:code "{.available=0, ...}"] " on "
        [:code "mino-lean"] " and "
        [:code "{.available=1, ...}"] " on the full build."]

       [:li [:strong "4-way parity green on the dev host."]
        " The " [:code "test-jit-parity"] " task runs the test "
        "suite four times (" [:code "MINO_JIT=auto"] ", "
        [:code "MINO_JIT=on"] ", " [:code "MINO_JIT=off"] ", and "
        "the " [:code "mino-lean"] " binary) and asserts the four "
        "stdouts are byte-identical and all four processes exit "
        "0:"
        [:pre [:code {:data-lang "text"}
"$ ./mino task test-jit-parity
93 tests, 93 assertions: 93 passed, 0 failed, 0 errors
  jit-parity: OK -- stdout byte-identical across
              jit-auto / jit-on / jit-off / lean, all exit 0"]]]

       [:li [:strong "Synthetic-blob selftests."]
        " The " [:code "tools/stencil-extract --selftest"] " "
        "binary builds hand-crafted Mach-O, ELF, and COFF object "
        "blobs with known function bodies, symbol tables, and "
        "relocation tables, then runs each format parser against "
        "them and asserts the extracted bytes match expected "
        "values:"
        [:pre [:code {:data-lang "text"}
"$ ./tools/stencil-extract --selftest
selftest_macho_synthetic: OK
selftest_elf_synthetic: OK
selftest_coff_synthetic: OK
stencil_extract selftest: OK"]]]]

      [:h2 "Per-host pipeline stages"]
      [:p "Each stage is independent. " [:strong "extract"] " parses "
       "the object file produced by the stencil compiler. "
       [:strong "generate"] " emits the on-disk byte table the "
       "runtime patcher reads. " [:strong "build"] " is the full "
       "in-tree compile against that byte table. "
       [:strong "smoke"] " is the test suite running through the "
       "JIT pipeline. " [:strong "parity"] " is the four-way "
       [:code "auto/on/off/lean"] " byte-identical-stdout check."]
      [:p "The committed byte tables are regenerated by a single "
       "pinned " [:code "zig cc"] " (a bundled, version-locked Clang "
       "with cross-compilation) that builds every target from one "
       "host — " [:code "./mino task gen-stencils-all"] ". Stencil "
       "sources are hermetic, so all five targets cross-compile with "
       "no platform SDK, and one toolchain emitting every table is "
       "what makes the bytes byte-for-byte reproducible. This is a "
       "maintainer-only step: " [:code "make"] " plus any C99 "
       "compiler still builds mino from the committed bytes, and "
       "embedders never invoke a stencil compiler."]

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

      [:h3 "Partial-cell notes"]
      [:ul
       [:li [:strong "x86_64 Darwin build/smoke/parity: "]
        "covered by the " [:code "stencil-determinism"] " job, which "
        "runs on one pinned-" [:code "zig"] " Linux runner every "
        "push, regenerates every committed byte table (including both "
        "Darwin targets, which need no macOS SDK) and asserts "
        [:code "git diff --exit-code"] ". End-to-end execution on "
        "Intel macOS is not in the GHA matrix; GitHub has been "
        "retiring Intel-Mac runners and the project does not yet "
        "host a self-hosted Intel runner. The Mach-O 64 stencil "
        "parser and patcher share their data flow with the ARM64 "
        "Darwin path, which is the dev host and is exercised on "
        "every push."]
       [:li [:strong "x86_64 Windows parity: "]
        "the four-way parity sits inside the release-gate "
        "composite, which depends on a libsanitizer build that "
        "MinGW does not ship. The Windows runner instead runs the "
        "smoke build (make + " [:code "tests/run.clj"] " through "
        "the JIT) on every push, which exercises the same code "
        "paths the parity check would but stops short of the "
        "byte-identical-stdout assertion."]]

      [:h2 "Runtime control"]
      [:p "Each row in the support table is a build claim. At "
       "runtime, every JIT-capable binary lets the host choose how "
       "the pipeline executes. The five public symbols are stable "
       "across releases:"]
      [:pre [:code {:data-lang "c"}
"void                  mino_state_set_jit_mode(mino_state *S,
                                              mino_jit_mode mode);
mino_jit_mode       mino_state_jit_mode(const mino_state *S);

void                  mino_state_set_jit_hot_threshold(mino_state *S,
                                                       unsigned n);
unsigned              mino_state_jit_hot_threshold(const mino_state *S);

mino_jit_capability mino_state_jit_capability(const mino_state *S);"]]

      [:h3 "Modes"]
      [:p [:code "MINO_JIT_MODE_AUTO"] " (default): compile when "
       "the hot-call threshold trips. " [:code "MINO_JIT_MODE_OFF"]
       ": never compile. " [:code "MINO_JIT_MODE_ON"] ": compile "
       "on first call. " [:code "ON"] " is for benchmarking and "
       "parity testing; " [:code "AUTO"] " is the default for "
       "embedders."]

      [:h3 "Hot threshold"]
      [:p "Default seed is the compile-time "
       [:code "MINO_JIT_THRESHOLD"] " (currently 10 calls). Lower "
       "for shorter-lived scripts where warm-up matters; raise to "
       "avoid compiling rarely-called functions in long-lived "
       "embedders. Inside an " [:code "AUTO"] " region the "
       "threshold collapses to 1 for callees, so the warm-up gap "
       "doesn't compound across nested JIT'd calls."]

      [:h3 "Capability discovery"]
      [:p [:code "mino_state_jit_capability"] " returns a struct "
       "with " [:code ":available"] ", " [:code ":mode"] ", "
       [:code ":threshold"] ", " [:code ":host_arch"] ", and "
       [:code ":host_os"] " fields. Embedders use this at startup "
       "to size their tuning before any script runs. "
       [:code "mino-lean"] " returns "
       [:code "{.available=0, ...}"] " so a host build that "
       "depends on JIT throughput knows to fall back."]

      [:h3 "CLI flags and env vars"]
      [:p "Mode and threshold are also reachable from outside the "
       "embed surface for scripting use:"]
      [:ul
       [:li [:code "--jit=auto|off|on"] " and "
        [:code "--jit-threshold=N"] " call through to the same "
        "internal setters."]
       [:li [:code "MINO_JIT"] " and "
        [:code "MINO_JIT_HOT_THRESHOLD"] " set the per-state "
        "default at " [:code "mino_state_new"] " time before the "
        "host gets a handle."]]

      [:h2 "Side-exit deopt path"]
      [:p "When a function's first unstenciled op sits past PC 0, "
       "the JIT compiles the supported prefix natively and plants "
       "an " [:code "OP_DEOPT_TO_INTERP"] " stencil at the first "
       "unstenciled position. The stencil records the resume PC "
       "on the state and returns NULL; "
       [:code "mino_jit_invoke"] " detects the deopt sentinel, "
       "clears the flag, and tail-calls "
       [:code "mino_bc_run_resume"] " to drive the interpreter "
       "over the same regs window from the recorded PC. The "
       "interpreter runs to function exit; subsequent calls re-"
       "enter the native prefix from the top, so the deopt cost "
       "is paid once per call, not per iteration."]

      [:p "Two safety gates apply: the resume PC must fit in the "
       "16-bit Bx slot the deopt stencil reads, and no direct-"
       "emit branch in the prefix may land past it. Both are "
       "checked by " [:code "mino_jit_eligible"] " before "
       "compile; fns failing either gate take the regular "
       "interpreter path. "
       [:code "MINO_CPJIT_STATS=tracing"] " surfaces an "
       [:code "ok-with-deopt"] " line per fn that took the "
       "compile-with-deopt path, and the bytes-blocked dashboard "
       "splits each op's total into "
       [:code "hard"] " (no native prefix) and "
       [:code "ok-with-deopt"] " counts so the reader can tell "
       "which blockers side-exit picked up."]

      [:h2 "Where the JIT shines: tight compute"]
      [:p "Loop kernels and recursive compute where the JIT's "
       "stencils cover the inner cycle end-to-end. These are the "
       "workloads the copy-and-patch substrate was designed for: "
       "no allocation per iteration, no transducer machinery, just "
       "fused tagged-int arithmetic and inline-cached call "
       "dispatch. Median of three runs each on Apple Silicon "
       "(arm64-darwin) against mino v0.323.0."]
      [:table
       [:thead
        [:tr [:th "Workload"]
             [:th "JIT off"]
             [:th "JIT on"]
             [:th "Speedup"]]]
       [:tbody
        [:tr [:td [:code "(dec-only 10M)"]
                  " — counted-down loop"]
             [:td "30.46 ms"] [:td "15.20 ms"] [:td "2.00x"]]
        [:tr [:td [:code "(lt-only 10M)"]
                  " — counted-up loop"]
             [:td "30.84 ms"] [:td "17.15 ms"] [:td "1.80x"]]
        [:tr [:td [:code "(sum-to 1M)"]
                  " — counter + accumulator"]
             [:td "19.41 ms"] [:td "3.01 ms"] [:td "6.46x"]]
        [:tr [:td [:code "(fib 30)"]
                  " — recursive compute"]
             [:td "107.15 ms"] [:td "53.34 ms"] [:td "2.01x"]]]]
      [:p "The " [:code "sum-to"]
       " row is the strongest case in current shapes: the JIT "
       "covers both " [:code "(< i n)"] " and " [:code "(+ acc i)"]
       " inline (fused " [:code "OP_LOOP_INT_LT_INC"]
       " stencil), eliminating the tagged-int dispatch overhead "
       "on both the counter and the accumulator. The other rows "
       "halve roughly because the JIT covers either the loop "
       "step or the recursion path, but the function-call layer "
       "still goes through the interpreter dispatcher for the "
       "recursive branch."]

      [:h2 "Where the JIT does not shine: alloc / GC pressure"]
      [:p "Median of three runs per cell, captured on Apple Silicon "
       "(arm64-darwin) against mino v0.323.0. All numbers in ms/op "
       "except the sub-ms row in µs/op."]
      [:table
       [:thead
        [:tr [:th "Row"]
             [:th "JIT on"]
             [:th "JIT off"]
             [:th "Ratio (off/on)"]
             [:th "Reading"]]]
       [:tbody
        [:tr [:td "build 5k int-map and sum"]
             [:td "10.05 ms"] [:td "10.34 ms"] [:td "1.03x"]
             [:td "within noise envelope"]]
        [:tr [:td "bump 5k int-map values"]
             [:td "17.97 ms"] [:td "16.94 ms"] [:td "0.94x"]
             [:td "within noise envelope"]]
        [:tr [:td "map/filter/map/reduce over 50k"]
             [:td "757 µs"] [:td "779 µs"] [:td "1.03x"]
             [:td "within noise envelope"]]
        [:tr [:td "nested vectors 500x100"]
             [:td "18.03 ms"] [:td "18.67 ms"] [:td "1.04x"]
             [:td "within noise envelope"]]
        [:tr [:td "realize 10k of lazy range"]
             [:td "4.19 ms"] [:td "4.48 ms"] [:td "1.07x"]
             [:td "within noise envelope"]]
        [:tr [:td "fibonacci(25)"]
             [:td "6.65 ms"] [:td "9.21 ms"] [:td "1.38x"]
             [:td "meaningful JIT win"]]]]
      [:p "Five of six rows land within the +/- 7% noise envelope. "
       "Allocation- and GC-dominated workloads are not where the "
       "JIT lives; they are dominated by nursery sizing, write-"
       "barrier cost, and minor-cycle frequency. The JIT sits "
       "above the GC and cannot accelerate work the allocator and "
       "collector are already doing. The one row that moves "
       "meaningfully is " [:code "fibonacci(25)"]
       ", pure compute that the JIT's recursive-call inline cache "
       "and fused tagged-int arithmetic cover end-to-end."]

      [:h2 "Out of scope"]
      [:ul
       [:li [:strong "Type-feedback specialization."]
        " Stencils dispatch on opcode shape, not on per-call-"
        "site type history. A future cycle can add an IC layer "
        "that captures observed types and patches a fast path; "
        "the current surface holds the interpreter-parity "
        "contract."]
       [:li [:strong "Forward stencil hooks."]
        " The side-exit path is one-way: native to interpreter, "
        "then interpreter to function exit. Re-entering native "
        "after a deopt-resumed dispatch reaches a stenciled run "
        "again is a future enhancement; deferred until a workload "
        "demonstrates the need."]
       [:li [:strong "Cross-module-leak static analysis "]
        "for the stencil extractor. Synthetic-blob selftests "
        "cover regression detection; static analysis is nice-to-"
        "have, not feature-complete-blocking."]
       [:li [:strong "Self-hosted Intel Mac runner "]
        "for x86_64 Darwin end-to-end verification. Operational, "
        "not code; the cross-compile parity job is the documented "
        "floor."]]

      [:h2 "How each cell is gated"]
      [:p "All five targets land their byte tables through the "
       "same extractor (" [:code "tools/stencil-extract"] "), so "
       "a regression in the format parser breaks " [:em "all"]
       " hosts that share that format. The synthetic-blob "
       "selftest in " [:code "tools/stencil_extract --selftest"]
       " catches parser regressions before any compile runs; the "
       "per-host generated byte table comparison catches drift "
       "introduced after the parser passes."]
      [:p "Three workflows produce the green cells:"]
      [:ul
       [:li [:strong "release-gate (every push, non-Windows): "]
        "produces extract / generate / build / smoke / parity "
        "for the three hosts where it runs."]
       [:li [:strong "cross-compile (every push, on macos-14): "]
        "regenerates every committed stencil byte table and "
        "asserts no diff. Covers x86_64 Darwin's first two "
        "columns and guards the byte-identical claim for the "
        "other four hosts."]
       [:li [:strong "ci-nightly (04:00 UTC): "]
        "re-runs release-gate plus extended suites (GC stress, "
        "fault injection, embedding stress) on the three non-"
        "Windows hosts. Surfaces toolchain drift on a daily "
        "cadence instead of waiting for the next PR to trip on "
        "it."]]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/bytecode-vm/#cpjit"}
             "Bytecode and VM -> The CPJIT layer"]
        ": the architectural tour of the stencil substrate, "
        "ICache discipline, and what the runtime patcher does."]
       [:li [:a {:href "/documentation/garbage-collection/"}
             "Garbage Collection"]
        ": where the floor lives for the allocation-heavy rows "
        "in the A/B table above."]
       [:li [:a {:href "/documentation/performance/"} "Performance"]
        ": the runtime-perf track that surrounds JIT engagement."]])))
