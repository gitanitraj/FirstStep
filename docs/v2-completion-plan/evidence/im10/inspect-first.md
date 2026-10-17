# IM-10 inspect-first record and bounded design

Authority inspected: IM-10 work order; v2.10 matrix §14A; approved P5 §9 temporal metadata and §11 P5-R5b; Decision051 Community Events generalization; existing IM-01 lifecycle, IM-04 Flyer semantics, IM-08 destination and IM-09 report/protection evidence. Branch copyofdev, HEAD67c006842a85fd0cde57bc0777802922c6925949. Only the two supplied IM-10 authority documents are untracked; predecessor implementation is committed. All initial files hashed before edits.

## Actual FL-008
Stored full row is fl008-stored.json. FLYER is assigned by Flyer constructor (not stored explicitly). event_date=2026-10-17 is the only structured occurrence fact. startsAt/endsAt/event end fields are absent. Summary contains 1:00–4:00 pm but is not temporal metadata. Community=wilmington-de; source=westside-grows-together/manual, registered COMMUNITY sector; Category=Community Support, Topic=Community Celebrations; tags contain exact controlled event; image=United Neighbors 2026.jpg. No expirationDate, status or helpAccess. organization_id is stored but not mapped to a provider/host by Flyer. No data edit authorized/needed. Original media remains /images/seasonal/United%20Neighbors%202026.jpg.

## Gap and existing seams
CommunityNoticesService selects on controlled kind and registered producer sector, but never checks occurrence. All views/counts/previews repeat that same selector. FlyerService already enforces editorial currentness and independently permits historical by-ID status-safe reads. No existing occurrence helper, structured start/end model binding or occurrence validator exists. Existing notice DTO is ContentItem (allowlist). News notices currently display publication date; publication cannot supply event occurrence. Existing notices lack explicit Community query support. No global Flyer cutoff is appropriate. Meetings and Announcements retain existing semantics; this correction targets EVENTS only.

## Bounded representation, documented before implementation
Add optional ISO offset timestamp startsAt/endsAt to the existing Flyer and NewsItem concrete models (no new entity/type). Existing Flyer event_date remains date-only and unchanged. Both Flyer and News loaders already bind public fields; backward compatible missing fields remain null. The internal EventOccurrence helper uses injected Clock and the existing configured Community-zone map independently of editorial expiry assessment. Read-only assessment returns CURRENT_OR_UPCOMING, CONCLUDED or UNKNOWN plus a deterministic reason; it is not a public DTO or persisted workflow state. Runtime UNKNOWN fails closed only for Events; Flyers/Meetings/Announcements/storage/history remain independently eligible.

Parsing policy: startsAt/endsAt require ISO offset timestamp, including Z; local ambiguous/nonexistent clock times without offset and date-only endsAt are invalid (no invented clock time). Date-only events use event_date; start-only uses the start instant converted to owning Community date. Explicit end wins the cutoff, inclusive through exact instant. All supplied occurrence facts are validated, including end-before-start; a date-only end must be corrected to proper event_date or an explicit offset end by its editor. Publication/audit/expiry/prose/media are never fallbacks. Unknown/malformed data gets a stable internal reason. No production row gains a timestamp.

Only the Events boundary uses this assessment; explicit Community request binding and view/count/preview snapshot consistency are added there. The focused real-catalog test emits a scoped read-only occurrence diagnostic audit for Event-tagged raw rows using the same runtime assessment, without modifying records. No scheduling/recurrence/RSVP/tickets/Admin/database/provider/host/platform work. Authoring validation impact: runtime assessment diagnoses malformed/missing optional fields; standard catalog validator remains compatible, with a focused occurrence diagnostic test command rather than changing resource validation or demanding event dates on non-events.

## Before/after truth table (expected)
|Facts|Before Events if kind/sector/current editorial|After Events|Flyers if editorially current|Stored/status-safe history|
|---|---|---|---|---|
|Past date-only|included|excluded after next Community midnight|retained|retained|
|Today date-only|included|included through local day|retained|retained|
|Future date-only/start/end|included|included|retained|retained|
|Explicit end exactly now|included|included|retained|retained|
|After explicit end|included|excluded|retained|retained|
|Start only, today locally|included|included through local day, no duration|retained|retained|
|Missing/malformed occurrence|included|excluded UNKNOWN plus diagnostic|retained|retained|
|Editorial expired, future event|excluded by service|excluded by same service|excluded current inventory|retained if public status|
|Approaching editorial expiry, future event|included|included; attention unchanged|retained|retained|
|Government/unknown source or no controlled kind|excluded|excluded regardless of dates/media|sector-specific as before|unchanged|

Spring/fall DST and independent America/New_York/America/Los_Angeles synthetic Communities are required proof. No second Community launch.

The existing /api/flyers and /api/flyers/{id} serialize raw internal Flyer fields (verified/status/audit). A narrow FlyerSummary allowlist is required for T18; it preserves resident identity, Community, classification, occurrence facts, source/location/media while withholding editorial diagnostics/status/helpAccess/review. No global FlyerService filter is added.
