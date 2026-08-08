(ns mino-site.content.documentation
  "Documentation hub page content.

  Three-section layout (Embed / Script / Reference) plus an
  Internals row. Role chips at the top let readers jump to the
  section that matches their role."
  (:require
    [hiccup2.core :as h]))

(defn documentation-page
  "Generates the Documentation hub page HTML body."
  []
  (str
    (h/html
      [:h1 "Documentation"]
      [:p "Pick the path that matches what you are doing."]

      [:div.role-chips
       [:a.role-chip {:href "#embed"} "Embed mino in a host"]
       [:a.role-chip {:href "#script"} "Write mino code"]
       [:a.role-chip {:href "#reference"} "Look something up"]]

      [:h2 {:id "embed"} "Embed"]
      [:p "Drop the runtime into any host language with C FFI: C, "
       "C++, Rust, Go, Java, .NET, Swift, Zig, and beyond. State "
       "lifecycle, capability registration, and the embedder's "
       "operational surface."]
      [:div.card-grid
       [:a.card {:href "/documentation/embedding/"}
        [:div.card-title "Embedding Guide"]
        [:div.card-desc
         "State lifecycle, value ownership, sandboxing, handles, "
         "sessions, and threading rules."]]

       [:a.card {:href "/documentation/cookbook/"}
        [:div.card-title "Embedding Cookbook"]
        [:div.card-desc
         "Twelve worked examples: hello-world walk-through, "
         "handle / record / atom decision tree, collection "
         "builders, agents, config loader, console, error "
         "handling, iterators, pipelines, plugins, rules "
         "engine, REPL on socket."]]

       [:a.card {:href "/documentation/bindings/"}
        [:div.card-title "Language Bindings"]
        [:div.card-desc
         "Worked examples of embedding mino from C, C++, Java, "
         "Zig, Rust, C#, Go, and Swift via the C API."]]

       [:a.card {:href "/documentation/errors/"}
        [:div.card-title "Error Diagnostics"]
        [:div.card-desc
         "Structured errors with stable codes, source snippets, "
         "and programmatic access. Errors are plain mino data."]]]

      [:h2 {:id "script"} "Script"]
      [:p "Write mino code. The language as it lands for someone "
       "coming from Clojure, plus the editor, test, and task tooling "
       "for day-to-day development."]
      [:div.card-grid
       [:a.card {:href "/documentation/coming-from-clojure/"}
        [:div.card-title "Coming from Clojure"]
        [:div.card-desc
         "What works the same, what differs, and intentional "
         "divergences for Clojure programmers."]]

       [:a.card {:href "/documentation/testing/"}
        [:div.card-title "Testing"]
        [:div.card-desc
         "Write and run tests in mino using "
         [:code "deftest"] ", " [:code "is"] ", and "
         [:code "testing"] ". CI-friendly exit codes."]]

       [:a.card {:href "/documentation/bytes/"}
        [:div.card-title "Bytes and Bit Syntax"]
        [:div.card-desc
         "Immutable binary-data values plus an Erlang-inspired "
         "destructure surface for packing and unpacking bit fields. "
         "Useful for binary protocols, sensor packets, and bitboards."]]

       [:a.card {:href "/documentation/tasks/"}
        [:div.card-title "Task Runner"]
        [:div.card-desc
         "Define build tasks in " [:code "mino.edn"]
         " as ordinary functions. Dependency resolution, "
         "incremental builds, and self-hosting."]]

       [:a.card {:href "/documentation/tooling/"}
        [:div.card-title "Tooling and Editors"]
        [:div.card-desc
         "tree-sitter grammar, LSP server, and nREPL server. "
         "Setup guides for Neovim, Helix, Emacs, VS Code, and "
         "IntelliJ."]]]

      [:h2 {:id "reference"} "Reference"]
      [:p "Lookup material. Two pages auto-generated from runtime "
       "introspection, two hand-curated comparison tables."]
      [:div.card-grid
       [:a.card {:href "/documentation/compatibility-matrix/"}
        [:div.card-title "Compatibility Matrix"]
        [:div.card-desc
         "Item-by-item table of Clojure core functions and "
         "macros: supported, differs, or absent in mino."]]

       [:a.card {:href "/documentation/intentional-divergences/"}
        [:div.card-title "Intentional Divergences"]
        [:div.card-desc
         "Where mino deliberately differs from Clojure and what "
         "it offers in place of each divergence."]]

       [:a.card {:href "/documentation/api/"}
        [:div.card-title "C API Reference"]
        [:div.card-desc
         "Every public function, type, enum, and macro in "
         [:code "mino.h"] ". Auto-generated from the source."]]

       [:a.card {:href "/documentation/language/"}
        [:div.card-title "Language Reference"]
        [:div.card-desc
         "Every built-in function, special form, and macro. "
         "Organized by category with usage examples."]]]

      [:h2 {:id "internals"} "Runtime internals"]
      [:p "How the runtime works under the hood. Read if you are "
       "tuning, profiling, or porting."]
      [:div.card-grid
       [:a.card {:href "/documentation/bytecode-vm/"}
        [:div.card-title "Bytecode and VM"]
        [:div.card-desc
         "Register-based bytecode layered on the tree-walker: value "
         "representation, opcode families, inline caches, fused "
         "loops, the soundness discipline, and how mino's VM "
         "compares to Lua, Janet, and BEAM."]]

       [:a.card {:href "/documentation/jit/"}
        [:div.card-title "JIT"]
        [:div.card-desc
         "The CPJIT layer end to end: 65 opcode stencils across "
         "five host arches, dual-binary build, four-way parity, "
         "side-exit deopt, cancellable JIT'd loops, runtime "
         "control, per-host verification matrix, and an on/off "
         "A/B against realistic_bench."]]

       [:a.card {:href "/documentation/garbage-collection/"}
        [:div.card-title "Garbage Collection"]
        [:div.card-desc
         "Two-generation tracing collector with incremental old-gen "
         "mark: phases, tuning knobs, stats fields, and environment "
         "variables."]]

       [:a.card {:href "/documentation/stm/"}
        [:div.card-title "Software Transactional Memory"]
        [:div.card-desc
         "Refs, " [:code "dosync"] ", " [:code "alter"] ", "
         [:code "commute"] ", " [:code "ensure"] ", "
         [:code "ref-set"] ", watches, and the C API mirror. "
         "Single-version optimistic locking and how it differs "
         "from JVM canon."]]]

      [:h2 {:id "operations"} "Operations"]
      [:p "Building, shipping, and running mino in production."]
      [:div.card-grid
       [:a.card {:href "/documentation/performance/"}
        [:div.card-title "Performance"]
        [:div.card-desc
         "Numbers, allocation costs, and guidance for keeping "
         "mino fast. When to move work to C."]]

       [:a.card {:href "/documentation/platforms/"}
        [:div.card-title "Platform Support"]
        [:div.card-desc
         "Operating systems, compilers, and language floors. "
         "What CI tests and the minimums below which nothing "
         "is exercised."]]

       [:a.card {:href "/documentation/dependencies/"}
        [:div.card-title "Dependencies"]
        [:div.card-desc
         "How the module resolver wires up "
         [:code "mino.edn"] " declarations: bundled stdlib, on-disk "
         [:code "lib/"] ", and git-fetched repos via "
         [:code "mino deps"] "."]]

       [:a.card {:href "/documentation/vendored-first/"}
        [:div.card-title "Zero dependencies, vendored first"]
        [:div.card-desc
         "How mino ships: the single-file amalgamation under "
         [:code "dist/"] ", no transitive build dependencies, no "
         "package manager, and the SQLite / Odin / sokol / stb "
         "spirit of vendor what you use."]]]))
