---
name: run-firststep-app
description: Launch and drive the First Step app (backend-served static HTML/JS frontend) in a headless browser for automated verification. Use when asked to run the app, take a screenshot, or confirm a change works in the real UI (not just curl/API checks).
---

First Step's frontend is static HTML/JS (`backend/src/main/resources/static/`)
served directly by the Spring Boot backend — there's no separate frontend dev
server. Drive it via the Playwright REPL at `driver.mjs` in this directory.
No xvfb needed — this runs on macOS, and Playwright's headless Chromium works
natively.

## Prerequisites

The backend must be running and reachable, normally via Docker:

```bash
export PATH=/Applications/Docker.app/Contents/Resources/bin:$PATH
cd /Users/composedmedia/Documents/Projects/FirstStep
docker compose up -d
# wait for readiness:
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health | grep -q 200; do sleep 1; done
```

(Docker's CLI isn't on the default `$PATH` on this machine — see
`firststep_build_toolchain` memory. `docker compose down` to tear down when done.)

Driver dependencies (Node.js + Playwright + Chromium) are already installed
in this directory. If missing/stale:

```bash
export PATH=/usr/local/bin:$PATH   # node/npm location on this machine
cd .claude/skills/run-firststep-app
npm install
npx playwright install chromium
```

## Run

```bash
export PATH=/usr/local/bin:$PATH
cd .claude/skills/run-firststep-app
node driver.mjs
```

For a one-shot scripted run (no tmux needed — this machine doesn't have tmux),
pipe commands via heredoc:

```bash
SCREENSHOT_DIR=/tmp/firststep-shots node driver.mjs <<'EOF'
launch
wait-text Latest Updates
ss 01-home
click-text Weekly Updates
ss 02-weekly-updates
console --errors
quit
EOF
```

Screenshots land in `/tmp/firststep-shots/` (override: `SCREENSHOT_DIR`).
Target URL defaults to `http://localhost:8080` (override: `APP_URL`).

### Commands

| command | what it does |
|---|---|
| `launch` | open a headless Chromium page at `APP_URL` |
| `nav [path or url]` | navigate (bare = `APP_URL`, `/foo` = `APP_URL/foo`, or a full URL) |
| `ss [name]` | full-page screenshot → `<SCREENSHOT_DIR>/<name>.png` |
| `click <css-selector>` | Playwright `.click()` on a selector |
| `click-text <text>` | click a `button`/`a`/`[role=button]`/`.filter-chip`/`.news-item`/`.resource-card` matching (or containing) text |
| `type <text>` / `press <key>` | keyboard input |
| `fill <css-selector> <text>` | fill a form field |
| `wait <css-selector>` | wait up to 10s for a selector |
| `wait-text <text>` | wait up to 10s for `document.body.innerText` to contain text |
| `eval <js>` | evaluate JS in the page, print JSON |
| `text [css-selector]` | print `innerText` of a selector (default: whole body) |
| `console` / `console --errors` | print captured browser console/page errors since launch |
| `quit` | close the browser, exit |

## Gotchas

- **No tmux on this machine.** Use the heredoc one-shot pattern above
  instead of the interactive tmux loop described in the general `run` skill.
- **Async sections need an explicit wait before clicking.** The home page's
  "Latest Updates" and "Delaware's Newest Laws" sidebars, and the Weekly
  Updates page's news list, all populate via a `fetch()` after initial page
  load — `launch`'s `domcontentloaded` wait does NOT wait for them.
  `wait-text` (or `wait` on a specific selector) before `click-text` on
  anything in those sections, or the click silently misses (`NOT_FOUND`)
  because the element doesn't exist yet.
- **`wait-text` is case-sensitive against `.innerText`, which reflects CSS
  `text-transform`.** E.g. the Source label renders as "SOURCE" (uppercase
  via CSS) even though the DOM/HTML literally says "Source" — `wait-text
  Source` times out where `wait-text SOURCE` succeeds. When a wait seems to
  wrongly time out, check the page's CSS before assuming the app is broken —
  the screenshot is the ground truth, not the wait.
- **i18n keys**: some button/label text comes from a `t("someKey")` lookup,
  not a literal string in `app.js` — don't `click-text`/`wait-text` the raw
  key name (e.g. `viewDetails`), use the rendered English string instead.
- **Piped/heredoc stdin and readline interact badly if you write your own
  REPL loop.** This driver's queue + `safePrompt()` machinery
  (`driver.mjs`) exists specifically because Node's `readline` auto-closes
  on EOF (immediate for heredoc input) independently of in-flight async
  commands, and naive `rl.on('line', async ...)` races commands against
  each other. If you fork this driver, keep that structure.
