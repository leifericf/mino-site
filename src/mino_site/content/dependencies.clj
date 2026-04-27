(ns mino-site.content.dependencies
  "Dependencies guide page content."
  (:require
    [hiccup2.core :as h]))

(defn dependencies-page
  "Generates the Dependencies guide page HTML body."
  []
  (str
    (h/html
      [:h1 "Dependencies"]
      [:p "mino projects use " [:code "mino.edn"]
       " as the project manifest. It declares source paths and "
       "external dependencies, and the mino binary wires them into "
       "the module resolver automatically."]

      [:h2 "Quick start"]
      [:p "Create a " [:code "mino.edn"] " in your project root:"]
      [:pre [:code {:data-lang "mino"}
"{:paths [\"src\"]

 :deps {:utils {:git \"https://github.com/yourorg/utils.git\"
                :rev \"a1b2c3d\"}}}"]]
      [:p "Fetch dependencies:"]
      [:pre [:code {:data-lang "bash"} "mino deps"]]
      [:p "Use them in your code:"]
      [:pre [:code {:data-lang "mino"}
"(require '[utils.core :as u])
(u/some-function)"]]
      [:p "That is it. When you run " [:code "mino file.clj"]
       " or start the REPL, the resolver automatically finds "
       "modules in your " [:code ":paths"] " directories and "
       "fetched dependencies."]

      [:h2 "Manifest reference"]
      [:h3 ":paths"]
      [:p "A vector of directories where " [:code "require"]
       " searches for your own source files. Defaults to "
       [:code "[\"src\" \"lib\"]"] " if omitted."]
      [:pre [:code {:data-lang "mino"}
"{:paths [\"src\" \"lib\" \"resources\"]}"]]
      [:p "This is useful on its own, even without dependencies. "
       "It tells mino where to find your code."]

      [:h3 ":deps"]
      [:p "A map from dependency name (keyword) to source coordinate. "
       "Two coordinate types are supported:"]

      [:h4 "Path dependencies"]
      [:p "Point directly at a local directory:"]
      [:pre [:code {:data-lang "mino"}
"{:deps {:mylib {:path \"../mylib/src\"}}}"]]
      [:p "The path is used as-is for module resolution. No fetching needed."]

      [:h4 "Git dependencies"]
      [:p "Clone a repository at a pinned revision:"]
      [:pre [:code {:data-lang "mino"}
"{:deps {:utils {:git \"https://github.com/org/utils.git\"
                 :rev \"a1b2c3d4\"}}}"]]
      [:p "The " [:code ":rev"] " is the commit SHA that pins the dependency. "
       "Updating it and running " [:code "mino deps"] " checks out the "
       "new revision."]
      [:p "By default, mino looks for source in the " [:code "src/"]
       " subdirectory of the cloned repo (matching standard project "
       "layouts). Override this with " [:code ":deps/root"] ":"]
      [:pre [:code {:data-lang "mino"}
"{:deps {:odd-lib {:git \"https://example.com/odd-lib.git\"
                   :rev \"abc123\"
                   :deps/root [\"lib\" \"modules\"]}}}"]]

      [:h2 "Using pure libraries from other ecosystems"]
      [:p "Libraries written in pure functional style, without "
       "platform-specific interop, can be used as git dependencies "
       "directly. The module resolver searches " [:code ".cljc"] ", "
       [:code ".clj"] ", and " [:code ".cljs"] " in that order, so "
       "portable Clojure libraries load directly."]
      [:p "Reader conditionals are supported:"]
      [:pre [:code {:data-lang "mino"}
"#?(:mino (mino-specific-code)
   :clj  (jvm-specific-code)
   :default (fallback))"]]
      [:p "This means a library can provide mino-specific code paths "
       "alongside its existing implementations."]
      [:p [:strong "What works:"] " Pure data manipulation, "
       "collection operations, string processing, any code that uses "
       "only functions mino implements from " [:code "clojure.core"] "."]
      [:p [:strong "What does not work:"] " Java/JVM interop "
       "(" [:code ".method"] " calls, " [:code "import"] ", "
       [:code "Class/staticMethod"] "), JVM-specific namespaces "
       "(" [:code "clojure.java.io"] ", " [:code "clojure.java.shell"]
       "), or features mino has not yet implemented."]

      [:h2 "Commands"]
      [:h3 "mino deps"]
      [:p "Reads " [:code "mino.edn"] " and fetches all git dependencies "
       "into " [:code ".mino/deps/"] ". Skips dependencies that are "
       "already cloned. Checks out the pinned revision every time, "
       "so updating " [:code ":rev"] " takes effect on the next run."]
      [:p "Add " [:code ".mino/"] " to your " [:code ".gitignore"]
       ". The fetched sources are a cache, not committed."]

      [:h3 "Auto-wiring"]
      [:p "When " [:code "mino.edn"] " exists in the working directory, "
       "the mino binary automatically adds " [:code ":paths"]
       " and dependency directories to the module resolver. This "
       "applies to " [:code "mino file.clj"] " and the REPL. "
       "No extra flags needed."]
      [:p "Projects without " [:code "mino.edn"] " behave exactly "
       "as before."]

      [:h2 "Workflow examples"]
      [:h3 "Adding a dependency"]
      [:pre [:code {:data-lang "bash"}
"# Add to mino.edn:
#   :deps {:newlib {:git \"https://github.com/org/newlib.git\"
#                   :rev \"abc1234\"}}
mino deps
# Now (require '[newlib.core :as n]) works"]]

      [:h3 "Updating a dependency"]
      [:pre [:code {:data-lang "bash"}
"# Change :rev in mino.edn to the new commit SHA
mino deps
# The new code is checked out"]]

      [:h3 "Re-fetching everything"]
      [:pre [:code {:data-lang "bash"}
"rm -rf .mino/deps
mino deps"]]

      [:h2 "Design notes"]
      [:ul
       [:li [:strong "The manifest is the lock."]
        " All coordinates are exact (pinned revisions, direct paths). "
        "There is no lockfile because there is nothing to resolve."]
       [:li [:strong "No version ranges."]
        " No constraint solver, no transitive dependencies, no registry. "
        "Dependencies are direct source coordinates."]
       [:li [:strong "Unknown keys are ignored."]
        " The manifest already uses " [:code ":tasks"]
        " for the task runner; future versions can add "
        [:code ":main"] " or other keys without breaking existing "
        "projects."]
       [:li [:strong "Standalone mode only."]
        " When mino is embedded in a host application, the host "
        "controls the module resolver and which primitives are "
        "available. The deps system is a feature of the standalone "
        "binary, not the embeddable library."]]

      [:h2 "Troubleshooting"]
      [:h3 "git: command not found"]
      [:p "mino shells out to " [:code "git"] " for cloning. "
       "Install git and ensure it is on your PATH."]
      [:h3 "repository not found"]
      [:p "Check the " [:code ":git"] " URL. For private repos, "
       "ensure your SSH keys or credentials are configured."]
      [:h3 "rev not found"]
      [:p "The " [:code ":rev"] " must be a valid commit SHA or "
       "ref (branch name, tag) in the repository. Run "
       [:code "git log"] " in the upstream repo to find the right "
       "value."]
      [:h3 "require cannot resolve module"]
      [:p "Check that the library's source is in " [:code "src/"]
       " within its repository. If not, add "
       [:code ":deps/root [\"lib\"]"] " (or the correct path) "
       "to the dependency spec."])))
