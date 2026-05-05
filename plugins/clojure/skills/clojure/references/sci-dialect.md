# SCI Dialect Reference

SCI (Small Clojure Interpreter) powers Babashka, Scittle, Joyride, nbb, and other embedded Clojure environments. This reference documents REPL-verified behaviors — what matches Clojure, what differs, and common misconceptions.

## Macro System — Full Clojure Fidelity

SCI implements the Clojure macro system, not a subset. `defmacro` works with complete fidelity:

- Syntax-quote, unquote, unquote-splicing (`~@`)
- Gensyms (auto `#` and manual `gensym`)
- `&body` / `&form` destructuring
- Macros that emit `binding`, `try/finally`, `let`, `loop/recur`
- Nested macro expansion
- Macros defined at runtime (in the REPL) are immediately available

```clojure
;; Complex macro with bindings, try/finally, code generation — works in SCI
(defmacro with-resource [binding & body]
  `(let [resource# ~(second binding)]
     (binding [~(first binding) resource#]
       (try
         ~@body
         (finally
           (cleanup! resource#))))))
```

This is a common misconception in LLM training data: SCI macros are frequently described as limited or needing special handling. They do not. Write macros exactly as you would for Clojure.

## What Matches Clojure

These features work identically to Clojure — no special handling required:

- **Dynamic vars and `binding`** — `^:dynamic` vars, `binding` forms, nested bindings, thread-local semantics
- **Multimethods** — `defmulti`, `defmethod`, hierarchies, `:default` dispatch
- **Protocols and records** — `defprotocol`, `defrecord`, `extend-type`, `extend-protocol`
- **Atoms** — `atom`, `swap!`, `reset!`, `add-watch`, `compare-and-set!`
- **Destructuring** — all forms: sequential, associative, `& rest`, `:keys`, `:as`, nested
- **Metadata** — `^:keyword`, `with-meta`, `meta`, `vary-meta`
- **Namespaces** — `ns`, `require`, `refer`, `alias`, `in-ns`
- **Sets as functions** — `(#{:a :b} :a)` → `:a` — sets are callable, unlike in Squint
- **Error handling** — `try`/`catch`/`finally`, `ex-info`/`ex-data`, `throw`
- **Core library** — most of `clojure.core`, `clojure.string`, `clojure.set`, `clojure.walk`, `clojure.edn`

## JS-Hosted SCI: Clojure Semantics, Not ClojureScript

SCI on JavaScript runtimes (Scittle, Joyride, nbb) uses **Clojure semantics** with JS interop — not ClojureScript semantics:

- **Macros** follow Clojure, not ClojureScript (no separate macro namespace, no `:require-macros`)
- **Keywords** are true Clojure keywords (not strings as in Squint)
- **Require** uses Clojure `(:require [ns :as alias])` — no `:include-macros true` needed
- **JS interop** uses `js/` prefix: `js/document`, `js/console.log`, `js/Promise`

## Async in SCI

- Mark functions `^:async` — they return Promises
- Unwrap with `await` (bare — NOT `js-await`, that's Squint-specific)
- No top-level `await` — must be inside an `^:async` function
- `doseq` with `await` works (unlike Squint)

See [runtime-patterns.md](runtime-patterns.md) for detailed async patterns per runtime.

## What Differs from Clojure

Genuine differences — these require adaptation:

- **No Java interop** — no `import`, no Java classes, no `.methodName` on Java objects (Babashka has its own subset of Java interop)
- **No `deftype`** — use `defrecord` or protocol extension instead
- **No `gen-class`** — JVM compilation concept, not applicable
- **No lazy-seq caching guarantees** — sequences work but internal caching behavior may differ
- **No `eval` in the host language sense** — `sci/eval-string` is the evaluation mechanism
- **Limited `refer-clojure`** — `:exclude` works, but not all options
- **No reader conditionals in interpreted code** — `.cljc` reader conditionals are resolved at SCI build time, not interpretation time
- **Namespace availability** — only namespaces registered in the SCI context are available; you cannot `require` arbitrary libraries

## Babashka-Specific

- Subset of Java interop (common classes: `java.io.File`, `java.time.*`, etc.)
- `babashka.fs`, `babashka.process`, `babashka.http-client` — Babashka-specific libraries
- `bb.edn` for task configuration
- Pods for extending functionality

## Scittle-Specific

- Runs in browser — full DOM access via `js/` interop
- Available namespaces depend on which Scittle plugins are loaded
- `sci.core` namespace available for meta-programming (eval within eval)

## nbb-Specific

- SCI running on Node.js — full Node.js API access via `js/` interop
- Can `require` npm modules directly: `(require '["fs" :as fs])`
- `nbb.core/load-file` and `nbb.core/load-string` for dynamic loading
- Uses `await` (SCI-style `^:async` + `await`, not Squint's `js-await`)
- `js/process`, `js/require`, `js/console` — standard Node.js globals available
- `package.json` dependencies are accessible after `npm install`
- Reader conditional feature: `:org.babashka/nbb`
- REPL: `npx nbb nrepl-server` starts an nREPL server

## Joyride-Specific

- SCI running inside VS Code's extension host (Node.js context)
- Full VS Code API access: `(require '["vscode" :as vscode])` — ES module-style require
- `process.cwd()` is undefined — use `vscode/workspace.workspaceFolders` for paths
- Two script scopes: User (`~/.config/joyride/`) and Workspace (`.joyride/`)
- Activation scripts: `user_activate.cljs` / `workspace_activate.cljs` run on startup
- Flares: keyboard-triggered evaluations of tagged forms in scripts
- Uses `await` (SCI-style `^:async` + `await`)
- Can register commands, keybindings, and disposables via VS Code API
