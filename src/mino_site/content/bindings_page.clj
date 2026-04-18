(ns mino-site.content.bindings-page
  "Language embedding examples page.

  Shows how to embed mino from C, C++, Java, and sketches for
  Zig, Rust, C#, Go, and Swift."
  (:require
    [clojure.java.io :as io]
    [hiccup2.core :as h]
    [hiccup.util :as hu]))

(def ^:private zig-snippet
  "const mino = @cImport({
    @cInclude(\"mino.h\");
});

pub fn main() !void {
    const S = mino.mino_state_new();
    defer mino.mino_state_free(S);

    const env = mino.mino_new(S);
    defer mino.mino_env_free(S, env);

    const result = mino.mino_eval_string(S,
        \"(+ 1 2)\", env);
    if (result) |r| mino.mino_println(S, r);
}")

(def ^:private rust-snippet
  "extern \"C\" {
    fn mino_state_new() -> *mut MinoState;
    fn mino_state_free(s: *mut MinoState);
    fn mino_new(s: *mut MinoState) -> *mut MinoEnv;
    fn mino_env_free(s: *mut MinoState, e: *mut MinoEnv);
    fn mino_eval_string(
        s: *mut MinoState, src: *const c_char,
        e: *mut MinoEnv,
    ) -> *mut MinoVal;
    fn mino_println(s: *mut MinoState, v: *const MinoVal);
}

fn main() {
    unsafe {
        let s = mino_state_new();
        let e = mino_new(s);
        let src = CString::new(\"(+ 1 2)\").unwrap();
        let r = mino_eval_string(s, src.as_ptr(), e);
        if !r.is_null() { mino_println(s, r); }
        mino_env_free(s, e);
        mino_state_free(s);
    }
}")

(def ^:private csharp-snippet
  "using System.Runtime.InteropServices;

class Mino {
    [DllImport(\"mino\")] static extern IntPtr mino_state_new();
    [DllImport(\"mino\")] static extern void mino_state_free(IntPtr s);
    [DllImport(\"mino\")] static extern IntPtr mino_new(IntPtr s);
    [DllImport(\"mino\")] static extern void mino_env_free(
        IntPtr s, IntPtr e);
    [DllImport(\"mino\")] static extern IntPtr mino_eval_string(
        IntPtr s, string src, IntPtr e);
    [DllImport(\"mino\")] static extern void mino_println(
        IntPtr s, IntPtr v);

    static void Main() {
        var s = mino_state_new();
        var e = mino_new(s);
        var r = mino_eval_string(s, \"(+ 1 2)\", e);
        if (r != IntPtr.Zero) mino_println(s, r);
        mino_env_free(s, e);
        mino_state_free(s);
    }
}")

(def ^:private go-snippet
  "// #cgo LDFLAGS: -lmino -lm
// #include \"mino.h\"
import \"C\"

func main() {
    s := C.mino_state_new()
    defer C.mino_state_free(s)

    e := C.mino_new(s)
    defer C.mino_env_free(s, e)

    src := C.CString(\"(+ 1 2)\")
    defer C.free(unsafe.Pointer(src))

    r := C.mino_eval_string(s, src, e)
    if r != nil { C.mino_println(s, r) }
}")

(def ^:private swift-snippet
  "// Bridging header: #include \"mino.h\"

let s = mino_state_new()!
defer { mino_state_free(s) }

let e = mino_new(s)!
defer { mino_env_free(s, e) }

if let r = mino_eval_string(s, \"(+ 1 2)\", e) {
    mino_println(s, r)
}")

(defn bindings-page
  "Render the language bindings page."
  [mino-root]
  (let [c-src   (try (slurp (io/file mino-root "examples" "bindings" "embed_c.c"))
                     (catch Exception _ nil))
        cpp-src (try (slurp (io/file mino-root "examples" "bindings" "embed_cpp.cpp"))
                     (catch Exception _ nil))
        java-src (try (slurp (io/file mino-root "examples" "bindings" "MinoEmbed.java"))
                      (catch Exception _ nil))
        jni-src  (try (slurp (io/file mino-root "examples" "bindings" "mino_jni.c"))
                      (catch Exception _ nil))]
    (str
      (h/html
        [:h1 "Language Bindings"]
        [:p "mino exposes a plain C ABI with simple types: pointers, integers, "
         "doubles, and null-terminated strings. Any language with C FFI support "
         "can embed it directly, with no wrapper library or code generation step."]
        [:p "The examples below all run the same scenario: build a vector of "
         "sensor events, evaluate a mino script that filters, groups, and "
         "summarizes the data, and print the result."]

        ;; C
        [:section.use-case-section
         [:h2 "C"]
         [:p "The baseline reference. Direct API calls, no translation layer."]
         (when c-src
           [:details {:open true}
            [:summary "embed_c.c"]
            [:pre [:code {:data-lang "c"} c-src]]])]

        ;; C++
        [:section.use-case-section
         [:h2 "C++"]
         [:p "Same API calls with C++17 patterns: auto, range-for, lambdas. "
          "The " [:code "extern \"C\""] " guards in mino.h mean no wrapper "
          "is needed."]
         (when cpp-src
           [:details {:open true}
            [:summary "embed_cpp.cpp"]
            [:pre [:code {:data-lang "c"} cpp-src]]])]

        ;; Java
        [:section.use-case-section
         [:h2 "Java (JNI)"]
         [:p "A thin JNI bridge maps native methods to the mino C API. "
          "State and environment handles are passed as Java longs."]
         (when java-src
           [:details {:open true}
            [:summary "MinoEmbed.java"]
            [:pre [:code {:data-lang "c"} java-src]]])
         (when jni-src
           [:details
            [:summary "mino_jni.c (JNI bridge)"]
            [:pre [:code {:data-lang "c"} jni-src]]])]

        ;; Snippets
        [:section.use-case-section
         [:h2 "Other languages"]
         [:p "Since mino is a plain C library, most systems languages can call "
          "it directly through their standard FFI mechanisms. Below are "
          "short sketches showing how the call looks in each language."]

         [:h3 "Zig"]
         [:p [:code "@cImport"] " reads mino.h directly. Zero overhead, "
          "full type safety."]
         [:pre [:code {:data-lang "c"} zig-snippet]]

         [:h3 "Rust"]
         [:p [:code "extern \"C\""] " block with unsafe wrappers. A safe "
          "Rust API could be layered on top."]
         [:pre [:code {:data-lang "c"} rust-snippet]]

         [:h3 "C# / .NET"]
         [:p "P/Invoke with " [:code "[DllImport]"] " attributes. Works "
          "on .NET Framework, .NET Core, and Mono."]
         [:pre [:code {:data-lang "c"} csharp-snippet]]

         [:h3 "Go"]
         [:p "cgo with " [:code "#include \"mino.h\""] " in a magic comment. "
          "The Go runtime handles the C-to-Go boundary."]
         [:pre [:code {:data-lang "c"} go-snippet]]

         [:h3 "Swift"]
         [:p "A C bridging header exposes the mino API to Swift. "
          "Optional return types map naturally to nullable pointers."]
         [:pre [:code {:data-lang "c"} swift-snippet]]]))))
