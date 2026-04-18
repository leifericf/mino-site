(ns mino-site.content.landing
  "Homepage content."
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(def ^:private embed-example
  "// Create a runtime and register a C++ type.
mino_state_t *S   = mino_state_new();
mino_env_t   *env = mino_new(S);

mino_host_enable(S);
mino_host_register_ctor(S, \"EventSource\", 0, source_new, NULL);
mino_host_register_method(S, \"EventSource\", \"next\", 0, source_next, NULL);
mino_host_register_getter(S, \"EventSource\", \"count\", source_count, NULL);

// Evaluate a mino script, extract the result.
mino_val_t *result = mino_eval_string(S, script, env);
if (result)
    mino_println(S, result);")

(def ^:private expose-example
  "// Wrap a C++ object as a mino handle.
static mino_val_t *source_new(mino_state_t *S, mino_val_t *,
                              mino_val_t *, void *) {
    auto *src = new EventSource;
    src->events.push_back(make_event(S, \"temp\", \"sensor-01\", 21.3));
    src->events.push_back(make_event(S, \"temp\", \"sensor-02\", 19.8));
    // ...
    return mino_handle_ex(S, src, \"EventSource\",
        [](void *p, const char *) { delete (EventSource *)p; });
}

// Return the next event as a mino map, or nil.
static mino_val_t *source_next(mino_state_t *S, mino_val_t *target,
                               mino_val_t *, void *) {
    auto *src = (EventSource *)mino_handle_ptr(target);
    if (src->cursor >= src->events.size())
        return mino_nil(S);
    return src->events[src->cursor++];
}")

(def ^:private script-example
  ";; Consume all events into a vector via self-recursion.
(defn drain [source acc]
  (let [evt (.next source)]
    (if (nil? evt)
      acc
      (drain source (conj acc evt)))))

;; Summarize a [device readings] group.
(defn summarize [[device readings]]
  [device {:count (count readings)
           :avg   (/ (reduce + (map :value readings))
                     (count readings))}])

;; Filter, group, summarize.
(defn analyze [events type-filter]
  (->> events
       (filter #(type-filter (:type %)))
       (group-by :device)
       (map summarize)
       (into (sorted-map))))

(let [events (drain (new EventSource) [])]
  (analyze events #{:temp}))
;; => {\"sensor-01\" {:count 4, :avg 22.0}
;;     \"sensor-02\" {:count 3, :avg 19.9}}")

(defn landing-page
  "Generates the homepage HTML body."
  [mino-root]
  (str
    (h/html
      [:div.banner
       "Unstable alpha proof-of-concept. The API may change before v1.0."]
      [:section.hero
       [:h1.hero-tagline
        "A tiny embeddable Lisp " [:br.desktop-br] "in pure ANSI C."]
       [:div.hero-ctas
        [:a.cta-primary {:href "/get-started/"} "Get Started"]
        [:a.cta-secondary {:href "/documentation/"} "Documentation"]
        [:a.cta-secondary {:href "/about/"} "About"]]]
      [:section {:style "margin-top: 4rem;"}
       [:h2 "Three roles, one runtime"]
       [:p {:style "margin-bottom: 1rem;"}
        "The application developer embeds mino. "
        "The C++ engineer exposes host types. "
        "The scripter writes logic."]
       [:div.step-switcher
        [:div.step-tabs
         [:button.step-tab.active {:data-step "0"} "1. Embed"]
         [:button.step-tab {:data-step "1"} "2. Expose"]
         [:button.step-tab {:data-step "2"} "3. Script"]]
        [:div.step-panels
         [:div.step-panel.active
          [:div.step-label "The application developer"]
          [:p.step-desc
           "A fresh runtime starts with zero capabilities. No file "
           "access, no network, no ambient globals to remove. The "
           "host opts in to exactly what the script can reach. "
           "Types are registered declaratively by name, arity, and "
           "role (constructor, method, getter) rather than pushing "
           "individual functions onto a stack. Each runtime is fully "
           "isolated so multiple runtimes can run on separate threads "
           "with no shared state."]
          [:pre [:code {:data-lang "c"} embed-example]]]
         [:div.step-panel
          [:div.step-label "The C++ engineer"]
          [:p.step-desc
           "Host objects are wrapped as type-tagged handles with "
           "automatic GC cleanup. The API uses direct value pointers "
           "rather than stack indices, so there is no stack balancing "
           "and no off-by-one indexing errors. Data returned to the "
           "script is immutable. The script cannot mutate your state "
           "through the values you hand it, and you never need to "
           "defensively copy data at the boundary."]
          [:pre [:code {:data-lang "c"} expose-example]]]
         [:div.step-panel
          [:div.step-label "The scripter"]
          [:p.step-desc
           "The scripter writes processing rules without knowing "
           "anything about C++. Every value flowing through the "
           "pipeline is immutable "
           "with structural sharing, so there are no aliasing bugs "
           "and no \"who mutated my table\" surprises. Keywords "
           "like " [:code ":device"] " double as data accessors. "
           "The set " [:code "#{:temp}"] " is used directly as a "
           "filter predicate because collections are callable. "
           [:code "drain"] " recurses in constant stack space "
           "via automatic tail-call optimization. "
           "Change the processing logic without recompiling."]
          [:pre [:code {:data-lang "mino"} script-example]]]]]]
      [:script (hu/raw-string
        "document.querySelectorAll('.step-tab').forEach(function(tab){
  tab.addEventListener('click',function(){
    var idx=this.getAttribute('data-step');
    document.querySelectorAll('.step-tab').forEach(function(t){t.classList.remove('active')});
    document.querySelectorAll('.step-panel').forEach(function(p){p.classList.remove('active')});
    this.classList.add('active');
    document.querySelectorAll('.step-panel')[idx].classList.add('active');
    if(window.hlAll)hlAll();
  });
});")])))
