# Seeing

Load when you need a picture of the page, or you are deciding what an eval can actually tell you.

You see the page through return values. `prn` and `println` may never reach the agent. The last expression's value is the result. Bind intermediates with `def` when you need to look again without re-running a large form.

`epupp.tools` is on the REPL as soon as it connects, and in a userscript via `epupp://epupp/tools.cljs`. No FS sync. Each function is `^:async` and resolves to `{:success bool :dataUrl string :error string}`. `:dataUrl` is a base64 image URL.

```clojure
(require '[epupp.tools :as tools])

(await (tools/capture-visible))
(await (tools/capture-visible :format "png"))
(await (tools/capture-visible {:format "jpeg" :quality 90}))

(await (tools/capture-selector "nav"))
(await (tools/capture-element (js/document.querySelector ".card") :quality 90))
```

Keyword arguments and a single options map both work: `(capture-visible :format "jpeg" :quality 40)` and `(capture-visible {:format "jpeg" :quality 40})`. `:format` is `"jpeg"` (default) or `"png"`. `:quality` is 0-100, default 75.

A burst of captures can come back `{:success false :error "Capture failed: This request exceeds the MAX_CAPTURE_VISIBLE_TAB_CALLS_PER_SECOND quota."}`. That is Chrome refusing the call. Wait, then try once.

JPEG is small enough to return through the nREPL socket, including a viewport capture. PNG is much larger and can kill the WebSocket if you return the whole data URL. For PNG, `def` the result and check `(count (:dataUrl result))` before evaluating an expression that yields the URL itself.

`capture-element` throws when the element is nil, has zero dimensions, or lies fully outside the viewport. `capture-selector` throws when nothing matches. Scroll the element into view, then capture.

Use a capture when layout, color, or overlap is the question. Use a DOM query when the question is text, structure, or a count. A screenshot does not replace reading the node.
