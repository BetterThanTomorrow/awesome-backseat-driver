---
name: squint
description: "Squint ClojureScript development — writing Squint code, compilation, REPL workflow, debugging, and tooling. Use when: working with Squint projects, planning, writing, or reviewing Squint code, .cljs files compiled to .mjs, Squint REPL sessions, debugging compiled output, checking whether clojure.core functions exist in Squint, or setting up Squint builds. Use whenever you are using Backseat Driver tools with Squint."
---

# Squint Skill

Squint is a ClojureScript-syntax compiler that emits ES module JavaScript. It is NOT standard ClojureScript (shadow-cljs) and NOT SCI — it has fundamentally different semantics for data structures, keywords, and async. The Clojure skill's S3 Dialects section provides cross-dialect comparison; this skill provides full Squint operational depth.

## Compilation Model

Squint compiles `.cljs` files to ES modules (`.mjs`) via configuration in `squint.edn`:

```clojure
{:paths ["src"]
 :output-dir "lib"
 :extension "mjs"}
```

Key implications:
- `require` compiles to ES `import` — circular dependencies cause the same issues as in JS
- Unrecognized symbols emit bare JavaScript calls — silent at compile time, crashes at runtime
- This is the most common source of `ReferenceError: X is not defined` errors
- Always check compilation output when a runtime error seems impossible from the source

## REPL and Development Workflow

Use `clojure_list_sessions` to find the Squint REPL session (typically `squint-repl`). Test pure functions and verify assumptions in the REPL before committing to files.

Development cycle:
1. Squint watch (`npx squint watch` or project-specific watcher) for continuous compilation
2. REPL for incremental development of pure functions
3. Compilation check for single files when needed
4. Check watcher output after edits — zero-warnings baseline

## Language Semantics

### Keywords ARE Strings

Squint compiles keywords to plain JavaScript strings:

- `:foo` → `"foo"`, `:foo/bar` → `"foo/bar"`
- `(= :loaded "loaded")` → `true` — they are the same value
- `str` works naturally: `(str "prefix-" :status)` → `"prefix-status"`
- `keyword?` does not exist — there is no distinct keyword type to test for

### JS-Native Data Structures

Maps are JS objects, vectors are JS arrays, sets are JS Sets. No persistent data structures:

- `assoc`, `conj`, `update` return shallow copies — the original is not mutated (REPL-verified)
- Nested objects are shared — deep mutations on nested values affect both original and copy
- `(= a b)` on maps/vectors is reference equality, not structural
- `assoc!` does NOT exist — use `assoc`

### Async: `^:async` + `js-await`

- Mark functions with `^:async` metadata — they return Promises
- Both `js-await` and bare `await` work (REPL-verified). Convention: use `js-await` in Squint code for clarity, since bare `await` is the SCI convention.
- No top-level `js-await` — must be inside an `^:async` function
- `doseq` does NOT properly await `js-await` calls — iterations fire concurrently
- Use `loop`/`recur` for sequential async iteration

```clojure
(defn ^:async process-items! [items]
  (loop [remaining items]
    (when (seq remaining)
      (js-await (handle-item! (first remaining)))
      (recur (rest remaining)))))
```

### No `js->clj` / `clj->js`

These functions do not exist. They compile to unqualified bare calls (`js__GT_clj()`) that crash at runtime with `ReferenceError`. Data is already JS-native — no conversion needed.

### `nil` is `null`

Squint compiles `nil` to JavaScript `null` (REPL-verified):

- `nil?` returns `true` for both `null` and `undefined` (uses JS loose equality)
- `identical?` distinguishes them: `(identical? nil js/undefined)` → `false`
- APIs that return `undefined` test as `nil?` but are not `identical?` to `nil`

## Function Availability

Several `clojure.core` functions are missing or behave differently in Squint:

| Function | Status | Workaround |
|---|---|---|
| `name` | does not exist | keywords are already strings — use directly |
| `keyword` | does not exist | use the string directly |
| `keyword?` | does not exist | no distinct keyword type |
| `sequential?` | `true` for strings | use `vector?` to distinguish |
| `js->clj` / `clj->js` | do not exist | data is already JS-native |

Set callability: inline set literals work (`(#{:a :b} :a)` → `"a"`) but sets bound to vars are not callable (REPL-verified). Use `(contains? s :a)` for consistent behavior.

No auto-resolved keywords — `::my-key` is a compiler error. Spell out the namespace.

### Checking Whether a Function Exists

When uncertain, query the Squint runtime directly:

```bash
node -e "import('squint-cljs/core.js').then(m => console.log(typeof m.someFn))"
```

`undefined` means the function does not exist. The Squint source at `src/squint/core.js` in the [Squint repo](https://github.com/squint-cljs/squint) is the definitive reference for available core functions.

When Squint does not recognize a symbol, it emits an unqualified JS call. The compiler does not error — but the generated code crashes at runtime. `refer` for missing functions silently succeeds but does not make them available.

## Property Access and Interop

- `(.-my-prop obj)` converts hyphens to underscores → `obj.my_prop`
- Use `(aget obj "my-prop")` for literal property names with hyphens
- `(:key obj)` works (compiles to string lookup on JS object)
- Bare namespace requires fail — always use vector form: `(:require [lib :as alias])`
- `js/` prefix works for global access: `js/document`, `js/console.log`, `js/fetch`

## Idiomatic Style

Prefer idiomatic Clojure functions over JavaScript interop when an equivalent exists. Squint supports most of `clojure.string` and the core library:

```clojure
;; Prefer
(string/includes? s "needle")
(string/replace s #"old" "new")
(mapv transform items)

;; Over
(.includes s "needle")
(.replace s "old" "new")
(.map items transform)
```

JS interop is appropriate for host-specific APIs (DOM, Node.js, browser APIs) where no Clojure equivalent exists.

## Debugging Compiled Output

When a runtime error does not match the source:

1. Read the compiled `.mjs` file to see what Squint generated
2. Search for unqualified function calls (e.g., `name(...)` instead of `squint_core.name(...)`)
3. Trace `ReferenceError: X is not defined` back to missing core functions
4. Check that the intended Clojure function is actually available in Squint's core.js

## Invariants

- `js-await` by convention in Squint code (distinguishes from SCI's `await`)
- Never use `js->clj`/`clj->js` — data is already JS-native
- Check compiled output on mysterious runtime errors — the compiler does not catch missing functions
- `contains?` for set membership — do not call sets as functions from variables
- Prefer Clojure functions over JS interop when both are available
- Zero compilation warnings — address root causes
