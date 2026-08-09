(ns mino-site.client.search
  "Command palette search island. Opens a centered modal on icon click,
  '/' key, or Ctrl/Cmd+K. Lazy-loads /search/index.json on first open,
  runs prefix matching in memory, renders grouped results with keyboard
  navigation."
  (:require [clojure.string :as str]
            [mino-site.search.tokens :as tokens]
            [replicant.dom :as r.dom]))

;; ---------- Index state --------------------------------------------------

(defonce ^:private index-state
  (atom {:status :idle
         :index  nil}))

(defn- ensure-index!
  "Fetch /search/index.json once. Returns a JS Promise."
  []
  (let [{:keys [status]} @index-state]
    (cond
      (= :ready status)
      (js/Promise.resolve (:index @index-state))

      (= :loading status)
      (js/Promise.
        (fn [resolve _]
          (let [check (fn check []
                        (let [s (:status @index-state)]
                          (cond
                            (= :ready s) (resolve (:index @index-state))
                            (= :error s) (resolve nil)
                            :else (js/setTimeout check 30))))]
            (check))))

      :else
      (do
        (swap! index-state assoc :status :loading)
        (-> (js/fetch "/search/index.json")
            (.then (fn [resp]
                     (if (.-ok resp)
                       (.json resp)
                       (throw (js/Error. (str "HTTP " (.-status resp)))))))
            (.then (fn [data]
                     (let [idx (js->clj data)]
                       (swap! index-state assoc :status :ready :index idx)
                       idx)))
            (.catch (fn [e]
                      (js/console.warn "search index unavailable" e)
                      (swap! index-state assoc :status :error)
                      nil)))))))

;; ---------- Matching -----------------------------------------------------

(def ^:private per-category 5)

(defn- score [needle hay]
  (cond
    (= needle hay) 0
    (and needle hay (str/starts-with? hay needle)) (count hay)
    (and needle hay (str/includes? hay needle)) (+ 1000 (count hay))
    :else nil))

(defn- match-category
  [q key items]
  (->> items
       (keep (fn [entry]
               (let [norm (get entry "norm")
                     sc (when norm (score q norm))]
                 (when sc [(assoc entry "kind" (name key)) sc]))))
       (sort-by second)
       (take per-category)
       (map first)))

(defn- match-all [q idx]
  (let [nq (tokens/normalize q)]
    (when (and (not (str/blank? nq)) (>= (count nq) 1))
      {"pages"    (match-category nq :pages (get idx "pages"))
       "api"      (match-category nq :api (get idx "api"))
       "language" (match-category nq :language (get idx "language"))})))

(defn- flat-hits [groups]
  (when groups
    (vec
      (mapcat #(get groups %) ["pages" "api" "language"]))))

;; ---------- Rendering ----------------------------------------------------

(def ^:private category-labels
  {"pages"    "Pages"
   "api"      "C API"
   "language" "Language"})

(def ^:private category-order ["pages" "api" "language"])

(defn- result-name [entry kind]
  (case kind
    "pages"    (get entry "title")
    "api"      (get entry "name")
    "language" (get entry "name")))

(defn- result-desc [entry]
  (let [d (get entry "desc")]
    (when (and d (pos? (count d)))
      (if (> (count d) 80)
        (str (subs d 0 77) "...")
        d))))

(defn- result-badge [entry kind]
  (case kind
    "pages"    "page"
    "api"      (get entry "type" "symbol")
    "language" (get entry "type" "function")))

(defn- result-uri [entry kind]
  (case kind
    "pages"    (get entry "uri")
    "api"      (get entry "uri")
    "language" (get entry "uri")))

(defn- result-row [h selected idx]
  (let [active? (= idx selected)
        kind (get h "kind")]
    [:li {:class (when active? "active")
          :role "option"
          :aria-selected (if active? "true" "false")}
     [:a {:href (result-uri h kind)}
      [:span.search-result-name (result-name h kind)]
      [:span.search-result-badge (result-badge h kind)]
      (when-let [d (result-desc h)]
        [:span.search-result-desc d])]]))

(defn- results-view [groups selected]
  (let [idx (atom -1)]
    [:div.search-modal-results
     {:role "listbox"}
     (for [cat category-order
           :let [hits (get groups cat)]
           :when (seq hits)]
       [:div.search-modal-cat
        [:div.search-modal-cat-label (category-labels cat)]
        (into [:ul]
              (for [h hits]
                (result-row h selected (swap! idx inc))))])]))

(defn- modal-view [state]
  (let [{:keys [query groups selected flat-len open?]} state]
    (if-not open?
      nil
      [:div.search-overlay
       {:on-click #(close-modal!)}
       [:div.search-modal
        {:on-click (fn [e] (.stopPropagation e))}
        [:div.search-modal-header
         [:input.search-modal-input
          {:type "search"
           :placeholder "Search mino docs..."
           :autocomplete "off"
           :autofocus true}]
         [:button.search-modal-close
          {:on-click #(swap! state-atom assoc :open? false)}
          "Esc"]]
        (cond
          (or (str/blank? query) (zero? (count query)))
          nil

          (and groups (pos? flat-len))
          (results-view groups selected)

          :else
          [:div.search-modal-results
           [:p.search-no-results "No matches found."]])]])))

;; ---------- Modal lifecycle ----------------------------------------------

(def ^:private state-atom
  (atom {:query "" :groups nil :selected nil :flat-len 0 :open? false}))

(defn- render! []
  (r.dom/render
    (.getElementById js/document "search-mount")
    (modal-view @state-atom)))

(defn- open-modal! []
  (swap! state-atom assoc :open? true :query "" :groups nil :selected nil :flat-len 0)
  (render!)
  (js/setTimeout
    #(.focus (.querySelector js/document ".search-modal-input"))
    0)
  (ensure-index!))

(defn- close-modal! []
  (swap! state-atom assoc :open? false)
  (render!))

(defn- navigate-to-selected! []
  (let [{:keys [groups selected]} @state-atom
        hits (flat-hits groups)]
    (when (and selected (>= selected 0))
      (when-let [hit (nth hits selected nil)]
        (let [kind (get hit "kind")]
          (set! (.-href (.-location js/window)) (result-uri hit kind)))))))

;; ---------- Event handlers ----------------------------------------------

(defn- debounce [f ms]
  (let [t (atom nil)]
    (fn [& args]
      (when-let [id @t] (js/clearTimeout id))
      (reset! t (js/setTimeout #(apply f args) ms)))))

(defn- current-query []
  (when-let [input (.querySelector js/document ".search-modal-input")]
    (.-value input)))

(def ^:private update-query!
  (debounce
    (fn []
      (let [q (or (current-query) "")]
        (-> (ensure-index!)
            (.then (fn [idx]
                     (let [groups (match-all q idx)
                           flat (flat-hits groups)]
                       (swap! state-atom assoc
                              :query q
                              :groups groups
                              :flat-len (count flat)
                              :selected (when (seq flat) 0))
                       (render!)))))))
    120))

(defn- keyboard-handler [e]
  (let [k (.-key e)
        in-input? (let [el (.-activeElement js/document)
                        tag (and el (.-tagName el))]
                    (and tag (contains? #{"INPUT" "TEXTAREA" "SELECT"} tag)))
        mod-k? (or (.-metaKey e) (.-ctrlKey e))]
    (cond
      ;; Open: / or Ctrl/Cmd+K (only when not in an input)
      (and (= k "/") (not in-input?))
      (do (.preventDefault e) (open-modal!))

      (and mod-k? (= (str/lower-case k) "k"))
      (do (.preventDefault e) (open-modal!))

      ;; Modal-only keys (only when modal is open)
      (and (:open? @state-atom) (= k "Escape"))
      (do (.preventDefault e) (close-modal!))

      (and (:open? @state-atom) (= k "ArrowDown"))
      (let [n (:flat-len @state-atom)]
        (when (pos? n)
          (.preventDefault e)
          (swap! state-atom update :selected #(mod (inc (or % -1)) n))
          (render!)))

      (and (:open? @state-atom) (= k "ArrowUp"))
      (let [n (:flat-len @state-atom)]
        (when (pos? n)
          (.preventDefault e)
          (swap! state-atom update :selected #(mod (dec (or % 0)) n))
          (render!)))

      (and (:open? @state-atom) (= k "Enter"))
      (do (.preventDefault e) (navigate-to-selected!))

      (and (:open? @state-atom) (not= k "/"))
      (update-query!))))

;; ---------- Init ---------------------------------------------------------

(defn init! []
  ;; Create the mount point for the modal
  (let [mount (.createElement js/document "div")]
    (set! (.-id mount) "search-mount")
    (.appendChild (.-body js/document) mount))

  ;; Watch state for re-renders when results change
  ;; (query is read from the DOM, not from state)

  ;; Global keyboard handler
  (.addEventListener js/document "keydown" keyboard-handler)

  ;; Wire up search trigger links
  (doseq [trigger (array-seq
                    (.querySelectorAll js/document "[data-search-trigger]"))]
    (.addEventListener trigger "click"
                       (fn [e]
                         (.preventDefault e)
                         (open-modal!)))))
