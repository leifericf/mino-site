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
      [:p "Guides and references for embedding mino and using the language."]

      [:div.card-grid
       [:a.card {:href "/documentation/api/"}
        [:div.card-title "C API Reference"]
        [:div.card-desc
         "Every public function, type, enum, and macro in " [:code "mino.h"]
         ". Auto-generated from the source."]]

       [:a.card {:href "/documentation/language/"}
        [:div.card-title "Language Reference"]
        [:div.card-desc
         "Every built-in function, special form, and macro available in "
         "the mino language. Organized by category with usage examples."]]

       [:a.card {:href "/documentation/cookbook/"}
        [:div.card-title "Embedding Cookbook"]
        [:div.card-desc
         "Six worked examples showing real-world embedding patterns: "
         "config loader, rules engine, REPL on socket, plugin host, "
         "data pipeline, and game scripting console."]]

       [:a.card {:href "/documentation/tooling/"}
        [:div.card-title "Tooling and Editor Integration"]
        [:div.card-desc
         "Connect your editor to mino via nREPL. Setup guides for "
         "Conjure, vim-fireplace, CIDER, Calva, and Cursive. "
         "Protocol reference for tools developers."]]]

      [:h2 "Additional resources"]
      [:ul
       [:li [:a {:href "/get-started/"} "Get Started"]
        ": step-by-step guide to embedding mino in your first project."]
       [:li [:a {:href "/changelog/"} "Changelog"]
        ": what changed in each release."]
       [:li [:a {:href "https://github.com/leifericf/mino" :target "_blank"
                 :rel "noopener"} "Source on GitHub"]
        ": browse the code, file issues, contribute."]])))
