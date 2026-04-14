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

;; --- Declaration renderers by kind ---

(defmulti render-declaration :kind)

(defmethod render-declaration :function
  [{:keys [name signature doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name]]
   (render-signature signature)
   (render-doc doc)])

(defmethod render-declaration :typedef-fn
  [{:keys [name signature doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "typedef"]]
   (render-signature signature)
   (render-doc doc)])

(defmethod render-declaration :typedef
  [{:keys [name struct-name signature doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name]
    [:span.decl-badge "typedef"]
    (when struct-name
      [:span.decl-meta " \u2192 " [:code (str "struct " struct-name)]])]
   (render-signature signature)
   (render-doc doc)])

(defmethod render-declaration :define
  [{:keys [name value inline-comment signature doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "macro"]]
   (render-signature signature)
   (render-doc (or doc inline-comment))])

(defmethod render-declaration :enum
  [{:keys [name variants doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code name] [:span.decl-badge "enum"]]
   (render-doc doc)
   [:table.enum-table
    [:thead [:tr [:th "Variant"] [:th "Description"]]]
    [:tbody
     (for [{:keys [name comment]} variants]
       [:tr
        [:td [:code name]]
        [:td (if comment (hu/raw-string (fmt/inline comment)) "")]])]]])

(defmethod render-declaration :struct
  [{:keys [name fields doc]}]
  [:div.decl {:id name :data-name name}
   [:h3.decl-name [:code (str "struct " name)] [:span.decl-badge "struct"]]
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
  [{:keys [name signature doc]}]
  (when name
    [:div.decl {:id name :data-name name}
     [:h3.decl-name [:code name]]
     (when signature (render-signature signature))
     (render-doc doc)]))

;; --- Section renderer ---

(defn- render-section
  "Render a single API section with heading and declarations."
  [{:keys [name declarations]}]
  (let [id (section-id name)]
    [:section.api-section {:id id}
     [:h2 name]
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
        [:div.filter-bar
         [:input#api-filter {:type "text"
                             :placeholder "Filter declarations..."
                             :autocomplete "off"}]]
        [:div.docs-layout
         (render-sidebar sections)
         [:div.docs-content
          (for [section sections]
            (render-section section))]]))))
