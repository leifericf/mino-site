(ns mino-site.parse.async-api
  "Parse lib/core/async.mino for the async API reference.

  Extracts public defn and defmacro forms with their docstrings."
  (:require
    [clojure.string :as str]))

(defn- balanced?
  "Returns true when the accumulated lines have balanced parens."
  [s]
  (loop [cs (seq s) depth 0 in-string false escape false]
    (if (empty? cs)
      (zero? depth)
      (let [c (first cs)]
        (cond
          escape    (recur (rest cs) depth in-string false)
          (= c \\)  (recur (rest cs) depth in-string true)
          in-string (recur (rest cs) depth (not= c \") false)
          (= c \")  (recur (rest cs) depth true false)
          (= c \;)  (recur (drop-while #(not= % \newline) (rest cs))
                           depth false false)
          (= c \()  (recur (rest cs) (inc depth) false false)
          (= c \))  (recur (rest cs) (dec depth) false false)
          :else     (recur (rest cs) depth false false))))))

(defn- collect-form
  "Collect lines starting from the current line until parens are balanced."
  [lines]
  (loop [ls lines collected []]
    (if (empty? ls)
      [(str/join "\n" collected) nil]
      (let [acc (conj collected (first ls))
            text (str/join "\n" acc)]
        (if (balanced? text)
          [text (rest ls)]
          (recur (rest ls) acc))))))

(defn- extract-docstring
  "Extract docstring from a defn/defmacro form source."
  [source]
  (when-let [m (re-find #"(?s)\"((?:[^\"\\]|\\.)*)\"" source)]
    (let [doc (second m)]
      (when-not (str/includes? doc "(")
        (-> doc
            (str/replace "\\n" "\n")
            (str/replace "\\\"" "\"")
            str/trim)))))

(defn parse
  "Parse lib/core/async.mino under the given mino root.
  Returns a vector of {:name str :kind keyword :doc str :source str}."
  [mino-root]
  (let [path (str mino-root "/lib/core/async.mino")]
    (if-not (.exists (java.io.File. path))
      []
      (let [lines (str/split-lines (slurp path))]
        (loop [lines lines
               forms []]
          (if (empty? lines)
            forms
            (let [line (first lines)]
              (cond
                (or (str/starts-with? line "(defn ")
                    (str/starts-with? line "(defmacro "))
                (let [[source remaining] (collect-form lines)
                      kind (if (str/starts-with? line "(defmacro ") :macro :fn)
                      name-m (re-find #"\(def(?:n|macro)\s+(\S+)" line)
                      name (when name-m (second name-m))
                      doc (extract-docstring source)]
                  (if (and name
                           (or (= name "go-loop")
                               (not (str/starts-with? name "go-")))
                           (not (= name "mix-should-pass?"))
                           (not (= name "mix-paused?"))
)
                    (recur (or remaining [])
                           (conj forms {:name name
                                        :kind kind
                                        :doc doc
                                        :source source}))
                    (recur (or remaining []) forms)))

                (str/starts-with? line "(def ")
                (let [[source remaining] (collect-form lines)
                      name-m (re-find #"\(def\s+(\S+)" line)
                      name (when name-m (second name-m))]
                  (if name
                    (recur (or remaining [])
                           (conj forms {:name name
                                        :kind :def
                                        :doc nil
                                        :source source}))
                    (recur (or remaining []) forms)))

                :else
                (recur (rest lines) forms)))))))))
