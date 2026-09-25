# Built-in UI

Load when a userscript should show the Epupp mark, a header, or a copy icon.

Inject `epupp://epupp/ui.cljs` and require `epupp.ui`. The functions return hiccup for Replicant or Reagent. They do not mount themselves. You render them.

```clojure
{:epupp/script-name "my/widget.cljs"
 :epupp/inject ["scittle://replicant.js" "epupp://epupp/ui.cljs"]}

(ns my.widget
  (:require [epupp.ui :as ui]
            [replicant.dom :as r]))
```

| Function | Role | Defaults |
|---|---|---|
| `epupp-icon` | The Epupp SVG mark | `:size` 36 |
| `copy-icon` | A copy glyph, `currentColor` | `:size` 14 |
| `epupp-header` | Icon plus title, and an optional italic tagline | `:size` 36, `:title` `"Epupp"`, `:tagline` `"Live Tamper your Web"` |

Keyword arguments:

```clojure
(ui/epupp-icon :size 22)
(ui/copy-icon :size 14)
(ui/epupp-header :size 22 :title "Active experiments" :tagline false)
```

`:tagline false` omits the tagline. The header's type size tracks `:size`.

```clojure
(r/render container
  [:div
   (ui/epupp-header :size 22 :title "Notes" :tagline false)
   [:button (ui/copy-icon :size 14)]])
```
