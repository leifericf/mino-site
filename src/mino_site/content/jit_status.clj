(ns mino-site.content.jit-status
  "Internals page: JIT feature-complete declaration page with the
  live verification checklist and runtime control surface."
  (:require
    [hiccup2.core :as h]))

(defn jit-status-page
  "Generates the JIT status page HTML body."
  []
  (str
    (h/html
      [:h1 "JIT status"]

      [:p.banner
       "The CPJIT layer is feature-complete on the dev host. The "
       "stencil substrate, host-format extractors, runtime patcher, "
       "embed API, CLI, env-var surface, and " [:code "mino-lean"]
       " parallel build are all in place; the per-host verification "
       "matrix lives on the "
       [:a {:href "/documentation/jit-matrix/"} "JIT support matrix"]
       " page. New stencils are out of scope for this cycle and "
       "queue for a future opcode-expansion cycle; bug fixes, "
       "portability fixes, and perf-neutral cleanup remain in "
       "scope after this declaration. This is not a freeze."]

      [:h2 "Feature-complete checklist"]

      [:p "A box is checked when the evidence is linkable, not when "
       "the work is claimed done verbally. The local boxes are "
       "verified on the dev host; the deferred boxes promote the "
       "claim from \"feature complete on the dev host\" to \"feature "
       "complete and continuously verified by CI\" after the first "
       "push lands on the upstream remote."]

      [:h3 "Local — verified at v0.252.0"]
      [:ul
       [:li [:strong "Stencils registered. "]
        "39 opcode stencils live on every supported host arch, "
        "covering arithmetic (" [:code "OP_ADD_II"] ", "
        [:code "OP_SUB_II"] ", " [:code "OP_MUL_II"] ", "
        [:code "OP_ADD_IK"] ", " [:code "OP_SUB_IK"] "), comparison ("
        [:code "OP_EQ_II"] ", " [:code "OP_LT_II"] ", "
        [:code "OP_LE_II"] ", " [:code "OP_GT_II"] ", "
        [:code "OP_GE_II"] ", k-immediate variants), fused loops ("
        [:code "OP_LOOP_INT_LT"] ", " [:code "OP_LOOP_INT_DEC"] ", "
        [:code "OP_LOOP_INT_LT_INC"] "), data-structure fast paths ("
        [:code "OP_NTH_VEC"] ", " [:code "OP_FIRST_VEC"] ", "
        [:code "OP_COUNT_VEC"] ", " [:code "OP_EMPTY_VEC"] ", "
        [:code "OP_CONJ_VEC"] ", " [:code "OP_GET_KW_MAP"] ", "
        [:code "OP_ASSOC"] "), dispatch ("
        [:code "OP_CALL"] ", " [:code "OP_CALL_CACHED"] ", "
        [:code "OP_TAILCALL"] ", " [:code "OP_GETGLOBAL_CACHED"]
        ", " [:code "OP_CLOSURE"] "), env management ("
        [:code "OP_PUSH_ENV"] ", " [:code "OP_POP_ENV"] ", "
        [:code "OP_ENV_BIND"] "), and the leaf shapes "
        "(" [:code "OP_LOAD_K"] ", " [:code "OP_MOVE"] ", "
        [:code "OP_RETURN_ARG0"] ", " [:code "OP_RETURN_IMM"]
        ", " [:code "OP_LOAD_K_RETURN"] ", " [:code "OP_INC_I"]
        ", " [:code "OP_DEC_I"] ", " [:code "OP_ZERO_INT_P"]
        ")."]

       [:li [:strong "5 host arches with on-disk byte tables. "]
        "Each generated header carries ~52-56 KiB of stencil bytes "
        "plus the symbol and relocation tables:"
        [:ul
         [:li [:code "stencils_arm64_darwin.h"] " — 55,805 bytes"]
         [:li [:code "stencils_arm64_linux.h"] " — 55,927 bytes"]
         [:li [:code "stencils_x86_64_darwin.h"] " — 52,031 bytes"]
         [:li [:code "stencils_x86_64_linux.h"] " — 51,613 bytes"]
         [:li [:code "stencils_x86_64_windows.h"] " — 53,429 bytes"]]]

       [:li [:strong "Dual-binary build shipping. "]
        "The full " [:code "mino"] " binary builds with "
        [:code "-DMINO_CPJIT=1"] " and links the entire JIT pipeline. "
        "The parallel " [:code "mino-lean"] " binary builds the same "
        "source tree with " [:code "MINO_CPJIT"] " undefined; the "
        "patcher, emitter, and stencil entry layers compile to no-ops "
        "and the runtime decision tree collapses to the tree-walker "
        "plus the bytecode VM. " [:code "mino_state_jit_capability"]
        " returns " [:code "{.available=0, ...}"] " on "
        [:code "mino-lean"] " and " [:code "{.available=1, ...}"]
        " on the full build, with " [:code "host_arch"] " and "
        [:code "host_os"] " populated from compile-time defines."]

       [:li [:strong "4-way parity green on the dev host. "]
        "The " [:code "test-jit-parity"] " task runs the test suite "
        "four times — " [:code "MINO_JIT=auto"] ", "
        [:code "MINO_JIT=on"] ", " [:code "MINO_JIT=off"] ", and the "
        [:code "mino-lean"] " binary — and asserts the four stdouts "
        "are byte-identical and all four processes exit 0:"
        [:pre [:code {:data-lang "text"}
"$ ./mino task test-jit-parity
47 tests, 47 assertions: 47 passed, 0 failed, 0 errors
  jit-parity: OK -- stdout byte-identical across
              jit-auto / jit-on / jit-off / lean, all exit 0"]]]

       [:li [:strong "Synthetic-blob selftests pass. "]
        "The " [:code "tools/stencil-extract --selftest"] " binary "
        "builds hand-crafted Mach-O / ELF / COFF object blobs with "
        "known function bodies, symbol tables, and relocation "
        "tables, then runs each format parser against them and "
        "asserts the extracted bytes match expected values:"
        [:pre [:code {:data-lang "text"}
"$ ./tools/stencil-extract --selftest
selftest_macho_synthetic: OK
selftest_elf_synthetic: OK
selftest_coff_synthetic: OK
stencil_extract selftest: OK"]]]

       [:li [:strong "Embed API stable. "]
        "The five public symbols below have stayed source-compatible "
        "since v0.240 and remain stable through the cycle close:"
        [:pre [:code {:data-lang "c"}
"void                  mino_state_set_jit_mode(mino_state_t *S,
                                              mino_jit_mode_t mode);
mino_jit_mode_t       mino_state_jit_mode(const mino_state_t *S);

void                  mino_state_set_jit_hot_threshold(mino_state_t *S,
                                                       unsigned n);
unsigned              mino_state_jit_hot_threshold(const mino_state_t *S);

mino_jit_capability_t mino_state_jit_capability(const mino_state_t *S);"]]
        "CLI flags " [:code "--jit=auto|off|on"] " and "
        [:code "--jit-threshold=N"] " call through to "
        [:code "mino_state_set_jit_mode"] " / "
        [:code "mino_state_set_jit_hot_threshold"] "; env vars "
        [:code "MINO_JIT"] " and " [:code "MINO_JIT_HOT_THRESHOLD"]
        " set the per-state default at " [:code "mino_state_new"]
        " time."]

       [:li [:strong "Known limitations documented. "]
        "Each of the following is by-design, not a regression:"
        [:ul
         [:li [:strong "No type-feedback specialization. "]
          "Stencils dispatch on opcode shape, not on per-call-site "
          "type history. A future cycle can add an IC layer that "
          "captures observed types and patches a fast path; the "
          "current cycle holds the substrate at the interpreter-"
          "parity contract."]
         [:li [:strong "No deoptimization. "]
          "The JIT does not speculate beyond what the bytecode "
          "compiler already proves at compile time. If the type or "
          "shape changes at runtime, the slow-path symbol the "
          "stencil references handles it; nothing has to be undone."]
         [:li [:strong "No adaptive stencil expansion. "]
          "The opcode surface is frozen at 39 stencils for this "
          "cycle. Adding a new stencil opens a new cycle."]]]]

      [:h3 "Deferred — pending first push"]
      [:p "The boxes below remain pending until "
       [:code "cpjit-cycle"] " rebases onto " [:code "main"] " on "
       "the upstream remote and the CI matrix fires its first "
       "matrix-wide run. Each will flip green with a linked workflow "
       "run URL when that decision lands; until then the local "
       "boxes above carry the feature-complete claim on their own."]
      [:ul
       [:li "GHA matrix green on "
        [:code "ubuntu-24.04"] " / " [:code "ubuntu-24.04-arm"]
        " / " [:code "macos-14"] " / " [:code "windows-2022"]
        " — pending first push."]
       [:li "Cross-compile parity green on "
        [:code "macos-14"] " (regenerated stencils, "
        [:code "git diff --exit-code"] ") — pending first push."]
       [:li "Nightly extended suite green (release-gate + GC "
        "stress + fault injection + embedding stress on the "
        "non-Windows hosts, 04:00 UTC daily) — pending first push."]]

      [:h2 "Embed API surface"]
      [:p "The five-function embed surface for runtime JIT control "
       "lives in " [:code "mino.h"] ":"]

      [:h3 "Mode control"]
      [:p [:code "mino_state_set_jit_mode"] " sets the per-state "
       "mode; " [:code "mino_state_jit_mode"] " reads it back. "
       "Modes are " [:code "MINO_JIT_MODE_AUTO"] " (compile when "
       "hot threshold trips, default), " [:code "MINO_JIT_MODE_OFF"]
       " (never compile), and " [:code "MINO_JIT_MODE_ON"]
       " (compile on first call). " [:code "ON"]
       " is for benchmarking and parity testing; " [:code "AUTO"]
       " is the default for embedders."]

      [:h3 "Hot threshold"]
      [:p [:code "mino_state_set_jit_hot_threshold"] " sets the "
       "call-count after which a function compiles under "
       [:code "AUTO"] "; "
       [:code "mino_state_jit_hot_threshold"] " reads it back. "
       "Default is the compile-time " [:code "MINO_JIT_THRESHOLD"]
       " seed (currently 10 calls). Lower for shorter-lived "
       "scripts where the warm-up phase matters; raise to avoid "
       "compiling rarely-called functions in long-lived embedders."]

      [:h3 "Capability discovery"]
      [:p [:code "mino_state_jit_capability"] " returns a struct "
       "with " [:code ":available"] ", " [:code ":mode"] ", "
       [:code ":threshold"] ", " [:code ":host_arch"] ", and "
       [:code ":host_os"] " fields. Embedders use this at startup "
       "to size their tuning before any script runs; "
       [:code "mino-lean"] " returns "
       [:code "{.available=0, ...}"] " so a host build that depends "
       "on JIT throughput knows to fall back."]

      [:h2 "Perf thresholds for the next cycle"]
      [:p "The runtime-perf track that follows feature-complete "
       "uses the threshold structure captured in "
       [:code ".local/perf-thresholds.md"] " (developer-side, not "
       "in the public repo). Headline numbers:"]
      [:ul
       [:li [:strong "Per-release regression ceiling: "]
        "no realistic_bench row regresses by more than 10% vs cycle "
        "baseline."]
       [:li [:strong "Per-release gain ratchet: "]
        "chosen row moves >= 10% vs cycle baseline, OR the release "
        "is declared perf-neutral cleanup (regression-only check)."]
       [:li [:strong "Cycle exit gate: "]
        "at least one \"hot row\" >= 10% vs cycle baseline before "
        "the cycle closes."]
       [:li [:strong "Measurement discipline: "]
        "median of three runs minimum for any number that enters "
        "a CHANGELOG perf table; +/- 7% on a single run reads as "
        "\"no change\"; below 7% requires a 3-run confirmation."]]
      [:p "Hot rows (subject to the gain ratchet): "
       [:code "build 5k int-map and sum"] ", "
       [:code "bump 5k int-map values"] ", "
       [:code "nested vectors 500x100"] ", "
       [:code "realize 10k of lazy range"] ". "
       "Floor rows (regression-only canaries): "
       [:code "map/filter/map/reduce over 50k"] " and "
       [:code "fibonacci(25)"] "."]

      [:h2 "Out of scope this cycle"]
      [:ul
       [:li [:strong "New stencils. "]
        "Opcode surface stays at 39 stencils until a future "
        "opcode-expansion cycle."]
       [:li [:strong "Type-feedback specialization, adaptive "
             "stencil expansion, deoptimization machinery. "]
        "All deliberately by-design omissions in the current "
        "soundness model. Adding any one of them opens a new "
        "cycle."]
       [:li [:strong "Cross-module-leak static analysis "]
        "for the stencil extractor. Synthetic-blob selftests cover "
        "regression detection; static analysis is nice-to-have, "
        "not feature-complete-blocking."]
       [:li [:strong "Self-hosted Intel Mac runner "]
        "for x86_64 Darwin end-to-end verification. Operational, "
        "not code; the cross-compile parity job is the documented "
        "floor."]]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/jit-matrix/"}
             "JIT support matrix"]
        ": per-host verification posture and the on/off A/B "
        "evidence captured at v0.251."]
       [:li [:a {:href "/documentation/bytecode-vm/#cpjit"}
             "Bytecode and VM -> The CPJIT layer"]
        ": the architectural tour of the stencil substrate, "
        "ICache discipline, and what the runtime patcher does."]
       [:li [:a {:href "/documentation/performance/"} "Performance"]
        ": the runtime-perf track that follows feature-complete."]])))
