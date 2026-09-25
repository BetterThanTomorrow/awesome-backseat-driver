# Connection

Load when starting a relay, choosing a session, configuring ports, or crossing a page load.

## The three programs

- The **page REPL** evaluates Scittle in the connected tab. It listens on a WebSocket.
- **browser-nrepl** is the relay on the computer. It speaks nREPL to the editor or agent, and WebSocket to the tab.
- The **nREPL client** is Calva, Backseat Driver, or another nREPL client.

Connect:

1. Start the relay. The Epupp popup can copy the command. The usual pair is `bb browser-nrepl --nrepl-port 3339 --websocket-port 3340`. A headquarters project starts it with `bb browser-nrepl` from the project root.
2. Click **Connect** in the popup for this tab.
3. Connect the nREPL client to the nREPL port.

The toolbar icon is white when this tab is disconnected and gold when it is connected. **Reveal** in the popup jumps to a connected tab.

## Sessions

`clojure_list_sessions`, then `clojure_evaluate_code` with that session's `replSessionKey`.

`epupp-default` is the general session. A workspace may define more connect sequences, each with its own WebSocket port and a name like `epupp-github`. Those names are workspace configuration, not a fixed table. Read the live session list, and the workspace's Calva connect sequences when you need the port. Confirm the session by evaluating something that only the intended page would return.

Several tabs can be connected at once. Each tab that should stay up together needs its own relay port pair. Point the tab at that relay. Per-site port edits in REPL Connect, when they differ from the defaults, stick for that site even if the default ports change.

## Settings that affect the door

- **Auto-connect**: Never (default), On page load, or On page load + tab activation.
- **Reconnect connected tabs on navigation** restores the connection after a load. Page-program definitions are gone. This setting is overridden while auto-connect is active.
- **Default ports** are what REPL Connect shows for sites without an override.

## Navigation

On a full page load, the eval that triggered the load never returns, and the connection hangs until someone reconnects. Defer the navigation so this eval finishes:

```clojure
(js/setTimeout
 #(set! (.-location js/window) "https://example.com/page")
 50)
```

The same wrap applies to form submits and link-style loads. Client-side routing that does not reload the document leaves the page program in place.

After a real load: wait, `clojure_list_sessions` until the session is back, then evaluate. Each load is a boundary. Do not chain the navigation and the next page's work in one eval.

REPL FS Sync drops when the REPL disconnects. After a reconnect, ask the person to enable it again before any `epupp.fs` call.
