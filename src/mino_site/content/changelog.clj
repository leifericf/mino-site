(ns mino-site.content.changelog
  "Changelog page content.

  Reads CHANGELOG.md and converts it to Hiccup with inline formatting."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup2.core :as h]
    [hiccup.util :as hu]
    [mino-site.format :as fmt]))

;; --- Simple markdown-to-hiccup converter ---

(defn- parse-changelog
  "Converts a Keep a Changelog markdown file into Hiccup.
  Handles ## headings, ### subheadings, bullet lists, inline code,
  and bold text."
  [md-text]
  (let [lines (str/split-lines md-text)
        ;; Skip everything before the first ## heading (title + intro text)
        lines (drop-while #(not (str/starts-with? % "## ")) lines)]
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
            (let [[items remaining]
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
                                 (map (fn [item]
                                        [:li (hu/raw-string (fmt/inline item))])
                                      items)))))

            ;; Blank line or other text
            (str/blank? line)
            (recur rest-lines result)

            ;; Plain paragraph. Accumulate consecutive non-blank lines
            ;; into a single <p>; markdown soft-wrap should not render
            ;; as one <p> per source line. Inner exit conditions must
            ;; match the outer cond's branch triggers exactly (blank,
            ;; heading variants, list start) -- otherwise outer falls
            ;; back to :else with the same line and the loop spins.
            :else
            (let [[para-lines remaining]
                  (loop [ls (cons line rest-lines)
                         acc []]
                    (let [l (first ls)]
                      (cond
                        (nil? l) [acc ls]
                        (or (str/blank? l)
                            (str/starts-with? l "# ")
                            (str/starts-with? l "## ")
                            (str/starts-with? l "### ")
                            (str/starts-with? l "- "))
                        [acc ls]
                        :else (recur (rest ls) (conj acc l)))))]
              (recur remaining
                     (conj result
                           [:p (hu/raw-string
                                 (fmt/inline (str/join " " para-lines)))])))))))))

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
