(ns user
  "REPL development setup.
  Start a local dev server with (start!), stop with (stop!)."
  (:require
    [ring.adapter.jetty :as jetty]
    [stasis.core :as stasis]
    [mino-site.build :as build]))

(defonce server (atom nil))

(defn app
  "Ring handler that serves pages from the Stasis page map."
  [request]
  (let [pages (build/pages "mino")]
    (if-let [page-fn (get pages (:uri request))]
      {:status  200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body    (page-fn {})}
      {:status 404
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body "<h1>404</h1>"})))

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
