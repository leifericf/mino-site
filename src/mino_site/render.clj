(ns mino-site.render
  "Page templates and HTML generators.

  html-page is the shared chrome (head, nav, footer) wrapping every page.
  Each generate-*-page fn returns an HTML string for Stasis."
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as hu]
    [mino-site.highlight :as highlight]
    [mino-site.styles :as styles]))

;; --- Configuration ---

(def site-title "mino")
(def site-description "A tiny embeddable Lisp interpreter in pure ANSI C. Single file, no dependencies, MIT licensed.")
(def site-url "https://mino-lang.org")

(def nav-items
  [{:href "/about/"          :label "About"         :page :about}
   {:href "/get-started/"    :label "Get Started"   :page :get-started}
   {:href "/documentation/"  :label "Documentation" :page :documentation}
   {:href "/changelog/"       :label "Changelog"     :page :changelog}
   {:href "https://github.com/leifericf/mino"
    :label "GitHub"
    :external true}])

;; --- Page chrome ---

(defn html-page
  "Wraps body content in a full HTML page with nav, footer, and styles.
  opts:
    :title       — page title (appended to site name)
    :description — meta description (falls back to site-description)
    :active-page — keyword matching a nav-item :page for highlighting
    :wide        — use wider container (for docs with sidebar)"
  [{:keys [title description active-page wide]} & body]
  (let [page-title (if title
                     (str title " \u2014 " site-title)
                     site-title)
        desc       (or description site-description)]
    (str
      "<!DOCTYPE html>\n"
      (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title page-title]
          [:meta {:name "description" :content desc}]
          ;; Open Graph
          [:meta {:property "og:type" :content "website"}]
          [:meta {:property "og:site_name" :content site-title}]
          [:meta {:property "og:title" :content page-title}]
          [:meta {:property "og:description" :content desc}]
          ;; Inline SVG favicon — a green lambda on dark background
          [:link {:rel "icon" :type "image/svg+xml"
                  :href (str "data:image/svg+xml,"
                             "%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E"
                             "%3Crect width='32' height='32' rx='4' fill='%231a1a2e'/%3E"
                             "%3Ctext x='16' y='24' font-size='22' text-anchor='middle' "
                             "fill='%232c5282' font-family='monospace'%3E%CE%BB%3C/text%3E"
                             "%3C/svg%3E")}]
          [:style (hu/raw-string (styles/site-css))]
          ;; Google Analytics 4 — hostname-gated so local dev and preview
          ;; deploys don't pollute the production stream.
          [:script {:async true
                    :src "https://www.googletagmanager.com/gtag/js?id=G-JV5PT1PXQ1"}]
          [:script (hu/raw-string
                     (str "if(location.hostname==='mino-lang.org'){"
                          "window.dataLayer=window.dataLayer||[];"
                          "function gtag(){dataLayer.push(arguments);}"
                          "gtag('js',new Date());"
                          "gtag('config','G-JV5PT1PXQ1',{anonymize_ip:true});"
                          "}"))]]
       [:body
        [:div {:class (if wide "container-wide" "container")}
         [:nav.nav
          [:a.nav-logo {:href "/"} site-title]
          [:button.nav-toggle {:aria-label "Menu"
                               :onclick "this.nextElementSibling.classList.toggle('open')"}
           "\u2630"]
          [:ul.nav-links
           (for [{:keys [href label page external]} nav-items]
             [:li
              [:a (cond-> {:href href}
                    (= page active-page) (assoc :class "active")
                    external             (assoc :target "_blank"
                                                :rel "noopener"))
               label
               (when external " \u2197")]])]]
         [:main (hu/raw-string (apply str (map str body)))]
         [:footer.footer
          [:p (str site-title " is MIT licensed. ")
           [:a {:href "https://github.com/leifericf/mino"} "Source on GitHub"]
           "."]]
         [:script (hu/raw-string highlight/highlight-js)]]]]))))
