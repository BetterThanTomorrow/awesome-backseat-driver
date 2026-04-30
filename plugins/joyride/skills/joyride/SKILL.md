---
name: joyride
description: 'Joyride development — VS Code scripting with ClojureScript. Use when: working with Joyride scripts, .joyride/ directories, joyride_evaluate_code, user_activate.cljs, workspace_activate.cljs, Flares, or VS Code API scripting. Ensures the Joyride extension skills are loaded.'
---

# Joyride

This is a routing skill. The Joyride VS Code extension bundles comprehensive skills — this plugin ensures they are loaded before any Joyride work begins.

## Mandatory Skill Loading

Before any Joyride work, load the following skills from the Joyride extension:

- **`joyride`** — always. Covers core REPL evaluation, async patterns, VS Code API access, Flares, and JS interop.
- **`joyride-user-scripting`** — when working with `~/.config/joyride/`, `user_activate.cljs`, or global keybindings.
- **`joyride-workspace-scripting`** — when working with `.joyride/`, `workspace_activate.cljs`, or project-specific automation.

These skills contain tested procedures and reference examples. Read them fully — do not skip or summarize from memory.
