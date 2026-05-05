---
name: joyride
description: 'Joyride development — VS Code scripting with ClojureScript. Use when: working with Joyride scripts, .joyride/ directories, joyride_evaluate_code, user_activate.cljs, workspace_activate.cljs, Flares, or VS Code API scripting. Ensures the Joyride extension skills are loaded.'
---

# Joyride

This is a routing skill. The Joyride VS Code extension bundles comprehensive skills — this plugin ensures they are loaded before any Joyride work begins.

## SCI Runtime

Joyride is powered by SCI (Small Clojure Interpreter) with full Clojure macro fidelity — `defmacro`, syntax-quote, gensyms, `binding`, `try/finally` all work identically to Clojure. No `:require-macros` needed; For macros, Clojure semantics apply, not ClojureScript. For comprehensive SCI feature parity details, load `references/sci-dialect.md` from the Clojure skill.

## Mandatory Skill Loading

Before any Joyride work, load the following skills from the Joyride extension:

- **`joyride`** — always. Covers core REPL evaluation, async patterns, VS Code API access, Flares, and JS interop.
- **`joyride-user-scripting`** — when working with `~/.config/joyride/`, `user_activate.cljs`, or global keybindings.
- **`joyride-workspace-scripting`** — when working with `.joyride/`, `workspace_activate.cljs`, or project-specific automation.

These skills contain tested procedures and reference examples. Read them fully — do not skip or summarize from memory.
