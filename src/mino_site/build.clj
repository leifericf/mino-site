(ns mino-site.build
  "Site build orchestrator.

  Defines the Stasis page map and exports to _site/.
  Run via: clj -X:build"
  (:require
    [clojure.java.io :as io]
    [stasis.core :as stasis]
    [mino-site.render :as render]
    [mino-site.parse.header :as parse.header]
    [mino-site.parse.builtins :as parse.builtins]
    [mino-site.parse.cookbook :as parse.cookbook]
    [mino-site.parse.smoke :as parse.smoke]
    [mino-site.content.landing :as landing]
    [mino-site.content.about :as about]
    [mino-site.content.get-started :as get-started]
    [mino-site.content.documentation :as documentation]
    [mino-site.content.changelog :as changelog]
    [mino-site.content.api :as api]
    [mino-site.content.language :as language]
    [mino-site.content.cookbook-page :as cookbook-page]
    [mino-site.content.not-found :as not-found]
    [mino-site.content.tooling :as tooling]
    [mino-site.content.testing :as testing]
    [mino-site.content.embedding :as embedding]
    [mino-site.content.performance :as performance]
    [mino-site.content.from-clojure :as from-clojure]
    [mino-site.parse.use-cases :as parse.use-cases]
    [mino-site.content.use-case-page :as use-case-page]))

(defn pages
  "Returns a Stasis page map: {path -> (fn [ctx] html-string)}.
  Paths use /dir/index.html so GitHub Pages serves them at /dir/.
  mino-root is the path to the mino source tree (submodule or local)."
  [mino-root]
  (let [api-data       (parse.header/parse (str mino-root "/src/mino.h"))
        builtin-data   (parse.builtins/parse (str mino-root "/src/prim.c"))
        cookbook-data   (parse.cookbook/parse mino-root)
        smoke-data     (parse.smoke/parse mino-root)
        use-case-data  (parse.use-cases/parse mino-root)
        use-case-index (into {} (map (juxt :slug identity) use-case-data))
        use-case-pages (into {}
                         (for [slug (use-case-page/use-case-slugs)
                               :let [uc (get use-case-index slug)]
                               :when uc]
                           [(str "/use-cases/" slug "/index.html")
                            (fn [ctx]
                              (render/html-page
                                {:title (use-case-page/use-case-title slug)
                                 :description (:description uc)
                                 :active-page :home
                                 :wide true}
                                (use-case-page/use-case-page uc)))]))]
    (merge use-case-pages
    {"/index.html"
     (fn [ctx]
       (render/html-page {:active-page :home}
         (landing/landing-page mino-root)))

     "/about/index.html"
     (fn [ctx]
       (render/html-page {:title "About"
                          :description "What mino is, why you would embed it, and the design principles behind it."
                          :active-page :about}
         (about/about-page)))

     "/get-started/index.html"
     (fn [ctx]
       (render/html-page {:title "Get Started"
                          :description "Copy the source, compile, and run your first mino program in under a minute."
                          :active-page :get-started}
         (get-started/get-started-page)))

     "/documentation/index.html"
     (fn [ctx]
       (render/html-page {:title "Documentation"
                          :description "C API reference, language reference, and embedding cookbook for mino."
                          :active-page :documentation}
         (documentation/documentation-page)))

     "/documentation/api/index.html"
     (fn [ctx]
       (render/html-page {:title "C API Reference"
                          :description "Every public function, type, enum, and macro in mino.h."
                          :active-page :documentation
                          :wide true}
         (api/api-page api-data)))

     "/documentation/language/index.html"
     (fn [ctx]
       (render/html-page {:title "Language Reference"
                          :description "Built-in functions, special forms, and standard library macros in the mino language."
                          :active-page :documentation
                          :wide true}
         (language/language-page builtin-data smoke-data)))

     "/documentation/cookbook/index.html"
     (fn [ctx]
       (render/html-page {:title "Embedding Cookbook"
                          :description "Six worked examples showing how to embed mino in a C application."
                          :active-page :documentation
                          :wide true}
         (cookbook-page/cookbook-page cookbook-data)))

     "/documentation/tooling/index.html"
     (fn [ctx]
       (render/html-page {:title "Tooling and Editor Integration"
                          :description "nREPL server for mino: connect any editor, evaluate code interactively, build tools with the standard protocol."
                          :active-page :documentation}
         (tooling/tooling-page)))

     "/documentation/testing/index.html"
     (fn [ctx]
       (render/html-page {:title "Testing"
                          :description "Write and run tests in mino using deftest, is, and testing. Built-in test runner with CI-friendly exit codes."
                          :active-page :documentation}
         (testing/testing-page)))

     "/documentation/embedding/index.html"
     (fn [ctx]
       (render/html-page {:title "Embedding Guide"
                          :description "State lifecycle, value ownership, sandboxing, handles, sessions, actors, and threading rules for embedding mino in a host application."
                          :active-page :documentation}
         (embedding/embedding-page)))

     "/documentation/performance/index.html"
     (fn [ctx]
       (render/html-page {:title "Performance"
                          :description "Preliminary performance characteristics: per-operation costs, collection throughput, actor scaling, and where the time goes."
                          :active-page :documentation}
         (performance/performance-page)))

     "/documentation/coming-from-clojure/index.html"
     (fn [ctx]
       (render/html-page {:title "Coming from Clojure"
                          :description "How mino differs from Clojure: syntax, namespaces, concurrency, interop, and what is intentionally absent."
                          :active-page :documentation}
         (from-clojure/from-clojure-page)))

     "/changelog/index.html"
     (fn [ctx]
       (render/html-page {:title "Changelog"
                          :description "Release history and changes for mino."}
         (changelog/changelog-page mino-root)))

     "/404.html"
     (fn [ctx]
       (render/html-page {:title "Page Not Found"
                          :description "This page does not exist."}
         (not-found/not-found-page)))})))


(defn build-site!
  "Entry point for clj -X:build.
  Exports all pages to out-dir (default: _site)."
  [& {:keys [mino-root out-dir]
      :or   {mino-root "mino" out-dir "_site"}}]
  (println "Building mino site into" out-dir "...")
  (stasis/empty-directory! out-dir)
  (stasis/export-pages (pages mino-root) out-dir)
  ;; Copy static assets from resources/public/ into the output directory
  (let [public-dir (io/file "resources/public")]
    (when (.isDirectory public-dir)
      (doseq [f (file-seq public-dir)
              :when (.isFile f)]
        (let [rel (.relativize (.toPath public-dir) (.toPath f))
              dest (io/file out-dir (str rel))]
          (.mkdirs (.getParentFile dest))
          (io/copy f dest)))))
  (println "Site built successfully!")
  (println (str "  " (count (pages mino-root)) " pages generated"))
  (println (str "  Open " out-dir "/index.html to preview")))
