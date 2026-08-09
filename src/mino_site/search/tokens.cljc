(ns mino-site.search.tokens
  "Shared normalizer for the search index. Lives in a .cljc namespace
  so the JVM-side index builder and the browser-side query parser
  agree on token shape.

  Lower-case, strip diacritics. No stemming; the corpus is small
  enough that prefix matching does the heavy lifting."
  (:require [clojure.string :as str]))

#?(:clj
   (defn- strip-diacritics [^String s]
     (-> (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)
         (str/replace #"\p{M}+" "")))
   :cljs
   (defn- strip-diacritics [s]
     (-> (.normalize ^js s "NFD")
         (.replace (js/RegExp. "[\\u0300-\\u036f]" "g") ""))))

(defn normalize
  "Lower-case and strip diacritics. Returns a single normalized
  string suitable for prefix matching."
  [s]
  (some-> s
          str/lower-case
          strip-diacritics))
