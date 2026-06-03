import json
import sys
import requests
from pathlib import Path
from datetime import date

# ─────────────────────────────────────────
# Config — paths relative to project root
# ─────────────────────────────────────────

INPUT_FILE = Path("app/data/resources.json")
STRICT_MODE = False  # Set to True to treat warnings as errors

# ─────────────────────────────────────────
# Controlled vocabularies
# ─────────────────────────────────────────

VALID_CATEGORIES = {
    "Clothing & Incidentals",
    "Furniture & Household Items",
    "Housing Assistance",
}

VALID_SUBCATEGORIES = {
    # Clothing & Incidentals
    "Clothing Closet",
    "Thrift Store",
    "Vouchers",
    # Furniture & Household Items
    "Appliances",
    "Starter Kits",
    # Housing Assistance
    "Emergency Shelter",
    "Transitional Housing",
    "Sober Living",
    "Rental Assistance",
    "Homeownership",
    "Senior Housing",
    "Youth Housing",
}

CATEGORY_TO_SUBCATEGORY = {
    "Clothing & Incidentals": {
        "Clothing Closet",
        "Thrift Store",
        "Vouchers",
    },
    "Furniture & Household Items": {
        "Appliances",
        "Starter Kits",
    },
    "Housing Assistance": {
        "Emergency Shelter",
        "Transitional Housing",
        "Sober Living",
        "Rental Assistance",
        "Homeownership",
        "Senior Housing",
        "Youth Housing",
    },
}

VALID_URGENCY = {"emergency", "time-limited", "standard"}
VALID_COST    = {"free", "low-cost"}
VALID_ACCESS  = {"in-person", "online"}
VALID_GENDER  = {"any", "male", "female"}

# ─────────────────────────────────────────
# Required fields
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

REQUIRED_LOCATION_FIELDS = ["label", "address", "city", "state", "zip", "confidential"]

# ─────────────────────────────────────────
# Enhancement 6: JSON Schema Export
# ─────────────────────────────────────────

def export_json_schema():
    schema = {
        "title": "First Step Resource Schema",
        "type": "object",
        "required": REQUIRED_FIELDS,
        "properties": {
            "id": {"type": "string"},
            "category": {"type": "string", "enum": list(VALID_CATEGORIES)},
            "subcategory": {"type": "string", "enum": list(VALID_SUBCATEGORIES)},
            "organization": {"type": "string"},
            "parent_organization": {"type": ["string", "null"]},
            "summary": {"type": "string"},
            "description": {"type": ["string", "null"]},
            "population": {"type": "string"},
            "eligibility": {"type": "string"},
            "eligibility_age_min": {"type": ["number", "null"]},
            "eligibility_age_max": {"type": ["number", "null"]},
            "eligibility_gender": {"type": "string", "enum": list(VALID_GENDER)},
            "locations": {"type": "array"},
            "phones": {"type": "array"},
            "websites": {"type": "array"},
            "county": {"type": "string"},
            "access_mode": {"type": "array"},
            "cost": {"type": "string", "enum": list(VALID_COST)},
            "urgency": {"type": "string", "enum": list(VALID_URGENCY)},
            "tags": {"type": "array"},
            "source": {"type": "string"},
            "retrieved": {"type": "string"},
            "verified": {"type": "boolean"},
            "notes": {"type": "string"},
        }
    }

    out = Path("data-cleaning/schema.json")
    out.write_text(json.dumps(schema, indent=2))
    print(f"Exported JSON schema → {out}")

# ─────────────────────────────────────────
# Enhancement 1–5: Validator
# ─────────────────────────────────────────

def validate_record(record, index):
    errors = []
    warnings = []
    org = record.get("organization", f"Record #{index}")

    # 1. Required fields
    for field in REQUIRED_FIELDS:
        if field not in record:
            errors.append(f"Missing required field: '{field}'")

    # 2. ID prefix
    rid = record.get("id", "")
    if rid:
        prefix = rid.split("-")[0]
        if prefix not in ("CI", "FH", "HA"):
            errors.append(f"ID '{rid}' has unexpected prefix.")

    # 3. Category + Subcategory mapping
    cat = record.get("category")
    subcat = record.get("subcategory")

    if cat not in VALID_CATEGORIES:
        errors.append(f"Invalid category: '{cat}'")

    if subcat not in VALID_SUBCATEGORIES:
        errors.append(f"Invalid subcategory: '{subcat}'")

    if cat in CATEGORY_TO_SUBCATEGORY:
        if subcat not in CATEGORY_TO_SUBCATEGORY[cat]:
            errors.append(f"Subcategory '{subcat}' does not belong to category '{cat}'")

    # 4. Controlled vocabularies
    if record.get("urgency") not in VALID_URGENCY:
        errors.append(f"Invalid urgency: {record.get('urgency')}")

    if record.get("cost") not in VALID_COST:
        errors.append(f"Invalid cost: {record.get('cost')}")

    if record.get("eligibility_gender") not in VALID_GENDER:
        errors.append(f"Invalid eligibility_gender: {record.get('eligibility_gender')}")

    # 5. Access mode
    access = record.get("access_mode", [])
    if not isinstance(access, list):
        errors.append("access_mode must be a list")
    else:
        for mode in access:
            if mode not in VALID_ACCESS:
                errors.append(f"Invalid access_mode value: '{mode}'")

    # 6. Summary length
    summary = record.get("summary", "")
    if not summary.strip():
        errors.append("summary is empty")
    elif len(summary) > 300:
        warnings.append(f"summary is {len(summary)} chars — consider shortening")

    # 7. Locations
    locations = record.get("locations", [])
    if not locations:
        errors.append("locations list is empty")
    else:
        for i, loc in enumerate(locations):
            for lf in REQUIRED_LOCATION_FIELDS:
                if lf not in loc:
                    errors.append(f"Location[{i}] missing '{lf}'")

            if not loc.get("confidential"):
                if not loc.get("city"):
                    errors.append(f"Location[{i}] missing city")
                if not loc.get("state"):
                    errors.append(f"Location[{i}] missing state")

    # 8. Phones
    for i, ph in enumerate(record.get("phones", [])):
        if "number" not in ph:
            errors.append(f"Phone[{i}] missing number")
        if "label" not in ph:
            errors.append(f"Phone[{i}] missing label")

    # 9. Websites + URL reachability
    for i, site in enumerate(record.get("websites", [])):
        url = site.get("url", "")
        if not url.startswith("http"):
            errors.append(f"Website[{i}] invalid URL: '{url}'")
        else:
            try:
                resp = requests.head(url, timeout=3)
                if resp.status_code >= 400:
                    warnings.append(f"Website[{i}] unreachable (HTTP {resp.status_code})")
            except Exception:
                warnings.append(f"Website[{i}] unreachable (network error)")

    # 10. Age logic
    age_min = record.get("eligibility_age_min")
    age_max = record.get("eligibility_age_max")
    if age_min is not None and age_max is not None:
        if age_min > age_max:
            errors.append("eligibility_age_min > eligibility_age_max")

    # 11. Verified boolean
    if not isinstance(record.get("verified"), bool):
        errors.append("verified must be boolean")

    # 12. Tags list
    if not isinstance(record.get("tags"), list):
        errors.append("tags must be a list")

    return errors, warnings

# ─────────────────────────────────────────
# Enhancement 2–3: Duplicate detection
# ─────────────────────────────────────────

def detect_duplicates(records):
    errors = []

    # Duplicate IDs
    seen_ids = {}
    for r in records:
        rid = r.get("id")
        if rid in seen_ids:
            errors.append(f"Duplicate ID: {rid}")
        seen_ids[rid] = True

    # Duplicate organization + address
    seen_pairs = {}
    for r in records:
        org = r.get("organization")
        for loc in r.get("locations", []):
            addr = loc.get("address")
            key = (org, addr)
            if key in seen_pairs:
                errors.append(f"Duplicate organization+address: {org} @ {addr}")
            seen_pairs[key] = True

    return errors

# ─────────────────────────────────────────
# Main
# ─────────────────────────────────────────

def main():
    export_json_schema()

    if not INPUT_FILE.exists():
        print(f"❌ File not found: {INPUT_FILE}")
        sys.exit(1)

    data = json.loads(INPUT_FILE.read_text())

    if isinstance(data, dict) and "records" in data:
        records = data["records"]
    else:
        records = data

    results = []
    all_errors = []

    for i, record in enumerate(records):
        errors, warnings = validate_record(record, i)
        results.append({
            "id": record.get("id"),
            "org": record.get("organization"),
            "errors": errors,
            "warnings": warnings,
        })
        all_errors.extend(errors)

    # Duplicate detection
    dup_errors = detect_duplicates(records)
    all_errors.extend(dup_errors)

    # Print report
    print("\n" + "═" * 60)
    print(" FIRST STEP — FULL SCHEMA VALIDATION REPORT")
    print("═" * 60)
    print(f" Records: {len(records)}")
    print(f" Errors:  {len(all_errors)}")
    print("═" * 60)

    for r in results:
        if r["errors"]:
            print(f"\n❌ {r['id']} — {r['org']}")
            for e in r["errors"]:
                print(f"   ✗ {e}")
        elif STRICT_MODE and r["warnings"]:
            print(f"\n⚠️  {r['id']} — {r['org']}")
            for w in r["warnings"]:
                print(f"   ! {w}")

    for e in dup_errors:
        print(f"   ✗ {e}")

    print("\n" + "═" * 60)

    # Exit code for CI/CD
    if all_errors or (STRICT_MODE and any(r["warnings"] for r in results)):
        sys.exit(1)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
