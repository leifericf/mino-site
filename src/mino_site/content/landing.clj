(ns mino-site.content.landing
  "Homepage content."
  (:require
    [clojure.java.io :as io]
    [hiccup2.core :as h]))

(defn- read-file
  "Read a file from the mino source tree, or nil if missing."
  [mino-root path]
  (let [f (io/file mino-root path)]
    (when (.exists f)
      (slurp f))))

(defn landing-page
  "Generates the homepage HTML body."
  [mino-root]
  (let [embed-src (read-file mino-root "examples/embed.c")]
    (str
      (h/html
        [:div.banner
         "Unstable alpha proof-of-concept. The API may change before v1.0."]
        [:section.hero
         [:h1.hero-tagline
          "A tiny embeddable Lisp" [:br] "in pure ANSI C."]
         [:div.hero-ctas
          [:a.cta-primary {:href "/get-started/"} "Get Started"]
          [:a.cta-secondary {:href "/documentation/"} "Documentation"]]]
        (when embed-src
          [:section {:style "margin-top: 4rem;"}
           [:h2 "Embed in your C project"]
           [:p {:style "margin-bottom: 1rem;"}
            "Register a host function, evaluate code, extract the result."]
           [:pre
            [:code {:data-lang "c"} embed-src]]])))))
