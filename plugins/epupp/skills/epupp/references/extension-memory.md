# Extension memory

Load when listing, reading, saving, renaming, or deleting scripts, persisting data across visits, or syncing a `userscripts/` directory.

Two stores:

- **Scripts** via `epupp.fs`. Needs FS REPL Sync.
- **User key-value data** via `epupp.storage`. Does not need FS REPL Sync.

Both survive navigation. A page atom does not. Export/Import in Settings moves scripts between profiles or machines. It does not include general extension settings.

## FS REPL Sync

Every `epupp.fs` operation needs both an active REPL on this tab and **Allow REPL FS Sync for this tab**. The toggle is disabled until a REPL is connected. Only one tab can hold sync. Enabling it elsewhere revokes the previous tab. Sync turns off when the REPL disconnects or the browser restarts. The person enables it again after each new connection, and only on a page they trust.

While sync is on, code in that page can list, read, write, and delete userscripts. That includes page scripts and other extensions. Ask before the first write. If a write throws, ask the person to enable sync. Do not look for another door.

`ls` returns `[]` when the call does not succeed, so an empty list can mean "sync is off" as well as "no scripts". `show` returns nil in that same case, including for a built-in that is certainly installed. A nil `show` does not mean the script is absent. A failed `save!` throws `FS Sync requires an active REPL connection and FS Sync enabled in settings`. Trust the throw, and ask.

All operations are `^:async`. `await` them. `save!`, `mv!`, and `rm!` throw on failure. Success maps use `:fs/*` keys.

Built-in names (`epupp/…`) can be listed with `:fs/ls-hidden?` and shown. They reject overwrite and delete.

## epupp.fs

```clojure
(await (epupp.fs/ls))
;; => [{:fs/name "github_tweaks.cljs"
;;      :fs/auto-run-match ["https://github.com/*"]
;;      :fs/enabled? true
;;      :fs/description "…"
;;      :fs/run-at "document-idle"
;;      :fs/inject []
;;      :fs/modified … :fs/created …}]

(await (epupp.fs/ls {:fs/ls-hidden? true}))

(await (epupp.fs/show "github_tweaks.cljs"))           ; string or nil
(await (epupp.fs/show ["a.cljs" "missing.cljs"]))      ; {name code-or-nil}

(await (epupp.fs/save! code-string))
(await (epupp.fs/save! code-string {:fs/force? true})) ; overwrite a normal script
(await (epupp.fs/save! code-string {:fs/enabled? false}))
(await (epupp.fs/save! [code-a code-b]))               ; bulk, index->info

(await (epupp.fs/mv! "old_name.cljs" "new_name.cljs"))
(await (epupp.fs/mv! "old_name.cljs" "existing.cljs" {:fs/force? true}))

(await (epupp.fs/rm! "my_script.cljs"))
(await (epupp.fs/rm! ["a.cljs" "b.cljs"]))             ; throws if any name is missing
```

`save!` reads the manifest from the code string. The string starts with the manifest map. A no-op save may include `:fs/unchanged? true`. A new script may include `:fs/newly-created? true`. Saving a script whose `:epupp/inject` contains supported SHA-pinned URLs fills the external-dependency cache.

Overwrite defaults to off. `:fs/force? true` overwrites a normal script. A forced rename keeps the source script's identity and still rejects a built-in target.

## epupp.storage

Available on the REPL with no inject, and in a userscript via `epupp://epupp/storage.cljs`. Values cross the boundary as EDN (`pr-str` / `edn/read-string`). Keys are keywords or strings. `keys` returns a vector of keywords. `clear!` clears the user bucket only, not scripts or settings. The bucket shares the extension storage quota with scripts and settings. Calls throw when the extension reports failure. They do not require FS sync.

```clojure
(require '[epupp.storage :as storage])

(defn ^:async demo-storage []
  (await (storage/set! :my/settings {:ui/theme :theme/dark}))
  {:got (await (storage/get :my/settings))
   :keys (await (storage/keys))
   :after-remove (do (await (storage/remove! :my/settings))
                     (await (storage/get :my/settings)))
   :keys-after-clear (do (await (storage/clear!))
                         (await (storage/keys)))})
```

## Headquarters sync

From the `userscripts/` directory of a my-epupp-hq style project, with the relay running, a tab connected, and FS REPL Sync on:

| Task | Effect |
|---|---|
| `bb ls` | List extension scripts. Built-ins are skipped. |
| `bb download` | Pull scripts into local files. |
| `bb upload` | Push local files. A file without a valid manifest is skipped. |
| `bb diff` | Unified diff, plus identical / different / remote-only / local-only. |

Arguments are relative to `userscripts/`. A path that does not end in `.cljs` is a directory prefix (`bb upload pez` uploads `pez/`). Flags: `--port` (default nREPL port `3339`), `--force`, `--dry-run` on download and upload.

This is why `epupp.fs` exists: scripts live in git, and the extension is the runtime store. REPL experiments stay in `live-tampers/` until they are worth that store.
