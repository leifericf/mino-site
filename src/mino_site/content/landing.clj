(ns mino-site.content.landing
  "Homepage content."
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(def ^:private embed-example
  "// Create a sandboxed runtime with host capabilities.
mino_state_t *S   = mino_state_new();
mino_env_t   *env = mino_new(S);

mino_host_enable(S);
mino_host_register_ctor(S, \"Sensor\", 0, sensor_new, NULL);
mino_host_register_method(S, \"Sensor\", \"read\", 0, sensor_read, NULL);
mino_host_register_getter(S, \"Sensor\", \"name\", sensor_name, NULL);

mino_val_t *result = mino_eval_string(S, script, env);

double avg;
if (mino_to_float(result, &avg))
    printf(\"average: %.1f\\n\", avg);")

(def ^:private expose-example
  "// Define host callbacks for a Sensor type.
static mino_val_t *sensor_new(mino_state_t *S, mino_val_t *,
                              mino_val_t *, void *) {
    return mino_handle(S, new Sensor(\"temp-01\"), \"Sensor\");
}

static mino_val_t *sensor_read(mino_state_t *S, mino_val_t *target,
                               mino_val_t *, void *) {
    auto *s = static_cast<Sensor *>(mino_handle_ptr(target));
    return mino_float(S, s->read_value());
}

static mino_val_t *sensor_name(mino_state_t *S, mino_val_t *target,
                               mino_val_t *, void *) {
    auto *s = static_cast<Sensor *>(mino_handle_ptr(target));
    return mino_string(S, s->name().c_str());
}")

(def ^:private script-example
  ";; Use host types with dot-syntax and tail recursion.
(defn sum-readings [sensor i acc]
  (if (= i 0)
    acc
    (sum-readings sensor (dec i) (+ acc (.read sensor)))))

(let [s (new Sensor)
      n 100]
  (println \"sensor:\" (.-name s))
  (/ (sum-readings s n 0.0) n))")

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
          [:pre [:code {:data-lang "c"} embed-example]]]
         [:div.step-panel
          [:div.step-label "The C++ engineer"]
          [:pre [:code {:data-lang "c"} expose-example]]]
         [:div.step-panel
          [:div.step-label "The scripter"]
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
