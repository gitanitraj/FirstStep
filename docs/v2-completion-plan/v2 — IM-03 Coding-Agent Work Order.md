# First Step v2 — IM-03 Coding-Agent Work Order

## Expert / FAQ Category-Integrity Enforcement

**Status:** AUTHORIZED FOR IMPLEMENTATION / DATA CORRECTION AS EVIDENCED  
**Package:** IM-03  
**Starting execution class:** Implementation/Data Correction Required  
**Dependency:** IM-02 — Satisfied (report-based acceptance October 15, 2026)  
**Issued:** October 15, 2026  
**Authority:** v2 Completion Contract; approved P4 and P5 logical/classification baseline; authoritative IM-01–IM-14 matrix and execution closure register; Decision 051/052 as applicable; approved Architecture Resolutions; IM-01 and IM-02 execution reports.  
**Journey/Epic:** J2, J4, J7; E5 (with implications for downstream J5/editor validation).

## 1. Mission and approved interpretation

Audit Expert Answers and FAQs for **explicit, valid controlled Category classification**, repair only demonstrably defective data or mapping, and add repeatable validation/regression coverage so defective records do not silently enter Category-scoped collections.

The approved P5 rule is **Category is required for Category-scoped delivery; Topic is required only for Topic-scoped delivery**. A valid Topic must belong to its approved parent Category, and an item's Topic membership requires that same Category membership. Neither a title, body, provider, producer, front-end placement nor the Topic value alone authorizes manufacturing missing Category membership. No new taxonomic node may be created merely to fix source data.

Preserve the existing five ContentTypes (`RESOURCE`, `NEWS`, `FLYER`, `LAW`, `EXPERT`), including FAQ/Expert Answer distinctions already expressed through concrete models and established presentation logic. Do not conflate approval/publication status or editorial expiry with category validity.

## 2. Protected baseline and source-control inspection

The IM-02 execution report states its starting branch was `copyofdev`, HEAD `f66b3ceea346e53bab786d4022032342eece4c0d`, clean; it also states IM-01 had been committed/pushed before IM-02 started. IM-02 itself performed **no stage, commit or push**, so its new code may remain uncommitted. These are historical facts **not** assumptions about the checkout now accessible.

Before any edit:

1. Record current branch, full HEAD, `git status --short`, tracked and untracked modifications, and relevant data-file diffs; preserve all user/agent work.
2. Confirm IM-01 lifecycle API and IM-02 diagnostic/report code exist and determine whether each is committed or uncommitted. If required predecessors are absent, stop and report the concrete discrepancy without recreating them.
3. Locate concrete FAQ and Expert Answer Java classes, `CivicContent` classification metadata, JSON files/loaders, repositories, services/controllers, category and topic DTO/projection services, taxonomy source, six existing Python validators, and relevant tests.
4. Identify actual `category_tags`/category fields (case/format), any fallback/derived values, and how each path normalizes, persists, validates and publishes them. Do not assume names or paths from planning artifacts are exact Java signatures.
5. Run relevant current tests and validators **before** altering data, recording results and classification inventory.

## 3. Deliverable A — Record-level classification audit

Produce an auditable inventory of **all stored FAQ and Expert Answer records**, with at least: immutable record ID, concrete class, owning `communityId`, stored Category tags, stored Topic/subcategory membership if any, relevant status/approval/expiry gating context, and each classification defect found. Include count totals for records inspected, already valid, missing Category, unknown Category, duplicate/noncanonical tags if invalid under current rules, invalid Topic, and Topic/parent-Category mismatch.

Inspect stored records rather than only resident-filtered service results, so invalid, expired, inactive or historical entries are not silently omitted from the stewardship audit. Distinguish whether an item is currently eligible for a particular resident projection from whether its stored classification is sound. Report whether missing `category_tags` exists in the **current** catalog; a historical allegation is not evidence of a present defect.

For records with absent/invalid Category, do **not** infer a correction from wording or source identity. Obtain a verifiable existing approved category assignment from trusted record metadata, source evidence or explicit editorial decision. If no justified classification is available, report a bounded unresolved data disposition; do not invent one or hide the record by arbitrary reassignment.

## 4. Deliverable B — Minimum data and integration correction

Where the audit proves a defect, make the smallest approved fix:

- Correct explicit Category/Topic tags only when the justified target is supported and is already part of the controlled taxonomy; log record ID, before/after, and justification.
- Preserve one Community owner, original CivicContent identity, producer/source, editorial approval, dates and status.
- Ensure any existing FAQ and Expert Answer Category-scoped retrieval uses **explicit valid Category membership** and obeys IM-01 resident expiry and independent publication gates.
- Ensure Topic-scoped retrieval uses valid Topic plus its matching parent Category; no inferred Category from Topic alone.
- Verify Category Destination's existing applicable supporting collection can retrieve classified eligible FAQ/Expert content, without implementing the unbuilt IM-08 destination redesign or IM-09 collections.
- Validate whether labels such as First Rate News are controlled presentation projections, not new ContentTypes or canonical categories.

If current code already satisfies a condition, document and test it rather than rewriting it. A validation failure should not authorize general source-model redesign.

## 5. Deliverable C — Recurrence prevention

Add or extend the **existing** catalog validator/test mechanism, preferring the project's established schema/classification infrastructure to a new general validator framework. The check must:

- reject missing or empty required Category classification on applicable Expert/FAQ records (in line with the approved data contract);
- reject Category values not in the controlled taxonomy and malformed Category/Topic associations;
- verify each explicitly assigned Topic belongs to its Category and that parent Category membership is present;
- support valid Category-only Expert/FAQ content without inventing Topic membership;
- avoid incorrectly rejecting valid multi-Category items;
- report actionable record identifiers and diagnostic reasons;
- detect defects in the real catalog dataset, not only synthetic fixtures; and
- cover the applicable Community partition, without making Wilmington the implied global scope.

If the data contract intentionally permits stored drafts awaiting classification, distinguish **stored-data intake** from eligibility for Category-scoped publication. Never allow an unclassified draft to become a resident Category item; document this exception and tests rather than imposing an undocumented global prohibition. Any ambiguity requiring an editorial classification decision is an evidence-based data issue, not permission to manufacture tags.

## 6. Acceptance tests (required names may differ; map them explicitly)

| ID | Test / verification scenario | Expected result |
|---|---|---|
| IM03-T01 | Audit all stored FAQs and Expert Answers | Exact inspected/valid/invalid counts, record IDs, and source paths recorded |
| IM03-T02 | Valid explicit Category-only FAQ | Eligible for its Category collection when other guards allow; no Topic fabricated |
| IM03-T03 | Valid explicit Category-only Expert Answer | Same Category behavior; retains existing content identity/type |
| IM03-T04 | Valid Topic with corresponding parent Category | Eligible for appropriately scoped Topic projection |
| IM03-T05 | Topic present but parent Category absent | Validator rejects; resident category membership not inferred |
| IM03-T06 | Missing/empty `category_tags` or equivalent required classification | Validation failure with precise record ID; not silently defaulted |
| IM03-T07 | Unknown or mismatched Category/Topic value | Validation rejects without creating taxonomy entries |
| IM03-T08 | Legitimate multi-Category classification | Retained, no false violation or duplicate identity |
| IM03-T09 | Expired Expert/FAQ with valid classification | Stored/auditable, excluded from applicable current inventory via IM-01 |
| IM03-T10 | Approaching-expiry and independently inactive/withdrawn cases | Expiry remains distinct from status/classification, no new eligibility rule |
| IM03-T11 | Category Destination existing service/API integration | Correct classified eligible Expert/FAQ reachable through the applicable existing Category collection |
| IM03-T12 | Malformed fixture injected into catalog validator | Nonzero exit and actionable message; restored valid inputs pass |
| IM03-T13 | Real catalog records after bounded correction | Valid affected records pass regression; before/after justification recorded |
| IM03-T14 | Second Community fixture or scoped test | Correct controlled classification independent of Wilmington-only assumptions |
| IM03-T15 | Deterministic repeated execution | Same finding/order/counts with unchanged catalog |
| IM03-T16 | Scope/preservation regression | No new ContentType, Category/Topic node, broad projection redesign, schema rewrite, or unrelated catalog edits |

**Note:** Empty record correction count is acceptable *if* the full audit finds no defects and negative validation/integration tests prove recurrence prevention. Do not fabricate corrections to fulfill a test label.

## 7. Quality gates and executable evidence

Run focused backend tests and changed validators first, then execute the project's full backend `verify` with its approved **JDK 25 Maven toolchain**, all six Python catalog validators (including updated validation if applicable), frontend `npm test`, `npx tsc --noEmit`, and `npm run build` when available. IM-02 reported **442 backend / 126 frontend** passing as historical baseline; independently record the actual numbers and any new tests. Do not weaken tests, edit counts solely to hide errors, disable TLS/checksums, or modify dependencies/lockfiles to ease execution.

For any justified data correction, run negative and positive validator fixtures, and verify all original classifications not targeted by the bounded fix are preserved. Capture actual commands, exit codes, sampled before/after output, and run identifiers/paths. IM-02's real expiry results are not substitutes for IM-03's classification tests.

## 8. Not authorized in IM-03

Do not implement IM-04 Flyer/Get Help overlap, IM-05 sub-destination pages, IM-06/07 featured selection, IM-08/09 destination design, IM-10 event occurrence, IM-12's **full** 99-rule identity/provenance/Community audit, IM-13 release testing or IM-14 reconciliation. No full Admin UI, auto taxonomy classification from AI/prose, new Article/FAQ ContentTypes, general audience/eligibility engine, new identity graph, database migration, or v3 workflow system. Do not replace approved P5 architecture for a bad record. No staging, commit or push unless separately authorized.

## 9. Final execution report / disposition gate

Return a written `IM-03 execution report` containing:

1. Starting branch/HEAD/status; IM-01/IM-02 preservation evidence.
2. Exact files/repos/services/validators inspected and pre-change test baseline.
3. Stored FAQ/Expert Answer audit table and totals, current missing/invalid cases, and evidence supporting each proposed correction.
4. Modified production, data, test, documentation and validator files, or `none` as appropriate; explain each change.
5. Named passing positive/negative tests mapped to IM03-T01–T16; commands and actual counts/exit codes.
6. Existing Category/Topic and current-resident projection regression evidence.
7. Confirmation ContentType, Community, source/provenance, expiry and Article approval boundaries were preserved.
8. Unresolved records requiring human editorial classification, with specific identifiers and no invented replacements.
9. Explicit Journey/Epic mapping (J2/J4/J7; E5), downstream integration notes for IM-05/06/08/09 and IM-12, and scope-protection statement.
10. One ending disposition: **Satisfied** only if all package-required controls have evidence; otherwise state the exact open blocker, correction needed or architectural escalation. No v2 completion declaration.

**Execution procedure:** inspect → inventory/audit → run baseline → establish actual gap → smallest justified correction + validation → focused regression → full gates → evidence report → stop. Begin no other IM package.
