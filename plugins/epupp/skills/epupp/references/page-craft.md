# Page craft

Load when changing the DOM, choosing a UI library, fetching, or writing to the clipboard.

## Inspect, then change

```clojure
(js/document.querySelector "#target")
(.-textContent (js/document.querySelector "h1"))
(mapv #(.-textContent %) (js/document.querySelectorAll "h2"))
(.-length (js/document.querySelectorAll "h2"))
```

A NodeList is seqable: `map`, `filter`, and `mapv` work. `count` throws `No protocol method ICounted.-count defined`. Use `.-length`.

Prefer a pure function of the data you just read. Apply it to the DOM at the edge.

A one-node change is a property write:

```clojure
(set! (.. (js/document.querySelector "#banner") -style -display) "none")
(set! (.-textContent (js/document.querySelector "h1")) "Better title")
(.add (.-classList (js/document.querySelector ".target")) "mine")
```

Work with the page's CSS. Override the specific property that is in the way. At `document-start`, `document.body` is null until DOMContentLoaded.

## Which UI tool

| Situation | Tool |
|---|---|
| One node, one write | DOM interop |
| A widget you render again from data | Replicant (`scittle://replicant.js`, `[replicant.dom :as r]`) |
| You want React component habits, local component state | Reagent (`scittle://reagent.js`) |
| Many events, subscriptions, and a shared event model | re-frame (`scittle://re-frame.js`, which includes Reagent) |

Reach for re-frame when the widget's event flow has earned it. A hide, a text swap, or a small render does not.

Replicant, injected and required:

```clojure
(let [container (doto (js/document.createElement "div")
                  (->> (.appendChild js/document.body)))]
  (r/render container
    [:div {:style {:position "fixed" :bottom "10px" :right "10px"
                   :z-index 99999 :padding "12px"}}
     [:p "Count: " n]
     [:button {:on {:click on-click}} "+"]]))
```

Replicant event maps use `:on`. Re-render from a function you call after the atom updates. For Epupp's own mark on a widget, load `references/built-in-ui.md`.

## Async

```clojure
(defn ^:async fetch-data [url]
  (let [response (await (js/fetch url))
        data (await (.json response))]
    (js->clj data :keywordize-keys true)))
```

`^:async` returns a Promise. `await` works in `let`, `do`, `if` / `when` / `cond`, `loop` / `recur`, `try` / `catch`, `case`, and threading macros. A bare `(await …)` in the REPL fails with `Unable to resolve symbol: await`. Parallel work is `js/Promise.all`. Catch with `catch :default`.

## Clipboard

Many pages block `navigator.clipboard.writeText`. A textarea plus `execCommand` works from a click handler inside a userscript. From a bare REPL eval it returns false, because there is no user activation.

```clojure
(defn copy-to-clipboard! [text]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) text)
    (.appendChild js/document.body el)
    (.select el)
    (js/document.execCommand "copy")
    (.removeChild js/document.body el)))
```
