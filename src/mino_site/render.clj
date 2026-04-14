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
                  (str title " \u2014 " site-title)
                  site-title)]
        [:style (hu/raw-string (styles/site-css))]
        ;; Google Analytics 4 — hostname-gated so local dev and preview
        ;; deploys don't pollute the production stream.
        [:script {:async true
                  :src "https://www.googletagmanager.com/gtag/js?id=G-LD8F7JFYGB"}]
        [:script (hu/raw-string
                   (str "if(location.hostname==='leifericf.com'){"
                        "window.dataLayer=window.dataLayer||[];"
                        "function gtag(){dataLayer.push(arguments);}"
                        "gtag('js',new Date());"
                        "gtag('config','G-LD8F7JFYGB',{anonymize_ip:true});"
                        "}"))]]
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
