;; Boot mino with the full capability surface and walk every
;; binding visible to script-side code, emitting each as EDN.
;;
;; Consumed by mino-site's parse/builtins.clj to build the
;; Language Reference page from runtime introspection rather than
;; from regex-scraping C source.

(require '[clojure.repl :as repl])

(let [names (sort (repl/apropos ""))]
  (println "[")
  (doseq [s names]
    (let [v (try (eval s) (catch _ ::unresolved))]
      (when (not= v ::unresolved)
        (let [k (type v)
              d (or (repl/doc-string s) "")]
          (println
            (str "  {:name "  (pr-str (str s))
                 " :kind "    k
                 " :doc "     (pr-str d)
                 "}"))))))
  (println "]"))
