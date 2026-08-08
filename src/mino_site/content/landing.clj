(ns mino-site.content.landing
  "Homepage content."
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(def ^:private embed-example
  "// Create a runtime and register a C++ type.
mino_state *S   = mino_state_new();
mino_env   *env = mino_env_new(S);
mino_install(S, env, MINO_CAP_DEFAULT | MINO_CAP_HOST);

mino_host_enable(S);
mino_host_register_ctor(S, \"EventSource\", 0, source_new, NULL);
mino_host_register_method(S, \"EventSource\", \"next\", 0, source_next, NULL);
mino_host_register_getter(S, \"EventSource\", \"count\", source_count, NULL);

// Evaluate a mino script, extract the result.
mino_val *result = mino_eval_string(S, script, env);
if (result)
    mino_println(S, result);")

(def ^:private expose-example
  "// Wrap a C++ object as a mino handle.
static mino_val *source_new(mino_state *S, mino_val *,
                              mino_val *, void *) {
    auto *src = new EventSource;
    src->events.push_back(make_event(S, \"temp\", \"sensor-01\", 21.3));
    src->events.push_back(make_event(S, \"temp\", \"sensor-02\", 19.8));
    // ...
    return mino_handle_ex(S, src, \"EventSource\",
        [](void *p, const char *) { delete (EventSource *)p; });
}

// Return the next event as a mino map, or nil.
static mino_val *source_next(mino_state *S, mino_val *target,
                               mino_val *, void *) {
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
        "Three tiers — Floor (the minimum embedder commits to), "
        "Sandbox (canonical Clojure-core surface plus safe bundled "
        "libs), and Standalone (the Homebrew bottle). Each card "
        "shows stripped footprint with the JIT pipeline included "
        "(the default), the in-process init cost from "
        [:code "mino_state_new"] " through the install call, and "
        "the JIT-vs-no-JIT delta — JIT costs 34-50 KB across the "
        "tiers and effectively zero cold-start ms. Measured on "
         "Apple Silicon (arm64-darwin)."]
       [:div.stat-grid
        [:div.stat-card
         [:div.stat-eyebrow "Floor — minimum"]
         [:div.stat-row
          [:span.stat-value "651 KB"]
          [:span.stat-aside "0.18 ms init"]]
         [:p.stat-detail
          "Reader, evaluator, GC, persistent collections, numeric "
          "ops, foundational macros. No "
          [:code "core.clj"] " eval. Call "
          [:code "mino_install_minimal"] " and you are running. "
          "JIT-less variant ships at 601 KB."]]
        [:div.stat-card
         [:div.stat-eyebrow "Sandbox — full Clojure"]
         [:div.stat-row
          [:span.stat-value "943 KB"]
          [:span.stat-aside "2.65 ms init"]]
         [:p.stat-detail
          "Floor plus regex, bignum, multimethods, protocols, "
          "transducers, and the safe bundled libs — every name a "
          "Clojure scripter expects. Call "
          [:code "mino_install_sandbox"]
          " or " [:code "mino_install(S, env, MINO_CAP_DEFAULT)"]
          ". JIT-less variant ships at 909 KB."]]
        [:div.stat-card
         [:div.stat-eyebrow "Standalone — everything"]
         [:div.stat-row
          [:span.stat-value "996 KB"]
          [:span.stat-aside "2.78 ms init"]]
         [:p.stat-detail
          "Sandbox plus I/O, FS, STM, agents, async, bundled "
          [:code "clojure.*"] " stdlib, REPL, crash handler. The "
          "released Homebrew bottle. JIT-less variant is the "
          [:code "mino-lean"] " sibling binary at 962 KB."]]]
       [:p.stat-footnote
        "Cold start (full process spawn, +JIT): Floor 4.0 ms, "
        "Sandbox 7.0 ms, Standalone 8.0 ms (median of 50 runs each). "
        "Disabling the JIT shifts none of those by more than the "
        "measurement noise floor. The JIT pays back 1.8-6.5x on "
        "compute-bound hot code — see "
        [:a {:href "/documentation/jit/"} "JIT"]
        " for the workload table, "
        [:a {:href "/documentation/performance/"} "Performance"]
        " for per-operation costs, footprint detail, and capability "
        "opt-in mechanics."]]
      [:section {:style "margin-top: 3rem;"}
       [:div.use-case-grid
        [:div.use-case
         [:strong "Drop into any host with C FFI"]
         [:p "C, C++, Rust, Go, Java, .NET, Swift, Zig, and beyond. "
          "Link the library, create a runtime, evaluate scripts. "
          "No external runtime, no daemon. An in-process "
          "copy-and-patch JIT speeds hot functions on every "
          "supported host arch and can be switched off per state; "
          "the parallel " [:code "mino-lean"] " build ships the "
          "interpreter only when a smaller binary is the priority. "
          "The host owns the process and calls in when it wants "
          "the script to run."]]
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
          "macros, and a REPL-driven workflow. mino is Clojure-inspired "
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
