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
(def site-description "An embeddable Clojure-inspired Lisp, written in portable C99. Isolated states, persistent immutable data, and capability-gated host interop for scripting inside native applications.")
(def site-url "https://mino-lang.org")

(def nav-items
  [{:href "/get-started/"    :label "Get Started"   :page :get-started}
   {:href "/documentation/"  :label "Documentation" :page :documentation}
   {:href "/use-cases/"      :label "Use Cases"     :page :use-cases}
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
                     (str title " | " site-title)
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
        [:div.container-wide
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
               (when external " \u2197")]])
           [:li.nav-search-trigger
            [:a {:href "/search/" :data-search-trigger true}
             "Search"
             [:kbd.search-hint "/"]]]]]]
        [:div {:class (if wide "container-wide" "container")}
         [:main (hu/raw-string (apply str (map str body)))]
         [:footer.footer
          [:p (str site-title " is MIT licensed. ")
           [:a {:href "https://github.com/leifericf/mino"} "Source on GitHub"]
           ". "
           [:a {:href "/about/"} "About"]
           "."]]
         [:script (hu/raw-string highlight/highlight-js)]
         [:script {:src "/js/app.js" :defer true}]
         [:script (hu/raw-string
            (str
             "if(window.matchMedia&&window.matchMedia('(min-width:1100px)').matches){"
             "var hs=document.querySelectorAll('main h2');"
             "if(hs.length>2){"
             "var toc=document.createElement('nav');"
             "toc.className='toc-sidebar';"
             "var h=document.createElement('div');"
             "h.className='sidebar-header';"
             "h.textContent='On this page';"
             "toc.appendChild(h);"
             "var ul=document.createElement('ul');"
             "hs.forEach(function(h2){"
             "if(!h2.id){h2.id=h2.textContent.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');}"
             "var li=document.createElement('li');"
             "var a=document.createElement('a');"
             "a.href='#'+h2.id;a.textContent=h2.textContent;"
             "a.addEventListener('click',function(e){e.preventDefault();h2.scrollIntoView({behavior:'smooth'});});"
             "li.appendChild(a);ul.appendChild(li);"
             "});"
             "toc.appendChild(ul);"
             "document.body.appendChild(toc);"
             "}}"))]]]]))))
