# Contributing

## Branch model

`master` is the marketplace. Copilot and Cursor installs from the repo URL use that branch. Work happens on `next`.

## Publishing

The human publishes with `bb publish`. Agents do not push a release. Help maintain the Babashka publish task.

## Manifests

- Edit **Copilot** manifests and agents only: `.github/plugin/marketplace.json`, `plugins/*/.github/plugin/plugin.json`, and `plugins/*/agents/`.
- **Never hand-edit** `.cursor-plugin/`, `.claude-plugin/`, or `plugins/*/claude-agents/` — those files are generated.

```sh
bb generate-cursor-plugins
bb generate-claude-plugins
bb validate
```

Commit the Copilot sources and the generated Cursor and Claude Code files together.The human publishes. As an agent you help with maintaining the Babashka tasks, including `bb publish`.
## Babashka development

Use a Babashka REPL (`bb` session) and the `babashka` / `babashka-tasks` skills in this repo. Prove behavior in the REPL before editing files; use `bb` tasks as integration gates.

## Agent and skill frontmatter

- `description` is required on every `SKILL.md` and agent `.md`
- `name` is required on skills; when present on agents, it must be kebab-case (e.g. `clojure-editor`)
- `description` is required on skills, agents, and instructions
- Human-readable titles belong in the markdown body, not display-style `name` values

## Host-specific fields

Future Copilot-only or Cursor-only manifest fields (e.g. `hooks`, `mcpServers`) must pass through the Cursor generator’s key filter. Add allowed keys to `allowed-plugin-keys` in `scripts/cursor_plugin.clj` when Cursor adds new manifest fields. The Claude Code generator has its own `allowed-plugin-keys` in `scripts/claude_plugin.clj`.

## Cursor component paths

Copilot `plugin.json` may list specific skill paths as arrays (e.g. `["./skills/clojure"]`). The Cursor generator rewrites `skills`, `agents`, `commands`, and `rules` to **directory path strings** (e.g. `"./skills/"`, `"./agents/"`) — that is what Cursor’s loader expects for local and marketplace installs.

## Claude Code plugins

`bb generate-claude-plugins` writes `.claude-plugin/marketplace.json`, a `.claude-plugin/plugin.json` per plugin, and `claude-agents/` with a Claude Code version of each Copilot agent.

- Skills load from `skills/` as they are. Claude Code loads every `skills/<name>/SKILL.md`, so `bb validate` fails when a skill folder is missing from the Copilot `plugin.json`.
- A generated agent keeps `name`, `description`, and the body. The Copilot `tools` allowlist becomes `disallowedTools`: the built-in tools of each Copilot tool set the agent is not granted. MCP tools stay inherited, because their names depend on what the user called the Backseat Driver server. An agent granted no extension tools also gets `mcp__*`. `model` is left out, so the agent follows the user's subagent model.
- `plugin.json` lists the generated agents. Claude Code loads agents only from the paths listed there, and not from inside `.claude-plugin/`.
- Main-agent personas are left out, with an empty `agents` array (`claude-agent-suppressed-plugins`). The `clojure` agent is one.
- Cursor `rules/` have no Claude Code counterpart and are not included.

`claude plugin validate .` checks the result when the Claude Code CLI is installed.
