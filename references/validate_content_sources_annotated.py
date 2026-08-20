"""
=============================================================================
ANNOTATED REFERENCE — data-cleaning/scripts/validate_content_sources.py
Slice I. See references/decisions.md Decision 045.
Keep this mirror in sync whenever the production script changes.
=============================================================================

WHAT THIS SCRIPT IS
    The gate for app/data/content-sources.json — the producer registry — and for
    every content record that references it. The fifth per-artifact validator,
    following validate_navigation.py's shape.

WHY IT IS LOAD-BEARING RATHER THAN HYGIENE
    At RUNTIME an unknown contentSource.id excludes an item from sector-scoped
    views and nothing more. That is deliberate: provenance resolution is a
    CAPABILITY, not a validity gate, and a civic service must not go offline over
    a JSON typo.

    THAT DESIGN ONLY HOLDS IF A BAD ID CANNOT SHIP. Stopping it is this script's
    entire job. The runtime is defense in depth; this is the defense.
=============================================================================
"""

# =============================================================================
# SECTION 1 — THE RULES
# =============================================================================
#
#   1. Every registry entry has a UNIQUE id.
#         Two entries with one id means the second silently loses; the loader
#         keeps the first and logs, but the author's intent is unknowable.
#
#   2. Every registry entry has a `sector` from the declared list, and a `name`.
#         Without a sector an item can reach neither destination page. Without a
#         name a resident sees no attribution at all — records no longer carry
#         one, so this is the only place it can come from, which brushes against
#         04-editorial-principles' "attribute information to its source".
#
#   3. Every id referenced by content EXISTS in the registry.
#         The rule that actually fires in practice. Checked across news.json
#         (source_id), flyers.json, expert-answers.json and faq.json
#         (contentSource.id) — news keeps its own raw vocabulary, so the field
#         name differs and the script carries a per-file key.
#
#   4. No two producers claim the same feedUrl.
#         See SECTION 2 — this replaced a rule that could never fire.
#
# =============================================================================
# SECTION 2 — A RULE THAT WAS WRITTEN, TESTED, AND THEN DELETED
# =============================================================================
# The first version had "a feedUrl requires a sector". It negative-tested green,
# which looked like success.
#
# It was DEAD LOGIC. Rule 2 already requires a sector on EVERY producer, so the
# feed-specific check could never fire on its own — the negative test passed
# because the OTHER rule caught it, and the error message printed proved it: it
# was rule 2's message, not this one's.
#
# Replaced with duplicate-feedUrl detection, which is genuinely non-redundant: two
# producers claiming one feed makes every item it yields ambiguously attributed —
# exactly the ambiguity the registry exists to remove. That version fails with its
# OWN message.
#
# THE LESSON WORTH KEEPING: a negative test that passes only tells you SOMETHING
# rejected the input. Read the message to learn what.
#
# =============================================================================
# SECTION 3 — WHAT IT REPORTS BUT DOES NOT FAIL ON
# =============================================================================
# A producer in the registry that no content references is INFORMATIONAL, not an
# error — an organisation can be registered before it has published anything.
# Producers with a `feedUrl` are excluded from that list entirely, since they
# publish at runtime and would otherwise always look unused.
#
# `type` on a producer is a strict-mode WARNING rather than an error. It is
# almost certainly a category mistake — producers have a SECTOR; contentSource
# .type is a per-record ingestion format — but it is inert rather than harmful.
#
# =============================================================================
# SECTION 4 — THE COST, RECORDED HONESTLY
# =============================================================================
# This is the SIXTH separate taxonomy-style loader in the codebase, after
# validate_schema, validate_news, validate_navigation, validate_homepage and
# Java's TaxonomyService. Extracting a shared loader is tech-debt item 1, deferred
# by the user with a clear instruction: raise it, do not start it.
#
# Raised here rather than fixed. Following the established pattern kept this
# script reviewable beside its four siblings; unifying six loaders mid-slice would
# mix an architectural refactor into functional work, which Decision 031 forbids.
#
# =============================================================================
# SECTION 5 — KNOCK-ON: validate_news.py CHANGED TOO
# =============================================================================
# news.json's REQUIRED_FIELDS listed `source_name`. The registry migration
# replaced it with `source_id`, so that validator was updated in the same breath —
# it went red immediately, which is the schema gate working.
#
# The two validators now split the job: validate_news checks that the FIELD is
# present; validate_content_sources checks that its VALUE resolves.
# =============================================================================
