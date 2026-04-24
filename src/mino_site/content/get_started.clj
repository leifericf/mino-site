(ns mino-site.content.get-started
  "Get Started page content."
  (:require
    [hiccup2.core :as h]))

(defn get-started-page
  "Generates the Get Started page HTML body."
  []
  (str
    (h/html
      [:h1 "Get Started"]

      [:h2 "1. Get the source"]
      [:p "Clone the repository:"]
      [:pre [:code "git clone https://github.com/leifericf/mino.git"]]
      [:p "Or "
       [:a {:href "https://github.com/leifericf/mino/archive/refs/heads/main.zip"}
        "download a zip archive"]
       ". mino is a small C99 codebase in " [:code "src/"]
       ". Any C99 compiler, no external dependencies."]

      [:h2 "2. Build"]
      [:p "Bootstrap the standalone REPL:"]
      [:pre [:code "cd mino\ncc -std=c99 -O2 -Isrc -o mino src/*.c main.c -lm\n./mino task build"]]
      [:p "Or compile mino directly into your own program:"]
      [:pre [:code "cc -std=c99 -Isrc -o myapp myapp.c src/*.c -lm"]]
      [:p "Run the test suite:"]
      [:pre [:code "./mino task test"]]

      [:h2 "3. Embed in your C program"]
      [:p "A minimal embedding creates a runtime, registers a host "
       "function, evaluates mino code, and extracts the result:"]
      [:pre
       [:code {:data-lang "c"}
"#include \"mino.h\"
#include <stdio.h>

/* A host function exposed to mino as (add-tax amount). */
static mino_val_t *host_add_tax(mino_state_t *S, mino_val_t *args,
                                mino_env_t *env)
{
    long long amount;
    (void)env;
    if (!mino_is_cons(args) || !mino_to_int(args->as.cons.car, &amount))
        return mino_nil(S);
    return mino_float(S, (double)amount * 1.08);
}

int main(void)
{
    mino_state_t *S   = mino_state_new();
    mino_env_t   *env = mino_new(S);       /* env + core + I/O   */
    mino_register_fn(S, env, \"add-tax\", host_add_tax);

    mino_val_t *result = mino_eval_string(S,
        \"(def prices [100 200 300])\\n\"
        \"(reduce + (map add-tax prices))\\n\",
        env);

    if (result) {
        double total;
        if (mino_to_float(result, &total))
            printf(\"total with tax: %.2f\\n\", total);
    }

    mino_env_free(S, env);
    mino_state_free(S);
    return 0;
}"]]
      [:p "Key points:"]
      [:ul
       [:li [:code "mino_state_new()"] " creates an isolated runtime "
        "state that owns the GC, intern tables, and all allocated "
        "objects."]
       [:li [:code "mino_new(S)"] " creates an environment with core "
        "and I/O bindings installed."]
       [:li [:code "mino_register_fn()"] " exposes a C function to "
        "mino code under any name."]
       [:li [:code "mino_eval_string()"] " reads and evaluates all "
        "forms, returning the last result."]
       [:li [:code "mino_to_float()"] " safely extracts a C value "
        "from the result (returns 0 on type mismatch)."]
       [:li [:code "mino_env_free()"] " and "
        [:code "mino_state_free()"] " tear down the environment "
        "and state."]]

      [:h2 "4. Try the REPL"]
      [:p "The standalone REPL is useful for exploring the language "
       "interactively:"]
      [:pre
       [:code {:data-lang "mino"}
"$ ./mino
mino 0.48.0
mino> (def greet (fn [name] (str \"hello, \" name \"!\")))
#<fn>
mino> (greet \"world\")
\"hello, world!\"
mino> (map greet [\"alice\" \"bob\" \"carol\"])
(\"hello, alice!\" \"hello, bob!\" \"hello, carol!\")
mino> (doc 'map)
\"(map f coll) -- apply f to each element, return a list of results.\""]]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/embedding/"} "Embedding Guide"]
        ": state lifecycle, value ownership, sandboxing, handles, "
        "and threading rules."]
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        ": every public function, type, and enum."]
       [:li [:a {:href "/documentation/language/"} "Language Reference"]
        ": every built-in function, special form, and macro."]
       [:li [:a {:href "/documentation/cookbook/"} "Embedding Cookbook"]
        ": six worked examples for real-world patterns."]]

      [:h2 "License"]
      [:p "mino is released under the "
       [:a {:href "https://opensource.org/licenses/MIT"} "MIT License"]
       ". Use it for anything."])))
