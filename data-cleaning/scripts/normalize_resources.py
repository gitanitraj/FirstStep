"""Normalize app/data/resources.json and write resources.normalized.json.

This script rewrites optional values to canonical JSON types:
- integer fields become integers or null
- boolean fields become true/false
- empty strings become null for optional fields
- location address/city/state/zip fields become null when blank
- remove empty phone/website entries

Usage:
    python data-cleaning/scripts/normalize_resources.py
    python data-cleaning/scripts/normalize_resources.py --input app/data/resources.json --output app/data/resources.normalized.json
"""

import argparse
import json
from pathlib import Path
from typing import Any, Dict, List, Optional

DEFAULT_INPUT = Path("app/data/resources.json")
DEFAULT_OUTPUT = Path("app/data/resources.normalized.json")


def parse_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float) and value.is_integer():
        return int(value)
    text = str(value).strip()
    if text == "":
        return None
    if text.isdigit() or (text.startswith("-") and text[1:].isdigit()):
        return int(text)
    try:
        return int(float(text))
    except (ValueError, TypeError):
        return None


def parse_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    text = str(value).strip().lower()
    return text in {"true", "t", "yes", "y", "1"}


def normalize_text(value: Any) -> Optional[str]:
    if value is None:
        return None
    text = str(value).strip()
    return text if text != "" else None


def normalize_list(value: Any) -> List[Any]:
    if value is None:
        return []
    if isinstance(value, list):
        return [item for item in value if item is not None]
    return [value]


def normalize_location(location: Dict[str, Any]) -> Dict[str, Any]:
    normalized = {
        "label": normalize_text(location.get("label")) or "",
        "address": normalize_text(location.get("address")),
        "city": normalize_text(location.get("city")),
        "state": normalize_text(location.get("state")),
        "zip": normalize_text(location.get("zip")),
        "confidential": parse_bool(location.get("confidential")),
    }

    if normalized["confidential"]:
        normalized["address"] = None
        normalized["city"] = None
        normalized["state"] = None
        normalized["zip"] = None

    return normalized


def normalize_phone(phone: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    number = normalize_text(phone.get("number"))
    label = normalize_text(phone.get("label"))
    if not number:
        return None
    return {"number": number, "label": label or ""}


def normalize_website(site: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    url = normalize_text(site.get("url"))
    label = normalize_text(site.get("label"))
    if not url:
        return None
    return {"url": url, "label": label or ""}


def normalize_record(record: Dict[str, Any]) -> Dict[str, Any]:
    normalized = record.copy()

    normalized["parent_organization"] = normalize_text(record.get("parent_organization"))
    normalized["description"] = normalize_text(record.get("description"))
    normalized["notes"] = normalize_text(record.get("notes"))

    normalized["eligibility_age_min"] = parse_int(record.get("eligibility_age_min"))
    normalized["eligibility_age_max"] = parse_int(record.get("eligibility_age_max"))
    normalized["verified"] = parse_bool(record.get("verified"))

    normalized["locations"] = [
        loc for loc in (
            normalize_location(loc) for loc in normalize_list(record.get("locations"))
        )
        if loc["label"] or loc["address"] or loc["city"] or loc["state"] or loc["zip"] or loc["confidential"]
    ]

    normalized["phones"] = [
        phone for phone in (
            normalize_phone(entry) for entry in normalize_list(record.get("phones"))
        )
        if phone is not None
    ]

    normalized["websites"] = [
        site for site in (
            normalize_website(entry) for entry in normalize_list(record.get("websites"))
        )
        if site is not None
    ]

    normalized["access_mode"] = [
        normalize_text(value) for value in normalize_list(record.get("access_mode"))
        if normalize_text(value)
    ]

    normalized["tags"] = [
        normalize_text(value) for value in normalize_list(record.get("tags"))
        if normalize_text(value)
    ]

    return normalized


def normalize_payload(payload: Any) -> Any:
    if isinstance(payload, list):
        return [normalize_record(record) for record in payload if isinstance(record, dict)]

    if isinstance(payload, dict):
        normalized = payload.copy()
        if "records" in payload and isinstance(payload["records"], list):
            normalized["records"] = [normalize_record(record) for record in payload["records"] if isinstance(record, dict)]
        elif "resources" in payload and isinstance(payload["resources"], list):
            normalized["resources"] = [normalize_record(record) for record in payload["resources"] if isinstance(record, dict)]
        else:
            normalized = normalize_payload(payload.get("records", payload))
        return normalized

    raise ValueError("Input payload must be a JSON array or object containing records/resources.")


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path: Path, payload: Any) -> None:
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize app/data/resources.json and save canonical JSON types.")
    parser.add_argument("--input", default=str(DEFAULT_INPUT), help="Input JSON file path.")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output normalized JSON file path.")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    payload = load_json(input_path)
    normalized = normalize_payload(payload)
    save_json(output_path, normalized)

    print(f"Normalized {input_path} -> {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
