# Composition

Load when a script should depend on another script, a bundled library, CSS, or code hosted on GitHub.

`:epupp/inject` is a vector of URLs. Epupp loads them before the script runs. Dependencies of those dependencies load too. Cycles are reported.

## Kinds of URL

| URL | What it brings |
|---|---|
| `scittle://….js` | A bundled Scittle library. |
| `epupp://some/script.cljs` | Another userscript, including built-ins. |
| a URL ending in `.css` | A stylesheet, as `<link rel="stylesheet">` in `document.head`, before scripts. Deduplicated per page. |
| `https://raw.githubusercontent.com/…` or `https://gist.githubusercontent.com/…` | Foreign code, pinned to a full 40-character SHA. |

Any script becomes a library when another script names it. `:epupp/library? true` puts a script that has no auto-run pattern in the popup Libraries section. A script with both `:epupp/library?` and `:epupp/auto-run-match` stays in its auto-run section. Disabled scripts and built-ins are valid inject targets.

When a script both auto-runs and is injected by another auto-run script on the same page, it can execute twice: once as a library, once as itself.

## Bundled Scittle libraries

| URL | Provides |
|---|---|
| `scittle://pprint.js` | `cljs.pprint` |
| `scittle://promesa.js` | `promesa.core` |
| `scittle://replicant.js` | Replicant |
| `scittle://js-interop.js` | `applied-science.js-interop` |
| `scittle://reagent.js` | Reagent and React |
| `scittle://re-frame.js` | re-frame, which includes Reagent and React |
| `scittle://cljs-ajax.js` | `cljs-http.client` |

`scittle://re-frame.js` pulls Reagent and React. There is no npm. These libraries, plus `epupp://` scripts, are the module system.

## Built-ins you inject on purpose

| Inject | Namespace | Role |
|---|---|---|
| `epupp://epupp/ui.cljs` | `epupp.ui` | Branding hiccup. See built-in-ui. |
| `epupp://epupp/storage.cljs` | `epupp.storage` | User key-value store. Also on the REPL without inject. |
| `epupp://epupp/tools.cljs` | `epupp.tools` | Screenshots. Also on the REPL without inject. |

`epupp/internal/…` is an implementation detail of those built-ins. Inject the public script, not the helper.

## A library and its consumer

```clojure
{:epupp/script-name "utils/dom.cljs"
 :epupp/description "DOM helpers"
 :epupp/library? true}

(ns utils.dom)

(defn hide! [selector]
  (when-let [el (js/document.querySelector selector)]
    (set! (.. el -style -display) "none")))
```

```clojure
{:epupp/script-name "my/tweaks.cljs"
 :epupp/auto-run-match "https://example.com/*"
 :epupp/inject ["scittle://replicant.js" "epupp://utils/dom.cljs"]}

(ns my.tweaks
  (:require [utils.dom :as dom]
            [replicant.dom :as r]))

(dom/hide! "#annoying-banner")
```

A library can itself inject other scripts. An early auto-run can inject a library at `document-start`; the library's namespace is there when the auto-run body evaluates.

## External code

Public GitHub content only. Branch names and tags are rejected. The URL is a full commit SHA.

| Host | Shape |
|---|---|
| `raw.githubusercontent.com` | `https://raw.githubusercontent.com/owner/repo/SHA/path/to/file.cljs` |
| `gist.githubusercontent.com` | `https://gist.githubusercontent.com/owner/GIST_ID/raw/SHA/filename.cljs` |

Epupp fetches and caches these when the script is saved (panel, web installer, or `epupp.fs/save!`). At page load they are injected from that cache.

On a GitHub gist page or a GitHub repo file page, a code block that declares `:epupp/library? true` can offer **Copy library URL**. That copies a pinned URL. It may be normalized, so it can differ from the page's Raw link.

## Loading the graph from the REPL

`epupp.repl` is present when the REPL is connected. No inject needed for the namespace itself.

```clojure
(await (epupp.repl/manifest! {:epupp/inject ["scittle://pprint.js"
                                             "epupp://utils/dom.cljs"]}))
```

`manifest!` returns a promise that resolves to `true`. The namespace exists after that await. Require it in a following evaluation, then use it:

```clojure
(require '[cljs.pprint :as pprint])
(with-out-str (pprint/pprint {:a 1}))
```

One evaluation that calls `manifest!` and then `require`s the new namespace fails with "Could not find namespace", because `require` runs before the inject finishes. A single `defn` that both awaits `manifest!` and names the new alias fails earlier, at analysis, with "Unable to resolve symbol". Calling `manifest!` again with the same set resolves to `true`. In a headquarters workspace, the Calva **Manifest** command evaluates `manifest!` on the manifest form under the cursor.
