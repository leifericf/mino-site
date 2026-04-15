(ns mino-site.content.embedding
  "Embedding Guide page content."
  (:require
    [hiccup2.core :as h]))

(defn embedding-page
  "Generates the Embedding Guide page HTML body."
  []
  (str
    (h/html
      [:h1 "Embedding Guide"]
      [:p "This guide covers the concepts you need to embed mino in a "
       "host application. It picks up where "
       [:a {:href "/get-started/"} "Get Started"] " leaves off and "
       "prepares you for the patterns in the "
       [:a {:href "/documentation/cookbook/"} "Embedding Cookbook"] ". "
       "For the full function-by-function listing, see the "
       [:a {:href "/documentation/api/"} "C API Reference"] "."]

      [:h2 "Runtime state"]
      [:p "Every mino session begins with a runtime state. The state owns "
       "the garbage collector, intern tables, module cache, and every "
       "object allocated within it."]
      [:pre [:code {:data-lang "c"}
"mino_state_t *S = mino_state_new();"]]
      [:p "Multiple states can coexist in the same process. They share "
       "nothing. This is the foundation of mino's isolation model: each "
       "state is a self-contained runtime that can be created, used, and "
       "destroyed independently."]

      [:h3 "Environments"]
      [:p "An environment holds name-to-value bindings. The quickest "
       "start installs everything (core + I/O) in one call:"]
      [:pre [:code {:data-lang "c"}
"mino_env_t *env = mino_new(S);       /* core + I/O included    */"]]
      [:p "Or start with an empty environment for a sandboxed context:"]
      [:pre [:code {:data-lang "c"}
"mino_env_t *env = mino_env_new(S);   /* empty: no bindings     */
mino_install_core(S, env);            /* pure functions only     */"]]
      [:p "Tear down in reverse order when done:"]
      [:pre [:code {:data-lang "c"}
"mino_env_free(S, env);
mino_state_free(S);"]]

      [:h2 "Evaluating code"]
      [:p "The simplest way to run mino code is " [:code "mino_eval_string"]
       ". It reads and evaluates all forms in a string and returns the "
       "value of the last one:"]
      [:pre [:code {:data-lang "c"}
"mino_val_t *result = mino_eval_string(S, \"(+ 1 2)\", env);
/* result is a mino integer with value 3 */"]]
      [:p "If evaluation fails (parse error, runtime error, undefined "
       "name), the return value is NULL. The error message is available "
       "via " [:code "mino_last_error(S)"] ":"]
      [:pre [:code {:data-lang "c"}
"mino_val_t *result = mino_eval_string(S, \"(undefined-fn 1)\", env);
if (result == NULL) {
    fprintf(stderr, \"error: %s\\n\", mino_last_error(S));
}"]]

      [:h3 "Protected calls"]
      [:p "When calling a function that might throw, use "
       [:code "mino_pcall"] " to catch the error without unwinding "
       "your C stack:"]
      [:pre [:code {:data-lang "c"}
"mino_val_t *out;
if (mino_pcall(S, fn, args, env, &out) != 0) {
    fprintf(stderr, \"caught: %s\\n\", mino_last_error(S));
}"]]

      [:h2 "Value ownership"]
      [:p "This is the most important concept for correct embedding. "
       "Every value returned by mino is " [:strong "borrowed"] ": it "
       "survives until the next garbage collection cycle. Allocation "
       "pressure triggers collection, so any mino call that allocates "
       "may invalidate previously returned values. In practice, use a "
       "value or extract its data promptly, and ref anything that must "
       "survive across many mino calls."]
      [:pre [:code {:data-lang "c"}
"mino_val_t *v = mino_int(S, 42);
/* v is valid here */

mino_val_t *w = mino_int(S, 99);
/* v might have been collected -- do not use it */"]]

      [:h3 "Retaining values with refs"]
      [:p "To keep a value alive across multiple mino calls, root it "
       "with a ref:"]
      [:pre [:code {:data-lang "c"}
"mino_ref_t *r = mino_ref(S, val);    /* root val               */

/* ... any number of allocations / evals ... */

mino_val_t *v = mino_deref(r);       /* get the value back     */
mino_unref(S, r);                    /* release the root       */"]]
      [:p "Refs are owned by the state. Forgetting to unref is not a "
       "leak in the traditional sense (the ref is freed when the state "
       "is freed), but holding refs longer than needed prevents the "
       "collector from reclaiming objects."]

      [:h3 "Environments are roots"]
      [:p "Any value bound in a live environment survives collection "
       "automatically. You do not need to ref values that you have "
       "bound with " [:code "mino_env_set"] ":"]
      [:pre [:code {:data-lang "c"}
"mino_env_set(S, env, \"my-val\", mino_int(S, 42));
/* The integer 42 is now rooted through env -- no ref needed */"]]

      [:h2 "Host functions"]
      [:p "Register C functions as mino primitives with "
       [:code "mino_register_fn"] ":"]
      [:pre [:code {:data-lang "c"}
"static mino_val_t *my_greet(mino_state_t *S, mino_val_t *args,
                            mino_env_t *env)
{
    const char *name;
    size_t len;
    (void)env;
    if (!mino_is_cons(args) ||
        !mino_to_string(args->as.cons.car, &name, &len))
        return mino_nil(S);
    char buf[256];
    snprintf(buf, sizeof(buf), \"hello, %s!\", name);
    return mino_string(S, buf);
}

mino_register_fn(S, env, \"greet\", my_greet);"]]
      [:p "The runtime passes the active " [:code "mino_state_t *S"]
       " as the first argument to every primitive callback. Use it "
       "for all value construction and API calls within the function."]

      [:h2 "Handles"]
      [:p "Handles wrap opaque host pointers so mino code can pass them "
       "around without knowing what they contain:"]
      [:pre [:code {:data-lang "c"}
"FILE *fp = fopen(\"data.txt\", \"r\");
mino_val_t *h = mino_handle(S, fp, \"file\");"]]
      [:p "Retrieve the pointer later with " [:code "mino_handle_ptr(h)"]
       " and check the type with " [:code "mino_handle_tag(h)"] "."]

      [:h3 "Finalizers"]
      [:p "Attach a cleanup function that fires when the handle is "
       "collected by the GC or when the state is freed:"]
      [:pre [:code {:data-lang "c"}
"void close_file(void *ptr, const char *tag) {
    fclose((FILE *)ptr);
}

mino_val_t *h = mino_handle_ex(S, fp, \"file\", close_file);"]]
      [:p "Finalizers must not call back into the mino API. They run "
       "during GC sweep when the runtime is not in a safe state for "
       "re-entry."]

      [:h2 "Sandboxing"]
      [:p "A fresh environment created with " [:code "mino_env_new"]
       " has no bindings at all. " [:code "mino_install_core"]
       " adds pure functions (arithmetic, collections, strings) but "
       "no I/O. The host controls exactly what untrusted code can do:"]
      [:pre [:code {:data-lang "c"}
"mino_env_t *sandbox = mino_env_new(S);
mino_install_core(S, sandbox);
/* sandbox has map, filter, reduce, etc. but no println, slurp, or exit */

/* Grant specific capabilities */
mino_register_fn(S, sandbox, \"query\", my_safe_query_fn);"]]

      [:h3 "Execution limits"]
      [:p "Cap eval steps and heap usage to prevent runaway scripts:"]
      [:pre [:code {:data-lang "c"}
"mino_set_limit(S, MINO_LIMIT_STEPS, 100000);
mino_set_limit(S, MINO_LIMIT_HEAP, 8 * 1024 * 1024);  /* 8 MB */"]]
      [:p "When a limit is exceeded, the current eval returns NULL and "
       [:code "mino_last_error"] " reports the cause. Pass 0 to disable "
       "a limit."]

      [:h2 "Modules"]
      [:p "Register a resolver to let mino code load files by name:"]
      [:pre [:code {:data-lang "c"}
"const char *my_resolver(const char *name, void *ctx) {
    static char path[256];
    snprintf(path, sizeof(path), \"scripts/%s.mino\", name);
    return path;
}
mino_set_resolver(S, my_resolver, NULL);"]]
      [:p "When mino code calls " [:code "(require \"utils\")"]
       ", the resolver maps the name to a file path. The file is loaded "
       "once; subsequent requires return the cached value."]

      [:h2 "Sessions"]
      [:p "Multiple independent evaluation contexts can share a single "
       "state by cloning an environment:"]
      [:pre [:code {:data-lang "c"}
"mino_env_t *base = mino_new(S);         /* core + I/O included    */

mino_env_t *session1 = mino_env_clone(S, base);
mino_env_t *session2 = mino_env_clone(S, base);"]]
      [:p "Each clone starts with the same bindings but evolves "
       "independently. Defining a name in one session does not affect "
       "the other. This is the building block for nREPL-style session "
       "management and multi-user environments."]

      [:h2 "Interruption"]
      [:p "Stop a running eval from another thread:"]
      [:pre [:code {:data-lang "c"}
"/* From any thread: */
mino_interrupt(S);"]]
      [:p "The eval loop checks the interrupt flag on every step. The "
       "running eval returns NULL with " [:code "mino_last_error"]
       " reporting \"interrupted\". The flag is cleared at the start "
       "of the next eval call."]
      [:p [:code "mino_interrupt"] " is the only mino API function "
       "safe to call from a different thread."]

      [:h2 "The REPL handle"]
      [:p "The in-process REPL lets a host drive read-eval-print one "
       "line at a time without managing a read buffer:"]
      [:pre [:code {:data-lang "c"}
"mino_repl_t *repl = mino_repl_new(S, env);
mino_val_t *result;

int rc = mino_repl_feed(repl, \"(+ 1 2)\", &result);
switch (rc) {
    case MINO_REPL_OK:    /* result is ready    */ break;
    case MINO_REPL_MORE:  /* need more input    */ break;
    case MINO_REPL_ERROR: /* see mino_last_error */ break;
}

mino_repl_free(repl);"]]
      [:p "This is useful for building interactive consoles, debuggers, "
       "and live inspection tools inside running applications."]

      [:h2 "Threading rules"]
      [:p "A " [:code "mino_state_t"] " is not thread-safe. The host "
       "must not call into a state from multiple threads at the same "
       "time. Different states can be used from different threads "
       "simultaneously since they share nothing."]
      [:p "To move values between states (which may live on different "
       "threads), use " [:code "mino_clone"] " for one-shot copies or "
       "a mailbox for ongoing communication:"]
      [:pre [:code {:data-lang "c"}
"/* One-shot copy */
mino_val_t *copy = mino_clone(dst_state, src_state, val);

/* Ongoing communication via mailbox */
mino_mailbox_t *mb = mino_mailbox_new();
mino_mailbox_send(mb, state_a, val);          /* thread A */
mino_val_t *msg = mino_mailbox_recv(mb, state_b);  /* thread B */
mino_mailbox_free(mb);"]]
      [:p "Only data values (numbers, strings, collections) can cross "
       "state boundaries. Functions, environments, atoms, and handles "
       "are not transferable."]

      [:h2 "Actors"]
      [:p "An actor bundles a state, environment, and mailbox into one "
       "entity. It is the primary abstraction for isolated concurrent "
       "work:"]
      [:pre [:code {:data-lang "c"}
"mino_actor_t *a = mino_actor_new();

/* Send a value from the host state to the actor */
mino_actor_send(a, host_state, mino_int(host_state, 42));

/* Receive and process in the actor's context */
mino_val_t *msg = mino_actor_recv(a);
mino_eval_string(mino_actor_state(a), \"(+ msg 1)\", mino_actor_env(a));

mino_actor_free(a);"]]
      [:p "No background thread is started. The host drives the actor "
       "by sending messages and calling eval. This gives the host "
       "complete control over scheduling: run actors in a thread pool, "
       "in an event loop, or synchronously in sequence."]

      [:h2 "Garbage collection"]
      [:p "The collector is a conservative mark-and-sweep that runs "
       "when cumulative allocation crosses a threshold. Any mino "
       "function that allocates may trigger a sweep, which is why "
       "borrowed values can become invalid after the next call."]
      [:p "Objects survive collection if they are reachable from a root: "
       "registered environments, host refs, the intern tables, the "
       "module cache, or the C stack (via conservative scanning)."]

      [:h3 "GC stress mode"]
      [:p "Set " [:code "MINO_GC_STRESS=1"] " in the environment to "
       "force collection on every allocation. This is slow but catches "
       "any code path that holds an unrooted pointer across an "
       "allocation boundary. Use it during development."]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/cookbook/"} "Embedding Cookbook"]
        ": six worked examples showing patterns for configuration, "
        "rules engines, plugins, data pipelines, and interactive consoles."]
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        ": every public function, type, and enum in " [:code "mino.h"] "."]
       [:li [:a {:href "/documentation/design/"} "Design"]
        ": the philosophy, trade-offs, and boundaries behind the "
        "runtime."]])))
