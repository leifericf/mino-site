(ns mino-site.parse.smoke
  "Parse mino test files for usage examples.

  Extracts (is (= expected actual)) assertions from *_test.mino files
  as raw text, preserving mino-specific syntax like backtick, @, and ~."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

;; --- Text-level form extraction ---
;; Uses raw character scanning instead of Clojure's reader to avoid
;; mangling mino syntax that Clojure interprets differently (backtick
;; expands to clojure.core/seq+concat, @ becomes clojure.core/deref,
;; symbols inside syntax-quote get gensym suffixes).

(def ^:private close-for
  "Maps opening delimiters to their closing counterparts."
  {\( \) \[ \] \{ \}})

(defn- skip-ws
  "Advance past whitespace, commas, and ;-comments."
  ^long [^String text ^long i]
  (let [len (.length text)]
    (loop [j i]
      (if (>= j len) j
        (let [c (.charAt text j)]
          (cond
            (or (Character/isWhitespace c) (= c \,)) (recur (inc j))
            (= c \;) (let [nl (.indexOf text "\n" (int j))]
                        (if (neg? nl) len (recur (inc nl))))
            :else j))))))

(defn- matching-close
  "Index of the delimiter that closes the opener at `start`, or -1.
  Tracks depth of the specific open/close pair, skipping string
  literals and ;-comments. Correct for well-formed source where
  different bracket types are properly nested."
  ^long [^String text ^long start]
  (let [len (.length text)
        open (.charAt text start)
        close (close-for open)]
    (loop [i (inc start), depth (int 1), in-str false, esc false]
      (if (>= i len) -1
        (let [c (.charAt text i)]
          (cond
            esc                     (recur (inc i) depth in-str false)
            (and in-str (= c \\))  (recur (inc i) depth true true)
            in-str                  (if (= c \")
                                     (recur (inc i) depth false false)
                                     (recur (inc i) depth true false))
            (= c \")               (recur (inc i) depth true false)
            (= c \;)               (let [nl (.indexOf text "\n" (int i))]
                                     (if (neg? nl) -1
                                       (recur (inc nl) depth false false)))
            (= c open)             (recur (inc i) (inc depth) false false)
            (= c close)            (if (= depth 1) i
                                     (recur (inc i) (dec depth) false false))
            :else                   (recur (inc i) depth false false)))))))

(defn- extract-string
  "Extract a string literal starting at the opening quote at position i.
  Returns [string-text end-pos] or nil."
  [^String text ^long i]
  (let [len (.length text)]
    (loop [j (inc i), esc false]
      (when (< j len)
        (let [c (.charAt text j)]
          (cond
            esc      (recur (inc j) false)
            (= c \\) (recur (inc j) true)
            (= c \") [(subs text i (inc j)) (inc j)]
            :else    (recur (inc j) false)))))))

(defn- extract-atom
  "Extract an atom (symbol, keyword, number) starting at position i.
  Scans until a delimiter, whitespace, or end of input."
  [^String text ^long i]
  (let [len (.length text)
        end (loop [j i]
              (if (or (>= j len)
                      (let [c (.charAt text j)]
                        (or (Character/isWhitespace c)
                            (contains? #{\( \) \[ \] \{ \} \; \, \"} c))))
                j (recur (inc j))))]
    (when (> end i)
      [(subs text i end) end])))

(defn- extract-form
  "Extract the raw text of the next form at or after position i.
  Returns [form-text end-pos] or nil."
  [^String text ^long start]
  (let [len (.length text)
        i (skip-ws text start)]
    (when (< i len)
      (let [c (.charAt text i)]
        (cond
          ;; Balanced delimiters
          (close-for c)
          (let [end (matching-close text i)]
            (when (>= end 0)
              [(subs text i (inc end)) (inc end)]))

          ;; String literal
          (= c \")
          (extract-string text i)

          ;; Single-form prefix reader macros
          (#{\' \` \@} c)
          (when-let [[form end] (extract-form text (inc i))]
            [(str c form) end])

          ;; Unquote / unquote-splicing
          (= c \~)
          (if (and (< (inc i) len) (= \@ (.charAt text (inc i))))
            (when-let [[form end] (extract-form text (+ i 2))]
              [(str "~@" form) end])
            (when-let [[form end] (extract-form text (inc i))]
              [(str "~" form) end]))

          ;; Metadata: ^ consumes metadata-form then target-form
          (= c \^)
          (when-let [[meta after-meta] (extract-form text (inc i))]
            (when-let [[target end] (extract-form text after-meta)]
              [(str "^" meta " " target) end]))

          ;; # dispatch
          (= c \#)
          (when (< (inc i) len)
            (let [nc (.charAt text (inc i))]
              (cond
                ;; Set #{...} or fn #(...)
                (#{\{ \(} nc)
                (let [end (matching-close text (inc i))]
                  (when (>= end 0)
                    [(subs text i (inc end)) (inc end)]))
                ;; Discard #_
                (= nc \_)
                (when-let [[form end] (extract-form text (+ i 2))]
                  [(str "#_" form) end])
                ;; Regex #"..."
                (= nc \")
                (extract-string text (inc i))
                ;; Other dispatch (rare)
                :else (extract-atom text i))))

          ;; Atom
          :else
          (extract-atom text i))))))

;; --- Assertion extraction ---

(defn- find-assertions
  "Find (is (= expected actual)) patterns in text as raw strings."
  [^String text test-name]
  (let [len (.length text)]
    (loop [i 0, results []]
      (let [idx (.indexOf text "(is" (int i))]
        (if (neg? idx)
          results
          (let [after-is (+ idx 3)]
            (if-not (and (< after-is len)
                         (Character/isWhitespace (.charAt text after-is)))
              ;; Not "(is " — could be "(issue" etc.
              (recur (inc idx) results)
              (let [inner (skip-ws text (inc after-is))]
                (if-not (and (< (inc inner) len)
                             (= \( (.charAt text inner)))
                  (recur (inc idx) results)
                  (let [after-open (skip-ws text (inc inner))]
                    (if-not (and (< after-open len)
                                 (= \= (.charAt text after-open))
                                 (let [past-eq (inc after-open)]
                                   (and (< past-eq len)
                                        (let [ch (.charAt text past-eq)]
                                          (or (Character/isWhitespace ch)
                                              (contains? close-for ch))))))
                      (recur (inc idx) results)
                      (let [after-eq (inc after-open)]
                        (if-let [[expected after-expected] (extract-form text after-eq)]
                          (if-let [[actual after-actual] (extract-form text after-expected)]
                            (recur (inc idx)
                                   (conj results
                                         {:description test-name
                                          :input actual
                                          :expected expected
                                          :kind :run
                                          :section nil}))
                            (recur (inc idx) results))
                          (recur (inc idx) results))))))))))))))

(defn- parse-test-file
  "Parse a *_test.mino file and return extracted examples as raw text."
  [^String text]
  (let [len (.length text)]
    (loop [i 0, results []]
      (let [idx (.indexOf text "(deftest " (int i))]
        (if (neg? idx)
          results
          (let [name-start (skip-ws text (+ idx 9))
                [test-name _] (extract-form text name-start)
                end (matching-close text idx)
                end (if (>= end 0) (inc end) len)
                body (subs text idx end)
                assertions (find-assertions body (or test-name "unknown"))]
            (recur end (into results assertions))))))))

;; --- Public API ---

(defn parse
  "Parse mino test files and return structured usage examples.
  `path` is the root of the mino project (directory containing tests/).
  Returns {:examples [{:description, :input, :expected, :kind, :section}]}."
  [path]
  (let [test-dir (io/file (str path "/tests"))
        test-files (when (.isDirectory test-dir)
                     (->> (.listFiles test-dir)
                          (filter #(str/ends-with? (.getName %) "_test.mino"))
                          (sort-by #(.getName %))))]
    {:examples (vec (mapcat #(parse-test-file (slurp %)) test-files))}))
