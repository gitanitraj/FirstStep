import csv
import json
import uuid
from pathlib import Path

INPUT_FILE = Path("../raw/original.csv")
OUTPUT_FILE = Path("../clean/resources.json")

def split_multi(value):
    """Split semicolon or comma-separated fields into clean lists."""
    if not value or value.strip() == "":
        return []
    parts = [v.strip() for v in value.replace(";", ",").split(",")]
    return [p for p in parts if p]

def parse_locations(address_str):
    """Convert address string into a structured location object."""
    if not address_str or address_str.strip() == "":
        return []

    # Detect confidential addresses
    confidential = "confidential" in address_str.lower()

    return [{
        "address_line1": address_str if not confidential else None,
        "address_line2": None,
        "city": "Wilmington",
        "state": "DE",
        "zip": None,
        "confidential": confidential
    }]

def parse_phones(phone_str):
    """Convert phone numbers into labeled objects."""
    if not phone_str:
        return []

    phones = split_multi(phone_str)
    result = []
    for p in phones:
        result.append({
            "label": "Main",
            "number": p
        })
    return result

def parse_websites(site_str):
    """Convert website URLs into labeled objects."""
    if not site_str:
        return []

    sites = split_multi(site_str)
    return [{"label": "Website", "url": s} for s in sites]

def determine_urgency(description):
    """Basic rule-based urgency classification."""
    if not description:
        return "ongoing"

    text = description.lower()

    if "deadline" in text or "until" in text or "limited" in text:
        return "time_limited"
    if "emergency" in text or "crisis" in text:
        return "crisis"

    return "ongoing"

def clean_record(row):
    """Transform a CSV row into the final JSON schema."""
    description = row.get("Services Description", "")

    return {
        "id": str(uuid.uuid4()),
        "type_of_service": row.get("Type of Service", "").strip(),
        "subcategory": None,  # You will fill this manually or infer later
        "organization_name": row.get("Organization Name", "").strip(),
        "parent_organization": None,

        "services_description": description,
        "population_served": split_multi(row.get("Population Served", "")),

        "urgency_level": determine_urgency(description),

        "locations": parse_locations(row.get("Full Address", "")),
        "phones": parse_phones(row.get("Phone", "")),
        "websites": parse_websites(row.get("Website", "")),

        "eligibility_age_min": None,
        "eligibility_age_max": None,
        "eligibility_gender": "any",

        "neighborhoods_served": split_multi(row.get("County Served", "")),

        "tags": [],

        "title_es": None,
        "summary_es": None
    }

def main():
    cleaned = []

    with open(INPUT_FILE, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            cleaned.append(clean_record(row))

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(cleaned, f, indent=2, ensure_ascii=False)

    print(f"Cleaned {len(cleaned)} records → {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
