# First Step v2 — IM-10 Coding-Agent Work Order
## Community Events Architectural Generalization and Occurrence Eligibility

**Status:** AUTHORIZED FOR EXECUTION — verification first; acceptance requires evidence  
**Package:** IM-10  
**Execution class:** Verification First — Bounded Correction If Needed  
**Dependencies:** IM-09 Satisfied; IM-01–IM-08 previously Satisfied  
**Issued:** October 17, 2026  
**Authority:** Approved v2 Completion Contract; authoritative IM-01–IM-14 matrix including IM-09 closure; approved P4/P5 (especially P5-R5b, event temporal distinctions, and controlled projections); Decision 051; IM-01 editorial expiry, IM-04 flyer semantics, IM-08 Category Destination, IM-09 controlled collections; approved **United Neighbors 2026** community-event test case.

## 1. Mission

Prove that a community-produced **event with flyer media and a scheduled occurrence** can be represented and delivered across **Community Notices → Events** and **Flyers** without creating duplicate CivicContent, treating the event as a Resource, or adding an EVENT ContentType. Implement only missing bounded occurrence/current-event rules demonstrated by tests. **Editorial expiry and occurrence are separate independent questions.** Preserve the fixed five ContentTypes: RESOURCE, NEWS, FLYER, LAW, EXPERT.

The work-order's acceptance sentence is binding: given one community-produced event with flyer media and a scheduled end time, First Step must show the same underlying item in Events and Flyers, retain source and Community, remove it from *current-event presentation* when occurrence eligibility ends, and retain its Flyer/media representation where otherwise eligible.

## 2. Protected start, mandatory inspection

1. Read actual `claude.md`, matrix (including IM-09 closure), this order, P5-R5b, IM-01/04/08/09 evidence and the Community Events decision. Confirm branch/full HEAD, Git status and any uncommitted predecessor changes. Do not assume report-time checkout state is current. Hash and protect all starting source/catalog/media/test/lockfile changes. No reset/stash/staging/commit/push unless separately requested.
2. Inspect `Flyer`, `CivicContent`, existing notice kind/classification, eventDate/start/end fields and actual JSON parsers; News/CommunityNotices services and controllers; FlyerService and Flyer media; Category collections and frontend Events/Meetings/Announcements/Flyers; `ContentSource.sector`, community and producer reference; Article approval and IM-01 `EditorialLifecycle`; current tests and validators. Record which fields are **stored** versus normalized/inferred. No new model based solely on preferred UI.
3. Audit the **actual** FL-008 United Neighbors 2026 record and any relevant community notice/event: concrete ID/type, source, Community, category, original image, eventDate/startsAt/endsAt if present, notice kind and current placement. Verify the exact stored times/dates rather than assuming the event has `endsAt`. If the fixture lacks a scheduled end, construct a synthetic controlled variant with `endsAt`; do not silently edit production FL-008.
4. Run and record untouched backend Maven verify with repository JDK25 toolchain, frontend test/type/build, Python unit tests and six validators. Separate existing failures from subsequent changes. Capture current Events vs Flyers outputs and any conflicts before coding.
5. Produce an inspect-first **truth table**: current versus past, date-only, start-only, end-time cases; presence/absence of editorial expiration; Community zone; notice kind; source/sector; current Events membership; current Flyers membership; stored/catalog retention and historical links.

## 3. Event occurrence contract — P5-R5b

Use an injected Clock and configured **owning Community timezone**, not the implicit machine zone. Establish an event's current-occurrence status from **explicit structured occurrence facts only**, distinct from editorial expirationDate and article/publication/retrieval dates.

- **Explicit `endsAt`:** current through that instant (inclusive); concluded immediately afterward. Confirm conversion/offset policy from actual stored representation, and document treatment of date-only end fields without inventing a clock time.
- **Date-only event:** current through the end of that Community-local calendar date; concluded at the start of the next local day.
- **Start time but no stated end:** current through the end of its Community-local calendar date; do not invent one-hour/two-hour duration.
- **Future event:** eligible for the current/upcoming Events projection where other gates pass; do not treat future occurrence as editorial expiry.
- **Missing or malformed necessary occurrence data:** never manufacture a date/end or declare a concluded event current merely from headline/body/flyer media. Report a deterministic diagnostic/validation issue; explain precise conservative presentation behavior and test it. Do not silently parse prose or use publication date as an event date.
- **DST and timezone:** test both spring and fall changes, inclusive boundaries and two independent Community zones. Avoid implicit UTC-midnight conversion of date-only facts.

An event's occurrence state must **not modify** IM-01's ACTIVE / APPROACHING_EXPIRY / EXPIRED assessment. Editorially expired content remains excluded from applicable current projections regardless of event time; approaching editorial expiry remains otherwise eligible and reportable to stewardship. Occurrence conclusion **removes from current/upcoming Events**, not automatically from stored records or all Flyers/current civic-information collections. It must not automatically 404 otherwise permitted historical detail.

## 4. Projection and identity generalization

- **Single identity:** use one existing stored CivicContent/Flyer record for both Event and Flyer projections; no duplicated normalized copy, converted RESOURCE, separate Event entity, or sixth ContentType.
- **Positive event classification:** use existing controlled event/meeting/announcement semantics and actual community-produced source identification. A picture, keyword or date by itself cannot manufacture event notice kind. Preserve existing producer-sector rules and Community scope; do not infer producer as Organization PROVIDER or HOST.
- **Current Events:** include only otherwise current, explicitly classified, not-concluded events. Distinguish from Meetings and Announcements; preserve existing URLs/filter chips/counts and applicable source provenance.
- **Flyers:** preserve underlying FLYER content type, image URL, producer/source, original media, Category and Community, and original stable item ID. A concluded event can remain in contextual Flyers if editorially current under the approved existing Flyer contract; do not mass-expire past-event media. Explicit `helpAccess` still controls **Get Help** separately (0/8 production Flyers approved as of IM-09); event classification alone does not admit it.
- **Consumers:** apply the occurrence contract consistently wherever the resident page explicitly claims a **current/upcoming event** (API, view/filter and matching count); do not insert a global filter into FlyerService, Topic Help, News, Search, or broad community announcements without demonstrating the applicable v2 rule. Preserve existing sector/source and `communityId` behavior; second-Community synthetic proof without launching it.
- **Presentation:** support accurate occurrence labels where stored, honest empty states, safe source/media navigation, keyboard/focus/loading/error/Spanish/high-contrast/responsive behavior touched by actual fixes. No fabricated event end times or false “open now”/“last verified” claims.

## 5. Bounded implementation choices and stop rules

Prefer an existing occurrence helper or narrow shared predicate consumed at the current-event projection boundary. Inspect existing event temporal data before deciding whether an optional `endsAt` field or narrowly scoped parsing/normalization support is actually necessary; if such a field is missing, **document that concrete requirement, current representation, backward compatibility, parser/validator impact and synthetic proof before adding it**. A narrow optional occurrence field necessary for P5-R5b can be within IM-10; a generalized event entity/platform is not.

**No authorization** to: change expiry lifecycle or editorial attention; assign any production flyer `helpAccess`; alter Event text or dates to force passing tests; change Topic/item selector rank; build scheduling/recurrences/RSVP/ticketing; invent PROVIDER/HOST relationships; add Admin UI/workflow/email; rename ContentTypes; introduce database migration; launch second Community; rewrite News/Article approvals; change the IM-09 membership manifest except to preserve it. Escalate genuine unsupportable schema/authority conflicts with precise evidence rather than silently redesigning P5.

## 6. Acceptance test matrix IM10-T01–T20

| ID | Required evidence |
|---|---|
| T01 | Record-level baseline audit of United Neighbors 2026 (FL-008 where current) distinguishes stored facts from assumed occurrence; no catalog edit. |
| T02 | A positive community-produced fixture with media and explicit scheduled end appears in both Events and Flyers with one canonical ID/FLYER type. |
| T03 | At exact `endsAt` boundary it remains current; immediately afterward it exits current Events. |
| T04 | Date-only event remains current through its owning local calendar date, exits at next local midnight. |
| T05 | Start-time-only event remains current through its local date, without invented duration. |
| T06 | Future explicit event qualifies as upcoming when other gates pass; a concluded event is not presented as upcoming. |
| T07 | Event temporal parsing handles offsets, different zones, DST spring/fall boundary cases with fixed Clock. |
| T08 | Malformed/missing occurrence data is diagnosed predictably; no inferred date from prose/title/publication; clear accepted behavior for unknown occurrence. |
| T09 | Editorial ACTIVE, APPROACHING_EXPIRY and EXPIRED remain independent of occurrence, preserving IM-01 resident/stewardship rules. |
| T10 | Concluded editorially-current event excluded from Events yet retained in Flyers and raw storage/detail where independently permitted. |
| T11 | Exact Community binding, producer/source and existing sector rules control notices; synthetic second-Community isolation. |
| T12 | EVENT is a controlled notice-kind/projection, not new ContentType/entity; no conversion into Resource/Get Help. |
| T13 | Meetings and Announcements unaffected except where they share a demonstrated approved occurrence contract; no arbitrary mass-filtering. |
| T14 | Counts, filtering and APIs describing current/upcoming Events agree with actual eligible items, no duplicate projection counts. |
| T15 | Flyer image original and safe source/media navigation survives conclusion, retaining identity/provenance. |
| T16 | Invalid/non-event flyer or unqualified metadata cannot get into Events merely from image/date/keyword. |
| T17 | Real FL-008 and synthetic scheduled-end variants regression evidence; real records untouched, expected results traceable to actual timestamps. |
| T18 | Public DTOs omit internal diagnostics/status/`helpAccess`/review/proof fields; existing Article privacy and IM-09 NewsSummary unaffected. |
| T19 | Full backend verify/package, frontend tests/type/build, Python unit tests, six validators; focused HTTP/browser checks where affected, responsive/keyboard as appropriate. |
| T20 | Git and SHA preservation checks, exact diff, no catalog/live Flyer mutation, no IM-11–14/V3 work, explicit evidence/report/disposition. |

Tests must demonstrate positive and negative behavior with fixed time and original data. A green current snapshot cannot replace concluded-event boundary cases.

## 7. Execution sequence

**Preserve → inspect actual FL-008/notice model → baseline run → prove current gap → choose minimum occurrence projection seam → bounded correction if needed → fixed-clock synthetic/real tests → broader regression/build/validator/browser gates → evidence report → stop at IM-10.** Do not begin IM-11 or IM-12 in the same execution. Never hide failing test runs; document corrections and final results.

## 8. Required agent execution report

Return a Markdown report with: starting disposition/branch/full HEAD/dirty state; authority inspected; exact real FL-008 metadata (including whether `endsAt` is truly present); before/after truth table and actual gap; code/data/test file changes and why; event occurrence rule and explicit Community time handling; original identity/media/producer preservation; 20 acceptance-ID-to-test mappings; actual commands, test counts and exit codes; packaged HTTP/browser evidence and limitations; SHA/preservation/lockfile/catalog checks; downstream risks; ending disposition `Satisfied` only if fully proven or explicit remaining blocker. Include logs/scripts/fixtures under `evidence/im10/` consistent with repository conventions and annotated mirrors required by `claude.md`.

## 9. Final instruction

**Execute IM-10, verification-first, within this bounded event-occurrence/generalization contract.** Do not silently rewrite editorial expiry or assume all historical Flyers are past/unavailable. If the current repository already meets a criterion, verify it and cite the existing implementation instead of rewriting it. Stop at this package boundary. IM-11 real selector collisions, IM-12 relationship/identity audits, IM-13 release gates and IM-14 documentation/closure remain open. First Step v2 is **not complete**.
