(ns mino-site.client.core
  "Entry point for the CLJS bundle. Mounts the search island."
  (:require [mino-site.client.search :as search]))

(defn init []
  (search/init!))
