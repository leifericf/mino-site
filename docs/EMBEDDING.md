# Embedding mino

This document specifies the contract between mino and a host program that
embeds it. It covers the state lifecycle, value ownership, GC interaction,
error handling, capability control, and threading rules.

The API is declared in `mino.h` and is **unstable until v1.0.0**.


## State lifecycle

Every mino session begins with a runtime state:

```c
mino_state_t *S = mino_state_new();
```

The state owns the garbage collector, intern tables, module cache, singletons,
and all GC-managed objects allocated within it. Multiple states may coexist in
the same process; they share no mutable data.

Create an environment with all bindings (core + I/O) in one call:

```c
mino_env_t *env = mino_new(S);          /* core + I/O included    */
```

Or step by step for finer control:

```c
mino_env_t *env = mino_env_new(S);      /* empty environment      */
mino_install_core(S, env);              /* pure functions only     */
```

Tear down in reverse order:

```c
mino_env_free(S, env);
mino_state_free(S);
```

`mino_state_free` releases every GC-managed object, every intern table entry,
the module cache, and any outstanding refs. Handle finalizers fire during this
sweep (see Handles below).


## Value ownership

**Borrowed by default.** Values returned by constructors (`mino_int`,
`mino_string`, etc.) and by `mino_eval` / `mino_eval_string` are borrowed.
A borrowed value is valid until the next allocation that may trigger GC.
In practice this means: use it or store it before calling another mino
function that allocates.

**Ref to retain.** To keep a value alive across allocations or GC cycles,
root it with a ref:

```c
mino_ref_t *r = mino_ref(S, val);       /* root val               */
/* ... any number of allocations / evals ... */
mino_val_t *v = mino_deref(r);          /* get the value back     */
mino_unref(S, r);                       /* release the root       */
```

Refs are owned by the state. If the host forgets to unref, the ref (and its
value) are freed when the state is freed. But holding refs longer than needed
prevents the collector from reclaiming objects, so unref promptly.

**Environments are roots.** Any value reachable through a live environment
survives collection. The host does not need to ref values that are bound in
an environment it has not freed.


## Garbage collection

The collector is a non-moving generational tracing collector with an
incremental old-gen mark phase. Short-lived allocations live in a
young-generation nursery; survivors of a minor collection are promoted
to old-gen, which is marked in paced slices between mutator
allocations. A write barrier tracks old-to-young pointers so minor
collections stay proportional to young reachability. The collector
also runs during `mino_state_free`.

### What triggers collection

Any function that allocates a GC-managed object may advance the
collector. This includes all constructors, `mino_eval`,
`mino_eval_string`, `mino_read`, and `mino_load_file`. A minor fires
when young-gen bytes exceed the nursery threshold; a major starts
when old-gen grows past a multiplier above the last major's baseline.

### What survives

An object survives if it is reachable from any root:

- Registered environments (via `mino_env_new` / `mino_new`)
- Host refs (via `mino_ref`)
- The intern tables (symbols and keywords)
- The module cache
- The metadata table
- The try/exception stack (during eval)
- The remembered set (old-gen containers holding young-gen pointers)
- The conservative stack scan (pointers on the C stack)

### Host-driven collection

Trigger collection at quiescent points (between REPL turns, after
bulk import, before long-idle) through `mino_gc_collect`:

```c
mino_gc_collect(S, MINO_GC_MINOR);  /* nursery sweep only        */
mino_gc_collect(S, MINO_GC_MAJOR);  /* drain or run a major cycle */
mino_gc_collect(S, MINO_GC_FULL);   /* minor + full STW major    */
```

### Tuning

Five knobs are exposed via `mino_gc_set_param`. Each setter returns 0
on success and -1 on bad parameter or out-of-range value.

| Parameter | Default | Range |
|-----------|---------|-------|
| `MINO_GC_NURSERY_BYTES` | 1 MiB | 64 KiB .. 256 MiB |
| `MINO_GC_MAJOR_GROWTH_TENTHS` | 15 (1.5x) | 11 .. 40 |
| `MINO_GC_PROMOTION_AGE` | 1 | 1 .. 8 |
| `MINO_GC_INCREMENTAL_BUDGET` | 4096 | 64 .. 65536 |
| `MINO_GC_STEP_ALLOC_BYTES` | 16 KiB | 1 KiB .. 16 MiB |

### Stats

Query counters via `mino_gc_stats`, which fills a plain out-struct
without allocating:

```c
mino_gc_stats_t st;
mino_gc_stats(S, &st);
/* st.collections_minor, st.collections_major, st.bytes_live,
   st.bytes_young, st.bytes_old, st.bytes_alloc, st.bytes_freed,
   st.total_gc_ns, st.max_gc_ns, st.remset_entries, st.phase */
```

### GC stress mode

Set `MINO_GC_STRESS=1` in the environment to force a full collection
on every allocation. This is slow but catches any caller that holds
an unrooted pointer across an allocation boundary. Use it during
development.


## Error handling

Most functions that can fail return `NULL` on error. The error message is
available via `mino_last_error(S)`, which returns a pointer to an internal
buffer valid until the next eval or read call on the same state.

For structured error handling from C, use `mino_pcall`:

```c
mino_val_t *result;
if (mino_pcall(S, fn, args, env, &result) != 0) {
    fprintf(stderr, "error: %s\n", mino_last_error(S));
}
```

From mino code, `try` / `catch` and `throw` provide exception handling.
`mino_pcall` catches any throw.


## Handles

A handle wraps an opaque host pointer and a type tag string:

```c
mino_val_t *h = mino_handle(S, my_ptr, "my-resource");
```

The tag is a static or interned string (not GC-owned). The host retrieves
the pointer and tag with `mino_handle_ptr(h)` and `mino_handle_tag(h)`.

### Finalizers

A handle may carry a finalizer that fires when the handle is collected by
the GC or when the state is freed:

```c
void my_cleanup(void *ptr, const char *tag) {
    fclose((FILE *)ptr);
}

mino_val_t *h = mino_handle_ex(S, fp, "file", my_cleanup);
```

If the handle is rooted (via ref or environment), the finalizer will not fire
until the root is released and the handle becomes unreachable. The finalizer
receives the same pointer and tag that were passed to `mino_handle_ex`.

Finalizers must not call back into the mino API. They run during GC sweep
or state teardown, when the runtime is not in a safe state for re-entry.


## Capability model

`mino_install_core` provides pure functions with no side effects. The host
opts in to I/O by calling `mino_install_io`, which adds `println`, `prn`,
`slurp`, `spit`, and `exit`.

A sandboxed environment starts with `mino_env_new` + `mino_install_core`
and exposes no I/O. The host can register custom primitives with
`mino_register_fn` to provide controlled access to host resources.

```c
mino_env_t *sandbox = mino_env_new(S);
mino_install_core(S, sandbox);
mino_register_fn(S, sandbox, "query", my_query_fn);
```


## Execution limits

The host can cap eval steps and heap usage:

```c
mino_set_limit(S, MINO_LIMIT_STEPS, 100000);  /* max eval steps */
mino_set_limit(S, MINO_LIMIT_HEAP, 8 * 1024 * 1024);  /* 8 MB   */
```

Step limits reset at the start of each `mino_eval` or `mino_eval_string`.
When a limit is exceeded the current eval returns `NULL` and
`mino_last_error` reports the cause. Pass 0 to disable a limit.


## Sessions

Multiple independent evaluation contexts can share a single state by
cloning an environment:

```c
mino_env_t *base = mino_new(S);         /* core + I/O included    */

mino_env_t *session1 = mino_env_clone(S, base);
mino_env_t *session2 = mino_env_clone(S, base);
```

Each clone is a new root environment with copies of all bindings from the
source. Values are shared (not deep-copied), so the clone is cheap. After
cloning, the two environments are independent: defining or redefining a
name in one does not affect the other.

This is the building block for nREPL-style session management. See
`NREPL.md` for the full protocol mapping.


## Interruption

A running eval can be stopped from another thread:

```c
mino_interrupt(S);
```

The eval loop checks the interrupt flag on every step. When set, the
current eval returns NULL and `mino_last_error` reports "interrupted".
The flag is cleared automatically at the start of the next `mino_eval`
or `mino_eval_string` call.

`mino_interrupt` is the only mino API function that is safe to call from
a thread other than the one running eval.


## Modules

The host registers a resolver to map module names to file paths:

```c
const char *my_resolver(const char *name, void *ctx) {
    /* return a file path, or NULL if unknown */
}
mino_set_resolver(S, my_resolver, my_ctx);
```

When mino code calls `(require "name")`, the resolver maps the name to a
path. The file is loaded once; subsequent requires return the cached value.


## Threading rules

**One thread per state at a time.** A `mino_state_t` is not thread-safe.
The host must not call into a state from multiple threads concurrently.
Different states may be used from different threads simultaneously since
they share no mutable data.

Refs, environments, and values belong to their state. Do not pass raw
pointers between states; use `mino_clone` to deep-copy instead.


## Value cloning

To copy a value from one state to another:

```c
mino_val_t *copy = mino_clone(dst, src, val);
```

Clone performs a deep copy. Transferable types: nil, bool, int, float,
string, symbol, keyword, cons, vector, map, set. Non-transferable types
(fn, macro, prim, handle, atom, lazy-seq) cause the clone to return NULL
with an error on the destination state.

Nested collections are cloned recursively. A single non-transferable
element anywhere in the tree fails the entire clone.


## Concurrency primitives

### Atoms and swap!

Atoms are mutable reference cells. `reset!` sets the value directly;
`swap!` applies a function to the current value and stores the result:

```
(def counter (atom 0))
(swap! counter + 1)     ;=> 1
(swap! counter + 10)    ;=> 11
(deref counter)         ;=> 11
```

`swap!` accepts extra arguments that are passed after the current value:
`(swap! a f x y)` is equivalent to `(reset! a (f (deref a) x y))`.

### Dynamic binding

The `binding` special form establishes dynamic bindings that are visible
to all code called within the body, regardless of lexical scope:

```
(def *verbose* false)

(defn log (msg)
  (when *verbose* (println msg)))

(binding (*verbose* true)
  (log "this prints"))     ; *verbose* is true inside binding

(log "this does not")      ; *verbose* is false again
```

Dynamic bindings are per-state and stack correctly with nesting. They
shadow lexical bindings of the same name for the duration of the body.

### Agents

Agents are mutable reference cells updated via function application.
Within a single runtime, updates are synchronous. The API provides a
familiar abstraction that maps to asynchronous patterns in multi-runtime
contexts:

```
(def a (agent 0))
(send-to a + 10)          ;=> 10
(send-to a + 5)           ;=> 15
(deref a)                 ;=> 15
```

### What mino does not provide

mino does not implement shared-memory concurrency primitives (STM, refs,
dosync). Concurrency in mino happens between runtimes via message passing,
not within a single runtime via coordinated mutation. This is a deliberate
design choice: the isolation model is simpler, safer, and maps naturally
to the embedding context where the host controls scheduling.


## REPL handle

The in-process REPL lets the host drive evaluation one line at a time
without managing a read buffer:

```c
mino_repl_t *repl = mino_repl_new(S, env);

const char *line = "(+ 1 2)";
mino_val_t *result;
int rc = mino_repl_feed(repl, line, &result);

switch (rc) {
    case MINO_REPL_OK:    /* result is ready     */ break;
    case MINO_REPL_MORE:  /* need more input      */ break;
    case MINO_REPL_ERROR: /* see mino_last_error  */ break;
}

mino_repl_free(repl);
```


## Inside a primitive

Host-defined primitives receive the active state as their first
parameter:

```c
mino_val_t *my_prim(mino_state_t *S, mino_val_t *args, mino_env_t *env) {
    return mino_int(S, 42);
}
```

Use `S` to call constructors, eval, or any API function that needs a
runtime state.


## Quick reference

### State lifecycle

| Function | Description |
|----------|-------------|
| `mino_state_new()` | Create a new isolated runtime state |
| `mino_state_free(S)` | Free the state and all its resources |

### Environments

| Function | Description |
|----------|-------------|
| `mino_env_new(S)` | Create an empty environment |
| `mino_new(S)` | Create an environment with core + I/O bindings |
| `mino_env_free(S, env)` | Unregister and free an environment |
| `mino_env_clone(S, env)` | Clone an environment (independent copy of bindings) |
| `mino_env_set(S, env, name, val)` | Bind a name |
| `mino_env_get(env, name)` | Look up a name (NULL if unbound) |
| `mino_install_core(S, env)` | Install pure core bindings |
| `mino_install_io(S, env)` | Install I/O bindings |
| `mino_register_fn(S, env, name, fn)` | Bind a C function as a primitive |

### Eval and read

| Function | Description |
|----------|-------------|
| `mino_eval(S, form, env)` | Evaluate one form |
| `mino_eval_string(S, src, env)` | Read and evaluate all forms in a string |
| `mino_load_file(S, path, env)` | Read and evaluate a file |
| `mino_read(S, src, end)` | Read one form from a string |
| `mino_call(S, fn, args, env)` | Call a callable value |
| `mino_pcall(S, fn, args, env, out)` | Protected call (catches errors) |
| `mino_last_error(S)` | Get the last error message |

### Constructors

| Function | Description |
|----------|-------------|
| `mino_nil(S)` | The nil singleton |
| `mino_true(S)` / `mino_false(S)` | Boolean singletons |
| `mino_int(S, n)` | Integer value |
| `mino_float(S, f)` | Float value |
| `mino_string(S, s)` | String (copies `s`) |
| `mino_string_n(S, s, len)` | String with explicit length |
| `mino_symbol(S, s)` | Interned symbol |
| `mino_keyword(S, s)` | Interned keyword |
| `mino_cons(S, car, cdr)` | Cons cell |
| `mino_vector(S, items, len)` | Persistent vector |
| `mino_map(S, keys, vals, len)` | Persistent hash map |
| `mino_set(S, items, len)` | Persistent hash set |
| `mino_prim(S, name, fn)` | Primitive function |
| `mino_handle(S, ptr, tag)` | Opaque host handle |
| `mino_handle_ex(S, ptr, tag, fin)` | Handle with finalizer |
| `mino_atom(S, val)` | Mutable atom |

### Predicates and accessors

| Function | Description |
|----------|-------------|
| `mino_is_nil(v)` | Test for nil |
| `mino_is_truthy(v)` | Truthiness (everything except nil and false) |
| `mino_is_cons(v)` | Test for cons cell |
| `mino_eq(a, b)` | Structural equality |
| `mino_car(v)` / `mino_cdr(v)` | Cons accessors |
| `mino_length(list)` | Length of a list |
| `mino_to_int(v, out)` | Extract integer (returns 1 on success) |
| `mino_to_float(v, out)` | Extract float |
| `mino_to_string(v, out, len)` | Extract string pointer and length |
| `mino_to_bool(v)` | Truthiness as C int |
| `mino_is_handle(v)` | Test for handle |
| `mino_handle_ptr(v)` | Get handle pointer |
| `mino_handle_tag(v)` | Get handle tag |
| `mino_is_atom(v)` | Test for atom |
| `mino_atom_deref(a)` | Read atom value |
| `mino_atom_reset(a, val)` | Set atom value |

### Printer

| Function | Description |
|----------|-------------|
| `mino_print(S, v)` | Print to stdout (no newline) |
| `mino_println(S, v)` | Print to stdout with newline |
| `mino_print_to(S, out, v)` | Print to a FILE* |

### Refs

| Function | Description |
|----------|-------------|
| `mino_ref(S, val)` | Root a value (survives GC) |
| `mino_deref(ref)` | Get the rooted value |
| `mino_unref(S, ref)` | Release the root |

### Modules

| Function | Description |
|----------|-------------|
| `mino_set_resolver(S, fn, ctx)` | Register a module resolver |

### Limits

| Function | Description |
|----------|-------------|
| `mino_set_limit(S, kind, value)` | Set step or heap limit (0 to disable) |
| `mino_interrupt(S)` | Request eval interruption (thread-safe) |

### Cloning

| Function | Description |
|----------|-------------|
| `mino_clone(dst, src, val)` | Deep-copy a value between states |

### REPL

| Function | Description |
|----------|-------------|
| `mino_repl_new(S, env)` | Create a REPL handle |
| `mino_repl_feed(repl, line, out)` | Feed one line of input |
| `mino_repl_free(repl)` | Free the REPL handle |
