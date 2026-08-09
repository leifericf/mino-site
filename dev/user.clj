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

(defn- serve-static
  "Serve a file from resources/public/ if it exists."
  [uri]
  (let [path (str "resources/public" uri)
        f (java.io.File. path)]
    (when (.isFile f)
      (let [ct (cond
                 (str/ends-with? uri ".js")   "application/javascript"
                 (str/ends-with? uri ".css")  "text/css"
                 (str/ends-with? uri ".json") "application/json"
                 :else                        "application/octet-stream")]
        {:status  200
         :headers {"Content-Type" ct}
         :body    (slurp f)}))))

(defn app
  "Ring handler that serves pages from the Stasis page map.
  Falls back to static assets in resources/public/."
  [request]
  (let [pages    (build/pages "mino")
        page-key (resolve-uri (:uri request))
        page-val (get pages page-key)]
    (cond
      page-val
      {:status  200
       :headers {"Content-Type" (if (str/ends-with? page-key ".json")
                                  "application/json"
                                  "text/html; charset=utf-8")}
       :body    (if (fn? page-val) (page-val {}) page-val)}

      (serve-static (:uri request))
      (serve-static (:uri request))

      :else
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
