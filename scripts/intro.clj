;; Boot mino, walk every binding visible after mino_install_core
;; and mino_install_io, and emit each as EDN.
;;
;; Consumed by mino-site's parse/builtins.clj to build the
;; Language Reference page from runtime introspection rather than
;; from regex-scraping C source.

(let [names (sort (apropos ""))]
  (println "[")
  (doseq [s names]
    (let [v (try (eval s) (catch _ ::unresolved))]
      (when (not= v ::unresolved)
        (let [k (type v)
              d (doc s)]
          (println
            (str "  {:name "  (pr-str (str s))
                 " :kind "    k
                 " :doc "     (pr-str d)
                 "}"))))))
  (println "]"))
