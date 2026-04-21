(ns mino-site.content.testing
  "Testing guide page content."
  (:require
    [hiccup2.core :as h]))

(defn testing-page
  "Generates the Testing Guide page HTML body."
  []
  (str
    (h/html
      [:h1 "Testing"]
      [:p "mino includes a test framework written in mino itself. It follows "
       "the same conventions as " [:code "clojure.test"] ": named tests with "
       [:code "deftest"] ", assertions with " [:code "is"] ", and contextual "
       "grouping with " [:code "testing"] "."]
      [:p "The framework is " [:code "clojure.test"]
       ", loaded via " [:code "(require '[clojure.test :refer [deftest is testing]])"]
       " from test files. " [:code "tests/test.mino"] " is a compatibility "
       "entry point that delegates to " [:code "clojure.test"] "."]

      [:h2 "Writing Tests"]
      [:p "A test file is a normal " [:code ".mino"] " file that requires "
       "the framework and defines tests:"]
      [:pre [:code {:data-lang "mino"}
"(require \"tests/test\")

(deftest addition
  (is (= 3 (+ 1 2))))

(deftest string-operations
  (testing \"concatenation\"
    (is (= \"hello world\" (str \"hello\" \" \" \"world\"))))
  (testing \"substring\"
    (is (= \"ell\" (subs \"hello\" 1 4)))))

(deftest error-handling
  (is (thrown? (throw \"expected error\"))))"]]

      [:h2 "API Reference"]

      [:h3 [:code "(deftest name & body)"]]
      [:p "Defines and registers a named test. The body contains one or more "
       "assertions. Tests are collected in registration order and executed "
       "when " [:code "run-tests"] " is called."]

      [:h3 [:code "(is expr)"] " / " [:code "(is expr msg)"]]
      [:p "The core assertion macro. Returns the result of the expression. "
       "On failure, records the original form, an optional message, and "
       "expected vs. actual values."]
      [:p "Three assertion modes:"]
      [:table
       [:thead
        [:tr [:th "Form"] [:th "Passes when"]]]
       [:tbody
        [:tr [:td [:code "(is (= expected actual))"]]
         [:td "Values are equal. Reports expected and actual on failure."]]
        [:tr [:td [:code "(is (thrown? body...))"]]
         [:td "Body throws an exception. Fails if no exception is raised."]]
        [:tr [:td [:code "(is expr)"]]
         [:td "Expression is truthy (not " [:code "nil"] " or "
          [:code "false"] ")."]]]]

      [:h3 [:code "(testing desc & body)"]]
      [:p "Adds a context string to failure messages. Context strings nest "
       "and are joined with " [:code " > "] " in the output. Must appear "
       "inside a " [:code "deftest"] "."]
      [:pre [:code {:data-lang "mino"}
"(deftest collections
  (testing \"vectors\"
    (testing \"equality\"
      (is (= [1 2 3] [1 2 3])))))
;; On failure: \"vectors > equality\""]]

      [:h3 [:code "(run-tests)"]]
      [:p "Executes all registered tests and prints a summary. Each test "
       "runs in a " [:code "try/catch"] " so one failure does not prevent "
       "the rest from running. Calls " [:code "(exit 1)"] " on any "
       "failures or errors, " [:code "(exit 0)"] " on success."]

      [:h2 "Running Tests"]
      [:pre [:code "# Run the test suite\n./mino task test\n\n# Run under GC stress (collects on every allocation)\nMINO_GC_STRESS=1 ./mino tests/run.mino"]]

      [:h2 "Test File Organization"]
      [:p "By convention, test files live in " [:code "tests/"] " and are "
       "named " [:code "*_test.mino"] ". A runner file loads all test "
       "modules and calls " [:code "run-tests"] ":"]
      [:pre [:code {:data-lang "mino"}
";; tests/run.mino
(require \"tests/test\")
(require \"tests/arithmetic_test\")
(require \"tests/string_test\")
;; ...
(run-tests)"]]
      [:p "Each test file starts with " [:code "(require \"tests/test\")"]
       " to load the framework. The " [:code "require"] " call is "
       "idempotent: the framework is loaded once and cached."]

      [:h2 "Output"]
      [:p "On success:"]
      [:pre [:code "790 tests, 3069 assertions: 3069 passed, 0 failed, 0 errors"]]
      [:p "On failure, each failing assertion is reported with its test "
       "name, context path, the original form, and a diff:"]
      [:pre [:code "Failures:\n  in addition\n    arithmetic > basic\n    (= 4 (+ 1 2))\n    expected: 4\n    actual: 3\n\n10 tests, 12 assertions: 11 passed, 1 failed, 0 errors"]]

      [:h2 "Testing in the REPL"]
      [:p "The test framework works in the REPL too. Load it, define a "
       "test, and call " [:code "run-tests"] " interactively:"]
      [:pre [:code {:data-lang "mino"}
"mino> (require \"test\")\nmino> (deftest quick-check (is (= 4 (+ 2 2))))\nmino> (run-tests)\n1 tests, 1 assertions: 1 passed, 0 failed, 0 errors"]])))
