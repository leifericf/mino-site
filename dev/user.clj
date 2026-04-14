(ns user
  "REPL development setup.
  Start a local dev server with (start!), stop with (stop!)."
  (:require
    [clojure.string :as str]
    [ring.adapter.jetty :as jetty]
    [mino-site.build :as build]))

(defonce server (atom nil))

(defn- resolve-uri
  "Maps clean URLs to page-map keys.
  /          → /index.html
  /about/    → /about/index.html
  /about     → /about/index.html"
  [uri]
  (cond
    (= uri "/")                "/index.html"
    (str/ends-with? uri "/")   (str uri "index.html")
    (str/includes? uri ".")    uri
    :else                      (str uri "/index.html")))

(defn app
  "Ring handler that serves pages from the Stasis page map.
  Resolves clean URLs (e.g. /about/) to /about/index.html paths."
  [request]
  (let [pages    (build/pages "mino")
        page-key (resolve-uri (:uri request))]
    (if-let [page-fn (get pages page-key)]
      {:status  200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body    (page-fn {})}
      {:status 404
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body    "<h1>404</h1>"})))

(defn start!
  "Start the dev server on port 3000."
  ([] (start! 3000))
  ([port]
   (when @server
     (.stop @server))
   (reset! server (jetty/run-jetty #'app {:port port :join? false}))
   (println (str "Dev server running at http://localhost:" port))))

(defn stop!
  "Stop the dev server."
  []
  (when @server
    (.stop @server)
    (reset! server nil)
    (println "Dev server stopped.")))
