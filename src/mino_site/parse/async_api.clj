(ns mino-site.parse.async-api
  "Parse the async API surface for the language reference.

  Reads both `lib/core/channel.clj` (channel/buffer/alts mechanics)
  and `lib/core/async.clj` (go macro, blocking bridges, combinators),
  extracting public defn/defmacro forms with their docstrings. Starred
  internal aliases (chan*, chan-put*, buf-fixed*, ...) and private go-
  helpers are filtered out."
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

(defn- public-name?
  "True if name should appear in the public async API reference."
  [name]
  (and name
       (not (str/ends-with? name "*"))
       (or (= name "go-loop")
           (not (str/starts-with? name "go-")))
       (not= name "mix-should-pass?")
       (not= name "mix-paused?")))

(defn- parse-file
  "Parse a single .clj file, returning a vector of API forms."
  [path]
  (if-not (.exists (java.io.File. path))
    []
    (let [lines (str/split-lines (slurp path))]
      (loop [lines lines forms []]
        (if (empty? lines)
          forms
          (let [line (first lines)]
            (cond
              (or (str/starts-with? line "(defn ")
                  (str/starts-with? line "(defmacro "))
              (let [[source remaining] (collect-form lines)
                    kind   (if (str/starts-with? line "(defmacro ") :macro :fn)
                    name-m (re-find #"\(def(?:n|macro)\s+(\S+)" line)
                    name   (when name-m (second name-m))
                    doc    (extract-docstring source)]
                (if (public-name? name)
                  (recur (or remaining [])
                         (conj forms {:name   name
                                      :kind   kind
                                      :doc    doc
                                      :source source}))
                  (recur (or remaining []) forms)))

              (str/starts-with? line "(def ")
              (let [[source remaining] (collect-form lines)
                    name-m (re-find #"\(def\s+(\S+)" line)
                    name   (when name-m (second name-m))]
                (if (public-name? name)
                  (recur (or remaining [])
                         (conj forms {:name   name
                                      :kind   :def
                                      :doc    nil
                                      :source source}))
                  (recur (or remaining []) forms)))

              :else
              (recur (rest lines) forms))))))))

(defn parse
  "Parse the async API surface under the given mino root.
  Reads lib/core/channel.clj first (channel/buffer/alts mechanics),
  then lib/core/async.clj (go macro, blocking bridges, combinators).
  Returns a vector of {:name str :kind keyword :doc str :source str}."
  [mino-root]
  (vec (concat
         (parse-file (str mino-root "/lib/core/channel.clj"))
         (parse-file (str mino-root "/lib/core/async.clj")))))
