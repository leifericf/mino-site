(ns mino-site.content.documentation
  "Documentation hub page content."
  (:require
    [hiccup2.core :as h]))

(defn documentation-page
  "Generates the Documentation hub page HTML body."
  []
  (str
    (h/html
      [:h1 "Documentation"]
      [:p "Guides and references for embedding mino and using "
       "the language."]

      [:h2 "Guides"]
      [:div.card-grid
       [:a.card {:href "/documentation/embedding/"}
        [:div.card-title "Embedding Guide"]
        [:div.card-desc
         "State lifecycle, value ownership, sandboxing, handles, "
         "sessions, and threading rules."]]

       [:a.card {:href "/documentation/garbage-collection/"}
        [:div.card-title "Garbage Collection"]
        [:div.card-desc
         "Two-generation tracing collector with incremental old-gen "
         "mark: phases, tuning knobs, stats fields, and environment "
         "variables."]]

       [:a.card {:href "/documentation/cookbook/"}
        [:div.card-title "Embedding Cookbook"]
        [:div.card-desc
         "Six worked examples: config loader, rules engine, "
         "REPL on socket, plugin host, data pipeline, and "
         "game scripting console."]]

       [:a.card {:href "/documentation/errors/"}
        [:div.card-title "Error Diagnostics"]
        [:div.card-desc
         "Structured errors with stable codes, source snippets, "
         "and programmatic access. Errors are plain mino data."]]

       [:a.card {:href "/documentation/testing/"}
        [:div.card-title "Testing"]
        [:div.card-desc
         "Write and run tests in mino using "
         [:code "deftest"] ", " [:code "is"] ", and "
         [:code "testing"] ". CI-friendly exit codes."]]

       [:a.card {:href "/documentation/dependencies/"}
        [:div.card-title "Dependencies"]
        [:div.card-desc
         "Declare dependencies in " [:code "mino.edn"]
         ", fetch git repos with " [:code "mino deps"]
         ", and use libraries from other ecosystems."]]

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
         "IntelliJ."]]

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

       [:a.card {:href "/documentation/coming-from-clojure/"}
        [:div.card-title "Coming from Clojure"]
        [:div.card-desc
         "What works the same, what differs, and intentional "
         "divergences for Clojure programmers."]]

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

       [:a.card {:href "/examples/bindings/"}
        [:div.card-title "Language Bindings"]
        [:div.card-desc
         "Worked examples of embedding mino from other "
         "languages via the C API."]]]

      [:h2 "References"]
      [:div.card-grid
       [:a.card {:href "/documentation/api/"}
        [:div.card-title "C API Reference"]
        [:div.card-desc
         "Every public function, type, enum, and macro in "
         [:code "mino.h"] ". Auto-generated from the source."]]

       [:a.card {:href "/documentation/language/"}
        [:div.card-title "Language Reference"]
        [:div.card-desc
         "Every built-in function, special form, and macro. "
         "Organized by category with usage examples."]]])))
