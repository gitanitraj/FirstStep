# V3A-00 evidence — Revision 1 (October 23, 2026)

R1 adds files only; every R0 file, including README.md, is unchanged (hashes in `sha256.json` re-verified). R1 hashes are in `supplement-sha256.json`.

- `V3A-00-evidence-report.r0.md` — byte-for-byte copy of the R0 report before revision.
- `supplement-audit.mjs` → `supplement-results.json`, `supp-<scenario>-<width>.png`, `supplement.log` — Find Help, Discover facet, catch-all/invalid routes, Article/Updates/Organization history, nonempty Events (fixture), Article 404-vs-503, loading announcements.
- `supplement-events-fixture.json` — manifest of the controlled, nonpersistent Events fixture (scratch copy of `app/data`, `event_date` +90 days only, served on 18081). The production catalog was not modified.
- `supplement-runtime-8080.log`, `supplement-runtime-18081-fixture.log`, `supplement-vite.log` — R1 runtime logs.

R1 reproduction uses the standard development configuration: verified jar on **8080** (no IPv4 flag needed — see report §2), Vite on 5173 with its unmodified proxy, plus the fixture jar on 18081:

```sh
java -jar backend/target/firststep-backend-0.1.0.jar --app.data.dir=app/data --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal --news.rss.initial-delay=86400000
java -jar backend/target/firststep-backend-0.1.0.jar --server.port=18081 --app.data.dir=<scratch copy per manifest> --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal --news.rss.initial-delay=86400000
node docs/v3-completion-docs/evidence/supplement-audit.mjs
```
