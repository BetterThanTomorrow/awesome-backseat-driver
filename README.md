# Awesome Backseat Driver [![Awesome](https://awesome.re/badge.svg)](https://awesome.re)

*Plugin marketplace for Clojure AI context in GitHub Copilot*: agents, skills, and workflows for REPL-first interactive programming with [Calva Backseat Driver](https://github.com/BetterThanTomorrow/calva-backseat-driver).

## VS Code Copilot Customization Docs

To make sense of what the content of this repository offers, you may want to check out some of these resources:

- [Overview](https://code.visualstudio.com/docs/copilot/customization/overview)
| [Customization concepts](https://code.visualstudio.com/docs/copilot/concepts/customization)
- [Custom instructions](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)
| [Prompt files](https://code.visualstudio.com/docs/copilot/customization/prompt-files)
| [Custom agents](https://code.visualstudio.com/docs/copilot/customization/custom-agents)
| [Agent skills](https://code.visualstudio.com/docs/copilot/customization/agent-skills)
| [Agent plugins](https://code.visualstudio.com/docs/copilot/customization/agent-plugins)
| [Troubleshooting](https://code.visualstudio.com/docs/copilot/troubleshooting)

## Install

To install the Marketplace and the **clojure** plugin:

1. Install/configure **Awesome Backseat Driver** as a provider of plugins:
    1. From the VS Code command palette: **Chat: Install Plugin from Source**
    1. Paste: https://github.com/BetterThanTomorrow/awesome-backseat-driver
    1. Select **clojure**
       ![Select Awesome Clojure Plugin](assets/howto/plugin-selector.png)
1. You will also want to installing the **clojure-editor** plugin:
    1. From the VS Code command palette: **Chat: Plugins**
    1. Append plugin name in the search box, e.g. **clojure-editor**
    1. Click **Install**

The minimal starter-pack, IMO, is the **clojure** plugin, the **clojure** instructions, and the **clojure-editor** subagent. There is some cross-referencing going on between these. Also IMO, you need the **babashka**, and **epupp**, plugins.

> [!NOTE]
> Plugins contain agents and skills. Both types of content are lazy loaded by the agent, so they will not hurt your agents context window just by being there. They will only add some little description so that the agent knows when to load them.

Instructions and prompts have install buttons in the rendered markdown from this site. (I recommend consolidating all Copilot config to `~/.copilot/`, but that's me.)

You can of course just copy the content of anything you want on your machine(s) from this repo and configure manually in VS Code.

The same repository URL works for [Cursor plugins](https://cursor.com/docs/plugins); see **Development** below for the dual-manifest workflow.

## Plugins

<!-- plugins-table-start -->
| Plugin | Description | Contents |
|---|---|---|
| [babashka](plugins/babashka/) | Babashka scripting and bb.edn task skills for idiomatic Babashka development. | Skills: [babashka](plugins/babashka/skills/babashka), [babashka-tasks](plugins/babashka/skills/babashka-tasks) |
| [clojure](plugins/clojure/) | REPL-first Clojure development — general agent and skill for any dialect and runtime; pairs with clojure-editor for safe edit delegation. | Agent: [clojure](plugins/clojure/agents/clojure.md) · Skill: [clojure](plugins/clojure/skills/clojure) |
| [clojure-editor](plugins/clojure-editor/) | Subagent for editing Clojure files using Backseat Driver structural editing tools. | Agents: [non-clojure-editor](plugins/clojure-editor/agents/non-clojure-editor.md), [clojure-editor](plugins/clojure-editor/agents/clojure-editor.md) |
| [epupp](plugins/epupp/) | Browser tampering and userscript development with Epupp (ClojureScript/Scittle in the browser). | Skill: [epupp](plugins/epupp/skills/epupp) |
| [squint](plugins/squint/) | Squint ClojureScript development — compilation, REPL workflow, debugging, and tooling for squint.edn projects. | Skill: [squint](plugins/squint/skills/squint) |
<!-- plugins-table-end -->

## Instructions

Instructions can't be bundled in plugins — install them separately:

| Instruction | Description | Install |
|---|---|---|
| `clojure` | Tiny. Nudge to help the Agent decide to load the Clojure skill | [![Install in VS Code](https://img.shields.io/badge/VS_Code-Install-0098FF?style=flat-square&logo=visualstudiocode&logoColor=white)](https://aka.ms/awesome-copilot/install/agent?url=vscode%3Achat-instructions%2Finstall%3Furl%3Dhttps%3A%2F%2Fraw.githubusercontent.com%2FBetterThanTomorrow%2Fawesome-backseat-driver%2Fmaster%2Finstructions%2Fclojure.instructions.md) [![Install in VS Code Insiders](https://img.shields.io/badge/VS_Code_Insiders-Install-24bfa5?style=flat-square&logo=visualstudiocode&logoColor=white)](https://aka.ms/awesome-copilot/install/agent?url=vscode-insiders%3Achat-instructions%2Finstall%3Furl%3Dhttps%3A%2F%2Fraw.githubusercontent.com%2FBetterThanTomorrow%2Fawesome-backseat-driver%2Fmaster%2Finstructions%2Fclojure.instructions.md) |

## Relationship to Calva Backseat Driver

The **Calva Backseat Driver extension** provides the foundational tooling layer of instructions so that the agent *can* and knows *how* to do REPL evaluation, structural editing, symbol lookup, etcetera. Install it from the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-backseat-driver).

This marketplace (as the Copilot team terms it) provides
1. More optinonated instructions for the AI to produce high quality Clojure code and use the REPL effectively.
2. More specific information around this or that Clojure dialect or runtime, or this or that library or framework / tool / etcetera.
3. More opinionated instructions about the use of subagents, etecetera

## WIP

This repo will mature both in terms of its content and structure. Right now it is very raw and maybe not the easiest to contribute to (and certainly not to maintain).

## Development

### Branch model

Development happens on the `next` branch. Releases merge `next` into `master` (fast-forward only).

### Dual-manifest workflow

Copilot manifests under `.github/plugin/` are the source of truth. Cursor manifests under `.cursor-plugin/` are **generated only** — never edit them by hand.

```sh
# after changing .github/plugin/ manifests or plugin content
bb generate-cursor-plugins
bb validate
git add .github/plugin/ .cursor-plugin/
```

`bb validate` runs Copilot checks plus strict Cursor validation (JSON Schema, path resolution, frontmatter policy, and L4 drift detection against committed `.cursor-plugin/` files).

Install the Cursor schema validator once:

```sh
npm install --prefix schemas/cursor
```

### Cursor

Generated `.cursor-plugin/marketplace.json` and `plugins/*/.cursor-plugin/plugin.json` follow [Cursor’s plugin reference](https://cursor.com/docs/reference/plugins). Install or test from the same git repo URL as Copilot; Cursor reads the generated tree, not `.github/plugin/`.

### Validating plugins

```sh
bb validate
```

Checks Copilot and Cursor plugin structure: JSON Schema, required fields, referenced paths, frontmatter identifiers, and regeneration fidelity.

### Publishing a release

```sh
bb publish
```

This runs `bb validate` first, then validates preconditions (on `next`, clean tree, ahead of `master`, changelog has unreleased entries), shows a summary, and on confirmation pushes a `[publish]` marker commit. CI validates again, runs `ci-release` (which regenerates both manifest trees), validates once more, commits Copilot and `.cursor-plugin/` artifacts, tags, creates a GitHub Release, and merges `next` into `master`.

## License 🍻🗽

[MIT](LICENSE.txt)

## Please sponsor my open source work ♥️

You are welcome to encourage my work, using this link:

* https://github.com/sponsors/PEZ