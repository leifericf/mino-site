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
       "Rapid proof-of-concept development. Expect breaking changes between releases until v1.0."]
      [:section.hero
       [:h1.hero-tagline
        "An embeddable Clojure-inspired Lisp."
        [:br.desktop-br] " Written in portable C99."]
       [:p.hero-subtitle
        "Cooperative async by default, with host-granted threading "
        "when needed."]
       [:div.hero-ctas
        [:a.cta-primary {:href "/get-started/"} "Get Started"]
        [:a.cta-secondary {:href "/documentation/"} "Documentation"]]]
      [:section {:style "margin-top: 3rem;"}
       [:h2 "Small enough to drop in. Fast enough to be useful."]
       [:p {:style "margin-bottom: 1rem;"}
        "Two footprints — the floor an embedder commits to and the "
        "ceiling the standalone CLI ships with — plus the in-process "
        "init cost an embedder pays per runtime. Cold start is the "
        "median of 100 invocations on x86_64 Linux."]
       [:div.stat-grid
        [:div.stat-card
         [:div.stat-eyebrow "Embedded — minimum"]
         [:div.stat-row
          [:span.stat-value "666 KB"]
          [:span.stat-aside "~7.0 ms cold start"]]
         [:p.stat-detail
          "State plus core only — no batteries, no I/O capabilities. "
          "What an embedder commits to."]]
        [:div.stat-card
         [:div.stat-eyebrow "Standalone — full"]
         [:div.stat-row
          [:span.stat-value "898 KB"]
          [:span.stat-aside "~7.5 ms cold start"]]
         [:p.stat-detail
          "Everything: I/O, FS, STM, agents, bundled "
          [:code "clojure.*"] " stdlib, REPL, crash handler. The "
          "released Homebrew bottle."]]
        [:div.stat-card
         [:div.stat-eyebrow "In-process init"]
         [:div.stat-row
          [:span.stat-value "5.4 ms"]
          [:span.stat-aside
           [:code "mino_state_new"] " → ready"]]
         [:p.stat-detail
          "Paid once per runtime. An embedder that spins one per "
          "request pays it every time; one runtime up-front pays it "
          "once."]]]
       [:p.stat-footnote
        "See "
        [:a {:href "/documentation/performance/"} "Performance"]
        " for the full bench numbers and the per-operation costs "
        "behind these."]]
      [:section {:style "margin-top: 3rem;"}
       [:div.use-case-grid
        [:div.use-case
         [:strong "Drop into any host with C FFI"]
         [:p "C, C++, Rust, Go, Java, .NET, Swift, Zig, and beyond. "
          "Link the library, create a runtime, evaluate scripts. "
          "No external runtime, no JIT, no daemon. The host owns the "
          "process and calls in when it wants the script to run."]]
        [:div.use-case
         [:strong "Isolated runtimes with explicit message-passing"]
         [:p "Each runtime is a failure domain with its own heap and "
          "garbage collector. Cross-runtime data moves by deep copy. "
          "Run many tenants in one process without shared mutable state."]]
        [:div.use-case
         [:strong "Capability-gated host interop"]
         [:p "Fresh runtimes start with no I/O, no filesystem, and no "
          "ambient state. The embedder opts in to each capability and "
          "registers exactly the host types and methods scripts can call."]]
        [:div.use-case
         [:strong "Clojure-inspired ergonomics"]
         [:p "Persistent immutable data structures, lazy sequences, "
          "macros, and a REPL-driven workflow. Mino is Clojure-inspired "
          "today and continues to close canon gaps over time."]]]]
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
      [:section {:style "margin-top: 4rem;"}
       [:h2 "Use cases"]
       [:div.use-case-grid
        [:a.use-case {:href "/use-cases/configuration/"}
         [:strong "Configuration"]
         [:p "Sandboxed evaluation of structured config with computed "
          "values, conditionals, and host queries."]]
        [:a.use-case {:href "/use-cases/rules_engine/"}
         [:strong "Rules engines"]
         [:p "Host state exposed to mino predicates for declarative "
          "business logic, validation, and policy."]]
        [:a.use-case {:href "/use-cases/plugins/"}
         [:strong "Plugins"]
         [:p "Load user scripts with controlled capabilities and "
          "resource limits. No ambient access."]]
        [:a.use-case {:href "/use-cases/console/"}
         [:strong "Interactive consoles"]
         [:p "In-app REPLs for live inspection, debugging, and "
          "runtime configuration."]]
        [:a.use-case {:href "/use-cases/data_pipeline/"}
         [:strong "Data pipelines"]
         [:p "Compose " [:code "map"] ", " [:code "filter"]
          ", " [:code "reduce"] " over persistent collections "
          "with structural sharing."]]
        [:a.use-case {:href "/use-cases/event_processing/"}
         [:strong "Event processing"]
         [:p "Filter, group, and aggregate streams of host data. "
          "Change the rules without recompiling."]]
        [:a.use-case {:href "/use-cases/game_scripting/"}
         [:strong "Game scripting"]
         [:p "Embed a programmable console with sandboxing and "
          "step limits for player-authored code."]]
        [:a.use-case {:href "/use-cases/automation/"}
         [:strong "Automation"]
         [:p "User-defined workflows over host APIs with "
          "full macro and REPL support."]]]]
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
