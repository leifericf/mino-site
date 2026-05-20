(ns mino-site.content.vendored-first
  "Internals page: mino's vendored-first, zero-dependency distribution
  philosophy. Borrowed Odin / SQLite / sokol / stb spirit; documents
  the contract embedders see when they integrate mino."
  (:require
    [hiccup2.core :as h]))

(defn vendored-first-page
  "Generates the Zero dependencies, vendored first internals page."
  []
  (str
    (h/html
      [:h1 "Zero dependencies, vendored first"]

      [:p.banner
       "mino's distribution philosophy: drop the runtime into your "
       "project, own it, no package manager. A C99 compiler plus "
       "libm plus pthreads is the entire build environment; the "
       "bundled Clojure stdlib (clojure.string, clojure.set, "
       "clojure.math, …) lives in-binary as generated C string "
       "literals, so there is no on-disk " [:code "lib/"] " path "
       "required at runtime."]

      [:h2 {:id "the-promise"} "The promise"]
      [:p "mino has no transitive build dependencies. The runtime "
       "compiles against the C99 standard library only; the "
       "platform layer needs " [:code "libm"] " (math) and "
       [:code "pthreads"] " (concurrency). There is no autotools, "
       "no cmake, no meson, no bazel. There is no package manager "
       "to invoke, no lockfile to commit, no fetch step in the "
       "build."]
      [:p "A version of mino in your vendor tree is bit-identical to "
       "the one tagged in this repo, frozen until you re-vendor it. "
       "If you ship a binary today and rebuild from the same "
       "vendor tree six months from now, you get the same runtime "
       "byte-for-byte (subject to the host compiler's own "
       "determinism)."]

      [:h2 {:id "why"} "Why"]
      [:p "Borrowed spirit from "
       [:a {:href "https://www.sqlite.org/amalgamation.html"} "SQLite"] ", "
       [:a {:href "https://odin-lang.org"} "Odin"] ", "
       [:a {:href "https://github.com/floooh/sokol"} "sokol"] ", and "
       [:a {:href "https://github.com/nothings/stb"} "stb"] ": drop "
       "the code into your project, own it, no package-manager "
       "surprise. The vendor copy is the source of truth for that "
       "build; it does not float, does not auto-update, does not "
       "phone home."]
      [:p "Embedded scripting hosts make a load-bearing trade-off at "
       "integration time: the cost of evaluating, integrating, and "
       "shipping a runtime versus the value the runtime adds. A "
       "single-file drop-in collapses the first cost to "
       "approximately zero. A build-system dependency, a header "
       "include chain, a runtime DLL chain, a locale-or-encoding "
       "negotiation – every one of these moves the integration "
       "cost upward, often enough to defeat the case for "
       "embedding."]

      [:h2 {:id "the-vendored-corner"} "The vendored corner"]
      [:p [:code "src/vendor/imath/"] " is the only external code "
       "in the runtime tree. " [:code "imath"] " is a small bignum "
       "library; mino needs arbitrary-precision integers, mino "
       "vendored " [:code "imath"] ", mino owns the audit surface. "
       "There is no plan to add more vendored libraries: every "
       "new Clojure-side namespace ships as bundled mino source "
       "in " [:code "lib/clojure/"] " (escaped into a C string "
       "literal at build time), and every new C primitive is "
       "written in mino's own style."]

      [:h2 {:id "the-amalgamation"} "The amalgamation"]
      [:p [:code "dist/mino.c"] " and " [:code "dist/mino.h"] " are "
       "produced by " [:code "./mino task amalgamate"] " and shipped "
       "as release assets alongside the binaries. The amalgamation "
       "is a single translation unit: every " [:code ".c"] " file "
       "in the runtime, with project-local " [:code "#include"] " "
       "directives pre-expanded inline, and a single bit-identical "
       "copy of " [:code "src/mino.h"] " for the public header."]
      [:pre [:code
        "# Vendor mino into your project:\n"
        "cp /path/to/mino/dist/mino.c vendor/mino/\n"
        "cp /path/to/mino/dist/mino.h vendor/mino/\n"
        "\n"
        "# Build:\n"
        "cc -std=c99 -O2 -c vendor/mino/mino.c -o vendor/mino/mino.o\n"
        "cc app.c vendor/mino/mino.o -lm -lpthread -o app\n"]]
      [:p "No " [:code "-I"] " paths beyond the vendor directory. "
       "No build-system dependency. No transitive header chain. "
       "The amalgamation is reproducible bit-for-bit from any "
       "commit by re-running the task."]

      [:h2 {:id "what-you-dont-have-to-think-about"} "What you don't have to think about"]
      [:ul
       [:li "No build-system dependency. Use whatever your project "
        "already uses (a one-line " [:code "Makefile"] " rule, a "
        [:code "Cargo.toml [build-dependencies]"] " "
        [:code "cc"] " block, an MSBuild step, anything that "
        "compiles " [:code "*.c"] " files)."]
       [:li "No transitive header dependency. " [:code "mino.h"] " "
        "includes only " [:code "<stddef.h>"] ", " [:code "<stdint.h>"] ", "
        "and " [:code "<stdio.h>"] " from the C standard library."]
       [:li "No runtime DLL chain. The amalgamation is statically "
        "linkable; the binary you ship has no shared-library "
        "runtime dependencies beyond " [:code "libc"] ", "
        [:code "libm"] ", and " [:code "libpthread"] "."]
       [:li "No locale / encoding negotiation. mino values are "
        "UTF-8 byte sequences; the runtime never reads "
        [:code "LANG"] " or " [:code "LC_*"] " environment "
        "variables."]
       [:li "No threading-library selection by the build. The "
        "platform layer is " [:code "pthreads"] " on POSIX, "
        "Win32 on Windows, selected at compile time by the "
        "host triple."]]

      [:h2 {:id "where-mino-diverges"} "Where mino diverges from peers"]
      [:p "SQLite is the spiritual cousin for distribution shape: "
       "one " [:code ".c"] " file, one " [:code ".h"] " file, "
       "compile, link, ship. Odin is the cousin for the "
       [:code "vendor:"] " mindset: every external dependency "
       "lives in the project's own tree, version-pinned by the "
       "act of copying it there."]
      [:p "Where mino picks differently:"]
      [:ul
       [:li "The bundled Clojure stdlib moves in lockstep with "
        "the runtime. Odin's " [:code "vendor:"] " packages can "
        "float independently; mino's bundled namespaces ride "
        "with the runtime tag."]
       [:li "mino does not ship a package manager. (Odin doesn't "
        "either; this is where the two are identical.) "
        [:code "mino deps"] " resolves git-fetched dependencies "
        "for projects that want them, but the runtime itself "
        "needs none."]
       [:li "The C API is the contract, not the CLI. Lua is the "
        "cousin for \"I am an embedded scripting engine first.\" "
        "An embedder treats " [:code "mino"] " the binary as a "
        "diagnostic tool — useful for trying things out, never "
        "in the production critical path."]]

      [:h2 {:id "pointers"} "Pointers"]
      [:ul
       [:li [:a {:href "/documentation/embedding/"} "Embedding mino in your C project"]
        " — the canonical first-five-minutes integration."]
       [:li [:a {:href "/documentation/api/"} "C API Reference"]
        " — every public function, type, and macro in "
        [:code "mino.h"] "."]
       [:li [:a {:href "/documentation/dependencies/"} "Dependencies"]
        " — how the bundled stdlib, on-disk " [:code "lib/"] ", "
        "and git-fetched repos compose via " [:code "mino deps"] "."]
       [:li [:a {:href "https://github.com/leiffredheim/mino/releases"} "Release assets"]
        " — pre-built binaries plus the "
        [:code "mino-amalgamation-vX.Y.Z.tar.gz"] " bundle for "
        "each tag."]])))
