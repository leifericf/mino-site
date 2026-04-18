(ns mino-site.styles
  "Garden CSS definitions — clean Nordic-inspired design."
  (:require
    [garden.core :as garden]
    [garden.stylesheet :refer [at-media]]))

;; --- Colors ---

(def colors
  {:bg           "#ffffff"
   :bg-subtle    "#f8f9fa"
   :text         "#2d3436"
   :text-muted   "#808b96"
   :heading      "#1a1a2e"
   :link         "#2c5282"
   :link-hover   "#1a365d"
   :border       "#e8ecf0"
   :code-bg      "#f5f7fa"
   :code-text    "#2d3436"
   ;; Syntax highlighting — muted, tasteful
   :code-keyword "#7c3aed"
   :code-string  "#16653e"
   :code-number  "#c2410c"
   :code-comment "#9ca3af"
   :code-type    "#2c5282"
   ;; Banner
   :banner-bg    "#fef3c7"
   :banner-text  "#92400e"
   :banner-border "#fcd34d"})

;; --- Fonts ---

(def font-body (str "-apple-system, BlinkMacSystemFont, 'Segoe UI', "
                    "Roboto, Oxygen, Ubuntu, Cantarell, sans-serif"))
(def font-mono (str "'SF Mono', 'Cascadia Code', 'JetBrains Mono', "
                    "'Fira Code', Menlo, Consolas, monospace"))

;; --- Base ---

(def base-styles
  [[:* {:box-sizing "border-box"
        :margin     0
        :padding    0}]
   [:html {:font-size "16px"}]
   [:body {:font-family font-body
           :background  (:bg colors)
           :color       (:text colors)
           :line-height "1.7"
           :-webkit-font-smoothing "antialiased"}]
   [:a {:color           (:link colors)
        :text-decoration "none"}]
   ["a:hover" {:color           (:link-hover colors)
               :text-decoration "underline"}]
   [:code {:font-family   font-mono
           :font-size     "0.8125rem"
           :background    (:code-bg colors)
           :padding       "0.15rem 0.4rem"
           :border-radius "3px"}]
   [:pre {:background    (:code-bg colors)
          :border        (str "1px solid " (:border colors))
          :border-radius "6px"
          :padding       "1.25rem"
          :overflow-x    "auto"
          :line-height   "1.5"
          :font-size     "0.8125rem"
          :max-width     "min(100vw - 3rem, 860px)"
          :margin-left   "auto"
          :margin-right  "auto"}]
   ["pre code" {:background "none"
                :padding    0}]
   [:p {:margin-bottom "1rem"
        :line-height   "1.8"}]
   ["p + pre" {:margin-top "0.75rem"}]
   ["pre + p" {:margin-top "1.25rem"}]
   [:h1 :h2 :h3 {:color       (:heading colors)
                  :font-weight "600"
                  :line-height "1.3"}]
   [:h1 {:font-size     "2.25rem"
         :margin-bottom "2rem"
         :letter-spacing "-0.025em"}]
   [:h2 {:font-size     "1.5rem"
         :margin-top    "3rem"
         :margin-bottom "1.25rem"
         :letter-spacing "-0.02em"}]
   [:h3 {:font-size     "1.125rem"
         :margin-top    "2rem"
         :margin-bottom "1rem"}]
   [:ul :ol {:margin-bottom "1rem"
             :padding-left  "1.5rem"}]
   [:li {:margin-bottom "0.4rem"
         :line-height   "1.7"}]
   [:hr {:border     "none"
         :border-top (str "1px solid " (:border colors))
         :margin     "2rem 0"}]
   ["::selection" {:background "#dbeafe"
                   :color      (:heading colors)}]])

;; --- Layout ---

(def layout-styles
  [[:.container {:max-width "680px"
                 :margin    "0 auto"
                 :padding   "0 1.5rem"}]
   [:.container-wide {:max-width "960px"
                      :margin    "0 auto"
                      :padding   "0 1.5rem"}]
   [:main {:padding-top "2.5rem"}]])

;; --- Nav ---

(def nav-styles
  [[:.nav {:display         "flex"
           :align-items     "baseline"
           :justify-content "space-between"
           :padding         "1.5rem 0 0.75rem"
           :border-bottom   (str "1px solid " (:border colors))}]
   [:.nav-logo {:font-family  font-mono
                :font-size   "1.125rem"
                :font-weight "700"
                :color       (:heading colors)
                :letter-spacing "-0.02em"}]
   ["a.nav-logo" {:text-decoration "none"}]
   ["a.nav-logo:hover" {:color (:link colors)
                        :text-decoration "none"}]
   [:.nav-links {:display    "flex"
                 :gap        "2rem"
                 :list-style "none"}]
   [:.nav-links>li>a {:color       (:text-muted colors)
                      :font-size   "0.9rem"
                      :font-weight "400"}]
   [".nav-links li a:hover" {:color           (:text colors)
                              :text-decoration "none"}]
   [".nav-links li a.active" {:color       (:heading colors)
                               :font-weight "500"}]
   ;; Hamburger toggle — hidden on desktop
   [:.nav-toggle {:display    "none"
                  :background "none"
                  :border     "none"
                  :cursor     "pointer"
                  :padding    "0.25rem"
                  :color      (:heading colors)
                  :font-size  "1.5rem"
                  :line-height "1"}]])

;; --- Footer ---

(def footer-styles
  [[:.footer {:margin-top  "6rem"
              :padding     "2.5rem 0"
              :border-top  (str "1px solid " (:border colors))
              :text-align  "center"
              :color       (:text-muted colors)
              :font-size   "0.85rem"}]
   [".footer a" {:color (:text-muted colors)}]
   [".footer a:hover" {:color (:text colors)}]])

;; --- Hero ---

(def hero-styles
  [[:.hero {:padding    "5rem 0 4rem"
            :text-align "center"}]
   [:.hero-tagline {:font-family   font-mono
                    :font-size     "2.25rem"
                    :font-weight   "600"
                    :color         (:heading colors)
                    :margin-bottom "1rem"
                    :line-height   "1.3"
                    :letter-spacing "-0.02em"}]
   [:.hero-points {:list-style     "none"
                   :padding-left   "0"
                   :color          (:text-muted colors)
                   :font-size      "1rem"
                   :line-height    "2"
                   :margin-bottom  "2.5rem"
                   :display        "inline-block"
                   :text-align     "left"}]
   [:.hero-subtitle {:font-size     "1.125rem"
                     :color         (:text-muted colors)
                     :margin-bottom "2.5rem"
                     :line-height   "1.6"}]
   [:.hero-ctas {:display         "flex"
                 :justify-content "center"
                 :gap             "1rem"
                 :margin-top      "2rem"}]
   [:.cta-primary {:display        "inline-block"
                   :padding        "0.75rem 1.75rem"
                   :background     (:heading colors)
                   :color          "#ffffff"
                   :border-radius  "6px"
                   :font-size      "0.95rem"
                   :font-weight    "500"}]
   ["a.cta-primary:hover" {:background      (:link colors)
                            :color           "#ffffff"
                            :text-decoration "none"}]
   [:.cta-secondary {:display        "inline-block"
                     :padding        "0.75rem 1.75rem"
                     :border         (str "1px solid " (:border colors))
                     :color          (:text colors)
                     :border-radius  "6px"
                     :font-size      "0.95rem"
                     :font-weight    "500"}]
   ["a.cta-secondary:hover" {:border-color    (:text-muted colors)
                              :text-decoration "none"}]])

;; --- Banner ---

(def banner-styles
  [[:.banner {:background    (:banner-bg colors)
              :border-bottom (str "1px solid " (:banner-border colors))
              :padding       "0.6rem 1rem"
              :text-align    "center"
              :font-size     "0.85rem"
              :color         (:banner-text colors)}]])

;; --- Cards ---

(def card-styles
  [[:.card-grid {:display               "grid"
                 :grid-template-columns  "repeat(auto-fit, minmax(280px, 1fr))"
                 :gap                    "1.5rem"
                 :margin-top             "1.5rem"}]
   [:.card {:border        (str "1px solid " (:border colors))
            :border-radius "8px"
            :padding       "1.75rem"
            :transition    "border-color 0.15s ease"}]
   ["a.card" {:color           (:text colors)
              :text-decoration "none"
              :display         "block"}]
   ["a.card:hover" {:border-color    (:text-muted colors)
                     :text-decoration "none"}]
   [:.card-title {:font-size     "1.125rem"
                  :font-weight   "600"
                  :color         (:heading colors)
                  :margin-bottom "0.5rem"}]
   [:.card-desc {:color       (:text-muted colors)
                 :font-size   "0.9rem"
                 :line-height "1.6"}]])

;; --- Docs sidebar ---

(def sidebar-styles
  [[:.docs-layout {:display  "flex"
                   :gap      "3rem"
                   :padding  "2.5rem 0"}]
   [:.docs-sidebar {:width       "200px"
                    :flex-shrink "0"
                    :position    "sticky"
                    :top         "1.5rem"
                    :align-self  "flex-start"
                    :max-height  "calc(100vh - 3rem)"
                    :overflow-y  "auto"}]
   [:.docs-sidebar>ul {:list-style "none"
                       :padding    0}]
   [".docs-sidebar li" {:margin-bottom "0.5rem"}]
   [".docs-sidebar a" {:color       (:text-muted colors)
                       :font-size   "0.85rem"
                       :font-weight "400"}]
   [".docs-sidebar a:hover" {:color (:text colors)}]
   [".docs-sidebar a.active" {:color       (:heading colors)
                               :font-weight "500"}]
   [:.sidebar-header {:font-size     "0.75rem"
                      :font-weight   "600"
                      :text-transform "uppercase"
                      :letter-spacing "0.05em"
                      :color         (:text-muted colors)
                      :margin-bottom "0.75rem"}]
   [:.docs-content {:flex      "1"
                    :min-width "0"}]])

;; --- Code block ---

(def code-styles
  [[:.code-block {:margin-bottom "1.5rem"}]
   [:.code-header {:font-family font-mono
                   :font-size   "0.8rem"
                   :color       (:text-muted colors)
                   :padding     "0.5rem 1.25rem"
                   :background  (:bg-subtle colors)
                   :border      (str "1px solid " (:border colors))
                   :border-bottom "none"
                   :border-radius "6px 6px 0 0"}]
   [".code-block pre" {:margin-top    0
                       :border-radius "0 0 6px 6px"}]
   ;; Syntax highlighting
   [:.hl-keyword {:color (:code-keyword colors)}]
   [:.hl-string  {:color (:code-string colors)}]
   [:.hl-number  {:color (:code-number colors)}]
   [:.hl-comment {:color  (:code-comment colors)
                  :font-style "italic"}]
   [:.hl-type    {:color (:code-type colors)}]
   ;; Step switcher (landing page)
   [:.step-switcher {:margin-top "1rem"}]
   [:.step-tabs {:display "flex"
                 :gap "0"
                 :border-bottom (str "2px solid " (:border colors))}]
   [:.step-tab {:font-family font-mono
                :font-size "0.85rem"
                :padding "0.6rem 1.25rem"
                :background "none"
                :border "none"
                :color (:text-muted colors)
                :cursor "pointer"
                :border-bottom "2px solid transparent"
                :margin-bottom "-2px"
                :transition "color 0.15s, border-color 0.15s"}]
   [:.step-tab:hover {:color (:text colors)}]
   [:.step-tab.active {:color (:heading colors)
                       :border-bottom-color (:link colors)
                       :font-weight "600"}]
   [:.step-panel {:display "none"
                  :padding-top "0.5rem"}]
   [:.step-panel.active {:display "block"}]
   [:.step-label {:font-size "0.85rem"
                  :color (:text-muted colors)
                  :margin-bottom "0.5rem"}]
   [:.step-desc {:font-size "0.9rem"
                 :line-height "1.6"
                 :color (:text colors)
                 :margin-bottom "1rem"}]
   ;; Use case grid (landing page)
   [:.use-case-grid {:display "grid"
                     :grid-template-columns "repeat(2, 1fr)"
                     :gap "1.25rem"
                     :margin-top "1rem"}]
   [:.use-case {:padding "1.25rem"
                :border (str "1px solid " (:border colors))
                :border-radius "6px"
                :background (:bg-subtle colors)}]
   [".use-case strong" {:display "block"
                        :margin-bottom "0.35rem"
                        :color (:heading colors)
                        :font-size "0.95rem"}]
   [".use-case p" {:font-size "0.85rem"
                   :line-height "1.5"
                   :color (:text-muted colors)
                   :margin 0}]])

;; --- Declaration entries (API + Language reference) ---

(def decl-styles
  [[:.decl {:padding-bottom "1.5rem"
            :margin-bottom  "1.5rem"
            :border-bottom  (str "1px solid " (:border colors))}]
   [".api-section .decl:last-child" {:border-bottom "none"
                                      :margin-bottom 0}]
   [:.decl-name {:font-size     "1rem"
                 :font-weight   "600"
                 :margin-bottom "0.5rem"
                 :display       "flex"
                 :align-items   "baseline"
                 :gap           "0.5rem"
                 :flex-wrap     "wrap"}]
   [".decl-name code" {:font-size   "0.9375rem"
                       :font-weight "600"
                       :background  "none"
                       :padding     0
                       :color       (:heading colors)}]
   [:.decl-badge {:font-size     "0.7rem"
                  :font-weight   "500"
                  :color         (:text-muted colors)
                  :background    (:bg-subtle colors)
                  :padding       "0.1rem 0.4rem"
                  :border-radius "3px"
                  :text-transform "uppercase"
                  :letter-spacing "0.03em"}]
   [:.decl-meta {:font-size "0.8rem"
                 :color     (:text-muted colors)}]
   [:.decl-doc {:color       (:text colors)
                :font-size   "0.9rem"
                :line-height "1.7"
                :margin-top  "0.5rem"}]
   [".decl pre" {:margin-top "0.5rem"
                 :margin-left 0
                 :margin-right 0
                 :max-width "100%"}]
   ;; Section headings
   [:.api-section {:margin-bottom "2rem"}]
   [".api-section > h2" {:padding-bottom "0.5rem"
                          :border-bottom  (str "2px solid " (:border colors))
                          :margin-bottom  "1.5rem"}]])

;; --- Enum and struct tables ---

(def table-styles
  [[:table {:width         "100%"
            :border-collapse "collapse"
            :font-size    "0.85rem"
            :margin-top   "0.5rem"
            :margin-bottom "0.5rem"}]
   [:th {:text-align    "left"
         :padding       "0.5rem 0.75rem"
         :font-weight   "600"
         :color         (:text-muted colors)
         :font-size     "0.75rem"
         :text-transform "uppercase"
         :letter-spacing "0.04em"
         :border-bottom (str "2px solid " (:border colors))}]
   [:td {:padding       "0.4rem 0.75rem"
         :border-bottom (str "1px solid " (:border colors))
         :vertical-align "top"}]
   ["td code" {:font-size "0.8rem"}]
   ["tr:last-child td" {:border-bottom "none"}]])

;; --- Filter bar ---

(def filter-styles
  [[:.filter-bar {:margin-bottom "1.5rem"}]
   [:.filter-bar>input {:width         "100%"
                        :padding       "0.6rem 1rem"
                        :font-family   font-mono
                        :font-size     "0.85rem"
                        :border        (str "1px solid " (:border colors))
                        :border-radius "6px"
                        :background    (:bg colors)
                        :color         (:text colors)
                        :outline       "none"}]
   [".filter-bar input:focus" {:border-color (:link colors)
                                :box-shadow   (str "0 0 0 2px " (:link colors) "20")}]])

;; --- Examples (Language reference) ---

(def example-styles
  [[:.examples {:margin-top "0.5rem"}]
   [:.example {:margin-bottom "0.5rem"}]
   [".example pre" {:margin-bottom "0.25rem"
                    :padding       "0.75rem 1rem"
                    :font-size     "0.8125rem"
                    :max-width     "100%"
                    :margin-left   0
                    :margin-right  0}]
   [:.example-result {:display     "flex"
                      :align-items "baseline"
                      :gap         "0.5rem"
                      :padding     "0.25rem 1rem"
                      :font-family font-mono
                      :font-size   "0.8125rem"
                      :color       (:text-muted colors)}]
   [:.example-arrow {:color (:text-muted colors)}]
   [".example-result code" {:background "none"
                            :padding    0
                            :color      (:code-string colors)}]])

;; --- Stdlib source toggle ---

(def stdlib-styles
  [[:.stdlib-source {:margin-top "0.5rem"}]
   [".stdlib-source summary" {:font-size  "0.8rem"
                               :color      (:text-muted colors)
                               :cursor     "pointer"}]
   [".stdlib-source summary:hover" {:color (:text colors)}]
   [".stdlib-source pre" {:margin-top  "0.5rem"
                          :max-width   "100%"
                          :margin-left 0
                          :margin-right 0}]])

;; --- Cookbook entries ---

(def cookbook-styles
  [[:.cookbook-entry {:margin-bottom "3rem"
                     :padding-bottom "2rem"
                     :border-bottom (str "1px solid " (:border colors))}]
   [".cookbook-entry:last-child" {:border-bottom "none"}]
   [:.cookbook-desc {:color       (:text colors)
                    :font-size   "0.95rem"
                    :line-height "1.7"}]
   [:.cookbook-demonstrates {:font-size     "0.85rem"
                            :color         (:text-muted colors)
                            :margin-bottom "0.5rem"}]
   [:.cookbook-build {:font-size     "0.85rem"
                     :color         (:text-muted colors)
                     :margin-bottom "1rem"}]
   [".cookbook-build code" {:font-size "0.8rem"}]
   [".cookbook-source summary" {:font-size  "0.85rem"
                                :color      (:text-muted colors)
                                :cursor     "pointer"
                                :margin-bottom "0.5rem"}]
   [".cookbook-source summary:hover" {:color (:text colors)}]
   [".cookbook-source pre" {:margin-top "0.5rem"
                            :max-height "600px"
                            :overflow-y "auto"
                            :max-width  "100%"
                            :margin-left 0
                            :margin-right 0}]])

;; --- Responsive ---

(def responsive-styles
  [(at-media {:max-width "768px"}
     ;; Layout
     [:.container {:padding "0 1.25rem"}]
     [:.container-wide {:padding "0 1.25rem"}]
     ;; Nav — hamburger toggle
     [:.nav {:flex-wrap   "wrap"
             :align-items "center"}]
     [:.nav-toggle {:display "block"}]
     [:.nav-links {:display        "none"
                   :flex-direction "column"
                   :width          "100%"
                   :gap            "0.25rem"
                   :padding-top    "0.75rem"}]
     [:.nav-links.open {:display "flex"}]
     [".nav-links li a" {:display "block"
                         :padding "0.4rem 0"}]
     ;; Hero — smaller tagline, let it wrap naturally
     [:.hero {:padding "3rem 0 2rem"}]
     [:.hero-tagline {:font-size "1.5rem"}]
     [:.desktop-br {:display "none"}]
     [:.hero-ctas {:flex-direction "column"
                   :align-items    "center"}]
     ;; Typography
     [:h1 {:font-size "1.75rem"}]
     [:h2 {:font-size "1.25rem"
            :margin-top "2rem"}]
     ;; Docs layout — sidebar stacks above content
     [:.docs-layout {:flex-direction "column"
                     :gap            "1.5rem"}]
     [:.docs-sidebar {:width      "100%"
                      :position   "static"
                      :max-height "none"
                      :border-bottom (str "1px solid " (:border colors))
                      :padding-bottom "1rem"}]
     [:.docs-content {:min-width "0"}]
     ;; Cards
     [:.card-grid {:grid-template-columns "1fr"}]
     ;; Tables — horizontal scroll
     [:table {:display "block"
              :overflow-x "auto"
              :white-space "nowrap"}]
     ;; Pre blocks — tighter padding, full width
     [:pre {:padding   "1rem"
            :font-size "0.75rem"
            :max-width "100%"}]
     ;; Declarations — compact on mobile
     [:.decl-name {:font-size "0.9rem"}]
     [:.use-case-grid {:grid-template-columns "1fr"}]
     ;; Footer
     [:.footer {:margin-top "3rem"}])])

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
            decl-styles
            table-styles
            filter-styles
            example-styles
            stdlib-styles
            cookbook-styles
            responsive-styles)))
