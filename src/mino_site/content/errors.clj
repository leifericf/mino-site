(ns mino-site.content.errors
  "Error diagnostics guide page content."
  (:require
    [hiccup2.core :as h]))

(defn errors-page
  "Generates the Error Diagnostics guide page HTML body."
  []
  (str
    (h/html
      [:h1 "Error Diagnostics"]
      [:p "Every error in mino produces a structured diagnostic: a plain map "
       "with stable keys, a classified error code, source location, and a "
       "human-readable message. The same model serves the REPL, embedded "
       "hosts, and user code."]

      [:h2 "What You See"]
      [:p "When an error occurs in the REPL or a script, mino renders a "
       "diagnostic with the error code, message, source file, line, column, "
       "and a snippet of the source with a caret pointer:"]
      [:pre [:code {:data-lang "text"}
"error[MTY001]: count: expected a collection, got int
  --> app.mino:18:3
   |
 18 |   (count 42)
    |   ^"]]

      [:p "Reader errors show the exact position:"]
      [:pre [:code {:data-lang "text"}
"error[MRE001]: unterminated string literal
  --> app.mino:7:8
   |
  7 | (def y \"unterminated
   |        ^"]]

      [:h2 "Errors Are Data"]
      [:p "In " [:code "catch"] " handlers, the exception value is always a "
       "map with these keys:"]
      [:table
       [:thead
        [:tr [:th "Key"] [:th "Type"] [:th "Meaning"]]]
       [:tbody
        [:tr [:td [:code ":mino/kind"]] [:td "keyword"]
         [:td "Category: " [:code ":reader"] ", " [:code ":syntax"] ", "
          [:code ":eval/type"] ", " [:code ":eval/arity"] ", "
          [:code ":eval/bounds"] ", " [:code ":user"] ", etc."]]
        [:tr [:td [:code ":mino/code"]] [:td "string"]
         [:td "Stable error code, e.g. " [:code "\"MTY001\""]]]
        [:tr [:td [:code ":mino/phase"]] [:td "keyword"]
         [:td "Processing phase: " [:code ":read"] ", " [:code ":eval"]]]
        [:tr [:td [:code ":mino/message"]] [:td "string"]
         [:td "Human-readable primary message"]]
        [:tr [:td [:code ":mino/data"]] [:td "any"]
         [:td "The original thrown value (for user exceptions)"]]]]

      [:p "Example: catching a type error and inspecting it."]
      [:pre [:code {:data-lang "mino"}
"(try
  (count 42)
  (catch e
    (println (:mino/kind e))    ;; :eval/type
    (println (:mino/code e))    ;; \"MTY001\"
    (println (:mino/message e)) ;; \"count: expected a collection, got int\"
    ))"]]

      [:h2 "throw and catch"]
      [:p [:code "(throw x)"] " accepts any value. The " [:code "catch"]
       " handler always receives a diagnostic map, regardless of what was "
       "thrown. The original value is accessible via " [:code "(ex-data e)"]
       ":"]
      [:pre [:code {:data-lang "mino"}
"(try
  (throw \"oops\")
  (catch e
    (println (ex-data e))    ;; \"oops\"
    (println (ex-message e)) ;; \"oops\"
    (println (error? e))     ;; true
    ))"]]

      [:p "If you throw an " [:code "ex-info"] " map, " [:code "ex-data"]
       " and " [:code "ex-message"] " extract the payload transparently:"]
      [:pre [:code {:data-lang "mino"}
"(try
  (throw (ex-info \"not found\" {:code 404}))
  (catch e
    (println (ex-data e))    ;; {:code 404}
    (println (ex-message e)) ;; \"not found\"
    ))"]]

      [:h2 "Helper Functions"]
      [:table
       [:thead
        [:tr [:th "Function"] [:th "Description"]]]
       [:tbody
        [:tr [:td [:code "(error? x)"]]
         [:td "True if " [:code "x"] " is a map with " [:code ":mino/kind"]]]
        [:tr [:td [:code "(ex-data e)"]]
         [:td "Extract the data payload from a diagnostic map or ex-info"]]
        [:tr [:td [:code "(ex-message e)"]]
         [:td "Extract the message from a diagnostic map or ex-info"]]
        [:tr [:td [:code "(last-error)"]]
         [:td "Return the last diagnostic map, or nil"]]
        [:tr [:td [:code "(ex-info msg data)"]]
         [:td "Create an exception map for throwing"]]]]

      [:h2 "Error Code Catalog"]
      [:p "Every error has a stable code that can be matched programmatically. "
       "Codes are grouped by prefix:"]
      [:table
       [:thead
        [:tr [:th "Prefix"] [:th "Category"] [:th "Examples"]]]
       [:tbody
        [:tr [:td [:code "MRE"]] [:td "Reader"]
         [:td "Unterminated string, unexpected delimiter, unterminated collection"]]
        [:tr [:td [:code "MSY"]] [:td "Syntax"]
         [:td "Malformed " [:code "def"] ", " [:code "fn"] ", " [:code "let"]
          " special forms"]]
        [:tr [:td [:code "MNS"]] [:td "Name resolution"]
         [:td "Unresolved symbol, missing namespace alias"]]
        [:tr [:td [:code "MAR"]] [:td "Arity"]
         [:td "Wrong number of arguments"]]
        [:tr [:td [:code "MTY"]] [:td "Type mismatch"]
         [:td "Operation received unsupported value type"]]
        [:tr [:td [:code "MBD"]] [:td "Bounds"]
         [:td "Index out of range"]]
        [:tr [:td [:code "MCT"]] [:td "Contract"]
         [:td "Precondition or invariant violation"]]
        [:tr [:td [:code "MHO"]] [:td "Host"]
         [:td "File I/O, capability denied, host callback failure"]]
        [:tr [:td [:code "MLM"]] [:td "Limit"]
         [:td "Step limit, heap limit, recursion depth exceeded"]]
        [:tr [:td [:code "MUS"]] [:td "User"]
         [:td "User-thrown exception"]]
        [:tr [:td [:code "MIN"]] [:td "Internal"]
         [:td "Out of memory, runtime bug"]]]]

      [:h2 "C API"]
      [:p "Embedders have structured access to the last error:"]
      [:pre [:code {:data-lang "c"}
"/* Backward-compatible string access */
const char *mino_last_error(S);

/* Structured diagnostic (internal fields) */
const mino_diag_t *mino_last_diag(S);

/* Diagnostic as a mino map with :mino/* keys */
mino_val_t *mino_last_error_map(S);

/* Render to buffer in compact or pretty mode */
int mino_render_diag(S, diag, MINO_DIAG_RENDER_PRETTY, buf, sizeof(buf));"]]

      [:p "Existing code using " [:code "mino_last_error()"]
       " continues to work unchanged. The string is rendered from the "
       "structured diagnostic internally."])))
