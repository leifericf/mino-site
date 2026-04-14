(ns mino-site.styles
  "Garden CSS definitions — retro green phosphor terminal aesthetic."
  (:require
    [garden.core :as garden]
    [garden.stylesheet :refer [at-media]]))

;; --- Colors ---

(def colors
  {:bg             "#0a0a0a"
   :bg-card        "#111111"
   :text           "#22cc22"
   :text-bright    "#33ff33"
   :text-dimmed    "#117711"
   :link           "#55ffff"
   :link-hover     "#88ffff"
   :border         "#1a5a1a"
   :border-bright  "#33ff33"
   ;; CGA accent colors — syntax highlighting only
   :code-keyword   "#ff55ff"
   :code-string    "#ffff55"
   :code-number    "#ffb000"
   :code-comment   "#555555"
   :code-type      "#55ffff"
   :warning        "#ff5555"
   :banner-bg      "#1a0a0a"})

;; --- Fonts ---

(def font-heading "'Press Start 2P', monospace")
(def font-mono (str "'SF Mono', 'Cascadia Code', 'JetBrains Mono', "
                    "'Fira Code', Menlo, Consolas, monospace"))

;; --- Base ---

(def base-styles
  [[:* {:box-sizing "border-box"
        :margin     0
        :padding    0}]
   [:html {:font-size "16px"}]
   [:body {:font-family font-mono
           :background  (:bg colors)
           :color       (:text colors)
           :line-height "1.7"}]
   [:a {:color           (:link colors)
        :text-decoration "none"}]
   ["a:hover" {:color (:link-hover colors)}]
   [:code {:font-family font-mono
           :font-size   "0.875rem"}]
   [:pre {:background  (:bg-card colors)
          :border      (str "1px solid " (:border colors))
          :padding     "1.25rem"
          :overflow-x  "auto"
          :line-height "1.5"}]
   ["pre code" {:background "none"
                :padding    0}]
   [:code {:background (:bg-card colors)
           :padding    "0.15rem 0.4rem"}]
   [:p {:margin-bottom "0.75rem"
        :line-height   "1.7"}]
   ["p + pre" {:margin-top "0.5rem"}]
   ["pre + p" {:margin-top "1rem"}]
   [:h1 :h2 :h3 {:font-family font-heading
                  :color       (:text-bright colors)
                  :line-height "1.4"}]
   [:h1 {:font-size     "1.5rem"
         :margin-bottom "1.5rem"}]
   [:h2 {:font-size     "1rem"
         :margin-top    "2.5rem"
         :margin-bottom "1rem"}]
   [:h3 {:font-size     "0.75rem"
         :margin-top    "1.5rem"
         :margin-bottom "0.75rem"}]
   [:ul :ol {:margin-bottom "0.75rem"
             :padding-left  "1.5rem"}]
   [:li {:margin-bottom "0.35rem"}]
   ["::selection" {:background (:text-bright colors)
                   :color      (:bg colors)}]])

;; --- Layout ---

(def layout-styles
  [[:.container {:max-width "720px"
                 :margin    "0 auto"
                 :padding   "0 1.5rem"}]
   [:.container-wide {:max-width "900px"
                      :margin    "0 auto"
                      :padding   "0 1.5rem"}]])

;; --- Nav ---

(def nav-styles
  [[:.nav {:display         "flex"
           :align-items     "center"
           :justify-content "space-between"
           :padding         "1.25rem 0"
           :border-bottom   (str "1px solid " (:border colors))}]
   [:.nav-logo {:font-family font-heading
                :font-size   "0.875rem"
                :color       (:text-bright colors)}]
   ["a.nav-logo:hover" {:color (:link-hover colors)}]
   [:.nav-links {:display    "flex"
                 :gap        "1.5rem"
                 :list-style "none"}]
   [:.nav-links>li>a {:color       (:text-dimmed colors)
                      :font-size   "0.8rem"
                      :font-family font-mono}]
   [".nav-links li a:hover" {:color (:text colors)}]
   [".nav-links li a.active" {:color (:text-bright colors)}]])

;; --- Footer ---

(def footer-styles
  [[:.footer {:margin-top  "4rem"
              :padding     "2rem 0"
              :border-top  (str "1px solid " (:border colors))
              :text-align  "center"
              :color       (:text-dimmed colors)
              :font-size   "0.75rem"}]
   [".footer a" {:color (:text-dimmed colors)}]
   [".footer a:hover" {:color (:text colors)}]])

;; --- Hero ---

(def hero-styles
  [[:.hero {:padding    "4rem 0 3rem"
            :text-align "center"}]
   [:.hero-prompt {:font-family font-mono
                   :font-size   "1.25rem"
                   :color       (:text-bright colors)
                   :margin-bottom "1rem"}]
   [:.cursor {:display   "inline-block"
              :width     "0.6em"
              :height    "1.1em"
              :background (:text-bright colors)
              :margin-left "2px"
              :animation "blink 1s step-end infinite"
              :vertical-align "text-bottom"}]
   ["@keyframes blink" {"0%, 100%" {:opacity 1}
                        "50%"      {:opacity 0}}]
   [:.hero-tagline {:font-family   font-heading
                    :font-size     "1.25rem"
                    :color         (:text-bright colors)
                    :margin-bottom "1rem"
                    :line-height   "1.6"}]
   [:.hero-stats {:color         (:text-dimmed colors)
                  :font-size     "0.875rem"
                  :margin-bottom "2rem"}]
   [:.hero-ctas {:display         "flex"
                 :justify-content "center"
                 :gap             "2rem"
                 :margin-top      "2rem"}]
   [:.cta {:font-family font-mono
           :font-size   "1rem"
           :color       (:link colors)}]
   ["a.cta:hover" {:color (:link-hover colors)}]])

;; --- Banner ---

(def banner-styles
  [[:.banner {:background  (:banner-bg colors)
              :border      (str "1px solid " (:warning colors))
              :padding     "0.5rem 1rem"
              :text-align  "center"
              :font-size   "0.75rem"
              :color       (:warning colors)}]])

;; --- Cards ---

(def card-styles
  [[:.card-grid {:display               "grid"
                 :grid-template-columns  "repeat(auto-fit, minmax(280px, 1fr))"
                 :gap                    "1.5rem"
                 :margin-top             "1.5rem"}]
   [:.card {:border  (str "1px solid " (:border colors))
            :padding "1.5rem"}]
   ["a.card" {:color (:text colors)}]
   ["a.card:hover" {:border-color (:border-bright colors)}]
   [:.card-title {:font-family   font-heading
                  :font-size     "0.625rem"
                  :color         (:text-bright colors)
                  :margin-bottom "0.75rem"}]
   [:.card-desc {:color     (:text-dimmed colors)
                 :font-size "0.875rem"}]])

;; --- Docs sidebar ---

(def sidebar-styles
  [[:.docs-layout {:display  "flex"
                   :gap      "3rem"
                   :padding  "2rem 0"}]
   [:.docs-sidebar {:width      "200px"
                    :flex-shrink "0"
                    :position   "sticky"
                    :top        "1rem"
                    :align-self "flex-start"
                    :max-height "calc(100vh - 2rem)"
                    :overflow-y "auto"}]
   [:.docs-sidebar>ul {:list-style "none"
                       :padding    0}]
   [".docs-sidebar li" {:margin-bottom "0.5rem"}]
   [".docs-sidebar a" {:color     (:text-dimmed colors)
                       :font-size "0.8rem"}]
   [".docs-sidebar a:hover" {:color (:text colors)}]
   [".docs-sidebar a.active" {:color (:text-bright colors)}]
   [:.docs-content {:flex "1"
                    :min-width "0"}]])

;; --- Code block header ---

(def code-styles
  [[:.code-block {:margin-bottom "1.5rem"}]
   [:.code-header {:font-size   "0.75rem"
                   :color       (:text-dimmed colors)
                   :padding     "0.5rem 1.25rem"
                   :background  (:bg-card colors)
                   :border      (str "1px solid " (:border colors))
                   :border-bottom "none"}]
   [".code-block pre" {:margin-top 0}]
   ;; Syntax highlighting
   [:.hl-keyword {:color (:code-keyword colors)}]
   [:.hl-string  {:color (:code-string colors)}]
   [:.hl-number  {:color (:code-number colors)}]
   [:.hl-comment {:color (:code-comment colors)}]
   [:.hl-type    {:color (:code-type colors)}]])

;; --- Responsive ---

(def responsive-styles
  [(at-media {:max-width "768px"}
     [:.container {:padding "0 1rem"}]
     [:.container-wide {:padding "0 1rem"}]
     [:.hero-tagline {:font-size "1rem"}]
     [:.hero-ctas {:flex-direction "column"
                   :gap            "1rem"
                   :align-items    "center"}]
     [:.docs-layout {:flex-direction "column"}]
     [:.docs-sidebar {:width    "100%"
                      :position "static"
                      :max-height "none"}]
     [:.card-grid {:grid-template-columns "1fr"}]
     [:.nav {:flex-direction "column"
             :gap "0.75rem"}]
     [:.nav-links {:flex-wrap "wrap"
                   :justify-content "center"}])])

;; --- Aggregate ---

(defn site-css []
  (garden/css
    (concat base-styles
            layout-styles
            nav-styles
            footer-styles
            hero-styles
            banner-styles
            card-styles
            sidebar-styles
            code-styles
            responsive-styles)))
