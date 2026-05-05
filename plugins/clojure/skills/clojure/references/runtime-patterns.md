# Runtime-Specific Patterns

Patterns that vary by Clojure runtime or require extended examples beyond the core skill.

## Async Control Flow with Promises

### ClojureScript (promesa)

Avoid synchronous `loop/recur` wrapping promises. Use promise-aware structures:

```clojure
;; p/loop for straightforward iteration
(p/loop [items remaining-items]
  (if (empty? items)
    "Done!"
    (p/let [_ (async-operation (first items))]
      (p/recur (rest items)))))

;; p/reduce when folding over items sequentially
(p/reduce (fn [acc item]
            (p/let [result (async-operation item)]
              (conj acc result)))
          []
          remaining-items)
```

### Squint

Mark functions `^:async`, unwrap promises with `js-await`. Use `loop/recur` for sequential async — `doseq` does not properly await `js-await` calls.

```clojure
(defn ^:async inject-sequentially! [tab-id files]
  (loop [remaining files]
    (when (seq remaining)
      (let [response (js-await (process-item! (first remaining)))]
        (recur (rest remaining))))))
```

### SCI (Scittle/Epupp, Joyride, nbb)

Mark functions `^:async`, unwrap promises with `await` (bare — NOT `js-await`). `doseq` with `await` works in SCI.

```clojure
(defn ^:async fetch-data! [url]
  (let [response (await (js/fetch url))
        data (await (.json response))]
    (js->clj data :keywordize-keys true)))
```

### Critical: `await` vs `js-await` are not interchangeable

- `js-await` in SCI → "Unable to resolve symbol: js-await"
- `await` in Squint → does not unwrap promises
- Identify the runtime before writing async code

## Reading from stdin

- Clojure `(read-line)` prompts the user through VS Code
- Babashka nREPL lacks stdin support — avoid `read-line` there
- Ask the user to restart the REPL if it blocks on stdin

## Rich Comment Form Examples

RCFs document validated REPL work in files. They are not evaluated on load.

```clojure
(defn process-user-data
  "Processes user data with validation"
  [{:user/keys [name email] :as user-data}]
  ;; implementation
  )

(comment
  ;; Basic usage
  (process-user-data {:user/name "John" :user/email "john@example.com"})

  ;; Edge case - missing email
  (process-user-data {:user/name "Jane"})

  ;; Integration example
  (->> users
       (map process-user-data)
       (filter :valid?))

  :rcf)
```

## SQL Result Keys with Spaces

SQL column names with spaces become keywords with spaces in next.jdbc results. Access with `get` and dynamically constructed keywords:

```clojure
(get result (keyword "Create Table"))
```
