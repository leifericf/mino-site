(ns mino-site.format
  "Inline text formatting shared across content pages."
  (:require
    [clojure.string :as str]))

(defn inline
  "Converts inline markdown and bare C identifiers to HTML.
  Handles backtick `code`, **bold**, [links](url), and bare C
  identifiers like NULL, mino_foo(), and MINO_* constants."
  [text]
  (-> text
      ;; Backtick-quoted code
      (str/replace #"`([^`]+)`" "<code>$1</code>")
      ;; Bold
      (str/replace #"\*\*([^*]+)\*\*" "<strong>$1</strong>")
      ;; Links
      (str/replace #"\[([^\]]+)\]\(([^)]+)\)" "<a href=\"$2\">$1</a>")
      ;; Bare C function calls: mino_foo(), mino_foo(args)
      ;; Negative lookbehind avoids double-wrapping things already in <code>
      (str/replace #"(?<!<code>|[`\w])(\bmino_\w+\([^)]*\))" "<code>$1</code>")
      ;; Bare C identifiers: mino_foo, MINO_FOO (not already wrapped, not followed by ()
      (str/replace #"(?<!<code>|[`\w/])(\bmino_\w+\b)(?!\(|</code>)" "<code>$1</code>")
      (str/replace #"(?<!<code>|[`\w])(\bMINO_\w+\b)(?!</code>)" "<code>$1</code>")
      ;; NULL
      (str/replace #"(?<!<code>|[\w])(\bNULL\b)(?!</code>)" "<code>$1</code>")))
