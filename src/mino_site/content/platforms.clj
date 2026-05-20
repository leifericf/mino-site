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
      [:p "Every push runs build + test suite + release-gate (where "
       "applicable) on four pinned GitHub-hosted runners covering "
       "every supported host architecture:"]
      [:table
       [:thead
        [:tr [:th "Platform"] [:th "Runner image"] [:th "Compiler"] [:th "Release gate"]]]
       [:tbody
        [:tr [:td "x86_64 Linux"] [:td [:code "ubuntu-24.04"]]    [:td "system " [:code "cc"] " (GCC)"]    [:td "yes"]]
        [:tr [:td "ARM64 Linux"]  [:td [:code "ubuntu-24.04-arm"]] [:td "system " [:code "cc"] " (GCC)"]    [:td "yes"]]
        [:tr [:td "ARM64 Darwin"] [:td [:code "macos-14"]]         [:td "system " [:code "cc"] " (Apple Clang)"] [:td "yes"]]
        [:tr [:td "x86_64 Windows"] [:td [:code "windows-2022"]]   [:td "MinGW-w64 " [:code "gcc"]]         [:td "smoke only"]]]]
      [:p "Windows skips the composite release-gate because the "
       "ASan step relies on a libsanitizer that MinGW does not "
       "ship; the smoke build + test suite still gate every push "
       "on that runner. A separate "
       [:code "cross-compile"] " job runs on macos-14 every push "
       "and verifies that the committed CPJIT stencil byte tables "
       "for every supported target (ARM64 Linux, x86_64 Linux, "
       "x86_64 Darwin, x86_64 Windows) regenerate identically — "
       "this is the verification floor for x86_64 Darwin since "
       "GitHub has been retiring Intel Mac runners."]
      [:p "A nightly workflow (04:00 UTC daily) re-runs the "
       "release-gate plus extended suites (GC stress, fault "
       "injection, embedding stress) on the three non-Windows "
       "runners. Toolchain drift surfaces from the cron tick "
       "instead of waiting for the next PR to trip on it."]

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
        "itself is single-threaded per " [:code "mino_state"]
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

      [:h2 "JIT support per host"]
      [:p "The copy-and-patch JIT (CPJIT) ships byte tables for "
       "every supported host arch. The full "
       [:code "mino"] " binary auto-detects the host and enables "
       "the JIT; the parallel "
       [:code "mino-lean"] " binary is the same build with the JIT "
       "pipeline compiled out — useful when a host has no executable "
       "memory primitives, or when a smaller binary is more valuable "
       "than peak throughput."]
      [:table
       [:thead
        [:tr [:th "Host"] [:th "Format"] [:th "CPJIT byte tables"]]]
       [:tbody
        [:tr [:td "ARM64 Darwin"]  [:td "Mach-O 64"] [:td "yes (dev host; release-gate every push)"]]
        [:tr [:td "ARM64 Linux"]   [:td "ELF64"]    [:td "yes (release-gate every push)"]]
        [:tr [:td "x86_64 Linux"]  [:td "ELF64"]    [:td "yes (release-gate every push)"]]
        [:tr [:td "x86_64 Darwin"] [:td "Mach-O 64"] [:td "yes (cross-compile parity every push)"]]
        [:tr [:td "x86_64 Windows"][:td "PE/COFF"]  [:td "yes (smoke build every push)"]]]]
      [:p "Per-state runtime control lives behind "
       [:code "mino_state_set_jit_mode"]
       " (AUTO / OFF / ON) and "
       [:code "mino_state_set_jit_hot_threshold"]
       " (call-count before the JIT compiles a function); the CLI "
       "exposes both as "
       [:code "--jit=auto|off|on"] " and "
       [:code "--jit-threshold=N"] ". Capability discovery returns a "
       [:code "mino_jit_capability"] " struct documenting "
       "{available, mode, threshold, host_arch, host_os} so an "
       "embedder can size the host's tuning at startup."]

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
