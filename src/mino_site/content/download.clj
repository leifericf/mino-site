(ns mino-site.content.download
  "Download page content."
  (:require
    [hiccup2.core :as h]))

(defn download-page
  "Generates the Download page HTML body."
  []
  (str
    (h/html
      [:h1 "Download"]

      [:div.banner
       "Unstable alpha proof-of-concept. The API may change before v1.0."]

      [:h2 "Source"]
      [:p "mino is hosted on GitHub:"]
      [:pre [:code "git clone https://github.com/leifericf/mino.git"]]
      [:p "You can also "
       [:a {:href "https://github.com/leifericf/mino/archive/refs/heads/main.zip"}
        "download a zip archive"] " of the latest source."]

      [:h2 "Embed in your project"]
      [:p "mino is a handful of C files in " [:code "src/"]
       ". Copy the directory into your project and compile. "
       "No build system or package manager required."]

      [:h2 "Building"]
      [:p "Build the standalone REPL binary:"]
      [:pre [:code "make"]]
      [:p "Or compile mino into your own program:"]
      [:pre [:code "cc -std=c99 -Isrc -o myapp myapp.c src/*.c -lm"]]
      [:p "Run the test suite:"]
      [:pre [:code "make test"]]

      [:h2 "Requirements"]
      [:ul
       [:li "Any C99-compliant compiler (gcc, clang, MSVC, etc.)"]
       [:li "No external libraries or runtime dependencies"]
       [:li "POSIX or Windows (any platform with a C99 compiler)"]]

      [:h2 "License"]
      [:p "mino is released under the "
       [:a {:href "https://opensource.org/licenses/MIT"} "MIT License"]
       ". Use it for anything."])))
