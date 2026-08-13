(ns mino-site.content.intentional-divergences
  "Intentional divergences from Clojure page content.

  Renders the structured divergence list from the census payload
  (resources/census/site_payload.edn), grouped by category. The
  intro and closing prose are editorial; every divergence entry
  comes from the census, never hand-maintained."
  (:require
    [hiccup2.core            :as h]
    [mino-site.parse.census  :as census]))

(defn- format-var
  [v]
  [:code (str v)])

(defn- format-vars
  [vars]
  (->> vars
       (map format-var)
       (interpose " ")))

(defn- divergence-entry
  "Render one divergence as a hiccup block."
  [d]
  [:div.divergence-entry
   [:h3 {:id (name (:id d))} (:title d)]
   [:p (:rationale d)]
   (when (seq (:affected d))
     [:p [:strong "Affected: "] (format-vars (:affected d))])
   (when (or (:clojure-example d) (:dialect-example d))
     [:table
      [:tbody
       (when (:clojure-example d)
         [:tr [:td [:strong "Clojure:"]]
          [:td [:code (:clojure-example d)]]])
       (when (:dialect-example d)
         [:tr [:td [:strong "mino:"]]
          [:td [:code (:dialect-example d)]]])]])
   (when-let [behavior (:behavior d)]
     [:p [:strong "Behavior: "]
      (case (:expectation behavior)
        :diverges (str "Diverges as expected"
                       (when (:note behavior) (str " -- " (:note behavior))))
        :matches  (str "Matches Clojure"
                       (when (:note behavior) (str " -- " (:note behavior)))
                       (str " (predicate: " (:predicate behavior) ")"))
        :skip     (str "Skipped"
                       (when (:note behavior) (str " -- " (:note behavior))))
        (str (:expectation behavior)))])
   (when (:doc-link d)
     [:p [:a {:href (:doc-link d)} "See Coming from Clojure"]])
   [:p [:em "Since " (:since d)]]])

(defn- category-section
  "Render one category block with its divergences."
  [{:keys [category divergences]}]
  [:section.category-section
   [:h2 {:id (name (:id category))} (:title category)]
   [:p.category-description (:description category)]
   (for [d divergences] (divergence-entry d))])

(defn intentional-divergences-page
  "Generates the Intentional divergences page HTML body from the
  census payload."
  [payload]
  (let [groups (census/divergences-by-category payload)]
    (str
      (h/html
        [:h1 "Intentional divergences from Clojure"]
        [:p "mino aims to be the Clojure dialect at embedded scale. "
         "Every divergence on this page is a deliberate design "
         "decision, not a missing feature waiting for a contributor. "
         "Each entry names what is different, why, and what mino "
         "offers in its place."]
        [:p "For an item-by-item rundown of which Clojure functions "
         "and macros are supported, differ, or are absent, see the "
         [:a {:href "/documentation/compatibility-matrix/"}
          "compatibility matrix"] "."]
        [:p "Coverage: "
         [:strong (census/coverage-percent payload)]
         " of the Clojure "
         (get-in payload [:meta :clojure-version])
         " surface. "
         (let [mc (get-in payload [:missing :count])]
           (str (:jvm-bound mc) " vars are intentionally absent (JVM-bound) and "
                (:gap mc) " are genuine gaps."))]

        (for [g groups] (category-section g))

        [:h2 "What is in scope for future versions"]
        [:ul
         [:li [:strong "ABI freeze"] " at v1.0. Until then "
          [:code "src/mino.h"] " is labelled evolving and the "
          "numeric-tower type tags (" [:code "MINO_BIGINT"]
          ", " [:code "MINO_RATIO"] ", " [:code "MINO_BIGDEC"]
          ") sit under the same evolving-API umbrella."]]
        [:p "The items above are stable design choices, not "
         "deferrals."]))))
