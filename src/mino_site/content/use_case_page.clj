(ns mino-site.content.use-case-page
  "Use case detail pages.

  Each page shows a complete worked example with three sections
  (Embed, Expose, Script) and prose explaining the approach.
  Reads like a workbook: all three roles visible top to bottom."
  (:require
    [clojure.string :as str]
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

;; --- Display metadata ---

(def ^:private use-case-meta
  "Display order and human-readable titles for each use case."
  [{:slug "configuration"
    :title "Configuration"
    :subtitle "Sandboxed evaluation of structured config with computed values."}
   {:slug "rules_engine"
    :title "Rules Engine"
    :subtitle "Host data exposed to mino predicates for declarative business logic."}
   {:slug "data_pipeline"
    :title "Data Pipeline"
    :subtitle "Compose map, filter, reduce over persistent collections."}
   {:slug "event_processing"
    :title "Event Processing"
    :subtitle "Filter, group, and aggregate streams of host data."}
   {:slug "plugins"
    :title "Plugins"
    :subtitle "Load user scripts with controlled capabilities and error isolation."}
   {:slug "console"
    :title "Interactive Console"
    :subtitle "In-app REPL for live inspection, scripted commands, and step limits."}
   {:slug "game_scripting"
    :title "Game Scripting"
    :subtitle "Entity system with scripted queries, commands, and step limits."}
   {:slug "automation"
    :title "Automation"
    :subtitle "User-defined workflows over host task primitives."}])

(defn- find-meta [slug]
  (some #(when (= (:slug %) slug) %) use-case-meta))

(defn- format-prose
  "Convert a prose string into hiccup with inline code spans.
  Recognizes backtick-wrapped `code` tokens from the source comments."
  [text]
  (when text
    (let [parts (str/split text #"`([^`]+)`")
          codes (re-seq #"`([^`]+)`" text)]
      (loop [result []
             ps (seq parts)
             cs (seq codes)]
        (if ps
          (let [result (if (seq (first ps))
                         (conj result (first ps))
                         result)]
            (if cs
              (recur (conj result [:code (second (first cs))])
                     (next ps)
                     (next cs))
              (recur result (next ps) nil)))
          result)))))

;; --- Rendering ---

(defn use-case-page
  "Render a single use case page from parsed data."
  [{:keys [slug source mino-script expose-prose script-prose]}]
  (let [meta (find-meta slug)
        title (or (:title meta) slug)
        subtitle (:subtitle meta)]
    (str
      (h/html
        [:h1 title]
        (when subtitle
          [:p.use-case-subtitle subtitle])

        [:p [:a {:href "/"} "\u2190 Back to use cases"]]

        ;; Full source listing
        [:section.use-case-section
         [:h2 "Full source"]
         [:p "This is a self-contained C++ program. Copy it, compile against "
          "the mino library, and run it."]
         [:pre [:code {:data-lang "c"} source]]

         [:div.use-case-build
          [:strong "Build and run:"]
          [:pre [:code
            (str "make\n"
                 "c++ -std=c++17 -Isrc -o examples/use-cases/" slug " \\\n"
                 "    examples/use-cases/" slug ".cpp src/[a-z]*.o -lm\n"
                 "./examples/use-cases/" slug)]]]]

        ;; Script excerpt
        (when mino-script
          [:section.use-case-section
           [:h2 "The mino script"]
           (when script-prose (into [:p] (format-prose script-prose)))
           [:pre [:code {:data-lang "mino"} mino-script]]])

        [:p.use-case-nav
         [:a {:href "/"} "\u2190 All use cases"]]))))

;; --- Index page helpers ---

(defn use-case-slugs
  "Return the ordered list of use case slugs for page generation."
  []
  (mapv :slug use-case-meta))

(defn use-case-title
  "Return the display title for a given slug."
  [slug]
  (:title (find-meta slug)))
