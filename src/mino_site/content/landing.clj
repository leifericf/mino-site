(ns mino-site.content.landing
  "Homepage content."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(defn embed-example
  "Read examples/embed.c from the mino source tree."
  [mino-root]
  (let [path (str mino-root "/examples/embed.c")]
    (when (.exists (io/file path))
      (slurp path))))

(defn landing-page
  "Generates the homepage HTML body."
  [mino-root]
  (let [embed-src (embed-example mino-root)]
    (str
      (h/html
        [:div.banner
         "v0.12.0 Release Candidate \u2014 API unstable until v1.0"]
        [:section.hero
         [:div.hero-prompt
          "$ ./mino" [:span.cursor]]
         [:p.hero-tagline "A tiny embeddable Lisp in pure ANSI C."]
         [:p.hero-stats
          "~7k LOC \u00b7 no dependencies \u00b7 MIT licensed \u00b7 sandboxed by default"]
         [:div.hero-ctas
          [:a.cta {:href "/get-started/"} "> (get-started)"]
          [:a.cta {:href "/documentation/"} "> (documentation)"]
          [:a.cta {:href "/download/"} "> (download)"]]]
        (when embed-src
          [:section {:style "margin-top: 3rem;"}
           [:h2 "Embed in 50 lines"]
           [:pre
            [:code (hu/raw-string
                     (str/escape embed-src {\< "&lt;" \> "&gt;" \& "&amp;"}))]]])))))
