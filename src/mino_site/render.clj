(ns mino-site.render
  "Page templates and HTML generators.

  html-page is the shared chrome (head, nav, footer) wrapping every page.
  Each generate-*-page fn returns an HTML string for Stasis."
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as hu]
    [mino-site.styles :as styles]))

;; --- Configuration ---

(def site-title "mino")

(def nav-items
  [{:href "/about/"          :label "About"         :page :about}
   {:href "/get-started/"    :label "Get Started"   :page :get-started}
   {:href "/documentation/"  :label "Documentation" :page :documentation}
   {:href "/download/"       :label "Download"      :page :download}
   {:href "https://github.com/leifericf/mino"
    :label "GitHub"
    :external true}])

;; --- Page chrome ---

(defn html-page
  "Wraps body content in a full HTML page with nav, footer, and styles.
  opts:
    :title       — page title (appended to site name)
    :active-page — keyword matching a nav-item :page for highlighting
    :wide        — use wider container (for docs with sidebar)"
  [{:keys [title active-page wide]} & body]
  (str
    "<!DOCTYPE html>\n"
    (h/html
      [:html {:lang "en"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title (if title
                  (str title " — " site-title)
                  site-title)]
        [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin ""}]
        [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap"}]
        [:style (hu/raw-string (styles/site-css))]]
       [:body
        [:div {:class (if wide "container-wide" "container")}
         [:nav.nav
          [:a.nav-logo {:href "/"} site-title]
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
           "."]]]]])))
