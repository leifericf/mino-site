(ns mino-site.build
  "Site build orchestrator.

  Defines the Stasis page map and exports to _site/.
  Run via: clj -X:build"
  (:require
    [stasis.core :as stasis]
    [mino-site.render :as render]
    [mino-site.content.landing :as landing]
    [mino-site.content.about :as about]
    [mino-site.content.get-started :as get-started]
    [mino-site.content.download :as download]
    [mino-site.content.documentation :as documentation]
    [mino-site.content.changelog :as changelog]))

(defn pages
  "Returns a Stasis page map: {path -> (fn [ctx] html-string)}.
  Paths use /dir/index.html so GitHub Pages serves them at /dir/.
  mino-root is the path to the mino source tree (submodule or local)."
  [mino-root]
  {"/index.html"
   (fn [ctx]
     (render/html-page {:title nil :active-page :home}
       (landing/landing-page mino-root)))

   "/about/index.html"
   (fn [ctx]
     (render/html-page {:title "About" :active-page :about}
       (about/about-page)))

   "/get-started/index.html"
   (fn [ctx]
     (render/html-page {:title "Get Started" :active-page :get-started}
       (get-started/get-started-page)))

   "/download/index.html"
   (fn [ctx]
     (render/html-page {:title "Download" :active-page :download}
       (download/download-page)))

   "/documentation/index.html"
   (fn [ctx]
     (render/html-page {:title "Documentation" :active-page :documentation}
       (documentation/documentation-page)))

   "/changelog/index.html"
   (fn [ctx]
     (render/html-page {:title "Changelog" :active-page nil}
       (changelog/changelog-page mino-root)))})

(defn build-site!
  "Entry point for clj -X:build.
  Exports all pages to out-dir (default: _site)."
  [& {:keys [mino-root out-dir]
      :or   {mino-root "mino" out-dir "_site"}}]
  (println "Building mino site into" out-dir "...")
  (stasis/empty-directory! out-dir)
  (stasis/export-pages (pages mino-root) out-dir)
  (println "Site built successfully!")
  (println (str "  " (count (pages mino-root)) " pages generated"))
  (println (str "  Open " out-dir "/index.html to preview")))
