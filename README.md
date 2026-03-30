# awesome-backseat-driver

Plugin marketplace for Clojure AI context in GitHub Copilot — agents, skills, and workflows for REPL-first interactive programming with [Calva Backseat Driver](https://github.com/BetterThanTomorrow/calva-backseat-driver).

## Install

In VS Code, run **Chat: Install Plugin From Source** and enter:

```
https://github.com/BetterThanTomorrow/awesome-backseat-driver
```

Or add to your settings:

```json
"chat.plugins.marketplaces": ["BetterThanTomorrow/awesome-backseat-driver"]
```

## Plugins

| Plugin | Description |
|---|---|
| `clojure` | REPL-first Clojure development — agent and skill for any dialect and runtime |
| `clojure-editor` | Subagent for editing Clojure files using Backseat Driver structural editing tools |
| `babashka` | Babashka scripting and bb.edn task skills for idiomatic Babashka development |
| `joyride` | Joyride skills for VS Code automation with ClojureScript — scripting, user scripts, and workspace automation |
| `epupp` | Browser tampering and userscript development with Epupp (ClojureScript/Scittle in the browser) |

## Relationship to Calva Backseat Driver

The **Calva Backseat Driver extension** provides the foundational tooling layer of instructions so that the agent *can* and knows *how* to do REPL evaluation, structural editing, symbol lookup, etcetera. Install it from the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-backseat-driver).

This marketplace (as the Copilot team terms it) provides
1. More optinonated instructions for the AI to produce high quality Clojure code and use the REPL effectively.
2. More specific information around this or that Clojure dialect or runtime, or this or that library or framework / tool / etcetera.

## WIP

This repo will mature both in terms of its content and structure. Right now it is very raw and maybe not the easiest to contribute to (and certainly not to maintain).

## License

MIT
