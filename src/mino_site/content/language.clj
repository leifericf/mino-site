(ns mino-site.content.language
  "Language Reference page content.

  Renders structured data from parse/builtins.clj and parse/smoke.clj
  into a browsable reference organized by category."
  (:require
    [clojure.string :as str]
    [hiccup2.core :as h]))

;; --- Helpers ---

(defn- section-id [name]
  (-> name
      str/lower-case
      (str/replace #"[^\w\d]+" "-")
      (str/replace #"^-|-$" "")))

(defn- find-examples
  "Find smoke test examples for the given function name.
  Matches on exact test name or function call in the input expression.
  Returns up to max-count matching examples."
  [examples fn-name max-count]
  (->> examples
       (filter #(or (= (:description %) fn-name)
                    (str/includes? (:input %) (str "(" fn-name " "))
                    (str/includes? (:input %) (str "(" fn-name ")"))
                    (str/includes? (:input %) (str "(" fn-name "\n"))))
       (take max-count)
       vec))

(defn- render-example
  "Render a single smoke test example as input/output."
  [{:keys [input expected]}]
  [:div.example
   [:pre [:code {:data-lang "mino"} input]]
   [:div.example-result
    [:span.example-arrow "\u21d2"]
    [:code expected]]])

;; --- Primitive rendering ---

(defn- render-primitive
  "Render a single primitive function entry."
  [prim-name examples prim-docs]
  (let [exs (find-examples examples prim-name 3)
        doc (get prim-docs prim-name)]
    [:div.decl {:id (str "fn-" prim-name) :data-name prim-name}
     [:h3.decl-name [:code prim-name]]
     (when doc [:p.decl-doc doc])
     (when (seq exs)
       [:div.examples
        (for [ex exs]
          (render-example ex))])]))

;; --- Category rendering ---

(defn- render-category
  "Render a category section with its primitives."
  [{:keys [name primitives]} examples prim-docs]
  (let [id (section-id name)]
    (when (seq primitives)
      [:section.api-section {:id id}
       [:h2 (str/capitalize name)]
       (for [prim primitives]
         (render-primitive prim examples prim-docs))])))

;; --- Special forms ---

(defn- render-special-forms
  "Render the special forms section."
  [special-forms examples prim-docs]
  [:section.api-section {:id "special-forms"}
   [:h2 "Special forms"]
   [:p "These forms are recognized directly by the evaluator and cannot "
    "be redefined."]
   (for [sf special-forms]
     (render-primitive sf examples prim-docs))])

;; --- Stdlib macros ---

(defn- render-stdlib
  "Render the stdlib macros/functions section."
  [stdlib examples]
  [:section.api-section {:id "stdlib"}
   [:h2 "Standard library"]
   [:p "Defined in mino source at startup. View with "
    [:code "(source name)"] " at the REPL."]
   (for [{:keys [name kind doc source]} stdlib]
     (let [exs (find-examples examples name 3)]
       [:div.decl {:id (str "fn-" name) :data-name name}
        [:h3.decl-name [:code name]
         [:span.decl-badge (clojure.core/name kind)]]
        (when doc [:p.decl-doc doc])
        [:details.stdlib-source
         [:summary "Source"]
         [:pre [:code {:data-lang "mino"} source]]]
        (when (seq exs)
          [:div.examples
           (for [ex exs]
             (render-example ex))])]))])

;; --- I/O primitives ---

(defn- render-io-primitives
  "Render the I/O primitives section."
  [io-prims examples prim-docs]
  [:section.api-section {:id "io"}
   [:h2 "I/O primitives"]
   [:p "Available only after the host calls "
    [:code "mino_install_io()"] ". Not present in sandboxed environments."]
   (for [prim io-prims]
     (render-primitive prim examples prim-docs))])

;; --- Sidebar ---

(defn- render-sidebar
  "Render the sidebar navigation."
  [categories special-forms stdlib io-prims]
  [:nav.docs-sidebar
   [:div.sidebar-header "Categories"]
   [:ul
    (for [{:keys [name primitives]} categories
          :when (seq primitives)]
      [:li [:a {:href (str "#" (section-id name))} (str/capitalize name)]])
    [:li [:a {:href "#special-forms"} "Special forms"]]
    [:li [:a {:href "#stdlib"} "Standard library"]]
    (when (seq io-prims)
      [:li [:a {:href "#io"} "I/O primitives"]])]])

;; --- Public API ---

(defn language-page
  "Generates the Language Reference page HTML body.
  builtin-data is the output of parse/builtins.clj.
  smoke-data is the output of parse/smoke.clj."
  [builtin-data smoke-data]
  (let [{:keys [categories stdlib io-primitives special-forms prim-docs]} builtin-data
        examples (:examples smoke-data)]
    (str
      (h/html
        [:h1 "Language Reference"]
        [:p "Every built-in function, special form, and macro in the mino "
         "language. Organized by category with usage examples from the "
         "test suite. "
         [:a {:href "/documentation/coming-from-clojure/"}
          "Coming from Clojure?"] ]
        [:div.filter-bar
         [:input#lang-filter {:type "text"
                              :placeholder "Filter functions..."
                              :autocomplete "off"}]]
        [:div.docs-layout
         (render-sidebar categories special-forms stdlib io-primitives)
         [:div.docs-content
          (for [cat categories]
            (render-category cat examples prim-docs))
          (render-special-forms special-forms examples prim-docs)
          (render-stdlib stdlib examples)
          (render-io-primitives io-primitives examples prim-docs)]]))))
