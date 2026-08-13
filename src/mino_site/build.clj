(ns mino-site.build
  "Site build orchestrator.

  Defines the Stasis page map and exports to _site/.
  Run via: clj -X:build"
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [stasis.core :as stasis]
    [mino-site.render :as render]
    [mino-site.parse.header :as parse.header]
    [mino-site.parse.builtins :as parse.builtins]
    [mino-site.parse.census :as parse.census]
    [mino-site.parse.cookbook :as parse.cookbook]
    [mino-site.parse.smoke :as parse.smoke]
    [mino-site.content.landing :as landing]
    [mino-site.content.about :as about]
    [mino-site.content.get-started :as get-started]
    [mino-site.content.documentation :as documentation]
    [mino-site.content.api :as api]
    [mino-site.content.language :as language]
    [mino-site.content.cookbook-page :as cookbook-page]
    [mino-site.content.not-found :as not-found]
    [mino-site.content.tooling :as tooling]
    [mino-site.content.testing :as testing]
    [mino-site.content.embedding :as embedding]
    [mino-site.content.garbage-collection :as garbage-collection]
    [mino-site.content.stm :as stm]
    [mino-site.content.bytes :as bytes]
    [mino-site.content.platforms :as platforms]
    [mino-site.content.performance :as performance]
    [mino-site.content.bytecode-vm :as bytecode-vm]
    [mino-site.content.vendored-first :as vendored-first]
    [mino-site.content.jit :as jit]
    [mino-site.content.from-clojure :as from-clojure]
    [mino-site.content.compatibility-matrix :as compatibility-matrix]
    [mino-site.content.intentional-divergences :as intentional-divergences]
    [mino-site.parse.use-cases :as parse.use-cases]
    [mino-site.parse.async-api :as parse.async]
    [mino-site.content.use-case-page :as use-case-page]
    [mino-site.content.bindings-page :as bindings-page]
    [mino-site.content.errors :as errors]
    [mino-site.content.dependencies :as dependencies]
    [mino-site.content.tasks :as tasks]
    [mino-site.content.search :as search]
    [mino-site.search.index :as search-index]))

(defn pages
  "Returns a Stasis page map: {path -> (fn [ctx] html-string)}.
  Paths use /dir/index.html so GitHub Pages serves them at /dir/.
  mino-root is the path to the mino source tree (submodule or local)."
  [mino-root]
  (let [api-data       (parse.header/parse (str mino-root "/src/mino.h")
                                             {:strict? true})
        builtin-data   (parse.builtins/introspect mino-root "scripts/intro.clj")
        census-payload (parse.census/load-payload)
        cookbook-data   (parse.cookbook/parse "mino-examples/src")
        smoke-data     (parse.smoke/parse mino-root)
        async-data     (parse.async/parse mino-root)
        use-case-data  (parse.use-cases/parse "mino-examples")
        use-case-index (into {} (map (juxt :slug identity) use-case-data))
        use-case-pages (into {}
                         (for [slug (use-case-page/use-case-slugs)
                               :let [uc (get use-case-index slug)]
                               :when uc]
                           [(str "/use-cases/" slug "/index.html")
                            (fn [ctx]
                              (render/html-page
                                {:title (use-case-page/use-case-title slug)
                                 :description (:description uc)
                                 :active-page :use-cases
                                 :wide true}
                                (use-case-page/use-case-page uc)))]))]
    (merge use-case-pages
    {"/use-cases/index.html"
     (fn [ctx]
       (render/html-page {:title "Use Cases"
                          :description "Worked examples of embedding mino: configuration, rules engines, plugins, data pipelines, event processing, game scripting, automation, and interactive consoles."
                          :active-page :use-cases
                          :wide true}
         (use-case-page/use-case-index-page)))

     "/documentation/bindings/index.html"
     (fn [ctx]
       (render/html-page {:title "Language Bindings"
                          :description "Embed mino from C, C++, Java, Zig, Rust, C#, Go, and Swift."
                          :active-page :documentation
                          :wide true}
         (bindings-page/bindings-page mino-root)))

     "/index.html"
     (fn [ctx]
       (render/html-page {:active-page :home}
         (landing/landing-page mino-root)))

     "/about/index.html"
     (fn [ctx]
       (render/html-page {:title "About"
                          :description "What mino is, why you would embed it, and the design principles behind it."
                          :active-page :about}
         (about/about-page)))

     "/get-started/index.html"
     (fn [ctx]
       (render/html-page {:title "Get Started"
                          :description "Copy the source, compile, and run your first mino program in under a minute."
                          :active-page :get-started}
         (get-started/get-started-page)))

     "/documentation/index.html"
     (fn [ctx]
       (render/html-page {:title "Documentation"
                          :description "C API reference, language reference, and embedding cookbook for mino."
                          :active-page :documentation}
         (documentation/documentation-page)))

     "/documentation/api/index.html"
     (fn [ctx]
       (render/html-page {:title "C API Reference"
                          :description "Every public function, type, enum, and macro in mino.h."
                          :active-page :documentation
                          :wide true}
         (api/api-page api-data)))

     "/documentation/language/index.html"
     (fn [ctx]
       (render/html-page {:title "Language Reference"
                          :description "Built-in functions, special forms, and standard library macros in the mino language."
                          :active-page :documentation
                          :wide true}
         (language/language-page builtin-data smoke-data async-data)))

     "/documentation/cookbook/index.html"
     (fn [ctx]
       (render/html-page {:title "Embedding Cookbook"
                          :description "Twelve worked examples showing how to embed mino in a C application."
                          :active-page :documentation
                          :wide true}
         (cookbook-page/cookbook-page cookbook-data)))

     "/documentation/tooling/index.html"
     (fn [ctx]
       (render/html-page {:title "Tooling and Editor Integration"
                          :description "nREPL server for mino: connect any editor, evaluate code interactively, build tools with the standard protocol."
                          :active-page :documentation}
         (tooling/tooling-page)))

     "/documentation/testing/index.html"
     (fn [ctx]
       (render/html-page {:title "Testing"
                          :description "Write and run tests in mino using deftest, is, and testing. Built-in test runner with CI-friendly exit codes."
                          :active-page :documentation}
         (testing/testing-page)))

     "/documentation/errors/index.html"
     (fn [ctx]
       (render/html-page {:title "Error Diagnostics"
                          :description "Structured error reporting with stable codes, source snippets, and programmatic access from mino and C."
                          :active-page :documentation}
         (errors/errors-page)))

     "/documentation/embedding/index.html"
     (fn [ctx]
       (render/html-page {:title "Embedding Guide"
                          :description "State lifecycle, value ownership, sandboxing, handles, sessions, and threading rules for embedding mino in a host application."
                          :active-page :documentation}
         (embedding/embedding-page)))

     "/documentation/garbage-collection/index.html"
     (fn [ctx]
       (render/html-page {:title "Garbage Collection"
                          :description "Two-generation tracing collector with incremental old-gen mark: phases, tuning knobs, stats fields, and environment variables."
                          :active-page :documentation}
         (garbage-collection/garbage-collection-page)))

     "/documentation/stm/index.html"
     (fn [ctx]
       (render/html-page {:title "Software Transactional Memory"
                          :description "Refs, dosync, alter, commute, ensure, ref-set, watches, validators, and the C API mirror. Single-version optimistic locking; deviations from JVM canon."
                          :active-page :documentation}
         (stm/stm-page)))

     "/documentation/bytes/index.html"
     (fn [ctx]
       (render/html-page {:title "Bytes and Bit Syntax"
                          :description "MINO_BYTES (immutable binary-data value) and the Erlang-inspired bit-syntax surface: bits, bits-get, subbits, let-bits. Bit-precise field access for binary protocols, sensor packets, bitboards."
                          :active-page :documentation}
         (bytes/bytes-page)))

     "/documentation/performance/index.html"
     (fn [ctx]
       (render/html-page {:title "Performance"
                          :description "Preliminary performance characteristics: per-operation costs, collection throughput, and where the time goes."
                          :active-page :documentation}
         (performance/performance-page)))

     "/documentation/bytecode-vm/index.html"
     (fn [ctx]
       (render/html-page {:title "Bytecode and VM"
                          :description "How mino's register-based bytecode VM compiles function bodies, what its opcodes do, the soundness discipline behind compile-time folds, and where it sits relative to Lua, Janet, and BEAM."
                          :active-page :documentation}
         (bytecode-vm/bytecode-vm-page)))

     "/documentation/vendored-first/index.html"
     (fn [ctx]
       (render/html-page {:title "Zero dependencies, vendored first"
                          :description "How mino ships: a single-file amalgamation under dist/, no transitive build dependencies, the SQLite / Odin / sokol / stb spirit of vendor what you use."
                          :active-page :documentation}
         (vendored-first/vendored-first-page)))

     "/documentation/jit/index.html"
     (fn [ctx]
       (render/html-page {:title "JIT"
                          :description "Single canonical page on mino's copy-and-patch JIT (CPJIT): 65 opcode stencils across five host arches, dual-binary build, four-way parity, side-exit deopt path, cancellable JIT'd loops, runtime control surface, and an on/off A/B against realistic_bench."
                          :active-page :documentation}
         (jit/jit-page)))

     "/documentation/platforms/index.html"
     (fn [ctx]
       (render/html-page {:title "Platform Support"
                          :description "Supported operating systems, compilers, and language floors. Platforms covered by CI and minimum versions for building mino."
                          :active-page :documentation}
         (platforms/platforms-page)))

     "/documentation/coming-from-clojure/index.html"
     (fn [ctx]
       (render/html-page {:title "Coming from Clojure"
                          :description "How mino differs from Clojure: syntax, namespaces, concurrency, interop, and what is intentionally absent."
                          :active-page :documentation}
         (from-clojure/from-clojure-page)))

     "/documentation/compatibility-matrix/index.html"
     (fn [ctx]
       (render/html-page {:title "Clojure Compatibility Matrix"
                          :description "Item-by-item table of Clojure core functions and macros: supported, differs, or absent in mino."
                          :active-page :documentation
                          :wide true}
         (compatibility-matrix/compatibility-matrix-page)))

      "/documentation/intentional-divergences/index.html"
      (fn [ctx]
        (render/html-page {:title "Intentional Divergences"
                           :description "Where mino deliberately differs from Clojure and what it offers in place of each divergence."
                           :active-page :documentation}
          (intentional-divergences/intentional-divergences-page census-payload)))

     "/documentation/dependencies/index.html"
     (fn [ctx]
       (render/html-page {:title "Dependencies"
                          :description "Declare dependencies in mino.edn, fetch git repos, and wire them into the module resolver."
                          :active-page :documentation}
         (dependencies/dependencies-page)))

     "/documentation/tasks/index.html"
     (fn [ctx]
       (render/html-page {:title "Task Runner"
                          :description "Define and run build tasks in mino.edn. Tasks are ordinary mino functions with dependency resolution."
                          :active-page :documentation}
         (tasks/tasks-page)))

     "/search/index.html"
     (fn [ctx]
       (render/html-page {:title "Search"
                          :description "Search mino documentation: pages, C API symbols, and language forms."
                          :active-page nil}
         (search/search-page api-data builtin-data)))

     "/search/index.json"
     (search-index/->json (search-index/build api-data builtin-data))

     "/404.html"
     (fn [ctx]
       (render/html-page {:title "Page Not Found"
                          :description "This page does not exist."}
         (not-found/not-found-page)))})))


(defn- compile-cljs!
  "Compile the CLJS bundle via shadow-cljs. Skips gracefully if the
  CLJS toolchain is not available so dev builds still work."
  []
  (let [{:keys [exit out err]} (sh/sh "clojure" "-M:cljs" "release" "app")]
    (if (zero? exit)
      (do (when (seq out) (print out))
          (println "CLJS bundle compiled."))
      (do (when (seq err) (binding [*out* *err*] (println err)))
          (println "Skipping CLJS (toolchain not available). Search will use SSR fallback.")))))

(defn build-site!
  "Entry point for clj -X:build.
  Compiles CLJS, exports all pages to out-dir, copies static assets."
  [& {:keys [mino-root out-dir]
      :or   {mino-root "mino" out-dir "_site"}}]
  (println "Building mino site into" out-dir "...")
  (compile-cljs!)
  (stasis/empty-directory! out-dir)
  (stasis/export-pages (pages mino-root) out-dir)
  ;; Copy static assets from resources/public/ into the output directory
  (let [public-dir (io/file "resources/public")]
    (when (.isDirectory public-dir)
      (doseq [f (file-seq public-dir)
              :when (.isFile f)]
        (let [rel (.relativize (.toPath public-dir) (.toPath f))
              dest (io/file out-dir (str rel))]
          (.mkdirs (.getParentFile dest))
          (io/copy f dest)))))
  (println "Site built successfully!")
  (println (str "  " (count (pages mino-root)) " pages generated"))
  (println (str "  Open " out-dir "/index.html to preview")))
