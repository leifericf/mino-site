(ns mino-site.content.tasks
  "Task runner guide page content."
  (:require
    [hiccup2.core :as h]))

(defn tasks-page
  "Generates the Task Runner guide page HTML body."
  []
  (str
    (h/html
      [:h1 "Task Runner"]
      [:p [:code "mino task <name>"] " executes named tasks defined in "
       [:code "mino.edn"] ". Tasks are ordinary mino functions "
       "referenced by qualified symbols. Dependencies between tasks "
       "are resolved automatically."]

      [:h2 "Quick start"]
      [:p "Add a " [:code ":tasks"] " key to your " [:code "mino.edn"] ":"]
      [:pre [:code {:data-lang "mino"}
"{:paths [\"src\" \"lib\"]
 :tasks
 {hello {:doc  \"Say hello\"
         :task myproject.tasks/hello}
  greet {:doc  \"Greet then say hello\"
         :deps [hello]
         :task myproject.tasks/greet}}}"]]
      [:p "Define the task functions in a regular mino source file:"]
      [:pre [:code {:data-lang "mino"}
"(ns myproject.tasks)

(defn hello []
  (println \"Hello!\"))

(defn greet []
  (println \"Welcome to the project.\"))"]]
      [:p "Run a task:"]
      [:pre [:code {:data-lang "bash"}
"mino task greet
# --- hello ---
# Hello!
# --- hello (0.03ms) ---
# --- greet ---
# Welcome to the project.
# --- greet (0.02ms) ---"]]
      [:p "List available tasks:"]
      [:pre [:code {:data-lang "bash"}
"mino task
# Available tasks:
#   hello  Say hello
#   greet  Greet then say hello"]]

      [:h2 "Task definitions"]
      [:p "Each entry in the " [:code ":tasks"] " map is a symbol key "
       "mapped to a spec:"]
      [:pre [:code {:data-lang "mino"}
"task-name {:doc  \"Description shown by 'mino task'\"
           :deps [dep-a dep-b]
           :task some.namespace/function-name}"]]
      [:ul
       [:li [:code ":task"] " (required) " [:raw "&mdash;"]
        " a qualified symbol naming a zero-argument function. The "
        "namespace is loaded automatically via " [:code "require"] "."]
       [:li [:code ":doc"] " (optional) " [:raw "&mdash;"]
        " a short description displayed when listing tasks."]
       [:li [:code ":deps"] " (optional) " [:raw "&mdash;"]
        " a vector of task names that must run before this task. "
        "Dependencies are resolved in topological order; each task "
        "runs at most once even in a diamond dependency graph."]]

      [:h2 "Dependency resolution"]
      [:p "The task runner performs a depth-first topological sort. "
       "It detects and reports:"]
      [:ul
       [:li "Circular dependencies"]
       [:li "References to undefined tasks"]
       [:li "Missing " [:code ":task"] " keys"]]
      [:p "In a diamond dependency (D depends on B and C, both "
       "depend on A), task A runs exactly once."]

      [:h2 "Self-hosting build"]
      [:p "mino itself uses the task runner for its own build. The "
       [:code "mino.edn"] " in the mino repository defines tasks "
       "for compilation, testing, and release:"]
      [:pre [:code {:data-lang "mino"}
"{:paths [\"src\" \"lib\"]
 :tasks
 {gen-core-header {:doc  \"Escape src/core.mino into src/core_mino.h\"
                   :task mino.tasks.builtin/gen-core-header}
  build           {:doc  \"Compile and link the mino binary\"
                   :deps [gen-core-header]
                   :task mino.tasks.builtin/build}
  clean           {:doc  \"Remove build artifacts\"
                   :task mino.tasks.builtin/clean}
  test            {:doc  \"Build and run the test suite\"
                   :deps [build]
                   :task mino.tasks.builtin/test-suite}}}"]]
      [:p "The build task implements incremental compilation with "
       "header dependency tracking (via " [:code "-MMD"]
       " compiler flags), matching the behavior of a traditional "
       "Makefile but written entirely in mino."]

      [:h2 "Bootstrap"]
      [:p "Since " [:code "mino task build"] " requires a mino binary "
       "to run, the first build is a one-line bootstrap:"]
      [:pre [:code {:data-lang "bash"}
"cc -std=c99 -O2 -Isrc -o mino src/*.c main.c -lm"]]
      [:p "After that, " [:code "./mino task build"] " handles all "
       "subsequent builds with incremental compilation. Editing a "
       "header recompiles only the translation units that include it."]

      [:h2 "Writing task functions"]
      [:p "Task functions are ordinary mino functions. They can use "
       "any primitive or library available to the mino runtime. "
       "Some primitives commonly used in tasks:"]
      [:ul
       [:li [:code "sh!"] " " [:raw "&mdash;"]
        " run a shell command, throw on non-zero exit"]
       [:li [:code "file-mtime"] " " [:raw "&mdash;"]
        " file modification time in milliseconds (for incremental builds)"]
       [:li [:code "file-exists?"] " " [:raw "&mdash;"]
        " check whether a file or directory exists"]
       [:li [:code "spit"] " / " [:code "slurp"] " " [:raw "&mdash;"]
        " write and read files"]
       [:li [:code "str-replace"] " " [:raw "&mdash;"]
        " single-pass string replacement"]
       [:li [:code "getenv"] " " [:raw "&mdash;"]
        " read environment variables"]]
      [:p "Tasks print a timing banner automatically. No special "
       "macros or DSL required."]

      [:h2 "Error handling"]
      [:p "If a task function throws an exception, execution stops "
       "immediately. Tasks that already completed are not rolled back "
       "(there is no transaction model). The exception propagates to "
       "the caller with a full stack trace."]
      [:p "The " [:code "mino task"] " CLI validates task names "
       "before evaluation. Only alphanumeric characters, hyphens, and "
       "underscores are accepted."])))
