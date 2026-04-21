(ns mino-site.format
  "Inline text formatting shared across content pages."
  (:require
    [clojure.string :as str]))

(defn- escape-html
  "Escape &, <, > for safe embedding inside HTML tags."
  [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn inline
  "Converts inline markdown and bare C identifiers to HTML.
  Handles backtick `code`, **bold**, [links](url), and bare C
  identifiers like NULL, mino_foo(), and MINO_* constants."
  [text]
  (let [;; Step 1: Replace backtick code spans with placeholders to protect them
        code-spans (atom [])
        placeholder (fn [[_ content]]
                     (let [idx (count @code-spans)]
                       (swap! code-spans conj (escape-html content))
                       (str "\u0000CODE" idx "\u0000")))
        text (str/replace text #"`([^`]+)`" placeholder)
        ;; Step 2: Apply bare-identifier auto-code on unprotected text
        text (-> text
                 (str/replace #"(?<![`\w])(\bmino_\w+\([^)]*\))" "<code>$1</code>")
                 (str/replace #"(?<![`\w/])(\bmino_\w+\b)(?!\(|</code>)" "<code>$1</code>")
                 (str/replace #"(?<![`\w])(\bMINO_\w+\b)(?!</code>)" "<code>$1</code>")
                 (str/replace #"(?<![\w])(\bNULL\b)(?!</code>)" "<code>$1</code>"))
        ;; Step 3: Bold and links
        text (-> text
                 (str/replace #"\*\*([^*]+)\*\*" "<strong>$1</strong>")
                 (str/replace #"\[([^\]]+)\]\(([^)]+)\)" "<a href=\"$2\">$1</a>"))
        ;; Step 4: Restore backtick code spans
        spans @code-spans
        text (str/replace text #"\u0000CODE(\d+)\u0000"
                          (fn [[_ idx-str]]
                            (str "<code>" (nth spans (parse-long idx-str)) "</code>")))]
    text))
