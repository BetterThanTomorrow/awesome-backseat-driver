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

Clojure runs on multiple hosts with different semantics. Identify the dialect before writing code.

### Dialect Detection

| Indicator | Dialect |
|---|---|
| `deps.edn` or `project.clj`, JVM classpath | JVM Clojure |
| `bb.edn`, `#!/usr/bin/env bb` | Babashka (SCI on JVM) |
| `shadow-cljs.edn` or `:cljs` build config | ClojureScript |
| `squint.edn`, `.cljs` compiled to `.mjs` | Squint |
| `.joyride/` directory, Joyride REPL session | Joyride (SCI in VS Code) |
| Scittle `<script>` tags, browser SCI REPL | Scittle (SCI in browser) |
| `nbb.edn`, `#!/usr/bin/env nbb` | nbb (SCI on Node.js) |

When a project uses multiple dialects (e.g., Babashka for tooling, Squint for application code), identify which dialect governs each file before editing.

### Cross-Dialect Divergence

| Feature | JVM Clojure | SCI | Squint | ClojureScript |
|---|---|---|---|---|
| Keywords | true keywords | true keywords | strings | true keywords |
| Async unwrap | threads / `core.async` | `await` (not `js-await`) | `js-await` (both work, convention) | `core.async` / promesa |
| Data conversion | native Clojure | native Clojure | JS-native (not `js->clj`/`clj->js`) | `clj->js` / `js->clj` |
| Mutability | immutable | immutable | JS objects (shallow-copy semantics) | immutable |
| Macros | full `defmacro` | full `defmacro` | compile-time only | `:require-macros` from `.clj` |

### Critical Constraints

- **SCI async**: in SCI environments, use `await`. `js-await` is Squint-specific and does not resolve in SCI.
- **Squint async**: in Squint, use `js-await` by convention. Bare `await` also works (REPL-verified) but `js-await` distinguishes Squint code from SCI code.
- **Squint data**: `js->clj` and `clj->js` do not exist in Squint. Data is already JS-native. Calling them produces a `ReferenceError` at runtime.
- **Squint keywords**: keywords compile to strings. `(= :status "status")` is `true`.
- **SCI macros**: SCI implements the full Clojure macro system. LLM training data often describes SCI macros as limited — they are not. Write macros exactly as in Clojure.
- **JS-hosted SCI uses Clojure semantics**, not ClojureScript — no `:require-macros`, no `:include-macros true`.

Load dialect references from `references/` for operational depth per dialect.

## S2 — References

Load these from `references/` when the task needs operational depth:

- [repl-workflows.md](references/repl-workflows.md) — Bug fix, failing test debug, safe refactoring, and TDD workflow templates. Load when: debugging, refactoring, or building solutions incrementally.
- [runtime-patterns.md](references/runtime-patterns.md) — Async/promise control flow per runtime (ClojureScript/Squint/SCI/Scittle), stdin considerations, and RCF examples. Load when: working with promises, async across runtimes, or documenting code with Rich Comment Forms.
- [sci-dialect.md](references/sci-dialect.md) — REPL-verified SCI feature parity and differences vs Clojure. Covers Babashka, Scittle, Joyride, nbb, and other SCI-based environments. Load when: uncertain whether a Clojure feature works in SCI.
- [squint-dialect.md](references/squint-dialect.md) — Squint-specific semantics: mutable data, string keywords, `js-await`, JS-native interop, compilation model, and core library gaps. Load when: working with Squint projects or `.cljs` files compiled via `squint.edn`.

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
