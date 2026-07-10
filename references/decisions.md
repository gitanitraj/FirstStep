# Decision 003

Housing Help returns many resource subtypes.

Observed during prototype testing.

Examples:
- Emergency shelter
- Transitional housing
- Rental assistance
- Domestic violence shelter

Conclusion:
A filter stage is necessary before showing results.

# Decision 004

Two filesystem reads used relative paths hardcoded to the repo root:
- ResourceService loaded `app/data/resources.json`
- ResourceController listed `backend/src/main/resources/static/images/seasonal`

This tied the app to being launched from the repo root and blocked deployment
from any other working directory (e.g. a container).

Conclusion:
Made both paths configurable via `@Value` properties, keeping the original
relative paths as defaults so existing behavior is unchanged:
- `app.data.dir` (default `app/data`)
- `app.seasonal.images.dir` (default `backend/src/main/resources/static/images/seasonal`)

A different working directory now overrides these via env/properties without
code changes. Chose `@Value` over `@ConfigurationProperties` to match the
existing style (OllamaService already injects `ollama.api.url` via `@Value`).

# Decision 005

Introducing the shared domain kernel (`shared/model/`) as part of the v2
vertical-slice migration.

`CivicContent` field list: `id, communityId, title, summary, verified, tags,
contentSource, createdDate, updatedDate` — taken directly from the project's
own domain-model UML (`docs/architecture/uml/domain-model-uml.md`), which
resolved the previously-open "KnowledgeObject: inheritance, composition, or
interfaces?" question in `01-domain-model.md` by drawing `CivicContent` as a
committed `<<abstract>>` class. `communityId` isn't shown as a boxed field in
the UML (Community is drawn as a relationship arrow instead), but it's kept
as a plain `String` field on the Java class — the diagram shows the
relationship, the code still needs a concrete FK under JSON-file storage.

`Contact` (new composite: phones, websites, email) does not replace
`Resource.phones`/`Resource.websites` in this pass. Those lists work fine
today; `Contact` ships in the shared kernel for Expert/Flyer to adopt once
built, per "don't refactor things that aren't broken."

# Decision 006

Bumped Spring Boot from 3.1.6 to 3.5.16 to add Spring AI (which requires
Spring Boot 3.3+). As of this decision, 3.5.16 is actually the final release
of the 3.5.x line (OSS end-of-life 2026-06-30) — Spring Boot 4.x is the
actively-patched line, paired with Spring AI 2.0 (which requires Boot 4).
Chose 3.5.16 anyway: it's a 4-minor-version jump from 3.1.6 with no
Jakarta/Boot-4 breaking changes to absorb, versus a much larger jump to Boot
4 (Spring Framework 7, changed auto-configuration/actuator behavior) that
this pass has not scoped time to audit. Spring AI 1.1.x still receives its
own security/dependency patches independent of Boot 3.5's own OSS EOL.

Added Spring AI's provider-agnostic `spring-ai-client-chat` dependency
(via the `spring-ai-bom` at 1.1.8) without any model-provider starter (no
Ollama, OpenAI, etc.) — no AI provider is available or subscribed to as of
this decision; the provider choice is explicitly deferred. `SpringAiAssistant`
(see `references/SpringAiAssistant_annotated.java`) takes an
`ObjectProvider<ChatClient.Builder>` so the application still boots cleanly
with zero providers configured; it throws `AiProviderNotConfiguredException`
only if `AiAssistant.generate()` is actually called, which
`DecisionAgentService`'s existing broad catch already handles by falling
back to a canned response — the same observable behavior v1 had when Ollama
was unreachable.

`docker-compose.yml`'s `ollama` sidecar container is left in place even
though the `app` service no longer reads `OLLAMA_API_URL` — removing the
sidecar is a separate infrastructure decision, not made here. Also removed
`app`'s `depends_on: [ollama]` — app startup was never actually blocked by
it (Compose's plain `depends_on` only orders container start, it doesn't
wait for health), but keeping it implied a dependency that doesn't exist;
removing it makes the compose file honestly reflect that `ollama` is a fully
optional, currently-unused sidecar.

Verified the whole stack end-to-end via `docker compose build && docker
compose up` once Docker Desktop was actually available in this environment
(it had to be installed from an existing but never-completed
`~/Downloads/Docker.dmg` — the CLI symlinks at `/usr/local/bin/docker*` were
pointing at a long-unmounted dmg volume from an earlier abandoned install
attempt). Findings from the real run:
- The app container starts and serves traffic within ~1 second — confirms
  it does not block on Ollama being present or healthy.
- `/api/decide` returns HTTP 200 with `ApiResponse.success` wrapping a
  fallback `DecisionResponse` whose `notes` field surfaces the real
  `AiProviderNotConfiguredException` message — the exact graceful-degradation
  path this pass was designed to produce.
- `/api/resources` (58 records) and `/api/news/rss` (349 live entries from
  the real Delaware legislature feed) both work correctly through the
  container. This means a separately-reported claim that "the RSS feed
  doesn't work in Docker" does not reproduce for the actual RSS endpoint.

# Decision 007

What's actually broken in the container (confirmed via the verified run
above, not assumed) is `/api/news` — the **static** news endpoint backed by
`app/data/news.json` — not the RSS feed. Container logs show: `Failed to
load news.json: app/data/news.json (No such file or directory)`. This is
the pre-existing inconsistency already noted during the News slice planning:
unlike `ResourceService` (which resolves its data path via the
`app.data.dir` property, overridden by the Dockerfile's `APP_DATA_DIR=/data`
env var), `NewsService` reads a hardcoded relative path
(`Path.of("app","data","news.json")`) that only resolves correctly when the
process's working directory is the repo root — true when running via `mvn
spring-boot:run` locally, false inside the container (`WORKDIR /app`, data
baked at `/data`).

Not fixed here — it predates this migration and isn't caused by it; fixing
it is folded into the News slice migration (Step 5), where `NewsService`'s
data-loading logic is being touched anyway. Recorded here so a future reader
doesn't mistake "the RSS feed is broken" (the original, imprecise report) for
what's actually true ("the static news list is broken; the live RSS feed
works fine").

# Decision 008

A real browser walkthrough of the deployed container (after Steps 1-5)
surfaced regressions this plan's curl-only verification had missed:
`app.js` still referenced `NewsItem`'s pre-migration flat field names
(`item.headline`, `item.sourceName`/`item.sourceUrl`/`item.source_name`/
`item.source_url`, `item.categoryTags`/`item.category_tags`) in every news
rendering function — `renderLawsColumn`, `renderNewsFilter`,
`renderNewsItems`, `loadSidebarNews`, `loadSidebarLaws`, and
`showNewsDetail`. These fields were renamed onto `CivicContent`
(`title`, `contentSource.name`/`.url`, `tags`) in Step 5, but Step 5's own
scope was backend-only per the plan — nobody went back through `app.js`'s
news-rendering code specifically for the *field renames* (Step 2's `app.js`
pass only handled the `ApiResponse` envelope unwrapping, which predated
Step 5's `NewsItem` field renames entirely). Symptom: "Latest Updates" and
"Delaware's Newest Laws" on the home page, the full "Weekly Updates" page,
and the single-story detail view all showed the literal text "undefined"
for titles and/or sources.

Fixed all six functions in `app.js` to read `item.title`,
`item.contentSource?.name`, `item.contentSource?.url`, and `item.tags`.

While investigating, also found: `renderLawsColumn`/`loadSidebarLaws`
displayed `item.why_it_matters || item.headline` — since `why_it_matters`
is *always* truthy (it's either the extracted "Relating to X" sentence or a
generic/keyword-based fallback sentence from `RssFeedService.classifyLegislation`),
the `|| item.headline` fallback never mattered, and the generic stock
message ("Stay informed about new laws signed by the Governor of
Delaware.") was being shown as if it were a headline whenever RELATING TO
extraction failed (confirmed against live feed data: 19 of 349 items).
Changed both to display `item.title` directly — it already holds the
extracted "Relating to X" sentence when available, or a sensible raw
fallback (the bill number, e.g. "HB 500") otherwise — never the generic
sentence.

Also fixed a real content-quality bug in `RssFeedService.extractRelatingTo`
(confirmed against live data, e.g. "Relating to delaware banks and trust
companies."): the old capitalization only capitalized the string's very
first letter and lowercased everything else, breaking embedded proper
nouns. Replaced with real title-case conversion (`toTitleCase`) plus a
`Delaware`-specific regex safety net, since the feed's content guarantees
that word appears constantly. See `references/RssFeedService_annotated.java`
for the full before/after.

**Explicitly deferred to after the full reconfiguration plan (Steps 6-8) is
complete**, per direct instruction: the resource-size ARIA filter buttons
don't work and should be *removed* (no fix has worked); there's no detail
view for Free/Low-Cost Essentials records. Neither is pre-existing-migration
fallout — both are separate, longer-standing product gaps.