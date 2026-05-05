# Squint Dialect Quick Reference

Essential Squint differences for agents that have loaded the Clojure skill but not the dedicated **Squint skill**. For full coverage — compilation workflow, REPL development, debugging, function availability — **load the Squint skill**. Planning or writing Squint code without loading the Squint skill is very, very, unwise.

## Critical Semantic Differences

- **Keywords are strings**: `:foo` → `"foo"`. `(= :status "status")` is `true`. No `keyword?`, no `name`.
- **Data is JS-native**: maps are objects, vectors are arrays, sets are Sets. `assoc`/`update` return shallow copies (originals unchanged). `assoc!` does not exist.
- **No `js->clj`/`clj->js`**: do not exist. Compile to bare calls that crash with `ReferenceError`.
- **Async**: use `js-await` (convention). Bare `await` also works. `doseq` does not properly await — use `loop`/`recur`.
- **`nil` is `null`**: `nil?` returns true for both `null` and `undefined`. `identical?` distinguishes them.

## Frequent Pitfalls

- Unrecognized symbols emit bare JS calls — silent at compile time, `ReferenceError` at runtime
- `sequential?` returns `true` for strings — use `vector?` to distinguish
- Set literals callable inline but not from variables — use `contains?`
- `(.-my-prop obj)` converts hyphens to underscores — use `aget` for literal hyphenated property names
- No auto-resolved keywords (`::key` is a compiler error)
