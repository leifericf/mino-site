(ns mino-site.content.changelog
  "Changelog page content.

  For now, reads CHANGELOG.md and renders it as a simple pre-formatted
  block. Phase 3 will replace this with a proper markdown-to-hiccup
  parser that structures entries by version."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup2.core :as h]))

;; --- Simple markdown-to-hiccup converter ---

(defn- parse-changelog
  "Converts a Keep a Changelog markdown file into Hiccup.
  Handles ## headings, ### subheadings, bullet lists, inline code,
  and bold text. Good enough for the well-structured CHANGELOG.md."
  [md-text]
  (let [lines (str/split-lines md-text)]
    (loop [lines lines
           result []]
      (if (empty? lines)
        result
        (let [line (first lines)
              rest-lines (rest lines)]
          (cond
            ;; Skip the top-level # heading
            (str/starts-with? line "# ")
            (recur rest-lines result)

            ;; ## Version heading
            (str/starts-with? line "## ")
            (let [text (subs line 3)
                  id (-> text
                         str/lower-case
                         (str/replace #"[^\w\d]+" "-")
                         (str/replace #"^-|-$" ""))]
              (recur rest-lines
                     (conj result [:h2 {:id id} text])))

            ;; ### Subheading
            (str/starts-with? line "### ")
            (recur rest-lines
                   (conj result [:h3 (subs line 4)]))

            ;; Bullet list item
            (str/starts-with? line "- ")
            (let [;; Collect consecutive bullet lines (including continuation)
                  [items remaining]
                  (loop [ls (cons line rest-lines)
                         items []]
                    (let [l (first ls)]
                      (cond
                        (nil? l) [items ls]
                        (str/starts-with? l "- ")
                        (recur (rest ls) (conj items (subs l 2)))
                        (str/starts-with? l "  ")
                        (recur (rest ls)
                               (if (seq items)
                                 (update items (dec (count items))
                                         str " " (str/trim l))
                                 items))
                        :else [items ls])))]
              (recur remaining
                     (conj result
                           (into [:ul]
                                 (map (fn [item] [:li item]) items)))))

            ;; Blank line or other text
            (str/blank? line)
            (recur rest-lines result)

            ;; Plain paragraph
            :else
            (recur rest-lines
                   (conj result [:p line]))))))))

(defn changelog-page
  "Generates the Changelog page HTML body."
  [mino-root]
  (let [path (str mino-root "/CHANGELOG.md")
        md   (when (.exists (io/file path))
               (slurp path))]
    (str
      (h/html
        [:h1 "Changelog"]
        [:p "All notable changes to mino, following "
         [:a {:href "https://keepachangelog.com/"} "Keep a Changelog"]
         " format."])
      (if md
        (apply str (map #(str (h/html %)) (parse-changelog md)))
        (str (h/html [:p "Changelog not available."]))))))
