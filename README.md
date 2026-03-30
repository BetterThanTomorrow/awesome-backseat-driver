# Awesome  Backseat Driver

Plugin marketplace for Clojure AI context in GitHub Copilot — agents, skills, and workflows for REPL-first interactive programming with [Calva Backseat Driver](https://github.com/BetterThanTomorrow/calva-backseat-driver).

## Install

In VS Code, run **Chat: Install Plugin From Source** and enter:

```
https://github.com/BetterThanTomorrow/awesome-backseat-driver
```

Or add to your settings:

```json
"chat.plugins.marketplaces": ["https://github.com/BetterThanTomorrow/awesome-backseat-driver"]
```

## Plugins

| Plugin | Description |
|---|---|
| `clojure` | REPL-first Clojure development — agent and skill for any dialect and runtime |
| `clojure-editor` | Subagent for editing Clojure files using Backseat Driver structural editing tools |
| `babashka` | Babashka scripting and bb.edn task skills for idiomatic Babashka development |
| `joyride` | Joyride skills for VS Code automation with ClojureScript — scripting, user scripts, and workspace automation |
| `epupp` | Browser tampering and userscript development with Epupp (ClojureScript/Scittle in the browser) |

## Instructions

Instructions can't be bundled in plugins — install them separately:

| Instruction | Description | Install |
|---|---|---|
| `clojure` | Tiny. Nudge to help the Agent decide to load the Clojure skill | [![Install in VS Code](https://img.shields.io/badge/VS_Code-Install-0098FF?style=flat-square&logo=visualstudiocode&logoColor=white)](https://aka.ms/awesome-copilot/install/agent?url=vscode%3Achat-instructions%2Finstall%3Furl%3Dhttps%3A%2F%2Fraw.githubusercontent.com%2FBetterThanTomorrow%2Fawesome-backseat-driver%2Fmain%2Finstructions%2Fclojure.instructions.md) [![Install in VS Code Insiders](https://img.shields.io/badge/VS_Code_Insiders-Install-24bfa5?style=flat-square&logo=visualstudiocode&logoColor=white)](https://aka.ms/awesome-copilot/install/agent?url=vscode-insiders%3Achat-instructions%2Finstall%3Furl%3Dhttps%3A%2F%2Fraw.githubusercontent.com%2FBetterThanTomorrow%2Fawesome-backseat-driver%2Fmain%2Finstructions%2Fclojure.instructions.md) |

## Relationship to Calva Backseat Driver

The **Calva Backseat Driver extension** provides the foundational tooling layer of instructions so that the agent *can* and knows *how* to do REPL evaluation, structural editing, symbol lookup, etcetera. Install it from the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-backseat-driver).

This marketplace (as the Copilot team terms it) provides
1. More optinonated instructions for the AI to produce high quality Clojure code and use the REPL effectively.
2. More specific information around this or that Clojure dialect or runtime, or this or that library or framework / tool / etcetera.
3. More opinionated instructions about the use of subagents, etecetera

## WIP

This repo will mature both in terms of its content and structure. Right now it is very raw and maybe not the easiest to contribute to (and certainly not to maintain).

## License

MIT
