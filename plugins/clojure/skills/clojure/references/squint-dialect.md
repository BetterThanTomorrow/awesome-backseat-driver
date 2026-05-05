# Squint Dialect Reference

Squint is a ClojureScript-syntax compiler that emits ES module JavaScript. It is NOT standard ClojureScript (shadow-cljs) and NOT SCI — it has fundamentally different semantics for data structures, keywords, and async.

## Keywords ARE Strings

Squint compiles keywords to plain JavaScript strings:

- `:foo` → `"foo"`, `:foo/bar` → `"foo/bar"`
- `(= :loaded "loaded")` → `true` — they are the same value
- No `name` function needed (or available) — keywords are their string representation
- `str` works naturally: `(str "prefix-" :status)` → `"prefix-status"`
- `keyword?` does not exist — there is no distinct keyword type to test for
- Idiomatic Clojure code that treats keywords as opaque identifiers works fine; code that relies on keyword identity or type does not

## JS-Native Data Structures

Maps are JS objects, vectors are JS arrays, sets are JS Sets. No persistent data structures:

- `assoc`, `conj`, `update` return shallow copies — the original is not mutated (REPL-verified)
- However, nested objects are shared — deep mutations on nested values affect both original and copy
- `(= a b)` on maps/vectors is reference equality, not structural — use caution in tests
- `assoc!` does NOT exist — use `assoc`

## Async: `^:async` + `js-await`

- Mark functions with `^:async` metadata — they return Promises
- Both `js-await` and bare `await` work in Squint (REPL-verified). Convention: use `js-await` in Squint code for clarity, since bare `await` is the SCI convention.
- No top-level `js-await` — must be inside an `^:async` function
- `doseq` does NOT properly await `js-await` calls — iterations fire concurrently
- Use `loop`/`recur` for sequential async iteration
- `js/Promise.all` works for intentionally parallel execution

## No `js->clj` / `clj->js`

These functions do not exist in Squint. They compile to unqualified bare calls (`js__GT_clj()`) that crash at runtime with `ReferenceError`:

- Data is already JS-native — no conversion needed
- Keyword maps are already string-keyed JS objects
- Access properties with `get`, `aget`, or `(.-prop obj)` directly

## `nil` is `null`

Squint compiles `nil` to JavaScript `null` (REPL-verified: `nil === null` is `true`, `nil === undefined` is `false`):

- `nil?` returns `true` for both `null` and `undefined` (uses JS loose equality)
- `identical?` distinguishes them: `(identical? nil js/undefined)` → `false`
- APIs that return `undefined` (e.g., missing object properties) test as `nil?` but are not `identical?` to `nil`

## Core Library Gaps

Several `clojure.core` functions are missing or behave differently:

- `name` — does not exist. Keywords are already strings; use direct string operations
- `sequential?` — returns `true` for strings (they are JS arrays of chars). Use `vector?` to distinguish
- Set literals are callable inline — `(#{:a :b} :a)` works — but sets bound to vars are not callable (REPL-verified). Use `(contains? s :a)` for consistent behavior.
- No auto-resolved keywords — `::my-key` is a compiler error. Spell out the namespace
- `count` works on arrays and strings but not on all collection types uniformly
- `keyword` function does not exist — use the string directly

## Property Access and Interop

- `(.-my-prop obj)` converts hyphens to underscores → `obj.my_prop`
- Use `(aget obj "my-prop")` for literal property names with hyphens
- `(:key obj)` works (compiles to string lookup on JS object)
- Bare namespace requires fail — always use vector form: `(:require [lib :as alias])`
- `js/` prefix works for global access: `js/document`, `js/console.log`, `js/fetch`

## Compilation Model

Squint compiles `.cljs` files to ES modules (`.mjs`). Key implications:

- Unrecognized symbols emit bare JavaScript calls — silent at compile time, crashes at runtime
- This is the most common source of `ReferenceError: X is not defined` errors
- Always check compilation output when a runtime error seems impossible from the source
- `require` compiles to ES `import` — circular dependencies cause the same issues as in JS
