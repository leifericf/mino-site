(ns mino-site.content.about
  "About page content.

  Kept as a thin index after the homepage absorbed the 'what is mino'
  pillars. Existing inbound links to /about/ still resolve and route
  readers onward to the homepage and Documentation hub."
  (:require
    [hiccup2.core :as h]))

(defn about-page
  "Generates the About page HTML body."
  []
  (str
    (h/html
      [:h1 "About mino"]

      [:p "mino is an embeddable Clojure-inspired Lisp runtime "
       "implemented in C. The host application links the library, "
       "creates one or more runtimes, installs capabilities, and "
       "evaluates user code through a compact C API. The language "
       "centers on immutable values and persistent collections; the "
       "embedding API is the product."]

      [:p "mino aims to close remaining canon gaps over time. "
       "Cooperative async channels work without OS threads; "
       "the host can grant threads per state when needed."]

      [:h2 "Where to go next"]
      [:ul
       [:li [:a {:href "/"} "Homepage"]
        " - the four pillars and the three-roles walkthrough."]
       [:li [:a {:href "/get-started/"} "Get Started"]
        " - install, build, and run your first script in under a minute."]
       [:li [:a {:href "/documentation/"} "Documentation"]
        " - C API reference, language reference, embedding cookbook, "
        "and the Clojure compatibility matrix."]
       [:li [:a {:href "/documentation/coming-from-clojure/"}
             "Coming from Clojure"]
        " - what's the same, what differs, and what's missing."]
       [:li [:a {:href "/changelog/"} "Changelog"]
        " - release history."]]

      [:h2 "Inspirations"]
      [:p "mino draws from four projects that each got something "
       "right: "
       [:strong "Clojure"] " for immutable values, persistent data "
       "structures, and a data-first programming model; "
       [:strong "Lua"] " for proving a small, portable C "
       "implementation can be a fast embeddable scripting "
       "language; "
       [:strong "Fennel"] " for showing Lisp syntax and macros can "
       "layer cleanly over a minimal runtime; and "
       [:strong "Erlang/BEAM"] " for the discipline of isolated "
       "processes communicating by message passing. mino combines "
       "these lineages into an embeddable runtime with persistent "
       "data and capability-gated host interop."]
      [:p "mino borrows from BEAM twice. The first borrowing is "
       "isolation: each "
       [:code "mino_state"] " owns its heap and communicates with "
       "peers via cross-state channels and values, never shared "
       "mutable references. The second is "
       [:a {:href "https://www.erlang.org/doc/system/bit_syntax.html"}
        "Erlang's bit syntax"]
       " - binary data as a first-class type with bit-precise "
       "field access. mino's "
       [:a {:href "/documentation/bytes/"}
        [:code "MINO_BYTES"]]
       " value and the "
       [:code "bits"] " / " [:code "bits-get"] " / "
       [:code "let-bits"]
       " surface bring the same shape to embedded scripts, where "
       "binary protocols and sensor packets are common."]

      [:h2 "Related projects"]
      [:ul
       [:li [:a {:href "https://jank-lang.org" :target "_blank"
                 :rel "noopener"} "jank"]
        " - native Clojure dialect with an LLVM-based compiler."]
       [:li [:a {:href "https://babashka.org" :target "_blank"
                 :rel "noopener"} "Babashka"]
        " - fast-starting Clojure scripting runtime on GraalVM."]
       [:li [:a {:href "https://www.lua.org" :target "_blank"
                 :rel "noopener"} "Lua"]
        " - embeddable scripting language in C with a mutable data model."]
       [:li [:a {:href "https://fennel-lang.org" :target "_blank"
                 :rel "noopener"} "Fennel"]
        " - Lisp that compiles to Lua."]
       [:li [:a {:href "https://janet-lang.org" :target "_blank"
                 :rel "noopener"} "Janet"]
        " - embeddable Lisp in C with a register-based VM and package manager."]
       [:li [:a {:href "https://github.com/Zelex/jo_clojure" :target "_blank"
                 :rel "noopener"} "JO Clojure"]
        " - native C/C++ Clojure-like runtime with persistent data structures."]
       [:li [:a {:href "https://github.com/rekola/nanoclj" :target "_blank"
                 :rel "noopener"} "nanoclj"]
        " - tiny Clojure interpreter in C."]
       [:li [:a {:href "https://github.com/mll/clojure-rt" :target "_blank"
                 :rel "noopener"} "clojure-rt"]
        " - LLVM-based Clojure implementation for deterministic performance."]])))
