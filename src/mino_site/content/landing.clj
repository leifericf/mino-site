(ns mino-site.content.landing
  "Homepage content."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(defn- read-file
  "Read a file from the mino source tree, or nil if missing."
  [mino-root path]
  (let [f (io/file mino-root path)]
    (when (.exists f)
      (slurp f))))

(defn- escape-html [s]
  (str/escape s {\< "&lt;" \> "&gt;" \& "&amp;"}))

(defn landing-page
  "Generates the homepage HTML body."
  [mino-root]
  (let [embed-src (read-file mino-root "examples/embed.c")]
    (str
      (h/html
        [:div.banner
         "v0.12.0 Release Candidate \u2014 API unstable until v1.0"]
        [:section.hero
         [:h1.hero-tagline
          "A tiny embeddable Lisp" [:br] "in pure ANSI C."]
         [:p.hero-subtitle
          "~7,000 lines of C. No dependencies. Sandboxed by default. MIT licensed."]
         [:div.hero-ctas
          [:a.cta-primary {:href "/get-started/"} "Get Started"]
          [:a.cta-secondary {:href "/documentation/"} "Documentation"]]]
        (when embed-src
          [:section {:style "margin-top: 4rem;"}
           [:h2 "Embed in your C project"]
           [:p {:style "margin-bottom: 1rem;"}
            "Create a runtime, register a host function, evaluate code, "
            "and extract the result \u2014 all in a single file."]
           [:pre
            [:code (hu/raw-string (escape-html embed-src))]]])))))
