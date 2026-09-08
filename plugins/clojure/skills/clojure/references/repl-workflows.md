# REPL Workflow Templates

Concrete REPL-first workflow patterns for common development tasks. Each template follows the same cycle: understand current behavior → develop fix/feature in REPL → verify → apply to files.

## Unable to resolve symbol

Very often **wrong current namespace**, not a missing def. Eval ran in one ns while the name lives in another; or that file was never loaded into this REPL. `(in-ns 'owning.ns)` (or eval from the owning file / `require`+alias), then retry. Calva: watch the ns chip. Scittle nREPL / SCI: same shape (`:type :sci/error`, callstack ns).

## Bug Fix

```clojure
(in-ns 'my.namespace)
(require '[namespace.with.issue :as issue])

;; 1. Understand current behavior
(issue/problematic-function test-data)

;; 2. Develop fix in REPL
(defn test-fix [data] ...)
(test-fix test-data)

;; 3. Test edge cases
(test-fix edge-case-1)
(test-fix edge-case-2)

;; 4. Delegate file edit and reload
```

## Debugging a Failing Test

```clojure
(in-ns 'my.namespace-test)

;; 1. Run the failing test
(clojure.test/test-vars [#'failing-test])

;; 2. Extract and recreate test data
(def test-input {:id 123 :name "test"})

;; 3. Run the function under test
(my/process-data test-input)
;; => Unexpected result!

;; 4. Debug step by step through the pipeline
(-> test-input
    (my/validate)     ; Check each step
    (my/transform)    ; Find where it breaks
    (my/save))

;; 5. Develop and verify fix
(defn process-data-fixed [data]
  ;; Fixed implementation
  )
(process-data-fixed test-input)
;; => Expected result!
```

## Safe Refactoring

```clojure
(in-ns 'my.namespace)

;; 1. Capture current behavior
(def test-cases [{:input 1 :expected 2}
                 {:input 5 :expected 10}
                 {:input -1 :expected 0}])
(def current-results
  (map #(original-fn (:input %)) test-cases))

;; 2. Develop new version incrementally
(defn my-fn-v2 [x]
  (* x 2))

;; 3. Compare results
(def new-results
  (map #(my-fn-v2 (:input %)) test-cases))
(= current-results new-results)
;; => true (refactoring is safe!)

;; 4. Check edge cases
(= (original-fn nil) (my-fn-v2 nil))
(= (original-fn []) (my-fn-v2 []))

;; 5. Performance comparison
(time (dotimes [_ 10000] (original-fn 42)))
(time (dotimes [_ 10000] (my-fn-v2 42)))
```

## REPL-First TDD

Iterate with real data before editing files:

```clojure
(def sample-text "line 1\nline 2\nline 3\nline 4\nline 5")

(defn format-line-number [n padding marker-len]
  (let [num-str (str n)
        total-padding (- padding marker-len)]
    (str (apply str (repeat (- total-padding (count num-str)) " "))
         num-str)))

(deftest line-number-formatting
  (is (= "  5" (editor-util/format-line-number 5 3 0))
      "Single digit with padding 3, no marker space")
  (is (= " 42" (editor-util/format-line-number 42 3 0))
      "Double digit with padding 3, no marker space"))
```

Benefits: verified behavior before committing, incremental development with immediate feedback, tests that capture known-good behavior, failing tests lock in intent before implementation.

## Incremental Function Development

Break complex functions into named, testable helpers:

1. Define and test individual helper functions in the REPL (consider TDD)
2. Compose them into the final function
3. Verify each step before proceeding
