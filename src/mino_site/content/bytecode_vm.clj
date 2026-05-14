(ns mino-site.content.bytecode-vm
  "Internals page for the bytecode VM: value representation, opcode
  encoding, dispatch, fast paths, inline caches, soundness, and a
  comparison to other small VMs."
  (:require
    [hiccup2.core :as h]))

(defn bytecode-vm-page
  "Generates the Bytecode VM internals page HTML body."
  []
  (str
    (h/html
      [:h1 "Bytecode and VM"]

      [:p.banner
       "mino runs a register-based bytecode VM layered on top of a "
       "tree-walking evaluator. The tree-walker is the ground-truth "
       "interpreter and the spec for semantics; the VM is an additive "
       "fast path that compiles function bodies lazily on first call "
       "and dispatches the result through a fixed-width 32-bit "
       "instruction stream. Any form the compiler does not recognise "
       "declines back to the tree-walker for that call, so behaviour "
       "is identical across both paths."]
      [:p "This page is a tour, not a reference. For the per-opcode "
       "detail, fast-lane shapes, fold rules, and benchmark deltas "
       "behind each landing, the "
       [:a {:href "/changelog/"} "changelog"]
       " carries the granular notes; the entries from v0.105.0 "
       "through v0.145.0 cover the bytecode VM end-to-end."]

      [:h2 "Value representation"]
      [:p "Every mino value flows through the runtime as a "
       [:code "mino_val_t *"] ". The low three bits of the pointer "
       "carry a tag that picks between a heap object and one of four "
       "inline shapes:"]
      [:pre [:code
"tag 000  ->  heap pointer to a struct mino_val
tag 001  ->  inline 61-bit signed int (payload in bits 63..3)
tag 010  ->  inline BOOL          (one bit at offset 3)
tag 011  ->  inline NIL           (the constant pointer itself)
tag 100  ->  inline CHAR          (21-bit Unicode codepoint)
tag 101..111 -> reserved"]]
      [:p "61-bit inline ints cover the range ±2"
       [:sup "60"]
       " (~±1.15×10"
       [:sup "18"]
       "). Anything wider widens to a heap-allocated BigInt. The "
       "decode relies on arithmetic right shift of a signed integer, "
       "which C99 leaves implementation-defined for negative operands; "
       "every supported toolchain (clang, gcc, msvc on x86_64 and "
       "arm64) implements it as sign-preserving. 64-bit hosts only."]
      [:p "The tag scheme has practical consequences. Tight integer "
       "loops skip allocation entirely because the inline-int payload "
       "lives in a register or stack slot. Tag tests are a single "
       [:code "AND"] " with " [:code "0x7"] " against a known "
       "constant. Heap pointers are 8-byte aligned by construction, so "
       "the bottom three bits are always zero and the runtime can "
       "discriminate the inline cases without touching the heap object."]

      [:h2 "Instruction encoding"]
      [:p "Instructions are 32-bit unsigned words in one of three "
       "shapes:"]
      [:pre [:code
"ABC   :  op (8)  | A (8)  | B (8)  | C (8)
ABx   :  op (8)  | A (8)  | Bx (16)
AsBx  :  op (8)  | A (8)  | sBx (16, biased by 0x8000)"]]
      [:p "Opcodes occupy the low byte. " [:code "A"] " is the "
       "destination register for the common ABC shape. "
       [:code "Bx"] " is an unsigned 16-bit index into the const pool "
       "or a wide immediate; " [:code "sBx"] " is the signed variant "
       "used by jumps so a zero offset encodes a no-op jump. The "
       "fixed-width design simplifies fetch and decode: the dispatch "
       "loop reads one 32-bit word, masks out the op byte, and switches."]
      [:p "Each compiled function record (" [:code "mino_bc_fn_t"]
       ") carries the instruction stream, a const pool indexed by "
       [:code "Bx"] ", a register count, an array of arity clauses, "
       "and an inline-cache slot array. The " [:code "MINO_FN"]
       " heap value owns one " [:code "mino_bc_fn_t"] " across all "
       "closures built from the same template, so two closures of "
       [:code "(fn [i] (fn [] i))"] " share the IC slots even though "
       "each carries its own captured environment."]

      [:h2 "Dispatch and the register window"]
      [:p "The runtime maintains a single growable register stack on "
       "the state (" [:code "S->bc_regs"]
       "). Each entry into a compiled function pushes a window of "
       [:code "n_regs"] " slots onto the stack and points "
       [:code "regs"] " at the window base. Arguments arrive in "
       [:code "regs[0..n_params)"]
       ", optionally followed by a collected rest list, and the body "
       "compiles to read and write within the window. On return the "
       "window is popped and the result lands at the caller's "
       "designated " [:code "ret_base"] " slot."]
      [:p "Dispatch is a single " [:code "switch"]
       " over the op byte inside one C function ("
       [:code "mino_bc_run"] "). The switch lets gcc and clang emit "
       "a jump table on platforms that have it; computed-goto "
       "dispatch is not used — the readability win of a single "
       "branch-and-decode loop wins against a fragile portability "
       "footprint, and the per-op cost is already low enough that the "
       "dispatch is rarely the bottleneck."]
      [:p "Any opcode that can re-enter user code (calls, global "
       "resolution, closure construction, var redefinition) re-reads "
       "the window pointer from " [:code "S->bc_regs + base"]
       " on the next cycle so a recursive " [:code "mino_bc_run"]
       " that triggers register-stack growth — and therefore "
       "reallocation — does not leave the outer frame with a dangling "
       "pointer."]

      [:h2 "Opcode catalog"]
      [:p "Operations group into a small handful of families. The "
       "complete enum and per-op encoding live in "
       [:code "src/eval/bc/internal.h"] "."]
      [:ul
       [:li [:strong "Generic forms."]
        " " [:code "OP_MOVE"] ", " [:code "OP_LOAD_K"] ", "
        [:code "OP_JMP"] ", " [:code "OP_JMPIFNOT"]
        ". Register-to-register move, const-pool load, and jumps."]
       [:li [:strong "Global resolution."]
        " " [:code "OP_GETGLOBAL_CACHED"]
        " (the form actually emitted) and " [:code "OP_SETGLOBAL"]
        ". The cached read goes through an inline-cache slot; see the "
        "section below."]
       [:li [:strong "Calls and returns."]
        " " [:code "OP_CALL"] ", " [:code "OP_TAILCALL"] ", "
        [:code "OP_RETURN"] ", " [:code "OP_CLOSURE"]
        ". Hot calls pass an argv slice straight from the caller's "
        "register window with no cons-spine allocation; tail calls "
        "produce a sentinel that an internal trampoline consumes so "
        "deep tail recursion stays on a constant-size C stack."]
       [:li [:strong "Integer fast lanes."]
        " Per-op opcodes for tagged-int arithmetic, comparisons, and "
        "the common unary forms (" [:code "inc"] ", " [:code "dec"] ", "
        [:code "zero?"] ", " [:code "pos?"] ", " [:code "neg?"] ", "
        [:code "even?"] ", " [:code "odd?"]
        "). Overflow on " [:code "+/-/*"]
        " bails to the boxed slow lane via the compiler builtin so "
        "Clojure's throw-on-overflow semantics stay intact. "
        "Immediate-operand variants fold a signed 8-bit literal into "
        "the " [:code "C"] " slot so "
        [:code "(< i 10)"] " avoids loading the literal separately."]
       [:li [:strong "Fused counted loops."]
        " Two common " [:code "loop"]
        " shapes — single-binding and two-binding inc/dec — emit a "
        "single fused opcode at the recur target. Each iteration is "
        "one decode plus one or two tagged-int updates and a back-jump. "
        "Emission is gated on canonical-prim resolution so a user "
        [:code "(defn dec ...)"] " shadow is honoured."]
       [:li [:strong "Collection fast lanes."]
        " " [:code "OP_NTH_VEC"]
        " reads a persistent vector's leaf directly; "
        [:code "OP_GET_KW_MAP"]
        " does a single hash + HAMT lookup. Any miss falls back to "
        [:code "prim_nth"] " / " [:code "prim_get"] "."]
       [:li [:strong "try / catch / throw and dynamic bindings."]
        " " [:code "OP_PUSHCATCH"] " / " [:code "OP_POPCATCH"] " / "
        [:code "OP_THROW"]
        " bracket a try region; the handler runs in the same bc "
        "frame with register window and dyn stack restored. "
        [:code "OP_PUSHDYN"] " / " [:code "OP_POPDYN"]
        " manage a dyn frame for " [:code "binding"]
        "; the cached-global IC skips the cache while a dyn stack is "
        "active so a stale var root never masks a dyn-shadowed value."]
       [:li [:strong "Lazy sequences."]
        " " [:code "OP_MAKE_LAZY"]
        " constructs a thunk from a const-pool function value. "
        "Realisation flows back through the apply path so an "
        "un-compileable lazy body drops to the tree-walker without "
        "breaking the value-level contract."]
       [:li [:strong "Env management for capturing forms."]
        " " [:code "OP_PUSH_ENV"] " / " [:code "OP_POP_ENV"] " / "
        [:code "OP_ENV_BIND"]
        " bracket let scopes only when an inner fn closes over the "
        "outer bindings. Fns without inner fns skip these ops "
        "entirely; the common case pays no env-frame cost."]]

      [:h2 "Inline cache for globals"]
      [:p "Every reference to a global name in a compiled body uses "
       "an IC slot that holds the resolved value plus an "
       [:code "ic_gen"]
       " snapshot. The state-wide counter bumps on "
       [:code "def"] " / " [:code "ns-unmap"] " / "
       [:code "var-set-root"] " / " [:code "var-unintern"]
       "; a bumped counter misses every slot in lockstep, no "
       "per-slot invalidation work. The cache fills only when no "
       "dyn-bindings are active, so the stored value is always the "
       "var's published root and never a dyn-shadowed value."]

      [:h2 "Compile-time folding and dead-binding elimination"]
      [:p "Two optimisations operate on the AST before emission, "
       "both gated by " [:code "ic_gen"] " coherence:"]
      [:ul
       [:li [:strong "Let-binding fold-through."]
        " When a let's right-hand side folds via the "
        [:code "PURE_PRIMS"]
        " table, the compiler remembers the folded value on the "
        "local. A later pure-prim call whose args all resolve "
        "through this substitution folds the whole expression. "
        [:code "(let [x (+ 1 2)] (* x x))"]
        " collapses to a single " [:code "OP_LOAD_K 9"] "."]
       [:li [:strong "Dead-binding elimination."]
        " A let binding whose name appears nowhere in the body and "
        "whose right-hand side is observably side-effect-free is "
        "dropped at compile time. Macros that expand to verbose "
        [:code "(let [_ ... ...] body)"]
        " around a binding the body never reads collapse to just the "
        "body."]]
      [:p "Capturing let scopes (those that publish bindings into an "
       "env for an inner closure) opt out of both folds. Identity "
       "short-circuits in the prim layer give "
       [:code "(assoc m k v)"]
       " and set " [:code "conj"]
       " the property that "
       [:code "(identical? m (assoc m k (get m k)))"]
       " holds — a real signal callers can rely on, affordable "
       "because cached hashes keep the equality check O(1) in the "
       "typical no-match case."]

      [:h2 "Calling convention"]
      [:p "Two callee ABIs coexist:"]
      [:ul
       [:li [:strong "argv ABI."]
        " Bytecode-runnable user fns and the prims registered through "
        [:code "mino_prim_argv"]
        " receive an argv pointer plus a count directly. "
        [:code "OP_CALL"]
        " hands the caller's register slice through as the argv with "
        "zero cons cells allocated for the call. This is the hot path."]
       [:li [:strong "Cons-spine ABI."]
        " Fn1 prims, tree-walker user fns, macros, and non-fn "
        "callables receive a cons list of evaluated arguments. The "
        "call site builds the list lazily on the slow path so the "
        "hot path pays nothing."]]
      [:p "Tail calls produce a " [:code "MINO_TAIL_CALL"]
       " sentinel that the call-site trampoline consumes; bc-runnable "
       "tail targets unpack it back to argv and re-enter "
       [:code "mino_bc_run"]
       " in the same dispatch loop iteration so deep tail recursion "
       "stays on a constant-size C stack. Multi-arity fns carry an "
       "array of " [:code "mino_bc_clause_t"]
       " entries; the runtime picks the matching clause at fn entry "
       "and seats argv into the chosen clause's parameter registers, "
       "collecting trailing args into a rest list when "
       [:code "has_rest"] " is set."]

      [:h2 "GC integration"]
      [:p "mino's collector is a non-moving two-generation tracing "
       "collector. The VM cooperates with it in three places:"]
      [:ul
       [:li [:strong "Register stack roots."]
        " The GC root walk scans every live register slot in "
        [:code "[0, bc_top)"]
        " on " [:code "S->bc_regs"]
        ". The window grows on demand; "
        [:code "bc_pop_window"]
        " clears every slot before the next push lands on it so "
        "stale references do not pin objects past their last use. "
        "The body emitter writes to every register before any op "
        "that may collect, so the root walk sees filled slots rather "
        "than uninitialised state."]
       [:li [:strong "Const pool and IC slots."]
        " The " [:code "MINO_FN"]
        " mark pass walks the bc fn's const pool and IC slot array "
        "so children and cached resolutions stay reachable across "
        "minors. The write barrier records old-to-young pointers when "
        "an IC slot in an old fn is filled with a young var root, so "
        "the next minor's remset is honest without per-op pinning."]
       [:li [:strong "Trampoline-safe handoff."]
        " The argv passed to a callee is the live register slice of "
        "the caller. Because the slice lives on the GC-rooted "
        "register stack and the callee re-reads its base on every "
        "cycle, an allocation inside the callee cannot leave the "
        "argv with a dangling tail."]]

      [:h2 "Soundness considerations"]
      [:p "Worth being honest: the recent opcode-fusion and "
       "compile-time-fold work is the kind of optimisation that is "
       "easy to get subtly wrong. New tests and adversarial fuzzing "
       "give us confidence it is fairly stable, but the hard problem "
       "underneath is knowing in advance which aspects of Clojure are "
       "truly static and predictable so the compiler can treat them "
       "as axioms."]
      [:p "Every optimisation either has to be invariant under every "
       "reachable redefinition of a Clojure name, or carry a "
       "coherence check that catches a redefinition before the next "
       "dispatch. The current discipline:"]
      [:ul
       [:li [:strong "Canonical-prim resolution check."]
        " Fused-loop and fast-lane emission only fires when the "
        "symbol at the head resolves to the canonical core prim. A "
        "user " [:code "(defn dec [x] ...)"]
        " shadow declines the fused step automatically."]
       [:li [:strong "ic_gen coherence."]
        " Any optimisation that bakes in a global resolution at "
        "compile time stashes the state's " [:code "ic_gen"]
        " snapshot on the fn. A mismatch at dispatch invalidates "
        "the bc and forces recompile, so "
        [:code "(def + -)"] " re-emits the body."]
       [:li [:strong "Side-effect-free check."]
        " Dead-binding elimination only drops bindings whose RHS the "
        "compiler can see through to a literal, symbol, or "
        "pure-prim call. Anything else fails closed."]
       [:li [:strong "Capture opt-out."]
        " Both folds skip let scopes that publish bindings into an "
        "env for an inner closure — env publishing is itself "
        "observable."]
       [:li [:strong "Overflow handling."]
        " Tagged-int fast lanes decline to the boxed slow lane on "
        "overflow so " [:code "(dec MIN_INT)"]
        " still throws Clojure-correct."]]
      [:p "The conservative shape of these checks is the cost of "
       "playing this game with Clojure's late-bound semantics. A "
       "statically-typed dialect could do more here; a dialect with "
       "mutable globals would have to do less."]

      [:h2 "What this design borrows and where it differs"]
      [:ul
       [:li [:strong "Lua 5.5."]
        " Register-based dispatch, fixed-width ABC/ABx/AsBx encoding, "
        "and a switch interpreter loop are direct Lua influences. "
        "mino differs by using low-tag pointer tagging rather than "
        "NaN-boxing (better for arbitrary-precision ints), by treating "
        "persistent vectors / HAMTs / lists / sets as distinct heap "
        "shapes rather than collapsing them into Lua's tables, and "
        "by having no JIT."]
       [:li [:strong "Janet."]
        " Small, embeddable, register-based, no JIT, ship the "
        "bytecode interpreter and call it done — matches mino's "
        "ethos closely. mino differs by being a Clojure dialect: "
        "persistent immutable values are the default, lazy sequences "
        "are first-class, STM and agents are in the core surface, "
        "and a per-state recursive mutex serialises script execution "
        "rather than Janet's fiber-cooperative model."]
       [:li [:strong "BEAM (Erlang / Elixir)."]
        " Per-process isolation, message-passing-only communication, "
        "and a preemptive scheduler are a fundamentally different "
        "concurrency model. mino shares a heap within a single "
        [:code "mino_state_t"]
        " and recovers BEAM's isolation property at a coarser grain "
        "via multiple runtimes and " [:code "mino_clone"]
        ". The per-state GIL is explicit rather than emergent."]]

      [:h2 "Why Clojure makes some of this work"]
      [:p "Several optimisations that would be unsound in a Lua-style "
       "or generic-Scheme VM become natural in a Clojure dialect:"]
      [:ul
       [:li [:strong "Homoiconicity."]
        " A " [:code "let"]
        " is a list with a vector of binding pairs, so fold-through "
        "and dead-binding elimination are ordinary AST rewrites "
        "rather than IR passes."]
       [:li [:strong "Immutable bindings."]
        " A let name never observably changes between bind and body, "
        "so the compiler can substitute without worrying about "
        "aliasing. The same property lets fused-loop registers "
        "update in place safely."]
       [:li [:strong "Persistent immutable collections."]
        " A direct trie walk is safe because the trie is immutable. "
        "Subvecs share a parent trie and the walker honours the "
        "window without materialising it. Identity short-circuits on "
        [:code "assoc"] " / " [:code "conj"]
        " are observable because " [:code "identical?"]
        " is a real signal."]
       [:li [:strong "Cached hashes."]
        " Structural-equality compares short-circuit on hash mismatch "
        "in O(1), which is what makes the identity short-circuit "
        "cheap in practice rather than only when the lookup happens "
        "to miss."]
       [:li [:strong "Lazy sequences as values."]
        " " [:code "OP_MAKE_LAZY"]
        " emits against a thunk const and trusts the GC to keep its "
        "captured env alive. Realisation flows back through the apply "
        "path so an un-compileable lazy body drops to the tree-walker "
        "transparently."]
       [:li [:strong "Late binding plus a coherence epoch."]
        " The " [:code "ic_gen"]
        " counter turns Clojure's late-bound vars into a single "
        "load-compare per cached site. The optimiser checks at "
        "dispatch against the snapshot; on a redefinition the bc "
        "invalidates and recompiles."]]

      [:h2 "Why no JIT yet"]
      [:p "A JIT is plausible — LuaJIT-style tracing or BEAM-style "
       "native compilation are both well-understood directions — but "
       "mino does not have one. The cost matters:"]
      [:ul
       [:li "A code-gen backend (DynASM, LLVM, or a bespoke "
        "assembler) plus the platform matrix that goes with it. mino "
        "currently builds as one self-contained C99 compile with no "
        "runtime page-protection ceremony, no signal-handler hooks, "
        "and no per-platform EH wiring. A JIT changes all three."]
       [:li "Every fast lane the bc compiler emits would need a "
        "JIT-time mirror with the same canonical-prim and "
        [:code "ic_gen"]
        " checks. The soundness surface grows against exactly the "
        "uncertainty the section above is already wrestling with."]
       [:li "Target workloads — config evaluation, rules engines, "
        "scripting consoles, plugins, embedded automation — do not "
        "need it. Hot inner loops already run at near-native speed "
        "through fused-loop opcodes; the bottlenecks left are "
        "allocation and dispatch a JIT would also handle "
        "conservatively."]]
      [:p "If a real production workload eventually demands hot-loop "
       "throughput beyond what fused bytecode delivers, JIT becomes "
       "worth doing. Until then, budget is better spent on widening "
       "compiler coverage and tightening soundness."]

      [:h2 "Recently picked up"]
      [:p "Five frontiers from earlier drafts of this page have "
       "shipped and folded into the steady-state VM. The benchmark "
       "deltas below are from the cycle's first-pass landings (all "
       "min-of-five, with the empty-thunk harness floor subtracted)."]
      [:ul
       [:li [:strong "Write-path fast lanes."]
        " " [:code "OP_CONJ_VEC"] " for arity-2 "
        [:code "(conj v x)"] " and " [:code "OP_ASSOC"]
        " for the arity-3 " [:code "(assoc coll k v)"]
        " shape land in v0.152.0. The runtime dispatches by "
        "collection type (vector or map) and falls back to the "
        "canonical prim on any miss. "
        [:em "conj-vec -34%, assoc-vec -56%, assoc-small -32%."]]
       [:li [:strong "Inlining of small pure prims."]
        " " [:code "OP_FIRST_VEC"] " / " [:code "OP_COUNT_VEC"]
        " / " [:code "OP_EMPTY_VEC"]
        " in v0.153.0 turn the hot single-arg seq prims on a "
        "vector into a direct field read; misses fall through to "
        "the canonical prim. "
        [:em "count-vec -94%, first-vec -93%, empty?-vec ~0 ns."]]
       [:li [:strong "Record fast paths and keyword-as-fn inlining."]
        " v0.154.0 extends " [:code "OP_GET_KW_MAP"]
        " with a record branch (fixed-slot fetch after a declared-"
        "field lookup), and rewrites the " [:code "(:kw coll)"]
        " keyword-as-fn shape to the same fast lane the "
        [:code "(get coll :kw)"] " form already used. "
        [:em "get-kw-record -93%, kw-fn-record -84%, kw-fn-map -77%."]]
       [:li [:strong "Inline-cached call sites."]
        " v0.155.0 fuses the global-symbol head resolution and the "
        "dispatch into one " [:code "OP_CALL_CACHED"] " opcode that "
        "re-uses the " [:code "OP_GETGLOBAL_CACHED"]
        " ic-slot discipline. Dynamic bindings and closure captures "
        "still shadow even on a hot site; the cached path drops a "
        "register window slot and one inline-cache lookup per call. "
        [:em "fib-30 -13%, loop-recur-1M -9%."]]
       [:li [:strong "Generic get and dissoc fast lanes."]
        " v0.156.0 opens " [:code "OP_GET_KW_MAP"]
        " to any hashable key on a map (the keyword guard now only "
        "fences the record branch), and adds " [:code "OP_DISSOC"]
        " for the arity-2 " [:code "(dissoc m k)"] " shape. Records, "
        "sorted-maps, transients, and variadic dissoc keep the "
        "canonical-prim path. "
        [:em "get-str-map -81%, dissoc-map -21%."]]]

      [:h2 "Still open"]
      [:p "Hypotheses worth picking up. Each line is a one-liner; "
       "the shape of the work and rough payoff are obvious from the "
       "sketch."]
      [:ul
       [:li [:strong "Dispatch shape rework."]
        " Both standard alternatives to the current switch have "
        "been tried and regressed. Apple clang at -O2 tail-merges "
        "every per-handler "
        [:code "goto *target"]
        " into a single dispatch site, collapsing the threaded "
        "interpreter back to switch shape; "
        [:code "asm goto"]
        " with an explicit label list is the canonical workaround "
        "but Apple clang miscompiles register-held label addresses. "
        [:code "[[clang::musttail]]"]
        " per-handler dispatch was tried in the v0.117.0 era and "
        "regressed too: per-handler prolog/epilog plus locals reload "
        "beat the stack-growth savings on short bodies. A real fix "
        "needs either a new compiler optimization or a different "
        "architecture (e.g., shared-locals threaded dispatch via "
        "explicit register pinning)."]
       [:li [:strong "Profile-guided opcode rewriting."]
        " Hot sites swap generic opcodes for specialised variants "
        "after a stable shape is observed — adaptive specialisation "
        "without a JIT. Type-feedback fast lanes for arith on "
        "observed int+int sites would extend the literal-arg "
        "fast lanes already shipped, and a runtime "
        [:code "OP_CALL"] " -> " [:code "OP_CALL_CACHED"]
        " rewrite would cover closure-bound heads the compiler "
        "can't statically prove. A first probe in this direction "
        "added an " [:code "OP_TAILCALL_CACHED"]
        " variant to test the substantiation; the new opcode itself "
        "worked but the added switch case bloated the dispatch loop "
        "in a way that regressed fib-30 by ~7%, even though fib's "
        "recursive calls are non-tail. The switch dispatcher has "
        "reached its case-count ceiling: every new opcode hurts the "
        "compiler's branch placement on the existing hot cases. "
        "Runtime rewriting needs the dispatch-shape rework above "
        "first, so the cached family can grow without taxing the "
        "central switch."]
       [:li [:strong "Transducer / lazy-seq force fusion."]
        " " [:code "(reduce f init (take n (filter p (map g s))))"]
        " compiled into a single per-element step that fuses the "
        "four operations without realising intermediates. Today "
        "the pipeline microbench "
        [:code "(->> (range 1000) (map inc) (filter odd?) "
                    "(take 100) (reduce + 0))"]
        " runs at ~85 µs/iter; a fused walker should drop it to "
        "~10–20 µs (one allocation per chain instead of N). The "
        "shape of the work: post-macroexpansion "
        [:code "->>"]
        " recognition in the compiler, transducer-shape matching "
        "across the named heads (each with its own 1-arg "
        "transducer-returning vs 2+-arg sequence-returning "
        "arity), a fused per-element opcode preserving the "
        [:code "reduced"]
        " short-circuit contract, and exception-safe cleanup of "
        "partially-walked chains. Multi-day; the most natural "
        "design fuses "
        [:code "->>"]
        " to the same C path "
        [:code "transduce"]
        " already takes, so user-side "
        [:code "transduce"]
        " calls benefit too."]
       [:li [:strong "Fused BigInt arithmetic."]
        " Multi-step BigInt op stays in BigInt form across the "
        "chain, avoiding the re-tag roundtrip per step."]
       [:li [:strong "Parallel reduce on persistent vectors."]
        " Fork/join trie walk via " [:code "fold"]
        " / " [:code "cat"]
        ", combined with a runtime-per-shard model."]
       [:li [:strong "Full TCO across more tail positions."]
        " Propagate the tail flag through every tail-shaped form "
        "so cross-fn tail calls stay constant-stack regardless of "
        "which side is compiled."]]

      [:h2 "Beyond opcodes: a formal Clojure spec"]
      [:p "The soundness discipline keeps coming back to one "
       "question: which properties of Clojure are safe to treat as "
       "axioms? Right now the answer is a case-by-case judgement "
       "call. A separate, much larger project — a formal and "
       "executable Clojure language spec plus a meta-analysis engine "
       "written in core.logic or Prolog with full runtime "
       "introspection — would turn that judgement call into a "
       "mechanical check. Each candidate optimisation would carry "
       "the axiom it depends on as data; the engine would mechanically "
       "verify the axiom holds for the dialect's surface and the "
       "fold's reachability."]
      [:p "That is not this project, and it is not next quarter's "
       "project. It is the natural endpoint of the soundness work "
       "the VM is already doing by hand."])))
