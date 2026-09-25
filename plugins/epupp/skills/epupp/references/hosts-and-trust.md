# Hosts and trust

Load when a script will not run, a connection will not come up, a browser behaves differently, or you are about to install code from a page.

The person is the gate. Userscripts run with full page access. You help them read what a script does. You do not bypass a permission they have not granted.

## Auto-run permission

- Chrome grants host access at install.
- Firefox starts without it. A matching auto-run shows a `!` badge on the icon. The person clicks the icon and grants access from the banner. Broad access is **about:addons**, Epupp, Permissions, "Access your data for all websites".
- Safari grants access per site, with Safari's own controls. Every script runs at `document-idle` there.

## What a page is allowed to do

On these exact origins, page code may install and update userscripts: `https://github.com`, `https://gist.github.com`, `https://gitlab.com`, `https://codeberg.org`, `http://localhost`, `http://127.0.0.1`. Page code on those origins cannot list or read scripts.

The built-in Web Userscript Installer adds **Install** near Epupp scripts on code-hosting pages. On the allowlist it can install. Elsewhere it shows copy-paste instructions. On GitHub gist pages and GitHub repo file pages, a block that declares `:epupp/library? true` can also offer **Copy library URL**.

The REPL listens on a localhost WebSocket. The person is responsible for what program is bound to that port.

FS REPL Sync is the privileged door into the script store. It is off until the person enables it, for one tab, and it drops when the REPL disconnects. Details: extension-memory.

## When something fails

- **No panel.** `chrome://` and the extension gallery cannot host it. Open an ordinary page.
- **Connect does nothing.** The relay is running, and the popup ports match it. Restart the relay if they already match.
- **Script does not run.** Auto-run is enabled in the popup. The glob matches this URL. The console has the error.
- **CSP noise.** Epupp injects in ways that usually satisfy a page's Content Security Policy. Read the console violation if something still fails.
- **Empty script list from `epupp.fs/ls`.** Sync may be off. `ls` yields `[]` on a failed call. Ask the person to enable FS REPL Sync, then try a write and read the error if it throws.

## Permissions the extension asks for

- `scripting`: inject userscripts
- `<all_urls>`: inject on any site
- `storage`: scripts, settings, and the user key-value bucket
- `webNavigation`: auto-injection on load
- `activeTab`: the DevTools panel

The extension collects no data.
