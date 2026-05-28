# clojure-editor

Subagent for editing Clojure files using Backseat Driver structural editing tools.

Version: 0.1.6

## Agents

### Non-Clojure-Editor

Scoped text-editing companion for Clojure workflows. Use when: editing non-Clojure files such as Markdown, JSON, YAML, EDN config, README files, plugin metadata, or editing/removing existing standalone zero-depth top-level Clojure line-comment blocks. Not for Clojure forms or structural Clojure edits.

### Clojure-editor

Subagent for editing Clojure files using Backseat Driver structural editing tools. Takes an edit plan and carries it out with validation, error checking, and reporting. Use when: editing Clojure forms, adding forms with adjacent top-level comments, applying structural edits, or creating new Clojure files.
