(ns mino-site.build
  "Site build orchestrator.

  Defines the Stasis page map and exports to _site/.
  Run via: clj -X:build"
  (:require
    [stasis.core :as stasis]
    [mino-site.render :as render]
    [mino-site.content.landing :as landing]))

(defn pages
  "Returns a Stasis page map: {path → (fn [ctx] html-string)}.
  mino-root is the path to the mino source tree (submodule or local)."
  [mino-root]
  {"/" (fn [ctx]
         (render/html-page {:title nil :active-page :home}
           (landing/landing-page mino-root)))})

(defn build-site!
  "Entry point for clj -X:build.
  Exports all pages to out-dir (default: _site)."
  [& {:keys [mino-root out-dir]
      :or   {mino-root "mino" out-dir "_site"}}]
  (println "Building mino site into" out-dir "...")
  (stasis/empty-directory! out-dir)
  (stasis/export-pages (pages mino-root) out-dir)
  (println "Site built successfully!")
  (println (str "  Open " out-dir "/index.html to preview")))
