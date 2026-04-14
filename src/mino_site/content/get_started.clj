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
      [:p "mino is two files: " [:code "mino.h"] " and " [:code "mino.c"]
       ". Copy them into your project, or clone the repository:"]
      [:pre [:code "git clone https://github.com/leifericf/mino.git"]]

      [:h2 "2. Compile"]
      [:p "Build the standalone REPL:"]
      [:pre [:code "cd mino\nmake"]]
      [:p "Or compile mino directly into your own program:"]
      [:pre [:code "cc -std=c99 -o myapp myapp.c mino.c"]]
      [:p "No build system, package manager, or external dependencies required."]

      [:h2 "3. Embed in your C program"]
      [:p "A minimal embedding creates a runtime, registers a host function, "
       "evaluates mino code, and extracts the result:"]
      [:pre
       [:code {:data-lang "c"}
"#include \"mino.h\"
#include <stdio.h>

/* A host function exposed to mino as (add-tax amount). */
static mino_val_t *host_add_tax(mino_val_t *args, mino_env_t *env)
{
    long long amount;
    (void)env;
    if (!mino_is_cons(args) || !mino_to_int(args->as.cons.car, &amount))
        return mino_nil();
    return mino_float((double)amount * 1.08);
}

int main(void)
{
    mino_env_t *env = mino_new();          /* env + core bindings */
    mino_register_fn(env, \"add-tax\", host_add_tax);

    mino_val_t *result = mino_eval_string(
        \"(def prices [100 200 300])\\n\"
        \"(reduce + (map add-tax prices))\\n\",
        env);

    if (result) {
        double total;
        if (mino_to_float(result, &total))
            printf(\"total with tax: %.2f\\n\", total);
    }

    mino_env_free(env);
    return 0;
}"]]
      [:p "Key points:"]
      [:ul
       [:li [:code "mino_new()"] " allocates an environment and installs "
        "core bindings in one call."]
       [:li [:code "mino_register_fn()"] " exposes a C function to mino code "
        "under any name."]
       [:li [:code "mino_eval_string()"] " reads and evaluates all forms, "
        "returning the last result."]
       [:li [:code "mino_to_float()"] " safely extracts a C value from the "
        "result (returns 0 on type mismatch)."]
       [:li [:code "mino_env_free()"] " unregisters the environment; the "
        "garbage collector reclaims memory."]]

      [:h2 "4. Try the REPL"]
      [:p "The standalone REPL is useful for exploring the language interactively:"]
      [:pre
       [:code {:data-lang "mino"}
"$ ./mino
mino> (def greet (fn (name) (str \"hello, \" name \"!\")))
#<fn>
mino> (greet \"world\")
\"hello, world!\"
mino> (map greet [\"alice\" \"bob\" \"carol\"])
(\"hello, alice!\" \"hello, bob!\" \"hello, carol!\")
mino> (doc 'map)
\"(map f coll) -- apply f to each element, return a list of results.\""]]

      [:h2 "5. Next steps"]
      [:ul
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        ": every public function, type, and enum."]
       [:li [:a {:href "/documentation/language/"} "Language Reference"]
        ": every built-in function, special form, and macro."]
       [:li [:a {:href "/documentation/cookbook/"} "Embedding Cookbook"]
        ": six worked examples for real-world patterns."]])))
