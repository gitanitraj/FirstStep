"""
validate_homepage.py
--------------------
Validates app/data/homepage.json — the HOMEPAGE PRESENTATION MODEL — against
app/data/taxonomy.json, the canonical domain vocabulary.

homepage.json says WHICH discovery pathways the homepage's Community Resources
column offers and in what order. It never defines vocabulary. This validator is
the gate that keeps the two layers honest, and it enforces the one rule Slice H's
architecture rests on (Decision 041):

    A pathway is a UX composition, not a domain entity.

    kind="category"  -> MUST name a key that exists in taxonomy.json, and MUST
                        NOT author its own label/icon (they are resolved from the
                        taxonomy, so two files can never drift).
    kind="discovery" -> a controlled query over existing CivicContent metadata.
                        MUST author its own label/icon, and MUST NOT collide with
                        a taxonomy key — a discovery pathway that is also a
                        category means someone promoted a facet into the
                        vocabulary, which is exactly what must not happen.

That last check is the one with teeth: it is what would catch "Seniors" being
quietly added to taxonomy.json to make the homepage easier to build.

Usage:
    python data-cleaning/scripts/validate_homepage.py
    python data-cleaning/scripts/validate_homepage.py --strict

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

INPUT_FILE    = Path("app/data/homepage.json")
TAXONOMY_FILE = Path("app/data/taxonomy.json")

CATEGORY  = "category"
DISCOVERY = "discovery"
VALID_KINDS = (CATEGORY, DISCOVERY)


# ─────────────────────────────────────────
# Loader
# ─────────────────────────────────────────

def load_taxonomy():
    """Return {category_key: label} — the canonical vocabulary this file
    validates against and resolves labels from."""
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    return {cat["key"]: cat.get("label", cat["key"]) for cat in data["categories"]}


# ─────────────────────────────────────────
# Per-pathway validator
# ─────────────────────────────────────────

def validate_pathway(entry, index, taxonomy):
    """Validate one Community Resources pathway. Returns (errors, warnings)."""
    errors   = []
    warnings = []

    key = entry.get("key")
    if not key:
        errors.append(f"Entry #{index} is missing 'key'.")
        return errors, warnings

    kind = entry.get("kind")
    if kind not in VALID_KINDS:
        errors.append(
            f"'{key}' has kind '{kind}'. Must be one of {list(VALID_KINDS)} — the "
            f"kind is what records whether a pathway is taxonomy or a query."
        )
        return errors, warnings

    if kind == CATEGORY:
        if key not in taxonomy:
            errors.append(
                f"Unknown category key '{key}'. A category pathway must exist in "
                f"{TAXONOMY_FILE}."
            )
        for field in ("label", "icon"):
            if entry.get(field):
                errors.append(
                    f"'{key}' authors its own '{field}'. A category pathway must "
                    f"resolve {field} from {TAXONOMY_FILE} so the two files cannot "
                    f"drift. Remove it here."
                )

    if kind == DISCOVERY:
        if key in taxonomy:
            errors.append(
                f"'{key}' is a discovery pathway but ALSO a category in "
                f"{TAXONOMY_FILE}. A facet must not be promoted into the "
                f"vocabulary — it answers 'who is this relevant to?', which the "
                f"taxonomy never asked."
            )
        for field in ("label", "icon"):
            if not entry.get(field):
                errors.append(
                    f"'{key}' is a discovery pathway and must author its own "
                    f"'{field}' — it has no taxonomy entry to resolve one from."
                )

    return errors, warnings


# ─────────────────────────────────────────
# Report printer
# ─────────────────────────────────────────

def print_report(results, unused, source, strict):
    passed = [r for r in results if not r["errors"]]
    failed = [r for r in results if r["errors"]]

    print("\n" + "═" * 62)
    print("  FIRST STEP — HOMEPAGE VALIDATOR")
    print("═" * 62)
    print(f"  File       : {INPUT_FILE}")
    print(f"  Taxonomy   : {TAXONOMY_FILE}")
    print(f"  Source     : {source}")
    print(f"  Pathways   : {len(results)}")
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

    if unused:
        print("\n── CATEGORIES NOT ON THE HOMEPAGE ────────────────────────")
        print("  (informational — the homepage is a curated front door, not an")
        print("   index. These stay reachable through Discover.)")
        print(f"  {', '.join(unused)}")

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
        description="Validate homepage.json against the canonical taxonomy."
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

    taxonomy = load_taxonomy()
    data = json.loads(INPUT_FILE.read_text(encoding="utf-8"))

    entries = data.get("communityResources")
    if not entries:
        print("❌ No communityResources found in homepage.json.")
        sys.exit(1)

    duplicate_keys = [
        key for key, count in Counter(e.get("key") for e in entries).items()
        if key and count > 1
    ]

    results = []
    for i, entry in enumerate(entries):
        errors, warnings = validate_pathway(entry, i, taxonomy)
        if entry.get("key") in duplicate_keys:
            errors.append(f"Pathway '{entry['key']}' appears more than once.")
        results.append({
            "key":      entry.get("key", f"index-{i}"),
            "errors":   errors,
            "warnings": warnings,
        })

    on_homepage = {e.get("key") for e in entries if e.get("kind") == CATEGORY}
    unused = sorted(k for k in taxonomy if k not in on_homepage)

    had_failure = print_report(
        results, unused,
        source=data.get("source", "unknown"),
        strict=args.strict,
    )

    sys.exit(1 if had_failure else 0)


if __name__ == "__main__":
    main()
