"""
validate_schema.py
------------------
Validates every record in app/data/resources.json against the
Wilmington Civic App v2 schema.

Usage:
    python data-cleaning/scripts/validate_schema.py
    python data-cleaning/scripts/validate_schema.py --input app/data/resources.communities.json
    python data-cleaning/scripts/validate_schema.py --strict
    python data-cleaning/scripts/validate_schema.py --check-urls
    python data-cleaning/scripts/validate_schema.py --export-schema

Run from the project root (FIRSTSTEP/).

Flags:
    --strict        Treat warnings as failures (non-zero exit on any warning).
    --check-urls    Send HEAD requests to every website URL. Slow + needs
                    network; off by default so normal/CI runs stay fast and
                    deterministic. Unreachable URLs are reported as WARNINGS,
                    never errors.
    --export-schema Write an OpenAPI-style JSON Schema to data-cleaning/schema.json
                    and exit. This is a deliberate, separate action — it does NOT
                    run as part of validation.
"""

import argparse
import json
import sys
from pathlib import Path

# ─────────────────────────────────────────
# Config — paths relative to project root
# ─────────────────────────────────────────

INPUT_FILE  = Path("app/data/resources.json")
SCHEMA_FILE = Path("data-cleaning/schema.json")

# ─────────────────────────────────────────
# Valid controlled vocabulary
# ─────────────────────────────────────────

# Controlled vocabulary comes from TWO artifacts, deliberately split (Decision 034):
#   taxonomy.json        First Step's canonical editorial vocabulary.
#   source-mappings.json how an upstream provider's category strings translate
#                        into it — a deterministic source adapter.
# Records here carry RAW provider categories, so validation keys on those and
# resolves each to its canonical category via the source mappings. Same variable
# names as before, so the rest of the validator is unchanged.
TAXONOMY_FILE        = Path("app/data/taxonomy.json")
SOURCE_MAPPINGS_FILE = Path("app/data/source-mappings.json")


def _load_taxonomy():
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    subs_by_key = {c["key"]: set(c.get("subcategories", [])) for c in data["categories"]}

    mappings = json.loads(SOURCE_MAPPINGS_FILE.read_text(encoding="utf-8"))
    cat_to_sub = {}
    for source in mappings.get("sources", []):
        for raw, key in (source.get("mappings") or {}).items():
            if key not in subs_by_key:
                raise SystemExit(
                    f"source-mappings.json maps '{raw}' to unknown category key '{key}'"
                )
            cat_to_sub[raw] = subs_by_key[key]

    valid_categories = set(cat_to_sub.keys())
    valid_subcategories = {s for subs in subs_by_key.values() for s in subs}
    return valid_categories, cat_to_sub, valid_subcategories


VALID_CATEGORIES, CATEGORY_TO_SUBCATEGORY, VALID_SUBCATEGORIES = _load_taxonomy()

VALID_URGENCY = {"emergency", "time-limited", "standard"}
VALID_COST    = {"free", "low-cost"}
VALID_ACCESS  = {"in-person", "online"}
VALID_GENDER  = {"any", "male", "female"}

# ─────────────────────────────────────────
# Required top-level fields
# ─────────────────────────────────────────

REQUIRED_FIELDS = [
    "id",
    "category",
    "subcategory",
    "organization",
    "summary",
    "population",
    "eligibility",
    "eligibility_age_min",
    "eligibility_age_max",
    "eligibility_gender",
    "locations",
    "phones",
    "websites",
    "county",
    "access_mode",
    "cost",
    "urgency",
    "tags",
    "source",
    "retrieved",
    "verified",
    "notes",
]

# ─────────────────────────────────────────
# Location object required fields
# ─────────────────────────────────────────

REQUIRED_LOCATION_FIELDS = ["label", "address", "city", "state", "zip", "confidential"]


# ─────────────────────────────────────────
# Per-record validator
# ─────────────────────────────────────────

def validate_record(record, index, check_urls=False):
    """
    Validate a single record. Returns (errors, warnings) as two lists.
    An empty errors list means the record passed all hard checks.
    """
    errors   = []
    warnings = []

    # ── 1. Required fields present ──────────────────────────────
    for field in REQUIRED_FIELDS:
        if field not in record:
            errors.append(f"Missing required field: '{field}'")

    # ── 2. ID format ─────────────────────────────────────────────
    rid = record.get("id", "")
    if rid:
        prefix = rid.split("-")[0] if "-" in rid else ""
        # CI/FH/HA = the hand-curated resources.json sets; SD = records mapped
        # from the DSCYF Service Directory (resources.communities.json).
        if prefix not in ("CI", "FH", "HA", "SD"):
            errors.append(f"ID '{rid}' has unexpected prefix. Expected CI, FH, HA, or SD.")

    # ── 3. Controlled vocabulary ─────────────────────────────────
    cat = record.get("category")
    if cat and cat not in VALID_CATEGORIES:
        errors.append(f"Invalid category: '{cat}'")

    subcat = record.get("subcategory")
    if subcat and subcat not in VALID_SUBCATEGORIES:
        errors.append(f"Invalid subcategory: '{subcat}'")

    # NEW: subcategory must belong to its parent category
    if cat in CATEGORY_TO_SUBCATEGORY and subcat:
        if subcat not in CATEGORY_TO_SUBCATEGORY[cat]:
            errors.append(
                f"Subcategory '{subcat}' does not belong to category '{cat}'."
            )

    urgency = record.get("urgency")
    if urgency and urgency not in VALID_URGENCY:
        errors.append(f"Invalid urgency: '{urgency}'. Must be: {VALID_URGENCY}")

    cost = record.get("cost")
    if cost and cost not in VALID_COST:
        errors.append(f"Invalid cost: '{cost}'. Must be: {VALID_COST}")

    gender = record.get("eligibility_gender")
    if gender and gender not in VALID_GENDER:
        errors.append(f"Invalid eligibility_gender: '{gender}'. Must be: {VALID_GENDER}")

    access = record.get("access_mode", [])
    if isinstance(access, list):
        for mode in access:
            if mode not in VALID_ACCESS:
                errors.append(f"Invalid access_mode value: '{mode}'. Must be: {VALID_ACCESS}")
    else:
        errors.append(f"access_mode must be a list, got: {type(access).__name__}")

    # ── 4. Summary not empty ─────────────────────────────────────
    summary = record.get("summary", "")
    if not summary or summary.strip() == "":
        errors.append("summary is empty.")
    elif len(summary) > 300:
        warnings.append(f"summary is {len(summary)} chars — consider shortening to under 300.")

    # ── 5. Locations structure ───────────────────────────────────
    locations = record.get("locations", [])
    if not isinstance(locations, list):
        errors.append("locations must be a list.")
    elif len(locations) == 0:
        errors.append("locations list is empty — every record needs at least one location entry.")
    else:
        for i, loc in enumerate(locations):
            for lf in REQUIRED_LOCATION_FIELDS:
                if lf not in loc:
                    errors.append(f"Location[{i}] missing field: '{lf}'")

            # If not confidential, city and state should be present
            if not loc.get("confidential", False):
                if not loc.get("city"):
                    errors.append(f"Location[{i}] is not confidential but 'city' is null or missing.")
                if not loc.get("state"):
                    errors.append(f"Location[{i}] is not confidential but 'state' is null or missing.")

    # ── 6. Phones structure ──────────────────────────────────────
    phones = record.get("phones", [])
    if not isinstance(phones, list):
        errors.append("phones must be a list.")
    else:
        for i, ph in enumerate(phones):
            if "number" not in ph:
                errors.append(f"Phone[{i}] missing 'number' field.")
            if "label" not in ph:
                errors.append(f"Phone[{i}] missing 'label' field.")

    # ── 7. Websites structure ────────────────────────────────────
    websites = record.get("websites", [])
    if not isinstance(websites, list):
        errors.append("websites must be a list.")
    else:
        for i, site in enumerate(websites):
            if "url" not in site:
                errors.append(f"Website[{i}] missing 'url' field.")
            if "label" not in site:
                errors.append(f"Website[{i}] missing 'label' field.")
            url = site.get("url", "")
            if url and not url.startswith("http"):
                errors.append(f"Website[{i}] url doesn't start with http: '{url}'")
            # NEW (opt-in): reachability is a WARNING, never an error.
            elif url and check_urls:
                ok, detail = _check_url(url)
                if not ok:
                    warnings.append(f"Website[{i}] {detail}: '{url}'")

    # ── 8. Age range logic ───────────────────────────────────────
    age_min = record.get("eligibility_age_min")
    age_max = record.get("eligibility_age_max")
    if age_min is not None and age_max is not None:
        if age_min > age_max:
            errors.append(f"eligibility_age_min ({age_min}) is greater than age_max ({age_max}).")

    # ── 9. verified must be boolean ──────────────────────────────
    verified = record.get("verified")
    if not isinstance(verified, bool):
        errors.append(f"verified must be true or false (boolean), got: {type(verified).__name__}")

    # ── 10. tags must be a list ──────────────────────────────────
    tags = record.get("tags", [])
    if not isinstance(tags, list):
        errors.append("tags must be a list.")

    # ── 11. county present ───────────────────────────────────────
    county = record.get("county", "")
    if not county or county.strip() == "":
        errors.append("county is missing or empty.")

    # ── Soft warnings — flags, not failures ──────────────────────
    if not record.get("description"):
        warnings.append("description is null — consider adding full text.")
    if not record.get("parent_organization"):
        warnings.append("parent_organization is null — confirm this is intentional.")
    if not record.get("phones"):
        warnings.append("No phone numbers listed.")
    if not record.get("websites"):
        warnings.append("No websites listed.")
    if not record.get("tags"):
        warnings.append("tags list is empty.")
    if not record.get("notes"):
        warnings.append("notes field is empty.")

    return errors, warnings


# ─────────────────────────────────────────
# URL reachability (opt-in, --check-urls)
# ─────────────────────────────────────────

def _check_url(url):
    """Return (ok, detail). Soft check — failures become warnings."""
    try:
        import requests
    except ImportError:
        return False, "reachability skipped (requests not installed)"
    try:
        resp = requests.head(url, timeout=3, allow_redirects=True)
        # Some servers reject HEAD; treat those as inconclusive, not failures.
        if resp.status_code in (403, 405):
            return True, ""
        if resp.status_code >= 400:
            return False, f"unreachable (HTTP {resp.status_code})"
        return True, ""
    except Exception:
        return False, "unreachable (network error)"


# ─────────────────────────────────────────
# Cross-record checks (duplicates)
# ─────────────────────────────────────────

def detect_duplicates(records):
    """Return (errors, warnings) as two lists of duplicate-related strings.

    The listing key is organization + address + category + subcategory. Both
    category levels belong in it because the same service is legitimately listed
    under several source categories — Learning Tree Academy appears under
    Before/After School Care, Child Care AND Early Childhood/Pre-K; the curated
    thrift records are dual-listed under Clothing and Furniture. Keying on
    org+address alone flagged 37 of those as duplicates.

    Two tiers, because a key collision means different things:
      - identical summary → a genuine duplicate listing (ERROR)
      - different summary → one organization running several programs at one
        site, e.g. Brandywine Valley SPCA's three volunteer programs. Worth an
        eyeball, not a failure (WARNING).
    """
    errors = []
    warnings = []

    # Duplicate IDs
    seen_ids = set()
    for r in records:
        rid = r.get("id")
        if rid is None:
            continue
        if rid in seen_ids:
            errors.append(f"Duplicate ID: '{rid}'")
        seen_ids.add(rid)

    # Duplicate listings
    seen_pairs = {}
    for r in records:
        org = r.get("organization")
        for loc in r.get("locations", []):
            addr = loc.get("address")
            # Skip confidential / addressless rows — collisions there are expected.
            if not addr or loc.get("confidential"):
                continue
            key = (org, addr, r.get("category"), r.get("subcategory"))
            label = f"{org} @ {addr} ({r.get('subcategory')})"
            first = seen_pairs.get(key)
            if first is None:
                seen_pairs[key] = r
            elif first.get("summary") == r.get("summary"):
                errors.append(f"Duplicate listing: {label} — {first.get('id')} and {r.get('id')} are identical")
            else:
                warnings.append(f"Same site and topic, different service: {label} — {first.get('id')} and {r.get('id')}")

    return errors, warnings


# ─────────────────────────────────────────
# JSON Schema export (opt-in, --export-schema)
# ─────────────────────────────────────────

def export_json_schema():
    """Write an OpenAPI-style JSON Schema. Deliberate, separate action."""
    schema = {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "title": "First Step Resource Schema",
        "type": "object",
        "required": REQUIRED_FIELDS,
        "properties": {
            "id": {"type": "string"},
            "category": {"type": "string", "enum": sorted(VALID_CATEGORIES)},
            "subcategory": {"type": "string", "enum": sorted(VALID_SUBCATEGORIES)},
            "organization": {"type": "string"},
            "parent_organization": {"type": ["string", "null"]},
            "summary": {"type": "string"},
            "description": {"type": ["string", "null"]},
            "population": {"type": "string"},
            "eligibility": {"type": "string"},
            "eligibility_age_min": {"type": ["integer", "null"]},
            "eligibility_age_max": {"type": ["integer", "null"]},
            "eligibility_gender": {"type": "string", "enum": sorted(VALID_GENDER)},
            "locations": {"type": "array"},
            "phones": {"type": "array"},
            "websites": {"type": "array"},
            "county": {"type": "string"},
            "access_mode": {"type": "array"},
            "cost": {"type": "string", "enum": sorted(VALID_COST)},
            "urgency": {"type": "string", "enum": sorted(VALID_URGENCY)},
            "tags": {"type": "array"},
            "source": {"type": "string"},
            "retrieved": {"type": "string"},
            "verified": {"type": "boolean"},
            "notes": {"type": ["string", "null"]},
        },
    }
    SCHEMA_FILE.parent.mkdir(parents=True, exist_ok=True)
    SCHEMA_FILE.write_text(json.dumps(schema, indent=2))
    print(f"Exported JSON schema → {SCHEMA_FILE}")


# ─────────────────────────────────────────
# Report printer
# ─────────────────────────────────────────

def print_report(results, total, dup_errors, dup_warnings, strict, input_file=INPUT_FILE):
    failed   = [r for r in results if r["errors"]]
    warned   = [r for r in results if r["warnings"]]
    passed   = [r for r in results if not r["errors"]]

    print("\n" + "═" * 60)
    print("  WILMINGTON CIVIC APP — SCHEMA VALIDATOR")
    print("═" * 60)
    print(f"  File:    {input_file}")
    print(f"  Records: {total}")
    print(f"  ✅ Passed:    {len(passed)}")
    print(f"  ❌ Failed:    {len(failed)}")
    print(f"  ⚠️  Warnings:  {len(warned)}")
    if strict:
        print("  (strict mode: warnings count as failures)")
    print("═" * 60)

    if failed:
        print("\n── FAILURES ──────────────────────────────────────────")
        for r in failed:
            print(f"\n  [{r['id']}] {r['org']}")
            for e in r["errors"]:
                print(f"    ✗ {e}")

    if dup_errors:
        print("\n── DUPLICATES ────────────────────────────────────────")
        for e in dup_errors:
            print(f"    ✗ {e}")

    if dup_warnings:
        print("\n── MULTI-PROGRAM SITES ───────────────────────────────")
        for w in dup_warnings:
            print(f"    ! {w}")

    if warned and (strict or not failed):
        print("\n── WARNINGS ──────────────────────────────────────────")
        for r in warned:
            print(f"\n  [{r['id']}] {r['org']}")
            for w in r["warnings"]:
                print(f"    ! {w}")

    if not failed and not dup_errors:
        print("\n  All records passed schema validation. ✅")

    print("\n" + "═" * 60 + "\n")

    failure_count = len(failed) + (1 if dup_errors else 0)
    if strict:
        failure_count += sum(1 for r in results if r["warnings"])
        failure_count += len(dup_warnings)
    return failure_count


# ─────────────────────────────────────────
# Main
# ─────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Validate resources.json against the v2 schema.")
    parser.add_argument("--input", default=str(INPUT_FILE),
                        help="Resource JSON file to validate (default: app/data/resources.json).")
    parser.add_argument("--strict", action="store_true",
                        help="Treat warnings as failures.")
    parser.add_argument("--check-urls", action="store_true",
                        help="Send HEAD requests to website URLs (slow, needs network).")
    parser.add_argument("--export-schema", action="store_true",
                        help="Write data-cleaning/schema.json and exit (does not validate).")
    args = parser.parse_args()

    if args.export_schema:
        export_json_schema()
        sys.exit(0)

    input_file = Path(args.input)
    if not input_file.exists():
        print(f"\n❌ File not found: {input_file}")
        print("   Run this script from the FIRSTSTEP/ project root.\n")
        sys.exit(1)

    with open(input_file, encoding="utf-8") as f:
        data = json.load(f)

    # Support both bare list and wrapped {"records": [...]} formats
    if isinstance(data, list):
        records = data
    elif isinstance(data, dict) and "records" in data:
        records = data["records"]
    else:
        print("❌ Unexpected JSON structure. Expected a list or {records: [...]}")
        sys.exit(1)

    results = []
    for i, record in enumerate(records):
        errors, warnings = validate_record(record, i, check_urls=args.check_urls)
        results.append({
            "id":       record.get("id", f"index-{i}"),
            "org":      record.get("organization", f"Record #{i}"),
            "errors":   errors,
            "warnings": warnings,
        })

    dup_errors, dup_warnings = detect_duplicates(records)

    failure_count = print_report(results, len(records), dup_errors, dup_warnings,
                                 args.strict, input_file)

    # Exit with error code so CI/CD pipelines can detect failures
    sys.exit(1 if failure_count > 0 else 0)


if __name__ == "__main__":
    main()
