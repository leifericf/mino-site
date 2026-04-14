(ns mino-site.content.cookbook-page
  "Embedding Cookbook page content.

  Renders structured data from parse/cookbook.clj into annotated
  cookbook entries with full source listings."
  (:require
    [clojure.string :as str]
    [hiccup2.core :as h]))

;; --- Helpers ---

(defn- entry-id
  "Convert a filename to a URL-friendly anchor ID."
  [filename]
  (str/replace filename #"\.c$" ""))

(defn- render-entry
  "Render a single cookbook entry."
  [{:keys [filename title description demonstrates build source]}]
  (let [id (entry-id filename)]
    [:section.cookbook-entry {:id id}
     [:h2 (or title filename)]
     (when description
       [:p.cookbook-desc description])
     (when (seq demonstrates)
       [:div.cookbook-demonstrates
        [:strong "Demonstrates: "]
        (str/join ", " demonstrates)])
     (when build
       [:div.cookbook-build
        [:strong "Build: "]
        [:code build]])
     [:details.cookbook-source {:open true}
      [:summary (str filename " \u2014 full source")]
      [:pre [:code {:data-lang "c"} source]]]]))

;; --- Sidebar ---

(defn- render-sidebar
  "Render the sidebar navigation from cookbook entries."
  [entries]
  [:nav.docs-sidebar
   [:div.sidebar-header "Examples"]
   [:ul
    (for [{:keys [filename title]} entries]
      [:li [:a {:href (str "#" (entry-id filename))}
        (or title filename)]])]])

;; --- Public API ---

(defn cookbook-page
  "Generates the Embedding Cookbook page HTML body.
  cookbook-data is the output of parse/cookbook.clj."
  [cookbook-data]
  (str
    (h/html
      [:h1 "Embedding Cookbook"]
      [:p "Six worked examples showing real-world embedding patterns. "
       "Each includes the full annotated C source with build instructions."]
      [:div.docs-layout
       (render-sidebar cookbook-data)
       [:div.docs-content
        (for [entry cookbook-data]
          (render-entry entry))]])))
