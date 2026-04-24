(ns mino-site.content.platforms
  "Platform support matrix page content."
  (:require
    [hiccup2.core :as h]))

(defn platforms-page
  "Generates the Platform Support page HTML body."
  []
  (str
    (h/html
      [:h1 "Platform support"]
      [:p "mino targets C99 with no external dependencies beyond the "
       "C runtime and POSIX file primitives where available. Any "
       "conforming C99 toolchain on a reasonably modern OS should "
       "build and run mino. The list below names the platforms and "
       "toolchains exercised by CI and the floors below which no "
       "testing happens."]

      [:h2 "Continuously tested"]
      [:p "Every commit runs the full test suite on three "
       "GitHub-hosted runners:"]
      [:table
       [:thead
        [:tr [:th "Platform"] [:th "Runner image"] [:th "Compiler"]]]
       [:tbody
        [:tr [:td "Linux"]   [:td [:code "ubuntu-latest"]]  [:td "system " [:code "cc"] " (GCC)"]]
        [:tr [:td "macOS"]   [:td [:code "macos-latest"]]   [:td "system " [:code "cc"] " (Apple Clang)"]]
        [:tr [:td "Windows"] [:td [:code "windows-latest"]] [:td "MinGW-w64 " [:code "gcc"]]]]]
      [:p "GitHub refreshes the " [:code "*-latest"] " images as new "
       "OS versions stabilize. At the time of writing these resolve "
       "to Ubuntu 24.04, macOS 14, and Windows Server 2022."]

      [:h2 "Language and library floors"]
      [:ul
       [:li [:strong "Standard: "] "C99. Builds pass "
        [:code "-std=c99"] "; the code does not rely on C11 or C++ "
        "features."]
       [:li [:strong "libc: "] "a hosted C99 implementation with "
        [:code "math.h"] " (link " [:code "-lm"] "). POSIX "
        [:code "unistd.h"] ", " [:code "dirent.h"] ", and the usual "
        [:code "sys/stat.h"] " surface are assumed on non-Windows."]
       [:li [:strong "Threads: "] "host responsibility. The runtime "
        "itself is single-threaded per " [:code "mino_state_t"]
        ". Cross-state serialization uses a small internal mutex "
        "(pthreads on POSIX, Win32 primitives on Windows)."]]

      [:h2 "Recommended minimums"]
      [:p "These floors are the oldest environments CI would plausibly "
       "cover if the runners moved down. Older versions often work "
       "but are not exercised."]
      [:table
       [:thead
        [:tr [:th "Platform"] [:th "Minimum"] [:th "Notes"]]]
       [:tbody
        [:tr [:td "Ubuntu"]  [:td "20.04"]
         [:td "glibc 2.31, GCC 9, Clang 10. Anything GitHub still "
          "builds " [:code "ubuntu-latest"] " from."]]
        [:tr [:td "macOS"]   [:td "11 (Big Sur)"]
         [:td "Apple Clang shipped with Xcode 12 and later."]]
        [:tr [:td "Windows"] [:td "10"]
         [:td "MinGW-w64 GCC 9+ via MSYS2. MSVC is discussed below."]]
        [:tr [:td "GCC"]     [:td "9"]
         [:td "Any GCC with full C99 support works. 9 is the CI floor."]]
        [:tr [:td "Clang"]   [:td "10"]
         [:td "Apple Clang 12 or mainline Clang 10. Sanitizer build "
          "targets assume the modern driver."]]]]

      [:h2 "Windows and MSVC"]
      [:p "Windows CI builds with MinGW-w64 " [:code "gcc"] ". MSVC is "
       "not in the CI matrix. Historical MSVC had weak C99 support; "
       "MSVC 2019 (v16.8) and later accept " [:code "/std:c11"] " or "
       [:code "/std:c17"] ", which is a superset of C99 sufficient "
       "for mino. If you build with MSVC, pass " [:code "/std:c11"]
       " (or later) and report build or runtime issues against the "
       "current release."]

      [:h2 "Out of scope"]
      [:ul
       [:li "32-bit targets. Pointer-tagging and heap layout assume "
        "a 64-bit word."]
       [:li "Exotic libc implementations (uClibc, musl on niche "
        "distros, embedded bare-metal). These may work; none are "
        "tested."]
       [:li "Pre-C99 compilers."]]

      [:h2 "Reporting platform issues"]
      [:p "If mino does not build or fails tests on a platform that "
       "meets the floors above, file an issue at "
       [:a {:href "https://github.com/leifericf/mino/issues"}
        "github.com/leifericf/mino/issues"]
       " with the compiler banner (" [:code "cc --version"] "), the "
       "OS release, and the failing command output."])))
