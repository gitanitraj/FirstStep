# =============================================================================
# ANNOTATED REFERENCE — data-cleaning/scripts/validate_navigation.py (D0.4).
# Mirrors the production script; keep in sync. See references/decisions.md
# Decision 029 and the D0.4/D0.5 plan (valiant-weaving-frog.md).
# =============================================================================
#
# WHAT THIS IS
#   The acceptance gate for app/data/navigation.json — the NAVIGATION artifact
#   that says how canonical topics are grouped for browsing (Category ->
#   topic-group -> topic, the middle level of the four-level nav from Decision
#   021). It validates that artifact against app/data/taxonomy.json.
#
# WHY NAVIGATION IS A SEPARATE FILE FROM THE TAXONOMY (the core design decision)
#   Decision 027 originally planned to put groups inside taxonomy.json, or to
#   generate a navigation.generated.json by analyzing content. The user pushed
#   back during D0.4 planning, correctly: topic GROUPS are a presentation model,
#   not domain vocabulary. Two artifacts, two lifecycles:
#
#       taxonomy.json    domain model      Category -> Subcategory. Stable.
#                                          The vocabulary everything validates
#                                          against. Changes rarely.
#       navigation.json  presentation      Group -> Topics. Editorial. Expected
#                                          to change as the site's information
#                                          architecture evolves, and eventually
#                                          to be AI-generated.
#
#   Why it matters: when navigation generation becomes AI-assisted, ONLY
#   navigation.json is regenerated. The domain taxonomy stays put, the backend
#   keeps aggregating live content, and the frontend is untouched because it
#   renders whatever navigation model it is handed. That is also why the file
#   carries a `source` field ("hand-authored" now, "ai-generated" later) instead
#   of a different FILENAME — provenance changes, wiring does not.
#
#   This script is the contract between those two layers. It is what a future
#   generator's output has to pass before anyone trusts it: the generator
#   changes, the gate does not.
#
# WHY STRUCTURE ONLY, NEVER COUNTS
#   navigation.json stores group labels and topic names — never "12 resources".
#   The backend already aggregates this way at runtime (OrganizationService
#   .getCuratedShortlist() groups loaded resources, counts, ranks, caps), and
#   Slice F's NavigationService will do the same for topics. Baking counts into
#   a data file would duplicate that logic and go stale the moment the loaded
#   data changes — and it would violate the standing principle "backend
#   aggregates and normalizes; frontend only displays" (Decision 019).
#
# WHY ONLY SOME CATEGORIES ARE GROUPED
#   A category absent from navigation.json renders a FLAT topic list. Only
#   housing (8 topics) and community-support (11) are grouped today, because a
#   group header above legal's single topic is noise, not hierarchy. The
#   validator therefore treats absence as legitimate and reports it (FLAT
#   CATEGORIES), rather than demanding every category be grouped.
#
# WHY ITS OWN SCRIPT
#   This repo already runs one validator per data artifact — validate_schema.py
#   for resources, validate_news.py for news. Navigation gets its own for the
#   same reason, and this file mirrors validate_news.py's shape (module
#   docstring with usage, INPUT_FILE constants, validate_*, print_report,
#   main() with argparse, exit 1 on failure) so all three read alike.
#
# =============================================================================

"""
validate_navigation.py
----------------------
Validates app/data/navigation.json — the NAVIGATION PRESENTATION MODEL — against
app/data/taxonomy.json, the canonical domain vocabulary.

navigation.json says how canonical topics are GROUPED for browsing. It never
defines vocabulary and never stores counts. This validator is the gate that keeps
the two layers honest: every category key and every topic named in navigation
must exist in the taxonomy, and a grouped category must place every one of its
topics exactly once (so no canonical topic is unreachable from navigation).

That contract is also what a future AI-generated navigation.json has to pass
before it can be trusted — the generator changes, this gate does not.

Usage:
    python data-cleaning/scripts/validate_navigation.py
    python data-cleaning/scripts/validate_navigation.py --strict

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
#
# Repo-root-relative paths, like every other script in data-cleaning/scripts.

INPUT_FILE    = Path("app/data/navigation.json")
TAXONOMY_FILE = Path("app/data/taxonomy.json")

# Loaded CivicContent, used only to REPORT which topics currently have no content.
# Counts are never written to navigation.json — the backend computes them live.
#
# ANNOTATION: this is the one place the validator touches content at all, and it
# is deliberately informational. "Eviction Prevention has no resources yet" is a
# fact about the DATA, not a defect in the NAVIGATION — the topic is canonically
# valid and Slice F may still want to render it (or hide it) on its own terms.
# Making it an error would couple the presentation model to today's data volume.
RESOURCE_FILES = [
    Path("app/data/resources.json"),
    Path("app/data/resources.communities.json"),
]
FLYER_FILE = Path("app/data/flyers.json")


# ─────────────────────────────────────────
# Loaders
# ─────────────────────────────────────────

def load_taxonomy():
    """Return {category_key: {"label": str, "subcategories": [str]}} plus the
    raw-source-category -> key map used to place resource records."""
    # ANNOTATION: two lookups from one pass. `categories` answers "is this topic
    # legal for this category" (the validation job); `raw_to_key` answers "which
    # display category does this record belong to" (the counting job). The raw ->
    # display mapping lives in taxonomy.json's matchCategories and mirrors
    # CategoryDefinition.java — records carry RAW source category strings
    # ("Mental Health"), not display keys ("health").
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    categories = {}
    raw_to_key = {}
    for cat in data["categories"]:
        categories[cat["key"]] = {
            "label":         cat.get("label", cat["key"]),
            "subcategories": cat.get("subcategories", []),
        }
        for raw in cat.get("matchCategories", []):
            raw_to_key[raw] = cat["key"]
    return categories, raw_to_key


def _records(path):
    # ANNOTATION: the data files disagree on shape — resources.json is a bare
    # list in some snapshots, resources.communities.json wraps records in
    # {"meta": ..., "records": [...]}, flyers.json uses its own key. This mirrors
    # the same tolerant unwrapping validate_schema.py's main() does. Returning []
    # for a missing file keeps the reporting path optional: the validator's REAL
    # job (taxonomy conformance) must not depend on content being present.
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


def count_topics(raw_to_key):
    """Count loaded CivicContent per (category_key, topic).

    EDITORIAL CLASSIFICATION ONLY: a topic is credited by `subcategory`, never by
    descriptive `tags`. The previous rule also counted any tag matching a topic
    name, which let search metadata decide navigation and inflated counts —
    exactly the conflation the CivicContent contract removes (Decision 032).
    """
    # ANNOTATION — THIS FUNCTION USED TO IMPLEMENT THE WRONG RULE.
    #
    # It previously followed taxonomy.json's original note: "a topic gathers
    # CivicContent where resource.subcategory == topic OR content.tags contains
    # the topic". The OR was there to make topics content-type-agnostic — flyers
    # and news carried no subcategory, so their free-form tags were the only
    # handle available.
    #
    # Slice F1 removed the reason for it. Every content type now carries a real
    # `subcategory`, so the workaround is unnecessary — and it was actively
    # harmful:
    #
    #   1. It let DESCRIPTIVE metadata determine NAVIGATION. A record tagged
    #      "Crisis Services" for search purposes was counted as placed under the
    #      Crisis Services topic, whether or not an editor ever classified it
    #      there.
    #   2. It inflated counts. A record whose tags happened to include two topic
    #      names counted toward both, so topic counts did not sum to the
    #      category count and no one could tell why.
    #   3. It made the "empty topic" warning unreliable in the worst direction —
    #      a topic could look populated purely because some unrelated record used
    #      the same word as a search keyword.
    #
    # The old docstring's example is instructive: it cited Eviction Prevention as
    # a topic with "zero resources carrying the subcategory, but flyer FL-002
    # carries the tag". That is still true of the resources — and FL-002 now
    # carries `subcategory: "Eviction Prevention"` as real editorial
    # classification, so the topic is populated for the RIGHT reason. The
    # workaround and the correct answer happened to agree in that one case; they
    # would not have in general.
    #
    # The `tag != subcategory` double-count guard is gone with the rule it
    # guarded.
    counts = Counter()
    for path in RESOURCE_FILES:
        for record in _records(path):
            key = raw_to_key.get(record.get("category"))
            if not key or not record.get("subcategory"):
                continue
            counts[(key, record["subcategory"])] += 1
    return counts


def count_flyer_topics(label_to_key):
    """Flyers now carry their own editorial classification (category_tags +
    subcategory), so they count toward a (category, topic) pair like any other
    CivicContent instead of being credited by free-form tags."""
    # ANNOTATION: the signature change tells the story. The old function was
    # count_flyer_tags() and returned a FLAT Counter of tag names, with this
    # comment: "Flyer tags are free-form and category-less — returned as a flat
    # set of tag names so a topic can be credited in whichever category owns it."
    #
    # "Category-less" was the problem. A flyer had no way to say which category
    # it belonged to, so a tag matching a topic name credited that topic under
    # EVERY category declaring it — FL-002's "Eviction Prevention" tag counted
    # under both housing and legal not because an editor said so, but because the
    # function had no better information.
    #
    # Now it does: flyers.json carries category_tags. The return key is a
    # (category_key, topic) TUPLE, matching count_topics(), so both sources of
    # content are counted the same way. FL-002 still lands under housing AND
    # legal — but now because it is editorially classified as both.
    #
    # label_to_key maps a display label ("Furniture & Household") back to its key
    # ("furniture-household"), because category_tags hold LABELS while the rest of
    # this script keys on the stable category key.
    counts = Counter()
    for flyer in _records(FLYER_FILE):
        topic = flyer.get("subcategory")
        if not topic:
            continue
        for label in flyer.get("category_tags") or []:
            key = label_to_key.get(label)
            if key:
                counts[(key, topic)] += 1
    return counts


# ─────────────────────────────────────────
# Per-category validator
# ─────────────────────────────────────────
#
# ANNOTATION — the five rules and why each exists:
#   1. key exists in taxonomy      navigation may not invent categories
#   2. topic exists in that
#      category's subcategories    catches DRIFT: the real failure mode is
#                                  someone renaming a subcategory in the
#                                  taxonomy and leaving navigation pointing at
#                                  the old name
#   3. group topics COVER every
#      subcategory                 the subtle one. Without it a canonical topic
#                                  can exist, hold content, and be unreachable
#                                  from the UI — invisible data, the worst kind
#                                  of bug because nothing errors
#   4. no topic in two groups      one topic, one home within a category
#      / no duplicate labels       (cross-CATEGORY dual placement is still fine:
#                                  Eviction Prevention lives in housing AND
#                                  legal, per Decision 027)
#   5. a listed category must
#      actually declare groups     an empty groups[] is ambiguous — did someone
#                                  mean "flat" or forget to fill it in? Absence
#                                  is the unambiguous way to say "flat"
#
# Each rule returns a MESSAGE THAT SAYS WHAT TO DO, not just what is wrong —
# these fire on hand-edited JSON, often months later.

def validate_category(entry, index, taxonomy):
    """Validate one navigation category entry. Returns (errors, warnings)."""
    errors   = []
    warnings = []

    key = entry.get("key")
    if not key:
        errors.append(f"Entry #{index} is missing 'key'.")
        return errors, warnings

    # ANNOTATION: early return — with no valid key there is no vocabulary to
    # check the topics against, so every later rule would produce noise.
    if key not in taxonomy:
        errors.append(
            f"Unknown category key '{key}'. It must exist in {TAXONOMY_FILE}."
        )
        return errors, warnings

    groups = entry.get("groups")
    if not groups:
        errors.append(
            f"'{key}' declares no groups. Remove the entry instead — a category "
            f"absent from navigation.json renders a flat topic list."
        )
        return errors, warnings

    valid_topics = set(taxonomy[key]["subcategories"])
    seen_labels  = Counter()
    placed       = Counter()   # topic -> how many groups placed it

    for group in groups:
        label = group.get("label")
        if not label:
            errors.append(f"'{key}' has a group with no label.")
            continue
        seen_labels[label] += 1

        topics = group.get("topics") or []
        if not topics:
            errors.append(f"'{key}' group '{label}' has no topics.")
            continue

        for topic in topics:
            placed[topic] += 1
            if topic not in valid_topics:
                errors.append(
                    f"'{key}' group '{label}' names topic '{topic}', which is not "
                    f"a subcategory of '{key}' in {TAXONOMY_FILE}."
                )

    for label, count in seen_labels.items():
        if count > 1:
            errors.append(f"'{key}' has {count} groups labeled '{label}'.")

    # ANNOTATION: `and topic in valid_topics` prevents a cascade — a bogus topic
    # repeated twice already reported rule 2 for each occurrence; reporting it
    # again here would bury the real message.
    for topic, count in placed.items():
        if count > 1 and topic in valid_topics:
            errors.append(
                f"'{key}' places topic '{topic}' in {count} groups. A topic "
                f"belongs to exactly one group within a category."
            )

    # ANNOTATION: set difference is the whole coverage check. Sorted so the
    # message is stable across runs (dict/set iteration order is not a spec).
    unplaced = sorted(valid_topics - set(placed))
    if unplaced:
        errors.append(
            f"'{key}' groups do not cover every subcategory — unreachable from "
            f"navigation: {unplaced}"
        )

    return errors, warnings


# ─────────────────────────────────────────
# Report printer
# ─────────────────────────────────────────
#
# ANNOTATION: same visual contract as validate_news.py / validate_schema.py —
# a header block of counts, then sections that only appear when they have
# content. `source` is surfaced in the header because "who wrote this file,
# a human or a generator" is the first thing you want to know when a nav
# problem appears.

def print_report(results, ungrouped, empty_topics, source, strict):
    passed = [r for r in results if not r["errors"]]
    failed = [r for r in results if r["errors"]]

    print("\n" + "═" * 62)
    print("  FIRST STEP — NAVIGATION VALIDATOR")
    print("═" * 62)
    print(f"  File       : {INPUT_FILE}")
    print(f"  Taxonomy   : {TAXONOMY_FILE}")
    print(f"  Source     : {source}")
    print(f"  Grouped    : {len(results)}")
    print(f"  Mode       : {'STRICT' if strict else 'normal'}")
    print(f"  ✅  Passed      : {len(passed)}")
    print(f"  ❌  Failed      : {len(failed)}")
    print("═" * 62)

    if failed:
        print("\n── FAILURES ──────────────────────────────────────────────")
        for r in failed:
            print(f"\n  [{r['key']}]")
            for e in r["errors"]:
                print(f"    ✗ {e}")

    if strict:
        warned = [r for r in results if r["warnings"]]
        if warned:
            print("\n── WARNINGS (strict mode) ────────────────────────────────")
            for r in warned:
                print(f"\n  [{r['key']}]")
                for w in r["warnings"]:
                    print(f"    ⚠  {w}")

    # ANNOTATION: printed as INFORMATION, not a problem. It answers the question
    # a reader of this report actually has — "which categories are flat, and did
    # someone mean that?" — without implying they should be grouped.
    if ungrouped:
        print("\n── FLAT CATEGORIES (no groups — renders a flat topic list) ──")
        print(f"  {', '.join(ungrouped)}")

    if empty_topics:
        print("\n── TOPICS WITH NO LOADED CONTENT ─────────────────────────")
        print("  (informational — the topic is valid, the data is not there yet)")
        for key, topic in empty_topics:
            print(f"  · {key} → {topic}")

    any_failure = bool(failed)
    if strict:
        any_failure = any_failure or any(r["warnings"] for r in results)

    if not any_failure:
        print("\n  All checks passed. ✅")

    print("\n" + "═" * 62 + "\n")
    return any_failure


# ─────────────────────────────────────────
# Main
# ─────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Validate navigation.json against the canonical taxonomy."
    )
    # ANNOTATION: --strict exists for symmetry with the other two validators so
    # all three behave alike in CI. No warning currently fires — every navigation
    # rule is a hard error, because a broken navigation model produces silently
    # missing UI rather than a visibly bad record.
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Fail on warnings in addition to errors.",
    )
    args = parser.parse_args()

    for path in (INPUT_FILE, TAXONOMY_FILE):
        if not path.exists():
            print(f"\n❌ File not found: {path}")
            print("   Run from the FIRSTSTEP/ project root.\n")
            sys.exit(1)

    taxonomy, raw_to_key = load_taxonomy()
    data = json.loads(INPUT_FILE.read_text(encoding="utf-8"))

    entries = data.get("categories")
    if not entries:
        print("❌ No categories found in navigation.json.")
        sys.exit(1)

    # ANNOTATION: duplicate keys are detected ACROSS entries, so they cannot live
    # inside validate_category() (which sees one entry at a time). The finding is
    # attached to every offending entry so it shows up wherever you look.
    duplicate_keys = [
        key for key, count in Counter(e.get("key") for e in entries).items()
        if key and count > 1
    ]

    results = []
    for i, entry in enumerate(entries):
        errors, warnings = validate_category(entry, i, taxonomy)
        if entry.get("key") in duplicate_keys:
            errors.append(f"Category '{entry['key']}' appears more than once.")
        results.append({
            "key":      entry.get("key", f"index-{i}"),
            "errors":   errors,
            "warnings": warnings,
        })

    # Taxonomy categories with no navigation entry — flat by design, reported.
    grouped   = {e.get("key") for e in entries}
    ungrouped = sorted(k for k in taxonomy if k not in grouped)

    # ANNOTATION: a topic counts as "has content" if resources place it OR any
    # flyer tags it — the same OR from the topic rule. Eviction Prevention passes
    # on the flyer side alone (FL-002), which is exactly the content-agnostic
    # behavior the taxonomy note promises.
    label_to_key = {meta["label"]: key for key, meta in taxonomy.items()}
    counts       = count_topics(raw_to_key)
    flyer_counts = count_flyer_topics(label_to_key)
    empty_topics = []
    for entry in entries:
        key = entry.get("key")
        if key not in taxonomy:
            continue
        for group in entry.get("groups") or []:
            for topic in group.get("topics") or []:
                if not counts.get((key, topic)) and not flyer_counts.get((key, topic)):
                    empty_topics.append((key, topic))

    had_failure = print_report(
        results, ungrouped, empty_topics,
        source=data.get("source", "unknown"),
        strict=args.strict,
    )

    # Non-zero exit so CI can gate on this, same as the other two validators.
    sys.exit(1 if had_failure else 0)


if __name__ == "__main__":
    main()
