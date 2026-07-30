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

INPUT_FILE    = Path("app/data/navigation.json")
TAXONOMY_FILE = Path("app/data/taxonomy.json")

# Loaded CivicContent, used only to REPORT which topics currently have no content.
# Counts are never written to navigation.json — the backend computes them live.
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

def validate_category(entry, index, taxonomy):
    """Validate one navigation category entry. Returns (errors, warnings)."""
    errors   = []
    warnings = []

    key = entry.get("key")
    if not key:
        errors.append(f"Entry #{index} is missing 'key'.")
        return errors, warnings

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
    placed       = Counter()

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

    for topic, count in placed.items():
        if count > 1 and topic in valid_topics:
            errors.append(
                f"'{key}' places topic '{topic}' in {count} groups. A topic "
                f"belongs to exactly one group within a category."
            )

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

    grouped   = {e.get("key") for e in entries}
    ungrouped = sorted(k for k in taxonomy if k not in grouped)

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

    sys.exit(1 if had_failure else 0)


if __name__ == "__main__":
    main()
