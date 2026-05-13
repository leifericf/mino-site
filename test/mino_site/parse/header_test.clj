(ns mino-site.parse.header-test
  "Snapshot, coverage-floor, and strict-mode tests for the mino.h parser.

  - Snapshot: the parsed :sections value is pinned to
    test/resources/api-snapshot.edn. Real changes are reviewed by
    pretty-printing the diff and committing an updated snapshot.
  - Coverage floor: the count of public symbols per :kind may only
    grow. Drops trip the test and surface as `:by-kind` deltas.
  - Strict mode: with the current mino.h, parsing must produce zero
    warnings. New :unparseable-* hits are real defects, not noise."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [mino-site.parse.header :as p]))

(def header-path "mino/src/mino.h")
(def snapshot-path "test/resources/api-snapshot.edn")
(def floor-path    "test/resources/api-coverage-floor.edn")

(defn- parsed []
  (p/parse header-path))

(deftest strict-mode-clean
  (testing "parsing mino.h produces no warnings"
    (let [{:keys [warnings]} (parsed)]
      (is (empty? warnings)
          (str "Header parser produced " (count warnings)
               " warning(s). First: " (first warnings))))))

(deftest coverage-floor
  (testing "every declaration kind meets or exceeds the recorded floor"
    (let [floor (edn/read-string (slurp floor-path))
          {:keys [sections]} (parsed)
          decls    (mapcat :declarations sections)
          by-kind  (frequencies (map :kind decls))]
      (is (>= (count sections) (:sections floor))
          (str "Section count dropped from " (:sections floor)
               " to " (count sections)))
      (is (>= (count decls) (:total floor))
          (str "Total declarations dropped from " (:total floor)
               " to " (count decls)))
      (doseq [[k expected] (:by-kind floor)
              :let [actual (get by-kind k 0)]]
        (is (>= actual expected)
            (str ":" (name k) " count dropped from "
                 expected " to " actual))))))

(deftest snapshot-matches
  (testing "parsed :sections matches the committed snapshot"
    (let [{:keys [sections]} (parsed)
          snapshot (edn/read-string (slurp snapshot-path))]
      (is (= snapshot sections)
          (str "Parser output diverged from " snapshot-path
               ". Review the diff; if intentional, regenerate via:\n"
               "  clojure -M -e '(require '\\''[mino-site.parse.header :as p])"
               " (spit \"" snapshot-path "\""
               " (with-out-str (clojure.pprint/pprint"
               " (:sections (p/parse \"" header-path "\")))))'\n"
               "and commit alongside the change.")))))

(deftest strict-mode-throws-on-injection
  (testing "strict? true escalates a malformed unstable tag into an exception"
    (let [tmp (io/file (System/getProperty "java.io.tmpdir")
                       "header_test_injection.h")
          dashes (apply str (repeat 73 \-))]
      (spit tmp (str "/* " dashes " */\n"
                     "/* Demo [MINO_UNSTABLE_lower] */\n"
                     "/* " dashes " */\n\n"
                     "void mino_demo(void);\n"))
      (is (thrown? clojure.lang.ExceptionInfo
                   (p/parse (.getPath tmp) {:strict? true})))
      (.delete tmp))))
