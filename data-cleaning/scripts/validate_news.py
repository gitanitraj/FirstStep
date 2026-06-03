"""
validate_news.py
----------------
Validates every record in app/data/news.json against the
Wilmington Civic App news/policy update schema.

Usage:
    python data-cleaning/scripts/validate_news.py
    python data-cleaning/scripts/validate_news.py --strict
    python data-cleaning/scripts/validate_news.py --show-expired

Run from the project root (FIRSTSTEP/).
"""

import json
import sys
import argparse
from pathlib import Path
from datetime import date

# ─────────────────────────────────────────
# Config
# ─────────────────────────────────────────

INPUT_FILE = Path("app/data/news.json")
TODAY      = date.today()

# ─────────────────────────────────────────
# Controlled vocabulary
# ─────────────────────────────────────────

VALID_TYPES = {
    "policy-update",
    "program-change",
    "deadline",
    "new-resource",
    "general-news",
}

VALID_GEOGRAPHY = {"wilmington", "delaware", "both"}
VALID_URGENCY   = {"emergency", "time-limited", "standard"}
VALID_AUTHOR    = {"manual", "rss", "api"}

VALID_CATEGORY_TAGS = {
    "Clothing & Incidentals",
    "Furniture & Household Items",
    "Housing Assistance",
    "General",
}

REQUIRED_FIELDS = [
    "id", "type", "headline", "summary", "body", "why_it_matters",
    "published", "expires", "geography", "source_name", "source_url",
    "category_tags", "resource_tags", "urgency", "author", "verified", "active",
]

# ─────────────────────────────────────────
# Single-record validator
# ─────────────────────────────────────────

def validate_record(record, index):
    errors   = []
    warnings = []

    # 1. Required fields
    for field in REQUIRED_FIELDS:
        if field not in record:
            errors.append(f"Missing required field: '{field}'")

    # 2. ID format — NP-###
    rid = record.get("id", "")
    if rid:
        parts = rid.split("-")
        if len(parts) != 2 or parts[0] != "NP" or not parts[1].isdigit():
            errors.append(f"ID '{rid}' malformed. Expected NP-### (e.g. NP-001).")

    # 3. Controlled vocabulary
    rec_type = record.get("type")
    if rec_type and rec_type not in VALID_TYPES:
        errors.append(f"Invalid type: '{rec_type}'. Valid: {VALID_TYPES}")

    geo = record.get("geography")
    if geo and geo not in VALID_GEOGRAPHY:
        errors.append(f"Invalid geography: '{geo}'. Valid: {VALID_GEOGRAPHY}")

    urgency = record.get("urgency")
    if urgency and urgency not in VALID_URGENCY:
        errors.append(f"Invalid urgency: '{urgency}'. Valid: {VALID_URGENCY}")

    author = record.get("author")
    if author and author not in VALID_AUTHOR:
        errors.append(f"Invalid author: '{author}'. Valid: {VALID_AUTHOR}")

    # 4. category_tags — each must be valid
    cat_tags = record.get("category_tags", [])
    if not isinstance(cat_tags, list):
        errors.append("category_tags must be a list.")
    else:
        for tag in cat_tags:
            if tag not in VALID_CATEGORY_TAGS:
                errors.append(f"Invalid category_tag: '{tag}'. Valid: {VALID_CATEGORY_TAGS}")
        if not cat_tags:
            warnings.append("category_tags is empty — consider tagging to at least one category.")

    # 5. resource_tags must be a list
    res_tags = record.get("resource_tags", [])
    if not isinstance(res_tags, list):
        errors.append("resource_tags must be a list.")
    elif not res_tags:
        warnings.append("resource_tags is empty — consider adding topic tags.")

    # 6. Date validation
    published_str = record.get("published")
    expires_str   = record.get("expires")

    published = None
    expires   = None

    if published_str:
        try:
            published = date.fromisoformat(published_str)
        except ValueError:
            errors.append(f"published '{published_str}' is not a valid ISO date (YYYY-MM-DD).")

    if expires_str:
        try:
            expires = date.fromisoformat(expires_str)
        except ValueError:
            errors.append(f"expires '{expires_str}' is not a valid ISO date (YYYY-MM-DD).")

    if published and expires:
        if expires < published:
            errors.append(f"expires ({expires_str}) is before published ({published_str}).")

    # 7. Expiry warnings
    if expires and expires < TODAY:
        warnings.append(
            f"Record has expired ({expires_str}). Set active: false or update expires."
        )
    elif expires:
        days_left = (expires - TODAY).days
        if days_left <= 7:
            warnings.append(f"Record expires in {days_left} day(s) ({expires_str}).")

    # 8. Length checks
    headline = record.get("headline", "")
    if len(headline) > 120:
        errors.append(f"headline is {len(headline)} chars — max is 120.")
    elif not headline.strip():
        errors.append("headline is empty.")

    summary = record.get("summary", "")
    if len(summary) > 300:
        errors.append(f"summary is {len(summary)} chars — max is 300.")
    elif not summary.strip():
        errors.append("summary is empty.")

    why = record.get("why_it_matters", "")
    if len(why) > 300:
        warnings.append(f"why_it_matters is {len(why)} chars — consider trimming to under 300.")
    elif not why.strip():
        errors.append("why_it_matters is empty.")

    body = record.get("body", "")
    if not body.strip():
        errors.append("body is empty.")

    # 9. source_url format
    source_url = record.get("source_url")
    if source_url and not source_url.startswith("http"):
        errors.append(f"source_url doesn't start with http: '{source_url}'")

    # 10. Type checks
    if not isinstance(record.get("verified"), bool):
        errors.append("verified must be a boolean (true/false).")
    if not isinstance(record.get("active"), bool):
        errors.append("active must be a boolean (true/false).")

    # 11. Deadline type should have an expires date
    if rec_type == "deadline" and not expires_str:
        errors.append("Records of type 'deadline' must have an expires date.")

    # 12. Time-limited urgency should have an expires date
    if urgency == "time-limited" and not expires_str:
        warnings.append("urgency is 'time-limited' but no expires date is set.")

    # 13. Statewide geography with no Wilmington context
    if geo == "delaware":
        warnings.append(
            "geography is 'delaware' — consider using 'both' if this directly affects Wilmington residents."
        )

    return errors, warnings


# ─────────────────────────────────────────
# Duplicate ID detector
# ─────────────────────────────────────────

def check_duplicate_ids(records):
    from collections import defaultdict
    seen = defaultdict(list)
    for r in records:
        rid = r.get("id")
        if rid:
            seen[rid].append(r.get("headline", "unknown"))
    return {rid: headlines for rid, headlines in seen.items() if len(headlines) > 1}


# ─────────────────────────────────────────
# Report printer
# ─────────────────────────────────────────

def print_report(results, dup_ids, total, strict, show_expired):
    passed  = [r for r in results if not r["errors"]]
    failed  = [r for r in results if r["errors"]]
    active  = [r for r in results if r["active"]]
    expired = [r for r in results if r["expired"]]

    print("\n" + "═" * 62)
    print("  WILMINGTON CIVIC APP — NEWS SCHEMA VALIDATOR")
    print("═" * 62)
    print(f"  File      : {INPUT_FILE}")
    print(f"  Records   : {total}")
    print(f"  Mode      : {'STRICT' if strict else 'normal'}")
    print(f"  ✅  Passed      : {len(passed)}")
    print(f"  ❌  Failed      : {len(failed)}")
    print(f"  📰  Active      : {len(active)}")
    print(f"  🗓   Expired     : {len(expired)}")
    print(f"  🔁  Dup IDs     : {len(dup_ids)}")
    print("═" * 62)

    if failed:
        print("\n── FAILURES ──────────────────────────────────────────────")
        for r in failed:
            print(f"\n  [{r['id']}] {r['headline']}")
            for e in r["errors"]:
                print(f"    ✗ {e}")

    if strict:
        warned = [r for r in results if r["warnings"]]
        if warned:
            print("\n── WARNINGS (strict mode) ────────────────────────────────")
            for r in warned:
                print(f"\n  [{r['id']}] {r['headline']}")
                for w in r["warnings"]:
                    print(f"    ⚠  {w}")

    if dup_ids:
        print("\n── DUPLICATE IDs ─────────────────────────────────────────")
        for rid, headlines in dup_ids.items():
            print(f"  '{rid}': {headlines}")

    if show_expired and expired:
        print("\n── EXPIRED RECORDS ───────────────────────────────────────")
        for r in expired:
            print(f"  [{r['id']}] {r['headline']} — expired {r['expires']}")

    any_failure = failed or dup_ids
    if strict:
        any_failure = any_failure or any(r["warnings"] for r in results)

    if not any_failure:
        print("\n  All checks passed. ✅")

    print("\n" + "═" * 62 + "\n")
    return bool(any_failure)


# ─────────────────────────────────────────
# Main
# ─────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Validate news.json against the Wilmington Civic App news schema."
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Fail on warnings in addition to errors.",
    )
    parser.add_argument(
        "--show-expired",
        action="store_true",
        help="Print a list of all expired records.",
    )
    args = parser.parse_args()

    if not INPUT_FILE.exists():
        print(f"\n❌ File not found: {INPUT_FILE}")
        print("   Run from the FIRSTSTEP/ project root.\n")
        sys.exit(1)

    with open(INPUT_FILE, encoding="utf-8") as f:
        data = json.load(f)

    records = data if isinstance(data, list) else data.get("records", [])

    if not records:
        print("❌ No records found in file.")
        sys.exit(1)

    results = []
    for i, record in enumerate(records):
        errors, warnings = validate_record(record, i)

        expires_str = record.get("expires")
        is_expired  = False
        if expires_str:
            try:
                is_expired = date.fromisoformat(expires_str) < TODAY
            except ValueError:
                pass

        results.append({
            "id":       record.get("id", f"index-{i}"),
            "headline": record.get("headline", f"Record #{i}"),
            "active":   record.get("active", False),
            "expired":  is_expired,
            "expires":  expires_str,
            "errors":   errors,
            "warnings": warnings,
        })

    dup_ids     = check_duplicate_ids(records)
    had_failure = print_report(
        results, dup_ids, len(records),
        strict=args.strict,
        show_expired=args.show_expired,
    )

    sys.exit(1 if had_failure else 0)


if __name__ == "__main__":
    main()