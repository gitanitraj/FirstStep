"""
=============================================================================
ANNOTATED REFERENCE — data-cleaning/scripts/validate_content_sources.py
Slice I (Decision 045); notice-kind rules added in Slice J (Decision 046).
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

WHY THE NOTICE-KIND RULES LANDED HERE TOO (Slice J)
    They needed the SECTOR of each record's producer to know which records the
    rules apply to, and this is the only validator that resolves a producer to a
    sector. Splitting them into a sixth script would have meant loading and
    joining the registry twice.
=============================================================================
"""

"""
validate_content_sources.py
---------------------------
Validates app/data/content-sources.json — the PRODUCER REGISTRY — and every
content record that references it.

The registry answers two questions about a producer: what are they called, and
what sector are they? Content records reference a producer by `id` and never
duplicate its attributes, so this script is what keeps those references honest.

THIS IS THE BUILD GATE. At runtime an unknown `contentSource.id` excludes an item
from sector-scoped views and nothing more — deliberately, because provenance
resolution is a CAPABILITY, not a validity gate, and a civic service must not go
offline over a JSON typo (Decision 045). That design only holds if a bad id
cannot ship, and stopping it is this script's job.

Rules:
    1. Every registry entry has a unique `id`.
    2. Every registry entry has a `sector` drawn from the declared list.
    3. Every `contentSource.id` / `source_id` referenced by content exists here.
    4. A `feedUrl` may only be declared by an entry that has a sector — an
       unattributable feed is how runtime feed titles became identity before.

Usage:
    python data-cleaning/scripts/validate_content_sources.py
    python data-cleaning/scripts/validate_content_sources.py --strict

Run from the project root (FIRSTSTEP/).
"""

import json
import sys
import argparse
from collections import Counter
from pathlib import Path

# ─────────────────────────────────────────
# Config
# ─────────────────────────────────────────

INPUT_FILE = Path("app/data/content-sources.json")
TAXONOMY_FILE = Path("app/data/taxonomy.json")

# Files whose COMMUNITY-sector records must declare a notice kind. Expert answers
# are community-produced too but are Q&A, not notices — they are not browsed
# through Events/Meetings/Announcements, so they are deliberately absent here.
NOTICE_FILES = [
    (Path("app/data/flyers.json"), "contentSource", "tags"),
    (Path("app/data/news.json"), "source_id", "resource_tags"),
]

# Every file whose records reference a producer. `key` is the field that holds
# the reference — news.json keeps its own raw vocabulary (source_id), the rest
# carry a nested contentSource.
CONTENT_FILES = [
    (Path("app/data/news.json"), "source_id"),
    (Path("app/data/flyers.json"), "contentSource"),
    (Path("app/data/expert-answers.json"), "contentSource"),
    (Path("app/data/faq.json"), "contentSource"),
]


# ─────────────────────────────────────────
# Loaders
# ─────────────────────────────────────────

def load_registry():
    data = json.loads(INPUT_FILE.read_text(encoding="utf-8"))
    return data, data.get("sources") or [], data.get("sectors") or []


def load_notice_kinds():
    """The controlled kind vocabulary, from the artifact that already owns
    controlled vocabulary. Not a new file — that would have meant a new loader
    and a new validator for three strings."""
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    return [k.lower() for k in (data.get("noticeKinds") or [])]


def _records(path):
    if not path.exists():
        return []
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        return data
    if "records" in data:
        return data["records"]
    for value in data.values():
        if isinstance(value, list):
            return value
    return []


def referenced_ids(path, key):
    """Every producer id this file references, as {id: [record ids]}."""
    refs = {}
    for record in _records(path):
        if key == "contentSource":
            source_id = (record.get("contentSource") or {}).get("id")
        else:
            source_id = record.get(key)
        if source_id:
            refs.setdefault(source_id, []).append(record.get("id", "?"))
    return refs


# ─────────────────────────────────────────
# Validators
# ─────────────────────────────────────────

def validate_registry(sources, sectors):
    """Rules 1, 2 and 4. Returns (errors, warnings)."""
    errors = []
    warnings = []

    counts = Counter(s.get("id") for s in sources)
    for source_id, count in counts.items():
        if source_id and count > 1:
            errors.append(f"Duplicate producer id '{source_id}' appears {count} times.")
        if not source_id:
            errors.append("A registry entry is missing 'id'.")

    # Two producers claiming one feed makes every item from it ambiguously
    # attributed — the exact failure the registry exists to prevent.
    feeds = Counter(s.get("feedUrl") for s in sources if s.get("feedUrl"))
    for url, count in feeds.items():
        if count > 1:
            owners = sorted(s.get("id") for s in sources if s.get("feedUrl") == url)
            errors.append(
                f"feedUrl '{url}' is claimed by {count} producers ({', '.join(owners)}). "
                f"A feed must have exactly one publisher, or the items it yields "
                f"cannot be attributed."
            )

    for source in sources:
        source_id = source.get("id") or "?"

        sector = source.get("sector")
        if not sector:
            errors.append(
                f"'{source_id}' has no 'sector'. Every producer must declare one — "
                f"an item can only reach Latest Updates or Community Notices "
                f"through its producer's sector."
            )
        elif sectors and sector not in sectors:
            errors.append(
                f"'{source_id}' has sector '{sector}', which is not one of {sectors}."
            )

        if not source.get("name"):
            errors.append(
                f"'{source_id}' has no 'name'. Records no longer carry one, so this "
                f"is the only place a resident's attribution can come from."
            )

        # NOTE: "feedUrl without a sector" was tried here and removed — the
        # every-producer-needs-a-sector rule above always fires first, so it
        # could never trigger on its own. What CAN happen is two producers
        # claiming one feed, which is checked below.

        # `type` on a producer would be a category error: contentSource.type means
        # ingestion format and lives on the RECORD, not on the producer.
        if "type" in source:
            warnings.append(
                f"'{source_id}' declares 'type'. Producers have a SECTOR; "
                f"contentSource.type is a per-record ingestion format."
            )

    return errors, warnings


def validate_references(known_ids):
    """Rule 3. Returns (errors, per-file reference counts)."""
    errors = []
    counts = {}
    for path, key in CONTENT_FILES:
        refs = referenced_ids(path, key)
        counts[path.name] = sum(len(v) for v in refs.values())
        for source_id, record_ids in sorted(refs.items()):
            if source_id not in known_ids:
                errors.append(
                    f"{path.name}: unknown contentSource id '{source_id}' "
                    f"(records: {', '.join(record_ids)}). Add it to {INPUT_FILE} "
                    f"or fix the reference — at runtime these items are excluded "
                    f"from both sector pages."
                )
    return errors, counts


def validate_notice_kinds(kinds, community_ids):
    """Every COMMUNITY-sector notice declares EXACTLY ONE kind.

    Three rules, each with its OWN diagnostic, because a negative test that only
    checks for "some failure" proves nothing about which rule caught it — a dead
    rule shipped that way in Slice I. Rule C in particular is masked by rule A
    unless the record under test also carries a valid kind.
    """
    errors = []
    counted = 0

    for path, source_key, tag_field in NOTICE_FILES:
        for record in _records(path):
            if source_key == "contentSource":
                source_id = (record.get("contentSource") or {}).get("id")
            else:
                source_id = record.get(source_key)
            if source_id not in community_ids:
                continue                      # government content has its own destination
            counted += 1

            rid = record.get("id", "?")
            tags = [t.lower() for t in (record.get(tag_field) or [])]
            found = [t for t in tags if t in kinds]

            # RULE A — no kind at all.
            if not found:
                errors.append(
                    f"{path.name}: '{rid}' is a community notice with NO kind. "
                    f"Add exactly one of {kinds} to {tag_field} — without it the "
                    f"item reaches no Community Notices view."
                )

            # RULE B — more than one. Guessing a winner would hide the ambiguity.
            elif len(set(found)) > 1:
                errors.append(
                    f"{path.name}: '{rid}' declares {len(set(found))} kinds "
                    f"{sorted(set(found))}. A notice is one kind; the views are "
                    f"lenses, but the kind itself is singular."
                )

            # RULE C — a NEAR MISS: plural or variant spelling of a real kind.
            # Fires independently of A and B, so a record can be well-formed and
            # still be caught carrying "meetings" alongside "meeting".
            for tag in tags:
                if tag in kinds:
                    continue
                for kind in kinds:
                    if tag in (kind + "s", kind + "es") or tag.rstrip("s") == kind:
                        errors.append(
                            f"{path.name}: '{rid}' has tag '{tag}', which looks like "
                            f"the kind '{kind}' but is not it. Controlled vocabulary "
                            f"is exact — '{tag}' silently reaches no view."
                        )
                        break

    return errors, counted


# ─────────────────────────────────────────
# Report printer
# ─────────────────────────────────────────

def print_report(sources, reg_errors, reg_warnings, ref_errors, counts, unused, source, strict):
    errors = reg_errors + ref_errors

    print("\n" + "═" * 62)
    print("  FIRST STEP — CONTENT SOURCE VALIDATOR")
    print("═" * 62)
    print(f"  File       : {INPUT_FILE}")
    print(f"  Source     : {source}")
    print(f"  Producers  : {len(sources)}")
    print(f"  References : {sum(counts.values())} across {len(counts)} files")
    print(f"  Mode       : {'STRICT' if strict else 'normal'}")
    print(f"  ✅  Passed      : {len(sources) - len({e.split(chr(39))[1] for e in reg_errors if chr(39) in e})}")
    print(f"  ❌  Failed      : {len(errors)}")
    print("═" * 62)

    if errors:
        print("\n── FAILURES ──────────────────────────────────────────────")
        for e in errors:
            print(f"    ✗ {e}")

    if strict and reg_warnings:
        print("\n── WARNINGS (strict mode) ────────────────────────────────")
        for w in reg_warnings:
            print(f"    ⚠  {w}")

    print("\n── REFERENCES PER FILE ───────────────────────────────────")
    for name, n in counts.items():
        print(f"  {name:24} {n}")

    if unused:
        print("\n── PRODUCERS WITH NO CONTENT ─────────────────────────────")
        print("  (informational — a registered producer that has published nothing yet)")
        print(f"  {', '.join(unused)}")

    any_failure = bool(errors) or (strict and bool(reg_warnings))
    if not any_failure:
        print("\n  All checks passed. ✅")

    print("\n" + "═" * 62 + "\n")
    return any_failure


# ─────────────────────────────────────────
# Main
# ─────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Validate the producer registry and every reference to it."
    )
    parser.add_argument("--strict", action="store_true", help="Fail on warnings too.")
    args = parser.parse_args()

    if not INPUT_FILE.exists():
        print(f"\n❌ File not found: {INPUT_FILE}")
        print("   Run from the FIRSTSTEP/ project root.\n")
        sys.exit(1)

    data, sources, sectors = load_registry()
    if not sources:
        print("❌ No sources found in content-sources.json.")
        sys.exit(1)

    known_ids = {s.get("id") for s in sources if s.get("id")}

    reg_errors, reg_warnings = validate_registry(sources, sectors)
    ref_errors, counts = validate_references(known_ids)

    community_ids = {s["id"] for s in sources if s.get("sector") == "community"}
    kind_errors, notice_count = validate_notice_kinds(load_notice_kinds(), community_ids)
    ref_errors = ref_errors + kind_errors
    counts["community notices"] = notice_count

    used = set()
    for path, key in CONTENT_FILES:
        used.update(referenced_ids(path, key).keys())
    # A producer with a feed publishes at runtime, so it is never "unused".
    has_feed = {s["id"] for s in sources if s.get("feedUrl")}
    unused = sorted(known_ids - used - has_feed)

    had_failure = print_report(
        sources, reg_errors, reg_warnings, ref_errors, counts, unused,
        source=data.get("source", "unknown"), strict=args.strict,
    )
    sys.exit(1 if had_failure else 0)


if __name__ == "__main__":
    main()

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
#   Added in Slice J, applying ONLY to community-sector notice records:
#
#   A. Every community notice declares a kind.
#         Without one it reaches no kind view — invisible on the destination
#         while looking perfectly fine in the file.
#
#   B. No community notice declares MORE than one.
#         TaxonomyService.noticeKindOf returns empty for two kinds rather than
#         guessing, so such a record also vanishes. The views are lenses and
#         overlap by design; the KIND itself is singular.
#
#   C. A near-miss spelling is an error, not a shrug.
#         "meetings", "events" and "announcements" all read as correct to a human
#         author and match nothing. This rule names the intended kind.
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

# =============================================================================
# SECTION 6 — SLICE J: THE TESTING RULE, APPLIED RATHER THAN JUST RECORDED
# =============================================================================
# Section 2's lesson became a standing rule in CLAUDE.md:
#
#     Negative tests must verify the intended failure path, not merely assert
#     that invalid input produces some failure.
#
# The three new rules are where it was first applied deliberately. All three
# reject a community notice, and their conditions OVERLAP — a record tagged
# "meetings" has no valid kind (rule A) and a near-miss spelling (rule C). A test
# that only asserted "exit code non-zero" would pass on either, and would keep
# passing if rule C were deleted entirely. That is exactly how the dead feedUrl
# rule survived its own test in Slice I.
#
# So each rule is negative-tested against ITS OWN MESSAGE:
#
#   A  "…is a community notice with NO kind."
#   B  "…declares 2 kinds ['event', 'meeting']."
#   C  "…looks like the kind 'meeting' but is not it."
#
# Rule C's implementation carries the same care. It checks `if tag in kinds:
# continue` BEFORE the near-miss comparison, so a record carrying a valid kind is
# never reported for a near-miss it also happens to contain — the collision guard
# that keeps the rules from firing on each other's inputs.
#
# =============================================================================
# SECTION 7 — WHY THE KIND RULES ARE SCOPED TO COMMUNITY RECORDS
# =============================================================================
# Government content has its own destination (Latest Updates) and is never
# selected by kind. Requiring kinds on it would have been a validator demanding
# metadata nothing reads — the kind of rule that gets suppressed rather than
# satisfied, and takes the credibility of the other rules with it.
#
# Expert answers and FAQs are excluded for the same reason: they are not notices.
# The scoping list is explicit at the top of the file rather than inferred, so a
# new content type is not silently swept into a rule that does not fit it.
# =============================================================================
