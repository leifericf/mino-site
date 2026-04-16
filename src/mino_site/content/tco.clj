(ns mino-site.content.tco
  "Proper Tail Calls guide page content."
  (:require
    [hiccup2.core :as h]))

(defn tco-page
  "Generates the Proper Tail Calls page HTML body."
  []
  (str
    (h/html
      [:h1 "Proper Tail Calls"]
      [:p "mino optimizes all function calls in tail position. "
       "Self-recursion, mutual recursion, and general tail calls "
       "run in constant stack space with no special syntax required."]

      [:h2 "What tail position means"]
      [:p "A function call is in " [:em "tail position"] " when it is "
       "the last thing a function does before returning. mino "
       "recognizes tail position in:"]
      [:ul
       [:li [:code "if"] " branches (both then and else)"]
       [:li "The last expression in " [:code "do"] ", " [:code "let"]
        ", and function bodies"]
       [:li [:code "when"] ", " [:code "cond"] ", " [:code "and"]
        ", " [:code "or"] " (these expand to " [:code "if"]
        " and " [:code "do"] ")"]]
      [:p "When the evaluator encounters a function call in tail "
       "position, it returns a trampoline value instead of growing "
       "the C stack. The caller's trampoline loop picks it up and "
       "jumps to the new function. No stack frame is allocated."]

      [:h2 "Self-recursion"]
      [:p "Write recursive functions naturally. As long as the "
       "recursive call is in tail position, it runs in constant "
       "stack space:"]
      [:pre [:code {:data-lang "mino"}
"(defn countdown (n)
  (if (= n 0) :done (countdown (- n 1))))

(countdown 1000000) ;; => :done (no stack overflow)"]]

      [:p "Accumulator patterns work the same way:"]
      [:pre [:code {:data-lang "mino"}
"(defn sum-to (n acc)
  (if (= n 0) acc (sum-to (- n 1) (+ acc n))))

(sum-to 100000 0) ;; => 5000050000"]]

      [:h2 "Mutual recursion"]
      [:p "Two or more functions can call each other in tail "
       "position without stack growth:"]
      [:pre [:code {:data-lang "mino"}
"(defn is-even? (n)
  (if (= n 0) true (is-odd? (- n 1))))

(defn is-odd? (n)
  (if (= n 0) false (is-even? (- n 1))))

(is-even? 100000) ;; => true"]]

      [:h2 "Non-tail calls"]
      [:p "If a call is " [:em "not"] " in tail position, normal "
       "recursion applies and will use stack space. This is correct "
       "behavior. For example, both calls in a tree traversal are "
       "non-tail:"]
      [:pre [:code {:data-lang "mino"}
"(defn fib (n)
  (if (< n 2)
    n
    (+ (fib (- n 1))    ;; not tail: + still needs to run
       (fib (- n 2))))) ;; not tail: + still needs to run"]]

      [:h2 "loop and recur"]
      [:p [:code "loop"] "/" [:code "recur"] " provide explicit "
       "iteration for code that reads better as a loop than as "
       "recursion. They continue to work exactly as before:"]
      [:pre [:code {:data-lang "mino"}
"(loop [n 100 acc 0]
  (if (= n 0)
    acc
    (recur (- n 1) (+ acc n))))"]]
      [:p "With proper tail calls, " [:code "loop"] "/" [:code "recur"]
       " are a stylistic choice rather than a necessity. Use "
       "whichever reads more clearly for the task at hand."]

      [:h2 "trampoline"]
      [:p [:code "trampoline"] " is available for code that returns "
       "thunks (zero-argument functions) to defer work:"]
      [:pre [:code {:data-lang "mino"}
"(defn bounce [n]
  (if (= n 0) :done (fn [] (bounce (- n 1)))))

(trampoline bounce 100000) ;; => :done"]]
      [:p "In practice, " [:code "trampoline"] " is rarely needed "
       "since tail calls are already optimized. It exists as a "
       "convenience for patterns that explicitly return thunks."]

      [:h2 "How it works"]
      [:p "The evaluator tracks whether each expression is in tail "
       "position via an internal flag. When a user-defined function "
       "call occurs in tail position, the evaluator returns a "
       [:code "MINO_TAIL_CALL"] " sentinel carrying the target "
       "function and arguments, instead of recursing into the "
       "function body immediately."]
      [:p "The " [:code "apply_callable"] " trampoline loop handles "
       "two kinds of sentinel:"]
      [:ul
       [:li [:code "MINO_RECUR"] " (from " [:code "recur"] "): "
        "rebinds parameters and loops within the same function."]
       [:li [:code "MINO_TAIL_CALL"] " (from a tail-position call): "
        "switches to the target function and loops. Works across "
        "function boundaries, enabling mutual recursion."]]
      [:p "This is the same approach used by Scheme and Erlang. "
       "The C stack stays flat regardless of recursion depth."])))
