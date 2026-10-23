# Flyer/Media Review — Future Requirements (recorded, not implemented)

**Status:** Requirements record only. Per the C-02 authorization, **no** `mediaReview` field, validator, editorial DTO, automatic image verification or Admin UI has been implemented. This document records what a future review capability must satisfy, so the C-02 failure cannot recur silently. Origin: [C-02 closure](C-02-flyer-integrity-closure.md).

## The failure this must prevent

Posters were uploaded with no metadata, renamed to theme names, and records were written to fit the names rather than the images. Nothing recorded that a person had compared a record with its image, and the validators passed throughout. Seven of eight published flyers were wrong.

## Requirements

| # | Requirement | Why (C-02 evidence) |
|---|---|---|
| MR-1 | **Reviewer confirmation.** A flyer is media-confirmed only when a named human reviewer records the outcome, the date, and which fields were checked against the image (title, organizer, date, location). Agent or automated inspection may assist but never counts as confirmation | C-02's comparison was made by a coding agent and is explicitly not administrator confirmation |
| MR-2 | **Image/version identity.** The confirmation binds to the exact image content (e.g. SHA-256), not the filename. Replacing or editing the image invalidates the confirmation | Filenames were changed once (`1985.jpg` → `Fundraiser.jpg`) with identical bytes; a filename-bound review would not detect a swapped image. Current hashes: [poster-sha256.txt](evidence-c02/poster-sha256.txt) |
| MR-3 | **Ongoing-program handling.** Posters for ongoing services (no single occurrence) must be representable without an invented event date or session, and must not enter Events | FL-002 (eviction legal-help line), FL-005 (rights information) and FL-007 (furniture program) are not events, yet their records claimed dated sessions |
| MR-4 | **Missing dates.** When the image shows no date, the record's date fields stay empty; a reviewer cannot confirm a date the image does not support. Year-less dates ("June 4th") need an explicit reviewer decision | FL-003 and FL-006 show day and month only; FL-002, FL-005 and FL-007 show no date |
| MR-5 | **Multiple sponsors.** A flyer can name several producers. Review records the primary producer used for sector placement and the co-sponsors shown, and **producer attribution is itself a reviewed field** because it drives Community Notices vs Program Updates | FL-002 shows five partners; FL-006 is co-hosted by a government agency and a chamber of commerce; FL-005 and FL-007 were placed in Program Updates under unverified government attribution |
| MR-6 | **Internal review metadata stays out of public DTOs.** Reviewer identity, review dates, checked fields and image hashes are editorial/internal and must not appear in resident payloads (`FlyerSummary`, `FlyerCard`, `ContentItem`) | Matches the Article boundary (Decision 048), where `editorialReview` is not exposed publicly, and the IM-12 public-projection rules |
| MR-7 | **Fail-visible default.** An unconfirmed flyer must not be indistinguishable from a confirmed one. Whether enforcement is fail-closed (runtime) or fail-loud (validator) is a later decision | The validators passed while C-02 existed |
| MR-8 | **Reversible holds stay the hold mechanism.** Withdrawal (`status: "withdrawn"`) remains how a failed review removes a flyer, preserving the record for correction | Already enforced by `EditorialLifecycle.isPublicStatus`; used by C-02 |

## Design direction (non-binding)

The smallest fit identified during C-02 is an optional per-record review object mirroring Article `editorialReview` (status, reviewedDate, reviewer, checked fields), extended with an image hash per MR-2. It would be enforced first by a validator rule and later, if wanted, by a runtime filter in `FlyerService.getAll()`. A V3B stewardship workflow would write the same field. Any implementation needs its own authorization.

## Open editorial items carried from C-02

- FL-001 to FL-007: correct each record against its poster, or replace the image, before removing `withdrawn`.
- FL-005, FL-007: resolve the producer attribution (currently government; posters show non-government producers).
- FL-006: confirm the producer (record: community; poster: government co-host).
- Locate the original upload metadata, if it exists outside the repository and local folders.
