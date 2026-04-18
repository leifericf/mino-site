(ns mino-site.content.landing
  "Homepage content."
  (:require
    [hiccup2.core :as h]))

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

(def ^:private cpp-example
  "// Define host callbacks for a Sensor type.
static mino_val_t *sensor_new(mino_state_t *S, mino_val_t *,
                              mino_val_t *, void *) {
    return mino_handle(S, new Sensor(\"temp-01\"), \"Sensor\");
}

static mino_val_t *sensor_read(mino_state_t *S, mino_val_t *target,
                               mino_val_t *, void *) {
    auto *s = static_cast<Sensor *>(mino_handle_ptr(target));
    return mino_float(S, s->read_value());
}")

(def ^:private mino-example
  ";; Use host types with dot-syntax and tail recursion.
(defn avg-readings [sensor n]
  (loop [i 0 total 0.0]
    (if (= i n)
      (/ total n)
      (recur (inc i) (+ total (.read sensor))))))

(let [s (new Sensor)]
  (println \"sensor:\" (.-name s))
  (avg-readings s 100))")

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
        "The scripter writes logic in mino."]
       [:div.code-panels
        [:div.code-panel
         [:div.code-panel-header "Embed"]
         [:pre [:code {:data-lang "c"} embed-example]]]
        [:div.code-panel
         [:div.code-panel-header "Expose"]
         [:pre [:code {:data-lang "c"} cpp-example]]]
        [:div.code-panel
         [:div.code-panel-header "Script"]
         [:pre [:code {:data-lang "mino"} mino-example]]]]])))
