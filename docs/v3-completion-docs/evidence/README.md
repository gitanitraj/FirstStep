# V3A-00 evidence

See [report](../V3A-00-evidence-report.md) for conclusions and limits. Captured October 23, 2026, on `copyofdev` at the SHA in `commit.txt`.

`browser-results.json` records 16 routes at two widths, DOM content, links, image loading/sizing, overflow, history probes, high contrast, focus and uncaught errors. Screenshots use `page-WIDTH-INDEX.png`; index matches the ordered route list in `browser-audit.mjs` and each width's results. `api.json` records actual live envelopes. `journeys.json` records expansion, narrowing, alternate scope, API invalid paths and synthetic 503 checks.

Browser reproduction requires installed local Playwright/Chromium, a freshly built backend on 18080 and Vite on 5173. From repository root:

```sh
java -Djava.net.preferIPv4Stack=true -jar backend/target/firststep-backend-0.1.0.jar --server.port=18080 --app.data.dir=app/data --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal --news.rss.initial-delay=86400000
```

In separate terminals, run `npm run dev -- --host 127.0.0.1` from `frontend`, then `node docs/v3-completion-docs/evidence/browser-audit.mjs` and `node docs/v3-completion-docs/evidence/journey-audit.mjs` from root. Probes overwrite generated evidence. They forward API/media requests in memory without modifying app configuration. These are local development checks, not combined production-package certification. The generic browser probe checks the existence of a “see more” label but the app actually calls it “32 more”; the dedicated journey probe exercises the correct action.

Automated exact commands and interpretation are in report §3; Python command exits are in `python-commands.json`. `frontend-test.log` and `backend-verify.log` are deliberately retained failed environment baselines; accepted runs have `compatible` and `approved` filenames. `runtime.log` / `vite.log` show sandbox bind failures; port-8080 approved launch was unreachable and was replaced for accepted checks by `runtime-18080.log`. `browser.log` / `journeys.log` are final probe process output; earlier probe import/forwarding errors were corrected before accepted JSON capture.

No dependency installation, application/test/catalog editing or remote publication was performed. Original catalog and media semantics remain unchanged. External source truth, full WCAG compliance, screen readers, real devices, remote RSS success and exhaustive combinations are outside demonstrated evidence.
