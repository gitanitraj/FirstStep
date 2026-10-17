# IM-04 semantic decision — approved

The user explicitly approved Option 1 during IM-04 execution on October 16, 2026:

> Approve a minimal optional helpAccess field on CivicContent with the sole controlled value ACTIONABLE_HELP_ACCESS.

This is narrow authorization to implement the existing P5 requirement, not a generalized eligibility engine or reopening of P5.

Conditions preserved:

1. Only the exact token positively establishes actionable-help/access semantics; absence does not qualify a Flyer.
2. Unknown/malformed values fail catalog validation and never grant membership.
3. Community, Category/Topic, lifecycle/status, identity and provenance gates remain intact.
4. A single FLYER identity may participate in both Flyers and Get Help without a duplicate or type conversion.
5. Synthetic fixtures prove actionable/non-actionable behavior, dual projection, expiry and invalid metadata handling.
6. All eight production Flyers remain unchanged. This decision grants **no record-level helpAccess assignments**.
7. RESOURCE eligibility and other ContentTypes, event occurrence and featured ranking are outside this change.
8. No new entity, ContentType, generalized eligibility system, Admin interface or workflow infrastructure.

Implementation contract: optional JSON string `helpAccess`; omit it by default. A present null, empty string, unknown token, wrong case, leading/trailing whitespace, number, Boolean, array or object fails the Flyer catalog gate. Only `ACTIONABLE_HELP_ACCESS` is valid when present. Runtime admission compares exact equality; object/array Jackson binding fails instead of granting eligibility. The field is Jackson write-only: stored JSON can populate it, but resident JSON serialization omits it. Normalized ContentItem also does not expose it.

`CategoryContentService.helpFor(categoryKey, topic, communityId)` is the reusable IM-05/08 seam. Null Topic means Category scope; supplied Topic must be declared under that Category and match the record. Unknown Category/Topic yields no results. Existing null/blank Community behavior is retained; explicit Community filters ownership. Lifecycle filtering remains in ResourceService/FlyerService. Resources do not require this field. No other type is admitted by it.

**Implemented capability is distinct from editorial activation:** synthetic acceptance can satisfy IM-04's authorized infrastructure requirement; zero production Flyers have been approved or classified for Get Help. Any live assignment needs a separate editorial decision.
