---
name: clojure
description: 'Clojure development skill — any dialect, any runtime. Use for all Clojure work, regardless of dialect or runtime: planning, reading, reviewing, designing, coding, debugging, testing, refactoring. Use whenever you are using Backseat Driver tools.'
---

# Clojure Skill

Organized using the Viable System Model (VSM). The core skill contains identity (S5), adaptation rules (S4), coordination policies (S3), and routing (S2). Operational recipes live in `references/` and should be loaded on demand.

## S5 — Identity

Data-oriented, REPL-first development for Clojure, ClojureScript, and Babashka. Pure functions transform data; side effects live at system edges. The REPL is the development environment — verify assumptions, test feasibility, and explore APIs regardless of role.

## S4 — REPL-First Development

Before any file modification: read → test → develop in REPL → verify → apply. Use inline `def` for debugging over `println` — inline bindings keep intermediate state inspectable.

```clojure
(defn process-instructions [instructions]
  (def instructions instructions)
  (let [grouped (group-by :status instructions)]
    grouped))
```

Delegate file edits to a Clojure editor subagent when available — protects your context.

## S3 — Coding Conventions

### Requiring and Aliasing

Prefer aliasing over referring, except for common test macros. Alias `clojure.string` as `string`.

### Docstrings

Immediately after function name, before argument vector. State responsibility first.

```clojure
(defn append-memory-section
  "Appends a new memory section to the specified file.
   Ensures consistent spacing for readability."
  [file-path section]
  ;; implementation
  )
```

### Definition Order

Define before use. Factor across namespaces to break circular dependencies rather than using `declare`.

### Indentation

Condition and body on separate lines. `and`/`or` arguments on separate lines:

```clojure
(if (and condition-a
         condition-b)
  this
  that)
```

### Function Naming

Respect existing names — rename only when semantic purpose fundamentally changed. No `V2`/`Enhanced`/`Improved` suffixes. Comments for unclear business logic only, not code evolution.

## S3 — Function Design

Each function does one thing well and returns a useful value. Compose small, focused functions.

- Actual values over boolean flags — track *what* not *whether*
- `data_in → data_out` — functions transform data
- Short functions: extract named helper when scrolling is needed
- Maximum 2-3 nesting levels; flatten via extraction, early return, or threading
- `cond`/`condp` over nested `if`; `when` guards over else branches

### Conditionals

- Binary → `if`; multiple → `cond`
- Bind-and-test → `if-let`/`when-let` instead of `(let [x ...] (if x ...))`
- Early exit → `when` with guard first

### Threading

`->` for subject-first, `->>` for collection-last. `some->`/`some->>` for nil-safe threading. Threading is for readability — no single-step threading, no threading side effects.

### Direct Parameter Usage

Use parameters directly. Intermediate bindings only when transforming or improving readability. Do not wrap core functions unless a name genuinely clarifies composition.

## S3 — Data Modeling

Flat structures with namespaced keywords, optimized for destructuring. Domain-qualified keys: self-documenting, collision-free, extensible.

```clojure
(defn handle-user-request
  [{:user/keys [id name email]
    :request/keys [method path headers]
    :config/keys [timeout debug?]}]
  (when debug?
    (println "Processing" method path "for" name)))
```

### Avoid Shadowing Built-ins

Rename incoming keys to avoid hiding core functions. Keep free: `count`, `filter`, `first`, `get`, `key`, `map`, `merge`, `name`, `reduce`, `set`, `str`, `type`, `update`.

### Options Maps

Prefer keyword destructuring varargs (`& {:keys [...]}`) over multi-arity for optional parameters. Accepts both keyword args and a single map (Clojure 1.11+):

```clojure
(defn fetch [url & {:keys [timeout retries] :or {timeout 5000 retries 3}}]
  ...)

(fetch "https://example.com" :timeout 10000)
(fetch "https://example.com" {:timeout 10000})
```

### Map Access and Preservation

Keywords as functions with defaults: `(:some-key some-map default-value)`. Destructure only keys used directly; preserve full map with `:as`. Set defaults at entry points — boundaries validate, internals trust.

### Context Passing

Explicit context maps with namespaced keys over dynamic vars, global atoms, or implicit state. Build at system edges, thread through the call stack. Flat and mergeable.

### State Management

Centralized `app-db` atom with namespaced keywords over scattered `defonce` atoms (when not using a unidirectional-flow event system).

## S3 — Composition and Side Effects

Side-effecting functions return rich summaries so callers can verify, test, and compose.

```clojure
(defn copy-files!
  [file-list target-dir]
  (let [results (for [file file-list]
                  (try
                    {:file (:name file) :status :copied
                     :target (path/join target-dir (:name file))}
                    (catch Exception e
                      {:file (:name file) :status :failed
                       :error (ex-message e)})))]
    {:total-files (count file-list)
     :successful (count (filter #(= (:status %) :copied) results))
     :failed (count (filter #(= (:status %) :failed) results))
     :details results}))
```

## S3 — Error Handling

Errors are data. `ex-info`/`ex-data` for rich context over string messages.

- Propagate over catch-and-ignore — flow until a meaningful boundary
- Catch at boundaries — HTTP handlers, CLI entry points, user-facing interfaces
- Fail fast and clearly — surface problems immediately
- Configuration errors demand clear failures — `(or value fallback)` hides broken config

## S3 — Abstractions

Earned through repeated need. Multimethods for data-shape dispatch; protocols for type-based polymorphism; spec for boundary contracts. Simplicity over abstraction.

## S3 — Namespace Organization

Group by domain, not by type. Acyclic dependency direction — `require` is an explicit contract. Compose behavior externally when core needs optional enhancement from a dependent module.

## S3 — Rich Comment Forms

`(comment ...)` documents validated REPL work. Not evaluated on load. Use after REPL validation for usage documentation and exploration preservation.

## S3 — Testing

Reload and test from the REPL for immediate feedback:

```clojure
(require '[my.project.some-test] :reload)
(clojure.test/run-tests 'my.project.some-test)
```

Break complex functions into named, testable helpers. Define and test individually, then compose.

## S4 — Problem-Solving Heuristics

1. **Read the error message** — often contains the exact issue
2. **Trust established libraries** — check your usage first
3. **Check framework constraints** — specific requirements exist
4. **Occam's razor** — simplest explanation first

Stay focused on the specific problem. No unnecessary checks or unrelated suggestions.

## S3 — Dialects

Clojure runs on multiple hosts. SCI (Small Clojure Interpreter) powers Babashka, Scittle, Joyride, nbb, and Epupp. Key orientation points:

- **SCI macros have full Clojure fidelity** — `defmacro` with syntax-quote, gensyms, `binding`, `try/finally`, nested expansion — all work exactly as in Clojure. No special handling needed.
- **JS-hosted SCI uses Clojure semantics**, not ClojureScript — no `:require-macros`, no `:include-macros true`, keywords are true keywords (not strings).
- **`await` vs `js-await`** — SCI uses `await`; Squint uses `js-await`. They are not interchangeable.

Load the dialect reference when working in SCI-based environments or when uncertain about feature parity.

## S2 — References

Load these from `references/` when the task needs operational depth:

- [repl-workflows.md](references/repl-workflows.md) — Bug fix, failing test debug, safe refactoring, and TDD workflow templates. Load when: debugging, refactoring, or building solutions incrementally.
- [runtime-patterns.md](references/runtime-patterns.md) — Async/promise control flow per runtime (ClojureScript/Squint/SCI/Scittle/Epupp), stdin considerations, and RCF examples. Load when: working with promises, async across runtimes, or documenting code with Rich Comment Forms.
- [sci-dialect.md](references/sci-dialect.md) — REPL-verified SCI feature parity and differences vs Clojure. Load when: working with Babashka, Scittle, Joyride, nbb, or Epupp, or when uncertain whether a Clojure feature works in SCI.

## S5 — Invariants

- REPL verification precedes file modification
- Data orientation: pure functions transform data; side effects at system edges
- Destructure at function boundaries — parameters carry context, not positional slots
- Namespaced keywords identify data across boundaries — flat structures over nested maps
- Conditional selection matches the decision shape: `if` for binary, `cond` for multiple, `when-let`/`if-let` for bind-and-test
- Errors are data: `ex-info`/`ex-data` with rich context; propagate rather than catch-and-ignore
- Abstractions are earned: multimethods and protocols appear when repeated need demands them
- Threading macros show data flow — extract named helpers when functions require scrolling
- Definition order: define before use; factor across namespaces to break cycles
- Root-cause focus: fix the actual issue; workarounds and fallback values that mask failures are not solutions
- When tests are available, verify they pass after changes; when creating new behavior, consider encoding expectations as tests
- Zero compilation warnings — address root causes; clean builds are the baseline
