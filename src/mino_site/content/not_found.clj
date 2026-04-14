(ns mino-site.content.not-found
  "404 page content."
  (:require
    [hiccup2.core :as h]))

(defn not-found-page []
  (str
    (h/html
      [:div {:style "text-align: center; padding: 6rem 0 4rem;"}
       [:h1 "404"]
       [:p {:style "font-size: 1.125rem; margin-bottom: 2rem;"}
        "This page does not exist."]
       [:p [:a {:href "/"} "Back to the homepage"]]])))
