# IM-05 reproduction

From repository root, with the existing JDK25 Maven helper and installed frontend dependencies:

```sh
bash /workspace/.firststep-tools/im01-mvn verify
cd frontend
npm test
npx tsc --noEmit
npm run build
```

Return to repository root. Run Python test discovery and each validator:

```sh
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
```

Start the packaged backend on8098 with real app/data, suppressing initial RSS fetch for deterministic offline catalog checks:

```sh
/workspace/.firststep-tools/jdk-25+36/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8098 --news.rss.initial-delay=86400000
```

For synthetic checks, copy app/data JSON files into `/tmp/im05-synthetic-data`. In this temporary directory ONLY, replace resources.json, resources.communities.json and flyers.json with the corresponding synthetic-* evidence files. Start a second jar with `--server.port=8099 --app.data.dir=/tmp/im05-synthetic-data --news.rss.initial-delay=86400000`. Fixtures deliberately include an invalid Flyer to prove runtime defense when the catalog validation gate is bypassed; this temporary catalog is not publishable.

In separate processes, serve the built frontend against each packaged API using the included verification harness:

```sh
python3 docs/v2-completion-plan/evidence/im05/serve-ui.py 9005 8098
python3 docs/v2-completion-plan/evidence/im05/serve-ui.py 9006 8099
node docs/v2-completion-plan/evidence/im05/browser-check.cjs
```

Browser check requires the already available Playwright library and system `/usr/bin/chromium`; no package/lockfile changes. The attempted managed-browser download was forbidden by the network proxy; system Chromium154 was usable. The harness serves actual Vite build output with SPA fallback and proxies APIs/media to the executable jar. It does not bundle frontend assets into the backend jar or claim a deployed release artifact.

Manual equivalent: enter Category Housing with communityId=wilmington-de; follow All Housing Help; expand the four-item list and verify36 records, including the Hilda Davis telephone link; narrow to Emergency Shelter and Rental Assistance; refresh each; Back/Forward; return to All Help; use Tab/Enter on disclosure/expansion; test375px and desktop widths and high-contrast toggle. Eviction Prevention is valid but empty; food-pantry under Housing is invalid; Food Help with Wilmington is empty while unscoped Food has12. Stop API response to check loading and abort it to check network alert. Repeat with temporary synthetic-town-de, inspect Acceso público yes and Programa José text, then switch language using the header control. No live Flyer flags are changed by these steps.
