import csv
import json
import uuid
from pathlib import Path
from datetime import date

INPUT_FILE = Path("../raw/revised_wilmington_cache.csv")
OUTPUT_FILE = Path("../clean/resources.json")

# -----------------------------
# Utility helpers
# -----------------------------

def split_multi(value):
    """Split comma or slash separated fields into clean lists."""
    if not value or value.strip() == "":
        return []
    parts = [v.strip() for v in value.replace(";", ",").replace("/", ",").split(",")]
    return [p for p in parts if p]

def parse_locations(address_str):
    """Convert address string into structured location objects."""
    if not address_str or address_str.strip() == "":
        return []

    # Multiple addresses separated by commas but containing full addresses
    # We detect multiple addresses by looking for multiple ZIP codes.
    raw = address_str.split(",")
    chunks = []
    current = []

    for piece in raw:
        current.append(piece.strip())
        if any(zipcode in piece for zipcode in ["198", "197"]):
            chunks.append(", ".join(current))
            current = []

    if current:
        chunks.append(", ".join(current))

    locations = []
    for idx, loc in enumerate(chunks):
        confidential = "confidential" in loc.lower()

        # Basic parsing
        address = None if confidential else loc.split(",")[0].strip()
        city = None
        state = None
        zip_code = None

        if not confidential:
            try:
                parts = [p.strip() for p in loc.split(",")]
                if len(parts) >= 3:
                    address = parts[0]
                    city = parts[1]
                    state_zip = parts[2].split()
                    state = state_zip[0]
                    zip_code = state_zip[1] if len(state_zip) > 1 else None
            except:
                pass

        locations.append({
            "label": "Primary" if idx == 0 else f"Location {idx+1}",
            "address": address,
            "city": city,
            "state": state,
            "zip": zip_code,
            "confidential": confidential
        })

    return locations

def parse_phones(phone_str):
    if not phone_str:
        return []
    phones = split_multi(phone_str)
    return [{"number": p, "label": "Main"} for p in phones]

def parse_websites(site_str):
    if not site_str:
        return []
    sites = split_multi(site_str)
    return [{"url": s, "label": "Program page"} for s in sites]

def extract_gender(text):
    if not text:
        return "any"
    t = text.lower()
    if "women" in t or "female" in t:
        return "female"
    if "men" in t or "male" in t:
        return "male"
    return "any"

def extract_age(text):
    if not text:
        return (None, None)
    t = text.lower()

    # Age 18 and older
    if "age" in t and "older" in t:
        try:
            num = int(t.split("age")[1].split("and")[0].strip())
            return (num, None)
        except:
            pass

    # Ages 16–23
    if "ages" in t and "–" in t:
        try:
            rng = t.split("ages")[1].strip()
            low, high = rng.split("–")
            return (int(low), int(high))
        except:
            pass

    return (None, None)

def determine_urgency(urgency_str):
    if not urgency_str:
        return "standard"
    u = urgency_str.lower()
    if "urgent" in u or "emergency" in u:
        return "emergency"
    if "time" in u or "limited" in u:
        return "time-limited"
    return "standard"

# -----------------------------
# Main record transformer
# -----------------------------

def clean_record(row):
    eligibility_raw = row.get("eligibility", "")
    age_min, age_max = extract_age(eligibility_raw)
    gender = extract_gender(eligibility_raw)

    return {
        "id": row.get("id"),
        "category": row.get("category"),
        "subcategory": None,  # You will fill or infer later

        "organization": row.get("organization"),
        "parent_organization": None,  # You will fill manually

        "summary": row.get("summary"),
        "description": None,

        "population": row.get("population"),
        "eligibility": eligibility_raw,
        "eligibility_age_min": age_min,
        "eligibility_age_max": age_max,
        "eligibility_gender": gender,

        "locations": parse_locations(row.get("address")),
        "phones": parse_phones(row.get("phone")),
        "websites": parse_websites(row.get("website")),

        "county": row.get("county"),
        "access_mode": split_multi(row.get("access_mode")),
        "cost": row.get("cost"),

        "urgency": determine_urgency(row.get("urgency")),

        "tags": [],

        "source": "FIRST Community Services Directory — Delaware DSCYF",
        "retrieved": str(date.today()),
        "verified": False,
        "notes": ""
    }

# -----------------------------
# Main script
# -----------------------------

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
