# Userscripts

Load when writing, saving, or explaining a userscript: manifest, timing, URL match, panel, or popup.

A userscript is a `.cljs` file. The first form is a manifest map. The rest is the page program.

```clojure
{:epupp/script-name "pez/github_tweaks.cljs"
 :epupp/auto-run-match "https://github.com/*"
 :epupp/description "Make the GitHub logo spin"
 :epupp/run-at "document-idle"
 :epupp/inject ["scittle://replicant.js"]}

(ns pez.github-tweaks)
```

## Manifest keys

| Key | Required | Default | Role |
|---|---|---|---|
| `:epupp/script-name` | yes | | Filename or namespace path. Normalized on save. |
| `:epupp/auto-run-match` | no | | URL glob, or a vector of globs. Omit for a manual script. |
| `:epupp/description` | no | | Popup text. |
| `:epupp/run-at` | no | `"document-idle"` | `"document-start"`, `"document-end"`, or `"document-idle"`. |
| `:epupp/inject` | no | `[]` | What must load first. See composition. |
| `:epupp/library?` | no | false | Popup classification for a script with no auto-run pattern. |

Scripts with `:epupp/auto-run-match` start disabled. The person enables them in the popup. Scripts without that key run from the Play button.

## Names

Normalization: dots become `/`, spaces and dashes become `_`, other non-alphanumeric characters are stripped, and `.cljs` is ensured. `"My Script"` becomes `my_script.cljs`. `"pez.linkedin-squirrel"` becomes `pez/linkedin_squirrel.cljs`.

Names that start with `epupp/` are reserved for built-ins. Built-ins can be read and copied. They cannot be edited in place, renamed onto, or deleted.

Rename or copy by changing `:epupp/script-name` and saving. A new name creates a new script.

## URL globs

`*` matches any characters.

```clojure
{:epupp/auto-run-match "https://github.com/*"}
{:epupp/auto-run-match ["https://github.com/*" "https://gist.github.com/*"]}
{:epupp/auto-run-match "*://example.com/*"}
```

## Timing

- `"document-idle"` (default): after the page has loaded.
- `"document-end"`: through the early loader at `document-start`. If the script needs DOM-ready behavior, wait explicitly.
- `"document-start"`: before page JavaScript. `document.body` is null.

```clojure
(js/document.addEventListener "DOMContentLoaded"
  (fn [] (js/console.log "DOM exists")))
```

Safari runs every script at `document-idle`, whatever `:epupp/run-at` says.

## Where scripts live in a headquarters project

In a [my-epupp-hq](https://github.com/PEZ/my-epupp-hq) style workspace:

- `userscripts/` holds scripts with manifests. This tree is what `bb upload` sends to the extension.
- `live-tampers/` holds REPL experiments, often grouped by site. Promote a tamper into `userscripts/` when it should persist.
- Develop the behavior in the REPL first. Write the file once the evals have shown the shape.

Calva custom commands in that template, from the command picker (`ctrl+alt+space` twice):

- **Manifest**: cursor in the manifest form. Evaluates `(epupp.repl/manifest! <that form>)`, which applies `:epupp/inject` in the connected page.
- **Upload current userscript**: `(epupp.fs/save! <file text> {:fs/force? true})`. Needs FS REPL Sync on the connected tab.

## Panel

The DevTools **Epupp** panel edits one script buffer.

- **New** clears the editor, and asks when there are unsaved changes.
- **Save Script** stores it when the buffer starts with a valid manifest.
- **Eval Script** runs the whole buffer. Ctrl/Cmd+Enter runs the selection when there is one, otherwise the whole script.
- Results appear under the buffer. **Clear** wipes that log.

The panel is absent on `chrome://` pages and on the extension gallery. Use an ordinary page.

## Popup sections

- **REPL Connect**, including connected tabs and Reveal.
- **Manual/on-demand**: no auto-run pattern. Play runs them.
- **Libraries**: `:epupp/library? true` and no auto-run pattern. Collapsed by default.
- **Auto-run for this page**, and auto-run that matches other pages.
- **Special**: built-ins with their own trigger. Today that is the Web Userscript Installer.
- **Settings**.

The eye icon loads a script into the panel.
