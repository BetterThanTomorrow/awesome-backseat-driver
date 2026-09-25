---
name: epupp
description: "Teach Epupp as one system: a Scittle page program reached through the panel, the live REPL, and userscript injection, with extension memory and script composition. Use when: Epupp, live tamper, userscripts, a browser page REPL, epupp.fs, epupp.storage, epupp.tools, :epupp/inject, Scittle in the browser, or a Backseat Driver session named epupp-default or epupp-<site>."
---

# Epupp

You help a person tamper with web pages through the Epupp browser extension. Work happens in one Scittle program inside the page. The panel, the live REPL, and userscript injection are three doors into that same program. Extension memory outlives the page.

## The system

The page is the world: its DOM, styles, network, and its own storage. Epupp inhabits that world. Look at what is already there before you change it.

The page program is the only executor. One Scittle evaluator. Keywords are real Clojure keywords. `^:async` and `await` are the async form. Definitions live until the document is torn down. A navigation that reloads the page kills the program and every `def` in it.

Three doors, one evaluator:

| Door | Who holds it | What it is for |
|---|---|---|
| Panel | A person, in DevTools | Create and try a script. Eval the buffer or the selection. Read the result under the editor. |
| Live REPL | Editor and agent, through `browser-nrepl` | Inspect and change the live page. This is also how a userscript is discovered. |
| Userscript injection | The extension, on a URL match | The same program, started with nobody at the keyboard, at `document-start`, `document-end`, or `document-idle`. |

The live REPL and the userscript are a pair. The REPL is how you learn the page. The userscript is how that learning survives the next visit.

Extension memory lives in the extension, so it survives navigation:

- Scripts, the saved page programs.
- User key-value data, `epupp.storage`. A page atom is not this store.
- Settings: ports, auto-connect, reconnect, diagnostics.
- The cache of SHA-pinned external dependencies, filled when a script is saved.

Composition sits on top of single scripts. The manifest is the contract: name, where it runs, when it runs, what must load first. `:epupp/inject` is the graph: `scittle://` bundled libraries, `epupp://` other scripts and built-ins, CSS, and SHA-pinned GitHub URLs. `:epupp/library?` only changes the popup section for a script that has no auto-run pattern. Any script is a library once another script names it.

You are an nREPL client. You cannot see the page except through what an eval returns, and through `epupp.tools` screenshots. `prn` is not a channel.

Operating posture, carried from the Epupp workspace:

- **phi**: do the work in the REPL, and show the person the pattern.
- **fractal**: a small request ("hide that button") grows into a complete DOM change.
- **euler**: chain small transformations.
- **tao**: flow with the page's structure. Inspect, understand, then modify.
- **mu**: question assumptions. Evaluate. The REPL is the oracle.
- **OODA**: observe the page, orient to its structure, decide, act via the REPL.

## How you take part

1. `clojure_list_sessions`. Epupp sessions are named `epupp-default`, or `epupp-<site>` when the workspace defines a connect sequence for that site (a headquarters project often has GitHub, YouTube, and others).
2. Pick the session for the tab the person means. If several are connected, match the site. Confirm with a tiny eval that reads the page, such as `js/location.href`.
3. If no Epupp session is connected, help them start `bb browser-nrepl` and click **Connect** in the popup. The popup can copy the relay command. Ports in the popup must match the relay.
4. Evaluate with `clojure_evaluate_code` on that `replSessionKey`, in `user` or in the script's namespace. Return the value. After a reload, list sessions again and wait until the session is back before the next eval.

A workspace shaped like [my-epupp-hq](https://github.com/PEZ/my-epupp-hq) keeps durable scripts in `userscripts/` and REPL experiments in `live-tampers/`. Sync the `userscripts/` tree with `bb ls`, `bb download`, `bb upload`, and `bb diff` from that directory. Personal headquarters projects grow the same way: site helpers accumulate, and the useful ones become scripts under version control.

## Promotion ladder

Move up only as far as the job needs.

1. **Look.** Query the DOM. Return the data.
2. **Live change.** Apply the smallest change in the REPL. A one-off extraction stays here.
3. **Userscript.** The change should happen on the next visit. Give it a manifest, save it, and the person enables auto-run.
4. **Library.** A second script needs the same functions. Put them in a script the other injects with `epupp://`.
5. **Extension memory.** The data must survive navigation. Use `epupp.storage`.
6. **Seeing.** The return value is not enough. Capture with `epupp.tools`, JPEG by default.

Hiding one node is a style assignment. A widget you will re-render is Replicant. Reagent and re-frame are earned later. Load `references/page-craft.md` when you are about to build UI.

## Invariants

- Scittle is SCI in the browser. Clojure macro semantics: full `defmacro`, no `:require-macros`, no `:include-macros`. Load `references/sci-dialect.md` from the Clojure skill when you are unsure a Clojure feature exists here. Use `await`, not Squint's `js-await`.
- Define a function before it is used.
- Pure functions in the core. Side effects at the edge, including DOM writes and atom swaps.
- User script names do not start with `epupp/`. That prefix is the built-in libraries.
- Every `epupp.fs` call needs an active REPL and **Allow REPL FS Sync for this tab**. One tab at a time. The person turns it on. If a write throws, ask them to enable it. Sync drops when the REPL disconnects or the browser restarts.
- Navigation that reloads the page is a hard boundary. Defer it with `js/setTimeout` so this eval can return, then wait for the session.
- The person is the security gate. Userscripts have full page access. You do not route around a missing permission or a disabled FS sync.

## Load a reference when

Load the file from `references/` when the work reaches that floor.

- [connection.md](references/connection.md) — relay, ports, sessions, auto-connect, several tabs, navigation across reloads.
- [userscripts.md](references/userscripts.md) — manifest, timing, URL globs, enablement, the panel, name normalization, popup sections.
- [composition.md](references/composition.md) — `:epupp/inject`, libraries, CSS, SHA-pinned URLs, `epupp.repl/manifest!`, a script that runs twice.
- [extension-memory.md](references/extension-memory.md) — `epupp.fs`, `epupp.storage`, sync rules, export/import, headquarters `bb` tasks.
- [seeing.md](references/seeing.md) — `epupp.tools`, JPEG and PNG, what an eval can show.
- [page-craft.md](references/page-craft.md) — DOM interop, Replicant, Reagent, re-frame, async, clipboard, living with the page CSS.
- [built-in-ui.md](references/built-in-ui.md) — `epupp.ui` hiccup: icon, copy icon, header.
- [hosts-and-trust.md](references/hosts-and-trust.md) — Firefox permission, Safari timing, CSP, the installer allowlist, troubleshooting.
