---
name: babashka
description: "Write idiomatic Babashka (bb) scripts and modules. Covers babashka.fs, babashka.process, babashka.cli, babashka.http-client, and built-in namespaces. Use when: writing bb scripts, creating or modifying a task, REPL-driven Babashka development, editing .clj files in directories with bb.edn or scripts/ folders, using Backset Driver tools with Babashka."
---

# Babashka

Babashka is a fast-starting Clojure interpreter for scripting, powered by SCI. It has the full Clojure macro system (`defmacro`, syntax-quote, gensyms, `binding`, `try/finally`) — identical to Clojure, no limitations. For comprehensive SCI feature parity details, load `references/sci-dialect.md` from the Clojure skill.

Prefer Babashka built-in namespaces over shell commands and external tools.

## Principles

- Pure functions at the core, side effects at the edges
- Define functions before use - no forward declares
- Never shadow built-in names (`count`, `name`, `filter`, `run!`, etc.)
- Prefer babashka namespace over shelling and avoid reimplementing things that babashka already provides
- Examples:
  - Prefer `babashka.fs` over shell file operations
  - Prefer `babashka.http-client` over curl/wget
  - Prefer `babashka.http-server` over `python -m http.server`
  - But they are just examples! See [references/namespaces.md](references/namespaces.md) for more built-ins and patterns.

## REPL-Driven Development

**Planning is development.** The REPL gate and exploration steps below apply whether you are implementing code, creating a plan document, or reviewing architecture. Use the REPL to test assumptions about APIs (`fs/glob` patterns, `diff` exit codes, JSON parsing) during planning — don't defer all exploration to the implementer.

All REPL-first patterns from the **Clojure skill** apply to Babashka: read → test → develop in REPL → verify → apply. If the Clojure skill is available, follow its S4 (REPL-First Development) and S3 (Coding Conventions) sections.

### REPL gate

Before writing or planning Babashka code, establish a REPL connection to the `bb` session:

1. Use `clojure_list_sessions` to look for a session named `bb`
2. If found: use it for all exploration, validation, and incremental development
3. If not found: **ask the user to jack in a Babashka REPL** (Calva jack-in with Babashka project type) — the `bb` session is essential for effective development

### REPL-loadable scripts

Scripts must be safe to `(require '[my-module] :reload)` without triggering side effects:

- No top-level I/O (`println`, `spit`, `shell`, HTTP calls) outside a `defn`
- No top-level `System/exit` (kills the REPL)
- No top-level `def`s with side effects (e.g. `(def files (fs/glob "."))`)

All behavior belongs inside functions.

### Script gate for standalone entry points

Some scripts run as standalone commands while remaining REPL-loadable. Use the `*file*` / `babashka.file` gate - Babashka's equivalent of Python's `if __name__ == "__main__"`:

```clojure
(ns my-script
  (:require [babashka.fs :as fs]))

(defn main! [args]
  (println "Running with" args))

;; Only fires when run as a script, not when loaded via require
(when (= *file* (System/getProperty "babashka.file"))
  (main! *command-line-args*))
```

### Data-oriented solutions

Design as pure data transformations. Three layers:

1. **Gather** (impure but safe): read the environment - globs, config, env vars. Capture as data.
2. **Transform** (pure): filter, validate, build the plan. Iterate freely in the REPL.
3. **Act** (impure, destructive): delete, write, shell out. Receives the plan as an argument.

```clojure
;; Gather
(defn gather-candidates [dir patterns]
  (->> patterns
       (mapcat #(fs/glob dir %))
       (mapv (fn [f] {:path (str f) :size (fs/size f)}))))

;; Transform (pure - test freely in the REPL)
(defn filter-plan [candidates min-size]
  (filterv #(> (:size %) min-size) candidates))

;; Act (thin, receives fully-formed plan)
(defn execute! [plan]
  (doseq [{:keys [path]} plan]
    (fs/delete path)
    (println "Deleted" path)))
```

The plan IS the dry run. Print it, filter it, count it before acting.

### REPL safety with side effects

Babashka scripts often touch the file system, spawn processes, and hit the network. When exploring side-effecting code in the REPL:

- **Use `fs/with-temp-dir`** for file operations - never test writes/deletes against real data
- **Point at test fixtures** or copies, not production directories
- **Evaluate the gather and transform layers freely** - they're pure data
- **Only call the act layer** once you've inspected the plan and confirmed the target is safe
- **Never evaluate `System/exit`** in the REPL - it kills the process

```clojure
;; Safe REPL exploration: temp dir for destructive operations
(fs/with-temp-dir [tmp {}]
  (spit (fs/file tmp "test.txt") "hello")
  (let [candidates (gather-candidates tmp ["**/*.txt"])
        plan (filter-plan candidates 0)]
    (execute! plan)
    (println "Files remaining:" (count (fs/glob tmp "**/*")))))
```

## Script Dependencies

For scripts requiring libraries, use script-adjacent `bb.edn` (v1.3.177+):
```clojure
;; my-script.clj location: ~/bin/my-script.clj
;; bb.edn location: ~/bin/bb.edn
{:deps {medley/medley {:mvn/version "1.3.0"}}}
```
Babashka automatically finds and uses the adjacent `bb.edn` when running the script from anywhere.

## shell vs process

| Need | Use |
|---|---|
| Run command, inherit I/O, fail on error | `(p/shell "cmd" "arg1" "arg2")` |
| Capture output for parsing | `(p/shell {:out :string} "cmd")` |
| Long-running background process | `(p/process ["cmd" "args"])` |
| Suppress errors, check exit code | `(p/shell {:continue true} "cmd")` |

**`shell`**: inherits I/O, throws on non-zero exit, tokenizes first string argument only.

**`process`**: captures streams to buffers, never throws, returns immediately.

**Tokenization**: only the FIRST string to `shell` is tokenized:

```clojure
(p/shell "npm install" "-g" "nbb")     ;; correct: 3 args
(p/shell "npm install" "-g nbb")       ;; WRONG: "-g nbb" is one arg
```

**`$` macro**: `(-> (p/$ ls -la) :out slurp)`

**Deadlock warning**: with large inputs and `check`, use `:out :string`.

## CLI argument parsing (babashka.cli)

```clojure
;; Basic parsing with coercion
(cli/parse-opts *command-line-args*
                {:coerce {:port :int :verbose :boolean}
                 :alias {:p :port :v :verbose}})

;; Separating flags from positional args
;; Use -- to separate: bb script.clj --flag -- positional-arg
(let [{:keys [args opts]} (cli/parse-args *command-line-args*
                                          {:coerce {:shards :int}
                                           :alias {:s :serial}})]
  (start! args opts))
```

**Spec format with validation:**

```clojure
(def ^:export cli-spec
  {:port {:coerce :long :alias :p :default 8080 :validate pos?}
   :verbose {:coerce :boolean :alias :v}
   :paths {:coerce [] :default ["./"] :desc "Input paths"}})

(cli/parse-opts ["--port" "3000" "-v" "--paths" "src" "--paths" "test"] {:spec cli-spec})
;; => {:port 3000, :verbose true, :paths ["src" "test"]}

;; Generate help text
(cli/format-opts {:spec cli-spec})
```

- `parse-opts` ignores bare positional args. Use `parse-args` for `{:opts {...} :args [...]}`.
- Auto-coercion: `"true"`/`"false"` become boolean, `"123"` becomes number, `"foo"` stays string.
- `:coerce []` collects multiple values into a vector.

## Resource lifecycle (with-* pattern)

```clojure
(defn with-server
  "Run f with HTTP server, clean up on exit."
  [port dir f]
  (let [stop-fn (server/serve {:port port :dir dir})]
    (try
      (f)
      (finally
        (stop-fn)))))

;; Compose lifecycle wrappers
(defn run-integration! [args]
  (with-server 8080 "test-data"
    #(with-process ["relay" "--port" "9090"]
       (fn []
         (let [result (p/shell {:continue true} "test-runner" args)]
           (System/exit (:exit result)))))))
```

## Error handling

```clojure
;; Exit with error to stderr
(binding [*out* *err*]
  (println "Error:" msg))
(System/exit 1)

;; Or throw with babashka exit code
(throw (ex-info "Failed" {:babashka/exit 1}))
```

## Common mistakes

| Mistake | Correction |
|---|---|
| Shelling out for file ops (`rm`, `cp`, `mkdir`) | Use `babashka.fs` functions |
| Using `curl`/`wget` for HTTP | Use `babashka.http-client` |
| Using `python -m http.server` | Use `babashka.http-server` |
| Reading `@atom` in pure helpers | Pass data as function arguments |
| `(shell "cmd -flag value")` for multi-arg | `(shell "cmd" "-flag" "value")` |
| Missing `:continue true` when checking exit codes | Without it, `shell` throws on non-zero |
| Using `System/exit` inside `with-*` wrappers | Return exit code, call `System/exit` after cleanup |
| Forward declaring functions | Define before use - rearrange file structure |
| Naming functions `count`, `name`, `filter`, etc. | Never shadow clojure.core built-ins |
| Assuming `**` and `*` are the same in globs | `**` is recursive, `*` is single level |
| Using `**/*.ext` expecting root-level matches | `**/*.ext` skips root; use `**.ext` for all levels |
| Top-level side effects (println, shell, System/exit) | Wrap in functions; use `*file*` gate |

## bb.edn Tasks

Define reusable tasks with automatic CLI parsing:

```clojure
{:paths ["scripts"]
 :tasks
 {summarize {:doc "Generate summary report"
             :requires ([babashka.cli :as cli])
             :task (exec 'my-script/main)
             :args->opts [:input :output]}}}
```

Run: `bb summarize input.xml output.txt`

## Namespace reference

For built-in namespaces, shell command replacements, HTTP patterns, addable deps, and extended examples, see [references/namespaces.md](references/namespaces.md).

Key built-ins (no deps required): `babashka.fs`, `babashka.process`, `babashka.cli`, `babashka.http-client`, `babashka.http-server`, `cheshire.core`, `clojure.edn`, `clojure.string`.

### Built-In Libraries
Always available without deps.edn:

- **Data**: `cheshire.core` (json), `clojure.data.csv`, `clojure.data.xml`, `clj-yaml.core`, `cognitect.transit`
- **Core**: `clojure.string`, `clojure.set`, `clojure.walk`, `clojure.edn`, `clojure.pprint`
- **Time**: `java.time.*` package
- **Async**: `clojure.core.async`
- **HTTP**: `org.httpkit.client`, `org.httpkit.server`

## Source Material

- **Babashka Book**: https://book.babashka.org/
- **babashka.fs**: [README](https://github.com/babashka/fs/blob/master/README.md) · [API](https://github.com/babashka/fs/blob/master/API.md)
- **babashka.process**: [README](https://github.com/babashka/process) · [API](https://github.com/babashka/process/blob/master/API.md)
- **babashka.cli**: [README](https://github.com/babashka/cli) · [API](https://github.com/babashka/cli/blob/main/API.md)
- **babashka.http-client**: [README](https://github.com/babashka/http-client) · [API](https://github.com/babashka/http-client/blob/main/API.md)
- **Tasks**: https://book.babashka.org/#tasks
- **Pods**: [docs](https://github.com/babashka/pods) · [registry + examples](https://github.com/babashka/pod-registry)
- **Examples**: [README](https://github.com/babashka/babashka/blob/master/examples/README.md) · [scripts](https://github.com/babashka/babashka/tree/master/examples)

## Gotchas

- **Path separators**: Use `(fs/path "dir" "file")` not string concatenation
- **Reader conditionals**: Babashka uses `:bb` feature: `#?(:bb ... :clj ...)`
- **Glob patterns**: `**` forces recursive; `*` is single directory level. `**/*.ext` does NOT match root-level files; use `**.ext` to include them
- **Resources on classpath**: Requires `{:paths ["resources"]}` in bb.edn
- **`delete-tree`**: does not follow symlinks. Use `:force true` for read-only files
