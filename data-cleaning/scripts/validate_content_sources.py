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
