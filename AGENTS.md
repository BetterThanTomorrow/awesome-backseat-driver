# Contributing

## Manifests

- Edit **Copilot** manifests only: `.github/plugin/marketplace.json` and `plugins/*/.github/plugin/plugin.json`.
- **Never hand-edit** `.cursor-plugin/` — those files are generated.

```sh
bb generate-cursor-plugins
bb validate
```

Commit both Copilot and generated Cursor paths together.

## Publishing

The human publishes. As an agent you help with maintaining the Babaska tasks, including `bb publish`.

## Babashka development

Use a Babashka REPL (`bb` session) and the `babashka` / `babashka-tasks` skills in this repo. Prove behavior in the REPL before editing files; use `bb` tasks as integration gates.

## Agent and skill frontmatter

- `description` is required on every `SKILL.md` and agent `.md`
- `name` is required on skills; when present on agents, it must be kebab-case (e.g. `clojure-editor`)
- `description` is required on skills, agents, and instructions
- Human-readable titles belong in the markdown body, not display-style `name` values

## Host-specific fields

Future Copilot-only or Cursor-only manifest fields (e.g. `hooks`, `mcpServers`) must pass through the Cursor generator’s key filter. Add allowed keys to `allowed-plugin-keys` in `scripts/cursor_plugin.clj` when Cursor adds new manifest fields.

## Cursor component paths

Copilot `plugin.json` may list specific skill paths as arrays (e.g. `["./skills/clojure"]`). The Cursor generator rewrites `skills`, `agents`, `commands`, and `rules` to **directory path strings** (e.g. `"./skills/"`, `"./agents/"`) — that is what Cursor’s loader expects for local and marketplace installs.
