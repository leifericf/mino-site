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
       [:code "stencil-determinism"] " job runs on one pinned-"
       [:code "zig"] " Linux runner every push and verifies that the "
       "committed CPJIT stencil byte tables for every supported "
       "target (both Darwin, both Linux, x86_64 Windows) regenerate "
       "byte-for-byte identically — one version-locked "
       [:code "zig cc"] " cross-compiles them all, so the check is "
       "reproducible across machines. This is the verification floor "
       "for x86_64 Darwin since GitHub has been retiring Intel Mac "
       "runners. It is kept off the per-OS matrix on purpose: host "
       "compiler-version skew is what made an earlier matrix-wide "
       "byte-identity check infeasible."]
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
      [:p "Windows CI gates with MinGW-w64 " [:code "gcc"] ". MSVC is "
       "not a gating compiler, but an informational "
       [:code "msvc-compile-canary"] " job compiles the single-file "
       "amalgamation with MSVC's C frontend (" [:code "cl /TC"] ") on "
       "every push, so an MSVC-specific C99 gap surfaces without "
       "blocking merges. Historical MSVC had weak C99 support; "
       "MSVC 2019 (v16.8) and later accept " [:code "/std:c11"] " or "
       [:code "/std:c17"] ", which is a superset of C99 sufficient "
       "for mino. If you build with MSVC, pass " [:code "/std:c11"]
       " (or later) and report build or runtime issues against the "
       "current release."]

      [:h2 "Cross-compiling releases"]
      [:p "The native per-platform runners build the platform-native "
       "binaries. From a single host, a maintainer also cross-compiles "
       "the Linux (amd64/arm64) and Windows (amd64) binaries with the "
       "pinned " [:code "zig cc"] " — " [:code "./mino task cross-build"]
       ". This is never required to build or embed mino (" [:code "make"]
       " + a C99 compiler stays canonical); it produces two extra "
       "things. First, a fully static " [:strong "musl"] " Linux binary "
       "(amd64/arm64) with zero shared-library dependencies — the "
       "single-file standalone download that runs on any Linux, glibc "
       "or musl alike — published alongside the glibc builds. Second, a "
       "Windows " [:code ".exe"] " linked via mingw without "
       [:code "-static"] ", importing only the system Universal CRT, "
       "with no " [:code "libgcc"] " / " [:code "libwinpthread"]
       " DLL dependency. macOS stays a native build: Zig bundles no "
       "macOS SDK, so a Linux host cannot cross-compile darwin (an "
       "informational CI canary evaluates building darwin natively with "
       [:code "zig cc"] " on a Mac runner). A CI job cross-builds and "
       "validates Linux + Windows from one Linux host on every release."]
      [:p "The pinned " [:code "zig cc"] " is a hard requirement for "
       "developing mino — it gates stencil regeneration, cross-builds, "
       "and the reproducible QA lanes (a UBSan + TSan sanitizer run, a "
       "curated strict-warning lens, an advisory clang static-analyzer "
       "report, and a hermetic build that does not depend on the runner "
       "image's compiler). It is never required of embedders. "
       [:code "./mino task doctor"] " checks the toolchain; the pinned "
       "version and the full task list live in "
       [:code "docs/MAINTAINER_TOOLCHAIN.md"] "."]

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
        [:tr [:td "x86_64 Darwin"] [:td "Mach-O 64"] [:td "yes (stencil-determinism job, pinned zig cc)"]]
        [:tr [:td "x86_64 Windows"][:td "PE/COFF"]  [:td "yes (smoke build every push)"]]]]
      [:p "Per-state runtime control lives behind "
       [:code "mino_set_option(S, MINO_OPT_JIT_MODE, ...)"]
       " (AUTO / OFF / ON) and "
       [:code "mino_set_option(S, MINO_OPT_JIT_HOT_THRESHOLD, ...)"]
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
