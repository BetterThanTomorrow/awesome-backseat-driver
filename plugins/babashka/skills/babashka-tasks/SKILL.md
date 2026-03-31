---
name: babashka-tasks
description: "Write idiomatic bb.edn tasks: thin wrappers delegating to scripts/*.clj modules. Use when: creating or modifying a bb task, babashka tasks, editing bb.edn or {scripts,bb}/*.clj files, using Backset Driver tools with Babashka."
---

# Babashka Tasks

Write idiomatic bb.edn tasks: thin declarative wrappers that delegate to well-structured scripts/*.clj modules.

**Prerequisite**: Always load the `babashka` skill. It covers REPL-driven development, REPL-loadable script patterns, shell vs process, data-oriented design, and namespace reference - all foundational to writing good task modules.

## When to invoke

- User asks to "add a bb task" or "create a new task"
- Editing bb.edn or scripts/*.clj files
- Automating a build, dev, or release workflow

## When NOT to invoke

- General Babashka scripting without bb.edn - use the `babashka` skill
- ClojureScript/Squint compilation (project-specific build docs)
- tools.build build.clj files (different API patterns)

## Architecture

```
bb.edn                          scripts/*.clj
  :requires [ns]                   (ns my-module
  task-name {:task (ns/fn args)}     (:require [babashka.fs :as fs]))
                                   (defn my-fn [opts] ...)
```

Tasks are thin. Modules hold logic.

For setting up a global task system (tasks available from any directory), see [references/global-bbg-setup.md](references/global-bbg-setup.md).

## bb.edn Structure

```clojure
{:paths ["scripts"]          ;; put modules on classpath
 :deps  {org.babashka/cli {:mvn/version "0.2.23"}}
 :tasks
 {:requires ([babashka.cli :as cli]
             [my-module])    ;; top-level requires shared across tasks

  my-task {:doc "What it does"
           :task (my-module/start! (cli/parse-opts *command-line-args*
                                                  {:coerce {:port :int}}))}

  -private-task {:doc "Internal helper (hidden from bb tasks listing)"
                 :task (do-something)}

  compound-task {:doc "Runs sub-tasks in parallel"
                 :depends [-private-task]
                 :task (run '-compound-all {:parallel true})}
  -compound-all {:depends [-private-task another-task]}}}
```

## Decision Framework

### When to inline vs delegate

| Situation | Pattern |
|---|---|
| Single shell command | Inline: `{:task (p/shell "cmd")}` |
| 2-3 lines, no branching | Inline with `do` |
| Validation, branching, error handling | Delegate to scripts/*.clj |
| Reused across multiple tasks | Always delegate |
| CLI argument parsing beyond simple coerce | Delegate |

## Module Template

```clojure
(ns my-module
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

;; ============================================================
;; Pure helpers (no side effects, defined before callers)
;; ============================================================

(defn- validate-args
  "Gather facts, return {:valid? bool :errors [...] :config {...}}"
  [opts]
  (let [errors (cond-> []
                 (not (:port opts)) (conj "Missing --port")
                 (not (fs/exists? (:dir opts "."))) (conj "Directory not found"))]
    {:valid? (empty? errors)
     :errors errors
     :config (merge {:port 8080} opts)}))

;; ============================================================
;; Side-effecting functions (edges only)
;; ============================================================

(defn start!
  "Entry point called from bb.edn task.
   Gather-then-decide: validate all inputs before acting."
  [opts]
  (let [{:keys [valid? errors config]} (validate-args opts)]
    (if valid?
      (do
        (println (str "Starting on port " (:port config)))
        (p/shell "my-server" "--port" (str (:port config))))
      (do
        (doseq [e errors] (println (str "Error: " e)))
        (System/exit 1)))))
```

## Patterns

### Task-to-module wiring

```clojure
;; bb.edn - thin wrapper passes parsed opts (see babashka skill: CLI argument parsing)
my-task {:task (my-module/start!
                (cli/parse-opts *command-line-args*
                                {:coerce {:port :int :verbose :boolean}
                                 :alias {:p :port :v :verbose}}))}

;; Separating task args from forwarded args (e.g. to Playwright)
;; Use -- to separate: bb my-task --my-flag -- --forwarded-arg
my-task {:task (let [{:keys [args opts]} (cli/parse-args *command-line-args*
                                                         {:coerce {:shards :int}
                                                          :alias {:s :serial}})]
                 (my-module/start! args opts))}
```

### CLI spec per module

Export a `cli-spec` from each module. Reuse it in `bb.edn` wiring and shell completions.

```clojure
;; scripts/my_module.clj
(def cli-spec
  {:coerce {:download :string :use :string :status :boolean}
   :alias {:d :download :u :use :s :status}})

(defn exec! [opts]
  (cond
    (:download opts) (download! (:download opts))
    (:use opts)      (use-version! (:use opts))
    :else            (status!)))
```

```clojure
;; bb.edn - task wires to module's cli-spec
my-task {:doc "Do something [--download <ref> | --use <ref> | --status]"
         :task (my-module/exec! (cli/parse-opts *command-line-args*
                                               my-module/cli-spec))}
```

### Zsh completions for tasks

A completion function that tab-completes task names and per-task options. Relies on a private helper task that reads `cli-spec` from each module.

```zsh
# completions.zsh — source in .zshrc
_bb_complete() {
    if (( CURRENT == 2 )); then
        local tasks=(`bb tasks | tail -n +3 | cut -f1 -d ' '`)
        compadd -a tasks
    else
        local task="${words[2]}"
        local opts=(`bb -task-options "$task"`)
        if (( ${#opts} )); then
            compadd -a opts
        fi
    fi
}
compdef _bb_complete bb
```

```clojure
;; bb.edn — private helper task that emits CLI options for a given task
-task-options {:task (let [task-specs {"my-task" my-module/cli-spec}]
                      (doseq [opt (-> (get task-specs (first *command-line-args*))
                                      :coerce keys)]
                        (println (str "--" (name opt)))))}
```

### Agent-Friendly Output

Write output to `.tmp/` files so AI agents can read results with `read_file` instead of parsing terminal output. Be sure to mention in the task's `:doc` string, and output, where results will be/are written.

```clojure
(defn- write-output! [filename content]
  (fs/create-dirs ".tmp")
  (spit (str ".tmp/" filename) content))

;; In your task function
(let [result (run-tests!)]
  (write-output! "test-output.txt" (:output result))
  (println "Results written to .tmp/test-output.txt"))
```

## Common mistakes

| Mistake | Correction |
|---|---|
| Putting logic in bb.edn `:task` | Delegate to scripts/*.clj module |
| Missing `:doc` string on task | Always add `:doc` for discoverability |
| Forward declaring functions | Define before use - rearrange file structure |
| Validation interleaved with execution | Gather all facts first, display diagnostics, then act |
| Using `System/exit` inside `with-*` wrappers | Return exit code, call `System/exit` after cleanup |
| Top-level side effects in modules | See the `babashka` skill: REPL-loadable scripts |

## Workflow: Adding a New Task

**This workflow applies to planning AND implementation.** When creating a plan document for a new task, use the REPL to verify API behavior, test glob patterns, and validate assumptions. Don't write a plan full of untested guesses and defer all exploration to the implementer.

1. **REPL gate**: Ensure a bb REPL is available (see `babashka` skill: REPL-driven development)
2. **Check existing tasks**: Read bb.edn to understand conventions
3. **Decide inline vs module**: Simple command? Inline. Logic? Delegate to scripts/*.clj
4. **Explore in the REPL**: Understand the data and APIs you will use
5. **Write the module function**: Pure validation first, side effects at edges
6. **Test pure helpers in REPL**: `(require '[my-module :as m] :reload)` then evaluate
7. **Add bb.edn entry**: Thin wrapper with `:doc` string
8. **Test full task**: Run `bb my-task` to verify end-to-end

## Quality Checklist

- [ ] REPL gate passed (bb session verified or user greenlighted REPL-less)
- [ ] Pure functions explored and validated in REPL before wiring side effects
- [ ] bb.edn entry is thin (just namespace call with args)
- [ ] No forward declares (definition order correct)
- [ ] Validation gathered before side effects
- [ ] Used `babashka.fs` instead of shell commands for file operations
- [ ] Used vector args for `shell`/`process`, not string interpolation
- [ ] Task has `:doc` string
