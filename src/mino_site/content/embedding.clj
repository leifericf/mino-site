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

      [:h2 "Vendoring mino"]
      [:p "The canonical drop-in form is the single-file "
       "amalgamation under " [:code "dist/"] ":"]
      [:pre [:code {:data-lang "shell"}
"# In the mino source tree, regenerate the amalgamation:
./mino task amalgamate

# Vendor the two files into your project:
cp mino/dist/mino.c vendor/mino/
cp mino/dist/mino.h vendor/mino/

# Build:
cc -std=c99 -O2 -c vendor/mino/mino.c -o vendor/mino/mino.o
cc app.c vendor/mino/mino.o -lm -lpthread -o app"]]
      [:p "No " [:code "-I"] " paths beyond the vendor directory. "
       "No transitive header dependencies. No build-system "
       "dependency. See the "
       [:a {:href "/documentation/vendored-first/"}
        "Zero dependencies, vendored first"]
       " Internals page for the distribution philosophy."]

      [:h2 "Runtime state"]
      [:p "Every mino session begins with a runtime state. The state owns "
       "the garbage collector, intern tables, module cache, and every "
       "object allocated within it."]
      [:pre [:code {:data-lang "c"}
"mino_state *S = mino_state_new();"]]
      [:p "Multiple states can coexist in the same process. They share "
       "nothing. This is the foundation of mino's isolation model: each "
       "state is a self-contained runtime that can be created, used, and "
       "destroyed independently."]

      [:h3 "Environments"]
      [:p "An environment holds name-to-value bindings. The quickest "
       "start installs the sandbox preset in one call:"]
      [:pre [:code {:data-lang "c"}
"mino_env *env = mino_env_new_default(S);   /* sandbox preset */"]]
      [:p "Or build the env explicitly and pick the capabilities you "
       "want with a bitmask:"]
      [:pre [:code {:data-lang "c"}
"mino_env *env = mino_env_new(S);
mino_install(S, env, MINO_CAP_DEFAULT | MINO_CAP_IO);"]]
      [:p "Tear down in reverse order when done:"]
      [:pre [:code {:data-lang "c"}
"mino_env_free(S, env);
mino_state_free(S);"]]

      [:h3 "Capability-gated install"]
      [:p "Capabilities are addressable as bits. Hand "
       [:code "mino_install"] " a bitmask and it installs exactly the "
       "subset you asked for. The runtime consults the bits at "
       "symbol-resolution time; a name from a disabled capability "
       "raises an MNS002 capability-disabled diagnostic instead of a "
       "bare unbound-symbol error."]
      [:p "Three named presets cover the common shapes one call away:"]
      [:table
       [:thead
        [:tr [:th "Preset"] [:th "Call"] [:th "What you get"]]]
       [:tbody
        [:tr [:td "Minimal"]
             [:td [:code "mino_install_minimal(S, env)"]]
             [:td "Floor only - reader, evaluator, GC, persistent "
                  "collections, numeric ops, foundational macros. No "
                  [:code "core.clj"] " evaluation. Smallest cold "
                  "start."]]
        [:tr [:td "Sandbox"]
             [:td [:code "mino_install_sandbox(S, env)"]]
             [:td "Floor + multimethods, protocols, transducers, "
                  "regex, bignum + the bundled "
                  [:code "clojure.*"] " libs that carry no I/O surface. "
                  "Evaluates " [:code "core.clj"] ". Excludes IO, FS, "
                  "PROC, STM, AGENT, HOST, ASYNC. Equivalent to "
                  [:code "mino_install(S, env, MINO_CAP_DEFAULT)"] "."]]
        [:tr [:td "Everything"]
             [:td [:code "mino_install_all(S, env)"]]
             [:td "Every capability and every bundled stdlib namespace "
                  "the standalone binary ships with. Equivalent to "
                  [:code "mino_install(S, env, MINO_CAP_ALL)"] "."]]]]
      [:p "Pick exactly the bits you want for a custom tier:"]
      [:pre [:code {:data-lang "c"}
"mino_install(S, env, MINO_CAP_DEFAULT | MINO_CAP_IO | MINO_CAP_REGEX);"]]
      [:p "Common bits include: "
       [:code "MINO_CAP_FLOOR"] ", "
       [:code "MINO_CAP_REGEX"] ", "
       [:code "MINO_CAP_BIGNUM"] ", "
       [:code "MINO_CAP_MULTIMETHODS"] ", "
       [:code "MINO_CAP_PROTOCOLS"] ", "
       [:code "MINO_CAP_TRANSDUCERS"] ", "
       [:code "MINO_CAP_IO"] ", "
       [:code "MINO_CAP_FS"] ", "
       [:code "MINO_CAP_PROC"] ", "
       [:code "MINO_CAP_STM"] ", "
       [:code "MINO_CAP_AGENT"] ", "
       [:code "MINO_CAP_HOST"] ", "
       [:code "MINO_CAP_ASYNC"] ", "
       [:code "MINO_CAP_STRING_LIB"] ", "
       [:code "MINO_CAP_SET_LIB"] ", "
       [:code "MINO_CAP_WALK"] ", "
       [:code "MINO_CAP_EDN"] ", "
       [:code "MINO_CAP_PPRINT"] ", "
       [:code "MINO_CAP_ZIP"] ", "
       [:code "MINO_CAP_DATA"] ", "
       [:code "MINO_CAP_TEST"] ", "
       [:code "MINO_CAP_REPL_LIB"] ", "
       [:code "MINO_CAP_DATAFY"] ", "
       [:code "MINO_CAP_INSTANT"] ", "
       [:code "MINO_CAP_SPEC"] ", "
       [:code "MINO_CAP_TOOLING"]
       ", and others. Call " [:code "mino_capability_list()"]
       " for the full registry. " [:code "MINO_CAP_FLOOR"] " is always installed implicitly. "
       [:code "mino_install"] " is idempotent: calling it again with "
       "additional bits adds the missing capabilities and does not "
       "re-evaluate " [:code "core.clj"] "."]
      [:p "Query what is installed at any time:"]
      [:pre [:code {:data-lang "c"}
"uint64_t caps = mino_capabilities(S);
int has_regex = mino_capability_installed(S, MINO_CAP_REGEX);

/* Iterate the registry, printing what is on / off */
for (const mino_capability_info *p = mino_capability_list();
     p->name != NULL; p++) {
    printf(\"  %s: %s\\n\", p->name,
           mino_capability_installed(S, p->bit) ? \"on\" : \"off\");
}"]]
      [:p "At the REPL, "
       [:code ":capabilities"] " (alias " [:code ":caps"]
       ") prints the table interactively. The banner shows "
       [:em "(embedded, N of M capabilities installed)"]
       " whenever a runtime is in a partial-install state."]

      [:h2 "Evaluating code"]
      [:p "The simplest way to run mino code is " [:code "mino_eval_string"]
       ". It reads and evaluates all forms in a string and returns the "
       "value of the last one:"]
      [:pre [:code {:data-lang "c"}
"mino_val *result = mino_eval_string(S, \"(+ 1 2)\", env);
/* result is a mino integer with value 3 */"]]
      [:p "If evaluation fails (parse error, runtime error, undefined "
       "name), the return value is NULL. The error message is available "
       "via " [:code "mino_last_error(S)"] ":"]
      [:pre [:code {:data-lang "c"}
"mino_val *result = mino_eval_string(S, \"(undefined-fn 1)\", env);
if (result == NULL) {
    fprintf(stderr, \"error: %s\\n\", mino_last_error(S));
}"]]

      [:h3 "Protected eval variants"]
      [:p "When calling a function that might throw, use "
       [:code "mino_pcall"] " to catch the error without unwinding "
       "your C stack:"]
      [:pre [:code {:data-lang "c"}
"mino_val *out = NULL;
mino_val *ex  = NULL;
if (mino_pcall(S, fn, args, env, &out, &ex) != 0) {
    /* ex carries the raw value the user passed to (throw ...) */
}"]]
      [:p "Three " [:code "_ex"] " variants extend the same shape to "
       "the eval-family entry points so embedders can distinguish "
       "\"real nil result\" from \"caught throw\" without consulting "
       [:code "mino_last_error"] ":"]
      [:table
       [:thead
        [:tr [:th "Function"] [:th "Same shape as"]
             [:th "Use when"]]]
       [:tbody
        [:tr [:td [:code "mino_eval_ex"]]
             [:td [:code "mino_eval"]]
             [:td "evaluating a parsed form"]]
        [:tr [:td [:code "mino_eval_string_ex"]]
             [:td [:code "mino_eval_string"]]
             [:td "evaluating source text"]]
        [:tr [:td [:code "mino_load_file_ex"]]
             [:td [:code "mino_load_file"]]
             [:td "evaluating a file"]]
        [:tr [:td [:code "mino_pcall"]]
             [:td "(none; original entry)"]
             [:td "calling a known function value with prepared args"]]]]
      [:p "Each " [:code "_ex"] " call returns 0 on success (writing "
       "the result through " [:code "out"] ") or -1 on a caught throw "
       "/ OOM / parse failure. When " [:code "out_ex"] " is non-NULL "
       "on error, it receives the raw payload - useful for handlers "
       "that want to surface the user's " [:code "ex-info"]
       " unchanged. See " [:code "cookbook/error_handling.c"] " for "
       "the canonical real-nil-vs-caught-throw pattern."]

      [:h3 "Structured error access"]
      [:p "After a call returns NULL, inspect the classified "
       "diagnostic without parsing the human-readable message:"]
      [:pre [:code {:data-lang "c"}
"const char *kind = mino_error_kind(S);   /* e.g. \"eval/type\" */
const char *code = mino_error_code(S);   /* e.g. \"MTY001\"   */
const char *msg  = mino_last_error(S);
/* ... handle ... */
mino_clear_error(S);                     /* reset for next call */"]]
      [:p "Kind strings group similar failures (" [:code "eval/type"]
       ", " [:code "eval/arity"] ", " [:code "eval/bounds"] ", "
       [:code "eval/contract"] ", " [:code "name"] ", "
       [:code "syntax"] ", " [:code "io"] "). Codes are stable across "
       "releases so handlers can switch on them without parsing the "
       "message text."]

      [:h2 "Value ownership"]
      [:p "This is the most important concept for correct embedding. "
       "Every value returned by mino is " [:strong "borrowed"] ": it "
       "survives until the next garbage collection cycle. Allocation "
       "pressure triggers collection, so any mino call that allocates "
       "may invalidate previously returned values. In practice, use a "
       "value or extract its data promptly, and ref anything that must "
       "survive across many mino calls."]
      [:pre [:code {:data-lang "c"}
"mino_val *v = mino_int(S, 42);
/* v is valid here */

mino_val *w = mino_int(S, 99);
/* v might have been collected -- do not use it */"]]

      [:h3 "Retaining values with refs"]
      [:p "To keep a value alive across multiple mino calls, root it "
       "with a ref:"]
      [:pre [:code {:data-lang "c"}
"mino_ref *r = mino_ref_new(S, val);    /* root val               */

/* ... any number of allocations / evals ... */

mino_val *v = mino_deref(r);       /* get the value back     */
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

      [:h2 "Building collections from C"]
      [:p "When the host produces a value element-by-element - parsing "
       "incremental input, copying a C array, gathering rows out of a "
       "database row iterator - use a builder. Each builder wraps a "
       "transient and exposes a per-shape add/push/put step plus a "
       [:code "_finish"] " finaliser that returns the persistent "
       "result; the builder itself must not be reused after "
       [:code "_finish"] " runs."]
      [:pre [:code {:data-lang "c"}
"/* Vector: positional accumulation. */
mino_vec_builder *vb = mino_vector_builder_new(S);
for (size_t i = 0; i < n; i++) {
    mino_vector_builder_push(vb, mino_int(S, items[i]));
}
mino_val *v = mino_vector_builder_finish(vb);

/* Map: insertion-ordered key-value pairs. */
mino_map_builder *mb = mino_map_builder_new(S);
mino_map_builder_put(mb, mino_keyword(S, \"a\"), mino_int(S, 1));
mino_map_builder_put(mb, mino_keyword(S, \"b\"), mino_int(S, 2));
mino_val *m = mino_map_builder_finish(mb);

/* Set: deduplicated values. */
mino_set_builder *sb = mino_set_builder_new(S);
mino_set_builder_add(sb, mino_int(S, 1));
mino_set_builder_add(sb, mino_int(S, 1));   /* dropped, already present */
mino_val *s = mino_set_builder_finish(sb);"]]
      [:p "When every element is already sitting in a C array, the "
       "fixed-arity constructors are simpler and shorter: "
       [:code "mino_vector(S, elems, n)"] ", "
       [:code "mino_map(S, keys, vals, n)"] ", "
       [:code "mino_set(S, elems, n)"] ". Builders pay off when the "
       "host is doing the accumulation loop itself, or when it does "
       "not know the length up front."]
      [:p "Builders root their staged values for the GC, so an "
       "allocation inside the loop will not reclaim the partial "
       "result. See " [:code "cookbook/build_collections.c"] " for a "
       "complete worked example."]

      [:h2 "Iterating collections from C"]
      [:p "One iterator type walks every sequential and associative "
       "collection mino exposes: vectors, maps (hashed and sorted), "
       "sets, lists, the empty-list singleton, lazy seqs, and chunked "
       "seqs. The host owns the iterator storage; allocate "
       [:code "mino_iter_sizeof()"] " bytes (typically on the C stack "
       "via " [:code "alloca"] ") and drive it with the lifecycle:"]
      [:pre [:code {:data-lang "c"}
"mino_iter *it = alloca(mino_iter_sizeof());
mino_iter_init(S, it, coll);
mino_val *k, *v;
while (mino_iter_next(it, &k, &v)) {
    /* vectors / sets / lists: k is the element, v is NULL.
     * maps: k is the key, v is the value. */
}
mino_iter_done(it);"]]
      [:p [:code "mino_iter_init"] " pins the collection so a GC fired "
       "mid-walk cannot reclaim the cells the iterator borrows pointers "
       "into. " [:code "mino_iter_done"] " releases that root and must "
       "always be called once, even if the walk exits early. Calling "
       [:code "mino_iter_next"] " after it returned 0 keeps returning "
       "0, and " [:code "mino_iter_done"] " on a NULL iterator is "
       "harmless."]
      [:p "Map iteration follows insertion order for hashed maps and "
       "key order for sorted maps. See "
       [:code "cookbook/iterate.c"] " for a worked example that "
       "covers vectors, maps, lazy seqs, and cons lists through the "
       "same surface."]

      [:h2 "Host functions"]
      [:p "Register C functions as mino primitives with "
       [:code "mino_register_fn"] ":"]
      [:pre [:code {:data-lang "c"}
"static mino_val *my_greet(mino_state *S, mino_val *args,
                            mino_env *env)
{
    const char *name;
    size_t len;
    (void)env;
    if (!mino_is_cons(args) ||
        !mino_to_string(mino_car(args), &name, &len))
        return mino_nil(S);
    char buf[256];
    snprintf(buf, sizeof(buf), \"hello, %s!\", name);
    return mino_string(S, buf);
}

mino_register_fn(S, env, \"greet\", my_greet);"]]
      [:p "The runtime passes the active " [:code "mino_state *S"]
       " as the first argument to every primitive callback. Use it "
       "for all value construction and API calls within the function."]

      [:h2 "Structured host interop"]
      [:p "For richer host integration, mino provides a type-oriented "
       "capability registry. The host registers constructors, methods, "
       "static methods, and getters per type, and mino code calls them "
       "through familiar dot-syntax:"]
      [:pre [:code {:data-lang "c"}
"mino_host_enable(S);

/* Register a Counter type with constructor and methods */
mino_host_register_ctor(S, \"Counter\", 0, counter_new, NULL);
mino_host_register_method(S, \"Counter\", \"inc\", 0, counter_inc, NULL);
mino_host_register_method(S, \"Counter\", \"get\", 0, counter_get, NULL);
mino_host_register_getter(S, \"Counter\", \"value\", counter_value, NULL);"]]
      [:p "mino code can then use dot-syntax:"]
      [:pre [:code {:data-lang "mino"}
"(def c (new Counter))
(.inc c)
(.-value c)  ;=> 1"]]
      [:p "Interop is disabled by default. The host must call "
       [:code "mino_host_enable(S)"] " to activate it. Unregistered "
       "types and methods produce clear error messages. "
       [:code "mino_register_fn"] " remains available for simpler "
       "one-off host functions."]

      [:h2 "Handles"]
      [:p "Handles wrap opaque host pointers so mino code can pass them "
       "around without knowing what they contain:"]
      [:pre [:code {:data-lang "c"}
"FILE *fp = fopen(\"data.txt\", \"r\");
mino_val *h = mino_handle(S, fp, \"file\");"]]
      [:p "Retrieve the pointer later with " [:code "mino_handle_ptr(h)"]
       " and check the type with " [:code "mino_handle_tag(h)"] "."]

      [:h3 "Finalizers"]
      [:p "Attach a cleanup function that fires when the handle is "
       "collected by the GC or when the state is freed:"]
      [:pre [:code {:data-lang "c"}
"void close_file(void *ptr, const char *tag) {
    fclose((FILE *)ptr);
}

mino_val *h = mino_handle_ex(S, fp, \"file\", close_file);"]]
      [:p "Finalizers must not call back into the mino API. They run "
       "during GC sweep when the runtime is not in a safe state for "
       "re-entry."]

      [:h2 "Sandboxing"]
      [:p "A fresh environment created with " [:code "mino_env_new"]
       " has no bindings at all. " [:code "mino_install_sandbox"]
       " adds the canonical Clojure-core surface (map, filter, "
       "reduce, regex, bignum, the safe " [:code "clojure.*"]
       " libs) without granting I/O, processes, host interop, or "
       "shared-state primitives. The host controls exactly what "
       "untrusted code can do:"]
      [:pre [:code {:data-lang "c"}
"mino_env *sandbox = mino_env_new(S);
mino_install_sandbox(S, sandbox);
/* sandbox has map, filter, reduce, etc. but no slurp, spit, sh,
   refs, agents, or host-interop. */

/* Grant specific capabilities */
mino_register_fn(S, sandbox, \"query\", my_safe_query_fn);"]]

      [:h3 "Execution limits"]
      [:p "Cap eval steps and heap usage to prevent runaway scripts:"]
      [:pre [:code {:data-lang "c"}
"mino_set_option(S, MINO_OPT_LIMIT_STEPS, 100000);
mino_set_option(S, MINO_OPT_LIMIT_HEAP, 8 * 1024 * 1024);  /* 8 MB */"]]
      [:p "When a limit is exceeded, the current eval returns NULL and "
       [:code "mino_last_error"] " reports the cause. Pass 0 to disable "
       "a limit."]

      [:h2 "Modules"]
      [:p "Register a resolver to let mino code load files by name:"]
      [:pre [:code {:data-lang "c"}
"const char *my_resolver(const char *name, void *ctx) {
    static char path[256];
    snprintf(path, sizeof(path), \"scripts/%s.clj\", name);
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
"mino_env *base = mino_env_new_default(S);       /* sandbox preset */

mino_env *session1 = mino_env_clone(S, base);
mino_env *session2 = mino_env_clone(S, base);"]]
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
"mino_repl *repl = mino_repl_new(S, env);
mino_val *result;

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
      [:p "A " [:code "mino_state"] " is not thread-safe. The host "
       "must not call into a state from multiple threads at the same "
       "time. Different states can be used from different threads "
       "simultaneously since they share nothing: each state owns its "
       "own GC heap, scheduler, intern tables, module cache, PRNG, and "
       "error reporting buffer. No mutable process-global state lives "
       "inside the runtime."]
      [:p "The one call safe to make from a non-owning thread is "
       [:code "mino_interrupt(S)"] ". It only writes a volatile flag "
       "that the owning thread observes at the next eval step."]

      [:h3 "Running many states across threads"]
      [:p "Host a fleet of isolated runtimes by giving each OS thread "
       "its own state. The orchestrator lives in the host application; "
       "mino itself starts no threads and holds no shared data."]
      [:pre [:code {:data-lang "c"}
"/* One pthread per state. */
void *worker(void *arg) {
    mino_state *S   = mino_state_new();
    mino_env   *env = mino_env_new_default(S);
    mino_load_file(S, \"bot.clj\", env);
    mino_state_free(S);
    return NULL;
}"]]
      [:p "Because the two intern tables and the PRNG live on the "
       "state, two workers running at the same instant never touch a "
       "shared memory location inside the runtime."]

      [:h3 "Cross-state values"]
      [:p "To move values between states (which may live on different "
       "threads), use " [:code "mino_clone"] ":"]
      [:pre [:code {:data-lang "c"}
"mino_val *copy = mino_clone(dst_state, src_state, val);"]]
      [:p "Only data values (numbers, strings, collections) can cross "
       "state boundaries. Functions, environments, atoms, and handles "
       "are not transferable. Clone is safe only when both states are "
       "quiescent on their owning threads: call from the thread that "
       "owns the destination, after synchronising with the thread that "
       "owns the source."]

      [:h3 "Delivering messages from a non-owning thread"]
      [:p "For ongoing cross-thread delivery (a network thread pushing "
       "messages into a mino worker, for example), let the host own a "
       "lock-free or mutex-protected queue per state. The owning "
       "thread drains the queue from inside mino code by calling a "
       "host-registered primitive that translates messages into mino "
       "values and hands them to a channel:"]
      [:pre [:code {:data-lang "c"}
"/* Per-thread pointer: each thread runs exactly one state. */
static _Thread_local host_inbox_t *current_inbox;

static mino_val *prim_inbox_drain(mino_state *S,
                                    mino_val *args,
                                    mino_env *env) {
    host_msg_t m;
    (void)args; (void)env;
    while (host_inbox_try_pop(current_inbox, &m)) {
        mino_val *v = host_msg_to_mino_val(S, &m);
        /* push v onto a channel held on the mino side */
        (void)v;
    }
    return mino_nil(S);
}

mino_register_fn(S, env, \"host-inbox-drain\", prim_inbox_drain);"]]
      [:p "The contract is unchanged: the mino side is touched only on "
       "the owning thread. The host's inbox is a thread-safe handoff "
       "surface sitting outside the runtime."]

      [:h3 "Resolvers must be reentrant"]
      [:p "The CLI binary ships a CWD-relative module resolver that "
       "uses file-scope static buffers. That resolver is not safe for "
       "use from multiple threads. Hosts should register their own "
       "resolver via " [:code "mino_set_resolver"] " and, in a "
       "multi-threaded context, write results into a caller-provided "
       "or per-state buffer rather than a function-scope static."]

      [:h2 "Garbage collection"]
      [:p "The collector is a non-moving generational tracing "
       "collector with an incremental old-gen mark phase. Short-lived "
       "allocations live in a young-generation nursery; values that "
       "survive a minor collection are promoted to old-gen, which is "
       "marked in paced slices between mutator allocations. A write "
       "barrier tracks old-to-young pointers so minor collections "
       "stay proportional to young reachability. Any mino function "
       "that allocates may advance the collector, which is why "
       "borrowed values can become invalid after the next call."]
      [:p "Objects survive collection if they are reachable from a root: "
       "registered environments, host refs, the intern tables, the "
       "module cache, or the C stack (via conservative scanning)."]
      [:p "See the "
       [:a {:href "/documentation/garbage-collection/"} "Garbage Collection"]
       " reference for collector phases, tuning knobs with accepted "
       "ranges, the full "
       [:code "(gc-stats)"] " field list, and the "
       [:code "MINO_GC_*"] " environment variables."]

      [:h2 "Next steps"]
      [:ul
       [:li [:a {:href "/documentation/cookbook/"} "Embedding Cookbook"]
        ": twelve worked examples - start with the five-minute "
        "hello-world and the handle / record / atom decision tree, "
        "then drill into configuration, rules engines, plugins, "
        "data pipelines, and interactive consoles."]
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        ": every public function, type, and enum in " [:code "mino.h"] "."]
       [:li [:a {:href "/about/"} "About"]
        ": design philosophy, trade-offs, and related projects."]])))
