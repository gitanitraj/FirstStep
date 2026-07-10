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