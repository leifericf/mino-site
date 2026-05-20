(ns mino-site.content.api
  "C API Reference page content.

  Renders structured data from parse/header.clj into a browsable
  reference with sidebar navigation, grouped by mino.h sections."
  (:require
    [clojure.string :as str]
    [hiccup2.core :as h]
    [hiccup.util :as hu]
    [mino-site.format :as fmt]))

;; --- Helpers ---

(defn- section-id
  "Convert a section name to a URL-friendly anchor ID."
  [name]
  (-> name
      str/lower-case
      (str/replace #"[^\w\d]+" "-")
      (str/replace #"^-|-$" "")))

(defn- render-signature
  "Render a C declaration signature as a syntax-highlighted code block."
  [signature]
  [:pre [:code {:data-lang "c"} signature]])

(defn- render-doc
  "Render a doc comment as paragraph(s) with inline code formatting."
  [doc]
  (when doc
    (let [paragraphs (str/split doc #"\n\n+")]
      (for [p paragraphs]
        [:p.decl-doc (hu/raw-string (fmt/inline p))]))))

(defn- unstable-badge
  "Yellow `subject to change` badge for declarations in MINO_UNSTABLE_*
  sections. Emit only when the parser marked the section unstable."
  [unstable?]
  (when unstable?
    [:span.decl-badge.unstable "subject to change"]))

;; --- Declaration renderers by kind ---

(defmulti render-declaration :kind)

(defmethod render-declaration :function
  [{:keys [name signature doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] (unstable-badge unstable)]
   (render-doc doc)
   (render-signature signature)])

(defmethod render-declaration :typedef-fn
  [{:keys [name signature doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "typedef"]
    (unstable-badge unstable)]
   (render-signature signature)
   (render-doc doc)])

(defmethod render-declaration :typedef
  [{:keys [name struct-name signature doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name]
    [:span.decl-badge "typedef"]
    (unstable-badge unstable)
    (when struct-name
      [:span.decl-meta " \u2192 " [:code (str "struct " struct-name)]])]
   (render-signature signature)
   (render-doc doc)])

(defmethod render-declaration :define
  [{:keys [name inline-comment signature doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "macro"]
    (unstable-badge unstable)]
   (render-signature signature)
   (render-doc (or doc inline-comment))])

(defmethod render-declaration :enum
  [{:keys [name variants doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "enum"]
    (unstable-badge unstable)]
   (render-doc doc)
   [:table.enum-table
    [:thead [:tr [:th "Variant"] [:th "Description"]]]
    [:tbody
     (for [{:keys [name comment]} variants]
       [:tr
        [:td [:code name]]
        [:td (if comment (hu/raw-string (fmt/inline comment)) "")]])]]])

(defmethod render-declaration :struct
  [{:keys [name fields doc unstable]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code (str "struct " name)] [:span.decl-badge "struct"]
    (unstable-badge unstable)]
   (render-doc doc)
   [:table.struct-table
    [:thead [:tr [:th "Type"] [:th "Field"] [:th "Description"]]]
    [:tbody
     (for [{:keys [type name comment]} fields]
       [:tr
        [:td [:code type]]
        [:td [:code name]]
        [:td (if comment (hu/raw-string (fmt/inline comment)) "")]])]]])

(defmethod render-declaration :default
  [{:keys [name signature doc unstable]}]
  (when name
    [:div.decl {:id name :data-name name}
     [:h3.decl-name [:code name] (unstable-badge unstable)]
     (when signature (render-signature signature))
     (render-doc doc)]))

;; --- Section renderer ---

(defn- render-section
  "Render a single API section with heading and declarations."
  [{:keys [name declarations unstable]}]
  (let [id (section-id name)]
    [:section.api-section {:id id}
     [:h2 name (when unstable
                 [:span.section-badge.unstable " subject to change"])]
     (when unstable
       [:p.section-preamble
        "This section is provisional. Symbols below may change in "
        "subsequent releases; pin your usage and re-test on each "
        "version bump."])
     (for [decl declarations]
       (render-declaration decl))]))

;; --- Sidebar ---

(defn- render-sidebar
  "Render the sidebar navigation from sections."
  [sections]
  [:nav.docs-sidebar
   [:div.sidebar-header "Sections"]
   [:ul
    (for [{:keys [name]} sections]
      [:li [:a {:href (str "#" (section-id name))} name]])]])

;; --- Public API ---

(defn- conventions-preamble
  "Short ownership/lifetime/threading note that applies to the whole
  reference. Lifted from the architecture contract so embedders see
  the cross-cutting rules before scrolling into per-symbol detail."
  []
  [:section.api-preamble
   [:h2 "Conventions"]
   [:dl.api-conventions
    [:dt "Naming"]
    [:dd [:code "*_new"] " and " [:code "*_alloc"]
     " return values the caller owns; release them with the matching "
     [:code "*_free"] " or " [:code "*_destroy"] ". "
     [:code "*_get"] " and " [:code "*_peek"]
     " return borrowed pointers that must not be freed. "
     [:code "*_take"] " transfers ownership from the runtime to the caller."]

    [:dt "Lifetime"]
    [:dd "Values returned by the runtime are borrowed: they stay valid "
     "until the next allocation, which can trigger collection. To keep "
     "a value alive across multiple runtime calls, root it with "
     [:code "mino_ref"] " and release it with " [:code "mino_unref"]
     ". Values bound in a live environment are rooted automatically. "
     "See "
     [:a {:href "/documentation/embedding/"} "Embedding Guide"]
     " for the full model."]

    [:dt "Threading"]
    [:dd "A " [:code "mino_state"] " is not thread-safe: the host "
     "must not call into one state from multiple threads at once. "
     "Worker threads for agents, futures, and the blocking async ops "
     "are spawned by the runtime itself, but only after the host "
     "raises the per-state ceiling via "
     [:code "mino_set_thread_limit"]
     " (default 1, which disables host-threaded primitives). "
     [:code "mino_interrupt(S)"] " is the only function in this "
     "reference that is safe to call from a non-owning thread."]]])

(defn api-page
  "Generates the C API Reference page HTML body.
  api-data is the output of parse/header.clj."
  [api-data]
  (let [sections (:sections api-data)]
    (str
      (h/html
        [:h1 "C API Reference"]
        [:p "Every public function, type, enum, and macro in "
         [:code "mino.h"] ". Auto-generated from the source."]
        (conventions-preamble)
        [:div.filter-bar
         [:input#api-filter {:type "text"
                             :placeholder "Filter declarations..."
                             :autocomplete "off"}]]
        [:div.docs-layout
         (render-sidebar sections)
         [:div.docs-content
          (for [section sections]
            (render-section section))]]))))
