# First Step — Civic Catalog Stewardship & Security Model

Authoritative approved architecture • Consolidated edition • 15 October 2026

## Document control and authority

Status: APPROVED ARCHITECTURAL BASELINE. Includes six approved stewardship areas and 99 rules. It is not an assertion that all rules have been implemented. Current v2 coding remains governed by the Completion Contract, approved P4/P5 and evidence-gated IM-01–IM-14 execution matrix.

Supersession: Revised CAT-02 and ACD-01 clarify earlier P5 single-Community and geographical filtering statements. Preserve historical Decision 013/P5 wording as implementation history; do not silently rewrite it. The exact relationship/cardinality implementation remains subject to GEO-01-C/D evidence review.

## A. Purpose and six architecture principles

AP-01 — Enforce catalog invariants at authoritative backend/service and validation boundaries rather than UI hiding.

AP-02 — Keep structural validation, resident eligibility, controlled projection and featured selection separate.

AP-03 — Expose only approved resident-safe data through public DTOs; protect editorial and privileged boundaries.

AP-04 — Preserve records separately from resident visibility; expiry/withdrawal does not itself imply deletion.

AP-05 — Use current JSON, Git, tests and deployment procedures for v2 recovery; do not mistake Git for complete audit or backup.

AP-06 — Extend narrowly within Completion Contract; avoid duplicating model concepts or advancing V3 systems.

## B. Cross-cutting approved architectural amendments

### ACD-01 — Category-Centered Discovery

Categories and Subcategories organize resident discovery. Programs/services are central when people seek help; organizations, physical locations, service areas, populations served, availability and provider-published eligibility are distinct supporting dimensions. Category is not the owner of all related entities. Community remains first-class for civic operations, not an implicit eligibility restriction. A record may appear in multiple controlled projections without duplication.

### CAT-02 — Revised Community semantics

Community association identifies catalog management and civic discovery context; it does not by itself establish or limit the offering’s physical location, municipal jurisdiction, service area, or eligibility. Provider location cannot substitute for service coverage. First Step presents provider-published or verified requirements and does not tell a person that they qualify.

### GEO-01 and GEO-01-LM

Reconcile community geography with existing records, Category-centered discovery and the library-inspired, FindHelp.org-informed program-first pattern. This is not authorization to clone FindHelp.org or implement all filters.

### GEO-01-A through D

A: examine city slug, locations[0].city, defaults, filters and assignments. B: verify discovery of relevant nearby/regionwide opportunities. C: evaluate whether existing records require one CivicContent identity to participate across future community experiences. D: reconcile Category–Subcategory–CivicContent cardinalities with approved controlled projections and existing data.

### Architecture Reconciliation Principle

Before a proposed rule becomes a coding obligation, cross-check the seven approved journeys, P5, Completion Contract, previously accepted discovery principles, earlier catalog audits and later approved clarifications. Identify conflicts expressly.

### P5 and current execution matrix interpretation

P5 is logically approved; not presumed implemented. Later ACD-01/CAT-02 geography clarification governs semantic interpretation. Featured topics prioritize resident-actionable inventory over law/news volume; selection uses TIMELY/DURABLE/VISUAL and approved deterministic collision rules.

### Eligibility and relevance

First Step provides published/verified eligibility information and application steps, but does not determine individual eligibility. Law content must pass deliberate relevance/admission rules; repeated RSS collection does not mandate permanent Law record creation.

### V3 recording obligation

Future Admin editing must record changes to individual records (record ID, action, time, actor/process, before/after where appropriate and rationale). Authorized correction reports depend on this data. V2 retains practical source-control history and procedures without building a new audit subsystem.

## C. Rule inventory — approved initial 99 rules

Rule IDs are preserved from the approved conversations. Text below consolidates their approved meaning; the exact repository enforcement, test coverage, and v2/V3 disposition must be evidenced during integrated reconciliation. Operational policies do not automatically become new Java services.

## 01 — Cataloging

| Rule | Approved architectural requirement |

| --- | --- |

| CAT-01 | Every CivicContent record has a stable, unique identity. |

| CAT-02 | Community association identifies civic catalog and discovery context; it does not by itself establish or limit provider location, jurisdiction, service area, or eligibility. Show provider-published requirements; do not determine individual qualification. |

| CAT-03 | Category and Subcategory relationships must be valid. |

| CAT-04 | Expert/FAQ Category admission requires explicit, valid category metadata. |

| CAT-05 | Preserve the approved fixed five ContentTypes. |

| CAT-06 | Resident collections are controlled projections, not new types or identities. |

| CAT-07 | ContentSource and Organization remain distinct identities. |

| CAT-08 | PROVIDER and HOST relationships must be explicitly distinguished. |

| CAT-09 | Organization references must resolve correctly. |

| CAT-10 | Intended-audience metadata must reuse explicit controlled or structured classifications. |

| CAT-11 | Flyers enter Get Help only where explicit actionable help/access semantics qualify them. |

| CAT-12 | Event occurrence dates and content expiry have different meanings. |

| CAT-13 | Source facts, producer attribution, and editorial provenance remain traceable. |

| CAT-14 | Duplicates and overlaps require a defined editorial/catalog disposition. |

| CAT-15 | Catalog files receive appropriate structural and referential validation, including known coverage gaps. |

| CAT-16 | Managed authenticated staff catalog UI belongs to V3/Later. |

CAT-AUDIT-01: retain all CAT-01–CAT-16 until code-to-rule implementation evidence is captured. Document coverage, negative tests, and genuine gaps.

## 02 — Maintenance

| Rule | Approved architectural requirement |

| --- | --- |

| MNT-01 | Actionable information identifies its source or authoritative basis. |

| MNT-02 | Verification dates accurately describe actual verification. |

| MNT-03 | Material changes to service details require verification before publication. |

| MNT-04 | Unknown or uncertain facts are not silently inferred. |

| MNT-05 | Routine corrections preserve a stable record identity unless the offering genuinely differs. |

| MNT-06 | Corrections preserve necessary source attribution and editorial provenance. |

| MNT-07 | Material revisions to First Step Originals undergo renewed editorial review. |

| MNT-08 | Review priorities reflect consequences of outdated information. |

| MNT-09 | Maintenance recheck diagnostics are distinct from expiration. |

| MNT-10 | Approaching-expiry items remain eligible but receive stewardship attention. |

| MNT-11 | Updates maintain valid Category, Community, producer, organization, service-area and eligibility information. |

| MNT-12 | Correctly resolving URLs and references is not proof a destination remains relevant. |

| MNT-13 | Source or organization corrections do not indiscriminately rewrite unrelated records. |

| MNT-14 | Suspected inaccuracies and source contradictions are flagged for human verification. |

| MNT-15 | Unresolved issues receive a documented correction, verification, qualification, retention or lifecycle disposition. |

Acceptance policy: routine verified record corrections preserve identity; material changes to First Step Originals undergo editorial review; expiry-affecting corrections follow lifecycle rules.

## 03 — Lifecycle Management

| Rule | Approved architectural requirement |

| --- | --- |

| LCM-01 | Active content remains eligible when other required conditions hold. |

| LCM-02 | Approaching-expiry content remains eligible and produces an attention diagnostic. |

| LCM-03 | Expired content is excluded from applicable current-availability projections. |

| LCM-04 | Temporal, editorial, classification and community-relevance controls remain distinct. |

| LCM-05 | Publication, occurrence, verification, effective and expiration dates are distinct. |

| LCM-06 | Missing expiration does not automatically mean expired. |

| LCM-07 | Temporal boundary and local timezone semantics must be consistent. |

| LCM-08 | Event/deadline relevance follows the actual temporal meaning, not simple publication age. |

| LCM-09 | Content may require removal for reasons other than time expiry. |

| LCM-10 | Operational withdrawal does not automatically destroy the source record. |

| LCM-11 | Replacement versus correction is decided deliberately, preserving identities appropriately. |

| LCM-12 | Superseded guidance is not misleadingly presented as current. |

| LCM-13 | Resident ineligibility does not automatically delete catalog history. |

| LCM-14 | Retained historical content is not automatically presented as actionable. |

| LCM-15 | Restored visibility requires fresh validation of applicable controls. |

| LCM-16 | Restoration cannot bypass editorial approval. |

| LCM-17 | Lifecycle exclusions and exceptions are explainable and diagnosable. |

Option A approved: operational withdrawal using existing controls, without a universal v2 withdrawal state. ACTIVE and APPROACHING_EXPIRY remain eligible where otherwise valid; EXPIRED is excluded from applicable current-availability projections.

## 04 — Continuing Resources

| Rule | Approved architectural requirement |

| --- | --- |

| CR-01 | Continuing sources retain stable, identifiable provenance. |

| CR-02 | Continuing source, producer organization, and individual CivicContent are distinct. |

| CR-03 | Feed URL changes do not automatically create new source identities. |

| CR-04 | Repeated collection does not produce duplicate records for unchanged items. |

| CR-05 | Re-retrieved content preserves source attribution. |

| CR-06 | Temporary source failures do not automatically delete prior catalog items. |

| CR-07 | Collection failure differs from a successful check with no new items. |

| CR-08 | Updates preserve record identity when the underlying resource is unchanged. |

| CR-09 | Materially distinct releases may need new CivicContent identities. |

| CR-10 | Upstream changes do not silently rewrite First Step-authored editorial interpretation. |

| CR-11 | Known source corrections or supersessions receive controlled maintenance and visibility treatment. |

| CR-12 | Publication, collection, verification, availability, effective and expiry dates remain distinct. |

| CR-13 | Publication age alone does not determine ongoing program availability. |

| CR-14 | Source rechecks must not masquerade as verification of every item. |

| CR-15 | Continuing-source material enters resident delivery through controlled catalog admission, classification, eligibility and projections. |

| CR-16 | V2 does not introduce generalized subscriptions, syndication, event-series or editorial workflow infrastructure. |

Option A approved: changes to existing source items require controlled correction. Delaware Law ingestion is selective: collection does not entail catalog admission, publication, or permanent retention.

## 05 — Security & Access Control

| Rule | Approved architectural requirement |

| --- | --- |

| SEC-01 | Approved resident-facing content is accessible without authentication. |

| SEC-02 | Public APIs expose only approved resident DTO fields. |

| SEC-03 | Unpublished and flagged editorial materials are not exposed through public APIs. |

| SEC-04 | Publication and lifecycle controls apply on all resident delivery paths; Community and geography are not authorization barriers. |

| SEC-05 | Anonymous users cannot perform catalog modification or approval actions. |

| SEC-06 | Ingestion/configuration/editorial management functions are not publicly executable or writable. |

| SEC-07 | Secrets and private configuration are not leaked via public assets or APIs. |

| SEC-08 | External input and feed content remain untrusted and validated/safely rendered. |

| SEC-09 | Route, search and classification parameters are validated consistently with Category-centered and Community discovery semantics. |

| SEC-10 | Public error responses exclude stack traces, credentials, private paths and internal details. |

| SEC-11 | Transport, cross-origin and HTTP protections match production deployment. |

| SEC-12 | External processing and search have reasonable resource and failure limits. |

| SEC-13 | Future Admin write actions require authenticated, authorized actors. |

| SEC-14 | Future permissions distinguish correction, editorial approval, withdrawal and configuration as needed. |

| SEC-15 | Future authorization is server-enforced, never only UI-gated. |

| SEC-16 | Future privileged actions are attributable via structured change history. |

| SEC-17 | Security boundaries have tests or appropriate deployment evidence. |

| SEC-18 | Investigation and recovery from unauthorized/erroneous changes are preserved. |

S1: public-first, controlled modification. S2: no general Admin authentication/CRUD system in v2. S3: structured attributable change recording in V3. Community is not an authorization boundary.

## 06 — Audit & Recovery

| Rule | Approved architectural requirement |

| --- | --- |

| AR-01 | Changes remain traceable to stable catalog records. |

| AR-02 | Routine corrections preserve identity and available change evidence. |

| AR-03 | Editorial decisions retain appropriate provenance. |

| AR-04 | V3 implements structured, attributable record-level change history. |

| AR-05 | Catalog changes are validated before resident deployment. |

| AR-06 | Previous valid catalog versions are recoverable through documented operations. |

| AR-07 | Recovered records and relationships must be internally consistent. |

| AR-08 | Recovery cannot bypass current editorial, lifecycle or security rules. |

| AR-09 | Recovery procedures are periodically verified, not assumed. |

| AR-10 | Resident exclusion, operational withdrawal and permanent disposal are separate. |

| AR-11 | Irrelevant collected material need not become or remain catalog CivicContent. |

| AR-12 | Disposal of admitted records is deliberate and documented under retention policy. |

| AR-13 | Internally retained history cannot automatically return to resident delivery. |

| AR-14 | Retention distinguishes CivicContent, editorial evidence, logs and backups. |

| AR-15 | Internal audit/recovery access is appropriately restricted. |

| AR-16 | V3 Administrators can report on recorded routine corrections and stewardship activity. |

| AR-17 | Audit and recovery duties have reproducible verification or operational evidence. |

Approved acceptance: preserve integrity and recovery procedures in v2; differentiate collection/admission, expiry, withdrawal, retention and permanent disposal. Restore data then revalidate eligibility. Structured change history, authorized Admin reporting and richer audit operations are V3.

## D. Evidence and implementation status

APPROVED means the rule has architectural authority; it does not mean source code or tests have been verified. Use Enforced+tested; Implemented/verification gap; Partial; Missing; Operationally satisfied; Deferred V3; Architectural exception. Record the precise source path, method, validation/test assertion, negative test, approved requirement and smallest bounded correction.

## E. Scope / no silent model expansion

Keep the five ContentTypes RESOURCE, NEWS, FLYER, LAW and EXPERT. Article, FAQ, EVENT, OFFERING, PROGRAM and SERVICE are not new content types. Preserve narrow PROVIDER and HOST semantics, distinct Organization and ContentSource identities, P5 editorial/lifecycle controls, and stable IDs. Avoid V3 Admin CRUD, dashboard, automated queues, workflow assignment, notifications, automatic articles, database migration, generalized geographic eligibility/recommendation engines and relationship graphs.

## F. Review register and traceability

| Artifact | Disposition |

| --- | --- |

| Six areas CAT/MNT/LCM/CR/SEC/AR | Approved architecture; 99 initial rules |

| ACD-01 / revised CAT-02 | Approved clarifications |

| GEO-01 / LM / A–D | Approved reconciliation obligations; record-level answer still open |

| Area 05 S1–S3 | Approved; Community is not security barrier |

| AR-01–AR-17 | Approved; V3 recording/reporting dependency |

| Integrated architecture reconciliation | Required before the next coding authorization |