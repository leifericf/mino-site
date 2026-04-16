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
         "sessions, actors, and threading rules."]]

       [:a.card {:href "/documentation/cookbook/"}
        [:div.card-title "Embedding Cookbook"]
        [:div.card-desc
         "Six worked examples: config loader, rules engine, "
         "REPL on socket, plugin host, data pipeline, and "
         "game scripting console."]]

       [:a.card {:href "/documentation/testing/"}
        [:div.card-title "Testing"]
        [:div.card-desc
         "Write and run tests in mino using "
         [:code "deftest"] ", " [:code "is"] ", and "
         [:code "testing"] ". CI-friendly exit codes."]]

       [:a.card {:href "/documentation/tooling/"}
        [:div.card-title "Tooling and Editors"]
        [:div.card-desc
         "tree-sitter grammar, LSP server, and nREPL server. "
         "Setup guides for Neovim, Helix, Emacs, VS Code, and "
         "IntelliJ."]]]

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
