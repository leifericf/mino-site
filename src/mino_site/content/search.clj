(ns mino-site.content.search
  "SSR fallback search page. Lists all indexed pages and symbols
  grouped by category. The CLJS command palette replaces this for
  JS-enabled browsers."
  (:require
    [hiccup2.core :as h]
    [mino-site.search.index :as search-index]))

(defn search-page
  "Generates the SSR search page HTML body."
  [api-data builtin-data]
  (let [index (search-index/build api-data builtin-data)]
    (str
      (h/html
        [:h1 "Search"]

        [:p "Filter by name or keyword. On JS-enabled browsers, press "
         [:code "/"] " or " [:code "Ctrl+K"] " anywhere for the search palette."]

        [:input.search-ssr-filter
         {:type "search"
          :placeholder "Type to filter..."
          :oninput "var q=this.value.toLowerCase();document.querySelectorAll('.search-ssr-item').forEach(function(el){el.style.display=el.textContent.toLowerCase().indexOf(q)>=0?'':'none'})"}]

        [:h2 "Pages"]
        [:ul.search-ssr-list
         (for [page (get index "pages")]
           [:li.search-ssr-item
            [:a {:href (get page "uri")} (get page "title")]
            ": " [:span.search-ssr-desc (get page "desc")]])]

        [:h2 "C API"]
        [:ul.search-ssr-list
         (for [sym (get index "api")]
           [:li.search-ssr-item
            [:a {:href (get sym "uri")}
             [:code (get sym "name")]]
            " " [:span.search-result-badge (get sym "type")]
            (when-let [d (get sym "desc")]
              (str ": " d))])]

        [:h2 "Language"]
        [:ul.search-ssr-list
         (for [form (get index "language")]
           [:li.search-ssr-item
            [:a {:href (get form "uri")}
             [:code (get form "name")]]
            " " [:span.search-result-badge (get form "type")]
            (when-let [d (get form "desc")]
              (str ": " d))])]))))
