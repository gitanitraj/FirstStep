"""Enrich app/data/resources.communities.json (and flyers.json) with the Claude API.

The 171 community records were only *structurally* mapped from the raw DSCYF
directory (see references/decisions.md Decision 013): they carry a raw `category`
but no `subcategory`, `cost`, `urgency`, or `tags`. This script proposes those
fields, constrained to the canonical vocabulary in app/data/taxonomy.json (D0.1),
so the Category Hub (F) can group them and the navigation generator (D0.4) has
tags to organize.

Two-phase, human-in-the-loop by design — the model NEVER writes the data file
directly:

    propose  ->  writes a proposals file (one entry per record) for a human to
                 review and correct. Each entry has "approved": true; flip it to
                 false to drop a record, or edit the proposed values in place.
    apply    ->  merges ONLY approved proposals into the target data file,
                 filling empty fields (never clobbering existing non-null values).

Two proposers (choose with --method):
  rules (default) : offline keyword + raw-category lexicon, no API, no dependency.
                    The raw source category often IS a subcategory (e.g. "Support
                    Group" -> "Support Groups"); broad categories are disambiguated
                    by keyword. Unmatched records get subcategory=null (flagged for
                    manual fill during review).
  ai              : Claude API, subcategory constrained to a per-category JSON-schema
                    enum so the model can only pick a valid canonical subcategory.
Both write the SAME proposals shape, so review/apply are identical. cost/urgency
are best-effort; tags are topic strings that feed navigation (topics gather by
subcategory OR tag).

The flyer pass (--task flyers) tags each flyer with topics drawn from the full
subcategory vocabulary, from its title/summary — flyers are content-agnostic
CivicContent that fold into the same navigation topics.

Usage (run from repo root; per-category, largest-first for D0.3):
    python data-cleaning/scripts/enrich_resources.py propose --task resources --category health
    # ...review data-cleaning/proposals/resources.communities.proposals.json...
    python data-cleaning/scripts/enrich_resources.py apply   --task resources --category health

    python data-cleaning/scripts/enrich_resources.py propose --task flyers
    python data-cleaning/scripts/enrich_resources.py apply   --task flyers

`propose` needs ANTHROPIC_API_KEY and hits the Messages API over raw HTTPS via the
Python standard library (urllib) — no third-party SDK to install. `apply` does not
call the API at all.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional

TAXONOMY_FILE = Path("app/data/taxonomy.json")
SOURCE_MAPPINGS_FILE = Path("app/data/source-mappings.json")
RESOURCES_FILE = Path("app/data/resources.communities.json")
FLYERS_FILE = Path("app/data/flyers.json")
PROPOSALS_DIR = Path("data-cleaning/proposals")
RESOURCES_PROPOSALS = PROPOSALS_DIR / "resources.communities.proposals.json"
FLYERS_PROPOSALS = PROPOSALS_DIR / "flyers.proposals.json"

MODEL = "claude-opus-4-8"
API_URL = "https://api.anthropic.com/v1/messages"
ANTHROPIC_VERSION = "2023-06-01"

VALID_COST = ["free", "low-cost", "unknown"]
VALID_URGENCY = ["emergency", "time-limited", "standard"]


# -----------------------------
# Taxonomy (canonical vocabulary — the constraint the model assigns from)
# -----------------------------

def load_taxonomy() -> Dict[str, Any]:
    """Return {raw_category -> {key,label,subcategories}} plus the full subcategory set."""
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    by_key: Dict[str, Dict[str, Any]] = {}
    all_subcategories: List[str] = []
    for cat in data["categories"]:
        by_key[cat["key"]] = {
            "key": cat["key"],
            "label": cat["label"],
            "subcategories": cat.get("subcategories", []),
        }
        for sub in cat.get("subcategories", []):
            if sub not in all_subcategories:
                all_subcategories.append(sub)

    # Raw provider vocabulary lives in its own artifact (Decision 034) — it is a
    # source adapter, not part of First Step's editorial taxonomy.
    raw_to_display: Dict[str, Dict[str, Any]] = {}
    if SOURCE_MAPPINGS_FILE.exists():
        mappings = json.loads(SOURCE_MAPPINGS_FILE.read_text(encoding="utf-8"))
        for source in mappings.get("sources", []):
            for raw, key in (source.get("mappings") or {}).items():
                if key in by_key:
                    raw_to_display[raw] = by_key[key]
    return {"raw_to_display": raw_to_display, "all_subcategories": all_subcategories}


# -----------------------------
# JSON I/O
# -----------------------------

def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def records_of(payload: Any) -> List[Dict[str, Any]]:
    """Both data files wrap their rows in a `records` list."""
    if isinstance(payload, dict) and isinstance(payload.get("records"), list):
        return payload["records"]
    if isinstance(payload, list):
        return payload
    raise ValueError("Expected a list or a {records:[...]} object.")


# -----------------------------
# Claude API — one constrained classification per record
# -----------------------------

def require_api_key() -> str:
    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        sys.exit("ANTHROPIC_API_KEY is not set. Export it before running `propose`.")
    return key


def call_claude(body: Dict[str, Any], api_key: str) -> str:
    """POST to the Messages API with the stdlib (no anthropic SDK). Return the
    first text block, which output_config.format guarantees is valid JSON."""
    request = urllib.request.Request(
        API_URL,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "x-api-key": api_key,
            "anthropic-version": ANTHROPIC_VERSION,
            "content-type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as err:
        sys.exit(f"Claude API error {err.code}: {err.read().decode('utf-8', 'replace')}")
    except urllib.error.URLError as err:
        sys.exit(f"Network error calling Claude API: {err.reason}")
    for block in payload.get("content", []):
        if block.get("type") == "text":
            return block["text"]
    sys.exit(f"No text block in Claude response: {payload}")


def resource_schema(allowed_subcategories: List[str]) -> Dict[str, Any]:
    """A per-category output schema: subcategory constrained to the allowed enum."""
    properties: Dict[str, Any] = {
        "tags": {
            "type": "array",
            "items": {"type": "string"},
            "description": "1-4 short resident-facing topic tags (e.g. 'Rental Assistance', 'Youth').",
        },
        "cost": {"type": "string", "enum": VALID_COST},
        "urgency": {"type": "string", "enum": VALID_URGENCY},
        "rationale": {"type": "string", "description": "One short sentence justifying the subcategory."},
    }
    required = ["tags", "cost", "urgency", "rationale"]
    if allowed_subcategories:
        properties["subcategory"] = {"type": "string", "enum": allowed_subcategories}
        required.insert(0, "subcategory")
    return {
        "type": "object",
        "properties": properties,
        "required": required,
        "additionalProperties": False,
    }


def classify_resource(api_key: str, record: Dict[str, Any], display: Dict[str, Any]) -> Dict[str, Any]:
    allowed = display["subcategories"]
    subline = (
        "Pick exactly ONE subcategory from this allowed list for the "
        f'"{display["label"]}" category: {allowed}.'
        if allowed
        else f'The "{display["label"]}" category has no subcategories; do not assign one.'
    )
    prompt = (
        "You are classifying a Delaware community service into a fixed civic taxonomy.\n\n"
        f"Organization: {record.get('organization')}\n"
        f"Summary: {record.get('summary')}\n"
        f"Description: {record.get('description')}\n"
        f"Population served: {record.get('population')}\n\n"
        f"{subline}\n"
        "Then give 1-4 short topic tags, a best-effort cost "
        "(free / low-cost / unknown when the directory does not say), and an "
        "urgency (emergency for crisis services, time-limited for seasonal or "
        "deadline-bound programs, otherwise standard)."
    )
    body = {
        "model": MODEL,
        "max_tokens": 1024,
        "output_config": {"format": {"type": "json_schema", "schema": resource_schema(allowed)}},
        "messages": [{"role": "user", "content": prompt}],
    }
    return json.loads(call_claude(body, api_key))


def flyer_schema(all_subcategories: List[str]) -> Dict[str, Any]:
    return {
        "type": "object",
        "properties": {
            "tags": {
                "type": "array",
                "items": {"type": "string", "enum": all_subcategories},
                "description": "1-3 canonical topic tags this flyer belongs under.",
            },
            "rationale": {"type": "string"},
        },
        "required": ["tags", "rationale"],
        "additionalProperties": False,
    }


def classify_flyer(api_key: str, flyer: Dict[str, Any], all_subcategories: List[str]) -> Dict[str, Any]:
    prompt = (
        "Tag this community flyer with 1-3 canonical topics so it can be grouped "
        "alongside directory services under the same navigation topics.\n\n"
        f"Title: {flyer.get('title')}\n"
        f"Summary: {flyer.get('summary')}\n"
        f"Organization: {flyer.get('organization')}\n\n"
        f"Choose only from this allowed topic list: {all_subcategories}."
    )
    body = {
        "model": MODEL,
        "max_tokens": 512,
        "output_config": {"format": {"type": "json_schema", "schema": flyer_schema(all_subcategories)}},
        "messages": [{"role": "user", "content": prompt}],
    }
    return json.loads(call_claude(body, api_key))


# -----------------------------
# Rules-based proposer (no API). Two tiers:
#   1. RAW_DEFAULT: for raw source categories that ARE a subcategory (e.g.
#      "Support Group" -> "Support Groups"). Applied when no keyword wins.
#   2. SUBCATEGORY_KEYWORDS: keyword lexicon for the broad categories
#      (Recreational, Mental Health, Housing, Food, Employment, Legal) where the
#      raw category doesn't pin a subcategory. Scored against the record text;
#      only subcategories ALLOWED for the record's display category are considered.
# Order matters: more specific subcategories are listed first so ties break toward
# the specific one. Every proposal is still human-reviewed via the proposals file.
# -----------------------------

RAW_DEFAULT: Dict[str, str] = {
    "Resource Information": "Information & Referral",
    "Education/Training": "Education & Training",
    "Parenting Education": "Parenting Support",
    "Financial Support": "Financial Assistance",
    "Support Group": "Support Groups",
    "Early Childhood/Pre-K": "Child Care & Early Learning",
    "Child Care": "Child Care & Early Learning",
    "Before/After School Care": "Child Care & Early Learning",
    "Volunteer": "Volunteer Opportunities",
    "Mentor": "Mentoring",
    "Life Skills": "Life Skills",
    "Transportation": "Transportation",
    "Entertainment": "Community Celebrations",
    "Healthcare/Medical": "Medical Care",
    "Substance Use": "Substance Use Treatment",
    "Mental Health": "Counseling & Therapy",
}

# subcategory -> trigger keywords (lowercase substrings). Insertion order = priority.
SUBCATEGORY_KEYWORDS: Dict[str, List[str]] = {
    # Health
    "Crisis Services": ["crisis", "24 hour", "24 hours", "23-hour", "hotline", "rape crisis", "sexual assault response"],
    "Substance Use Treatment": ["substance use", "opioid", "sober", "recovery", "addiction", "detox", "mat "],
    "Trauma & Grief Support": ["grief", "grieving", "bereavement", "sexual abuse", "trauma", "loss of a loved"],
    "Medical Care": ["urgent care", "medical", "birth control", "std", "pregnancy", "clinic", "health care"],
    "Counseling & Therapy": ["therapy", "counseling", "behavioral health", "psychological", "psychiatric", "mental health"],
    # Housing
    "Sober Living": ["sober-living", "sober living"],
    "Emergency Shelter": ["emergency shelter", "shelter"],
    "Public Housing": ["public housing", "housing choice", "voucher"],
    "Homeownership": ["homeownership", "home repair", "mortgage", "foreclosure"],
    "Rental Assistance": ["rent", "rental", "emergency assistance"],
    # Food
    "Home-Delivered Meals": ["meals on wheels", "home-delivered", "delivered meal"],
    "Farmers Market": ["farmers market", "produce", "market"],
    "Prepared Meals": ["dining room", "restaurant style", "prepared", "soup kitchen"],
    "Food Pantry": ["pantry", "food closet", "groceries", "food bank", "distribution", "food"],
    # Employment
    "Youth Employment": ["youth workforce", "summer and school year", "ages 14", "ages 16", "young"],
    "Vocational Training": ["vocational", "pre-vocational", "occupational", "skills needed"],
    "Job Search Assistance": ["job search", "job center", "joblink", "workforce", "workshops", "re-hired", "hr services"],
    # Legal / Advocacy
    "Eviction Prevention": ["eviction"],
    "Disability Advocacy": ["disabilities", "disability", "special needs", "intellectual", "epilepsy", "advocate"],
    # Community events (Recreational)
    "Libraries": ["library"],
    "Senior Activities": ["senior", "50 and older", "60 and older", "senior center"],
    "Parks & Outdoors": ["park", "trail", "nature", "hiking", "biking", "pond", "playground", "disc golf", "camping", "dog park", "fishing"],
    "Arts & Music": ["dance", "art ", "music", "guitar", "painting", "theatre", "theater", "vocal", "drawing", "mosaic", "woodwork", "carpentry", "craft", "wrestling"],
    "Sports & Fitness": ["sport", "soccer", "boxing", "martial arts", "karate", "jiujitsu", "jujitsu", "swim", "fitness", "athletic", "bowling", "archery", "ymca", "kickboxing", "tumbling"],
    "Youth Programs": ["boys and girls club", "4-h", "youth group", "summer camp", "afterschool", "after school", "kids", "children", "pal ", "story time", "day camp"],
}


def _detect_cost(text: str) -> Optional[str]:
    if any(k in text for k in ["no-cost", "no cost", "no charge", "free"]):
        return "free"
    if any(k in text for k in ["$", "tuition", "fee", "cost varies", "low cost", "low-cost"]):
        return "low-cost"
    return None


def _detect_urgency(text: str) -> str:
    if any(k in text for k in ["crisis", "24 hour", "24 hours", "23-hour", "hotline", "emergency shelter"]):
        return "emergency"
    return "standard"


def _population_tags(record: Dict[str, Any], text: str) -> List[str]:
    pop = (record.get("population") or "").lower()
    blob = pop + " " + text
    tags: List[str] = []
    if any(k in blob for k in ["youth", "child", "kids", "adolescent", "teen", "grades", "ages 0", "ages 1", "ages 2-", "ages 3", "ages 5", "ages 6", "ages 7", "ages 8"]):
        tags.append("Youth")
    if any(k in pop for k in ["60 and older", "50 and older", "senior"]):
        tags.append("Seniors")
    if "female" in pop or "women" in blob:
        tags.append("Women")
    if "disabilit" in blob or "special needs" in blob:
        tags.append("People with Disabilities")
    return tags


def classify_resource_rules(record: Dict[str, Any], display: Dict[str, Any]) -> Dict[str, Any]:
    allowed = display["subcategories"]
    text = " ".join(str(record.get(f) or "") for f in ("organization", "summary", "description")).lower()

    subcategory: Optional[str] = None
    best_score = 0
    for sub, keywords in SUBCATEGORY_KEYWORDS.items():
        if sub not in allowed:
            continue
        score = sum(text.count(k) for k in keywords)
        if score > best_score:
            best_score, subcategory = score, sub

    matched_by = "keyword"
    if subcategory is None:  # fall back to the raw-category default
        default = RAW_DEFAULT.get(record.get("category"))
        if default and default in allowed:
            subcategory, matched_by = default, "raw-category default"

    tags = _population_tags(record, text)
    if subcategory and subcategory not in tags:
        tags.insert(0, subcategory)

    rationale = (
        f"{matched_by}: {subcategory}" if subcategory
        else "no keyword or raw-category default matched -- assign subcategory manually"
    )
    return {
        "subcategory": subcategory,
        "tags": tags[:4],
        "cost": _detect_cost(text),
        "urgency": _detect_urgency(text),
        "rationale": rationale,
    }


def classify_flyer_rules(flyer: Dict[str, Any], all_subcategories: List[str]) -> Dict[str, Any]:
    text = " ".join(str(flyer.get(f) or "") for f in ("title", "summary", "organization")).lower()
    scored: List[tuple] = []
    for sub, keywords in SUBCATEGORY_KEYWORDS.items():
        score = sum(text.count(k) for k in keywords)
        if score > 0:
            scored.append((score, sub))
    scored.sort(key=lambda pair: pair[0], reverse=True)
    tags = [sub for _, sub in scored[:2]]
    return {
        "tags": tags,
        "rationale": "keyword match" if tags else "no keyword matched -- tag manually",
    }


# -----------------------------
# propose
# -----------------------------

def propose_resources(category_filter: Optional[str], limit: Optional[int], method: str) -> int:
    taxonomy = load_taxonomy()
    raw_to_display = taxonomy["raw_to_display"]
    records = records_of(load_json(RESOURCES_FILE))
    api_key = require_api_key() if method == "ai" else None

    proposals: List[Dict[str, Any]] = []
    skipped_unmapped: List[str] = []
    count = 0
    for record in records:
        display = raw_to_display.get(record.get("category"))
        if display is None:
            skipped_unmapped.append(record.get("id"))
            continue
        if category_filter and display["key"] != category_filter:
            continue
        if limit is not None and count >= limit:
            break
        if method == "ai":
            result = classify_resource(api_key, record, display)
        else:
            result = classify_resource_rules(record, display)
        proposals.append({
            "id": record.get("id"),
            "organization": record.get("organization"),
            "displayCategory": display["key"],
            "method": method,
            "approved": True,
            "proposed": result,
        })
        count += 1
        print(f"  {record.get('id')}  {display['key']:18}  {result.get('subcategory') or '(needs review)'}")

    save_json(RESOURCES_PROPOSALS, proposals)
    print(f"\nWrote {len(proposals)} proposals -> {RESOURCES_PROPOSALS}")
    if skipped_unmapped:
        print(f"Skipped {len(skipped_unmapped)} records with unmapped category: {skipped_unmapped}")
    print("Review the file (correct values / set approved:false to drop), then run `apply`.")
    return 0


def propose_flyers(limit: Optional[int], method: str) -> int:
    taxonomy = load_taxonomy()
    all_subcategories = taxonomy["all_subcategories"]
    flyers = records_of(load_json(FLYERS_FILE))
    api_key = require_api_key() if method == "ai" else None

    proposals: List[Dict[str, Any]] = []
    for count, flyer in enumerate(flyers):
        if limit is not None and count >= limit:
            break
        if method == "ai":
            result = classify_flyer(api_key, flyer, all_subcategories)
        else:
            result = classify_flyer_rules(flyer, all_subcategories)
        proposals.append({
            "id": flyer.get("id"),
            "title": flyer.get("title"),
            "existingTags": flyer.get("tags", []),
            "method": method,
            "approved": True,
            "proposed": result,
        })
        print(f"  {flyer.get('id')}  {result.get('tags')}")

    save_json(FLYERS_PROPOSALS, proposals)
    print(f"\nWrote {len(proposals)} flyer proposals -> {FLYERS_PROPOSALS}")
    print("Review the file, then run `apply --task flyers`.")
    return 0


# -----------------------------
# apply (merge approved proposals; fill only empty fields)
# -----------------------------

def _fill_empty(record: Dict[str, Any], field: str, value: Any) -> bool:
    """Set record[field] = value only if the field is currently empty. Returns True if changed."""
    current = record.get(field)
    empty = current is None or current == "" or current == []
    if empty and value not in (None, "", [], "unknown"):
        record[field] = value
        return True
    return False


def apply_resources(category_filter: Optional[str]) -> int:
    if not RESOURCES_PROPOSALS.exists():
        sys.exit(f"No proposals file at {RESOURCES_PROPOSALS}. Run `propose` first.")
    proposals = load_json(RESOURCES_PROPOSALS)
    payload = load_json(RESOURCES_FILE)
    records = records_of(payload)
    by_id = {r.get("id"): r for r in records}

    changed = 0
    for entry in proposals:
        if not entry.get("approved"):
            continue
        if category_filter and entry.get("displayCategory") != category_filter:
            continue
        record = by_id.get(entry["id"])
        if record is None:
            print(f"  WARN: proposal id {entry['id']} not found in data; skipping")
            continue
        proposed = entry["proposed"]
        touched = False
        touched |= _fill_empty(record, "subcategory", proposed.get("subcategory"))
        touched |= _fill_empty(record, "tags", proposed.get("tags"))
        touched |= _fill_empty(record, "cost", proposed.get("cost"))
        touched |= _fill_empty(record, "urgency", proposed.get("urgency"))
        if touched:
            changed += 1

    save_json(RESOURCES_FILE, payload)
    print(f"Applied approved proposals to {changed} records -> {RESOURCES_FILE}")
    print("Now run: python data-cleaning/scripts/validate_schema.py --strict")
    return 0


def apply_flyers() -> int:
    if not FLYERS_PROPOSALS.exists():
        sys.exit(f"No proposals file at {FLYERS_PROPOSALS}. Run `propose --task flyers` first.")
    proposals = load_json(FLYERS_PROPOSALS)
    payload = load_json(FLYERS_FILE)
    flyers = records_of(payload)
    by_id = {f.get("id"): f for f in flyers}

    changed = 0
    for entry in proposals:
        if not entry.get("approved"):
            continue
        flyer = by_id.get(entry["id"])
        if flyer is None:
            print(f"  WARN: proposal id {entry['id']} not found in flyers; skipping")
            continue
        # Union the proposed canonical topics into the flyer's existing tags.
        existing = flyer.get("tags", [])
        merged = list(existing)
        for tag in entry["proposed"].get("tags", []):
            if tag not in merged:
                merged.append(tag)
        if merged != existing:
            flyer["tags"] = merged
            changed += 1

    save_json(FLYERS_FILE, payload)
    print(f"Applied topic tags to {changed} flyers -> {FLYERS_FILE}")
    return 0


# -----------------------------
# CLI
# -----------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description="Enrich community resources / flyers (rules by default; --method ai uses the Claude API).")
    parser.add_argument("action", choices=["propose", "apply"])
    parser.add_argument("--task", choices=["resources", "flyers"], default="resources")
    parser.add_argument("--method", choices=["rules", "ai"], default="rules",
                        help="rules = offline keyword/raw-category proposer (no API); ai = Claude API (needs ANTHROPIC_API_KEY).")
    parser.add_argument("--category", help="Display-category key filter (e.g. health, housing) — resources only.")
    parser.add_argument("--limit", type=int, help="Cap the number of records processed.")
    args = parser.parse_args()

    if args.task == "flyers":
        if args.action == "propose":
            return propose_flyers(args.limit, args.method)
        return apply_flyers()

    if args.action == "propose":
        return propose_resources(args.category, args.limit, args.method)
    return apply_resources(args.category)


if __name__ == "__main__":
    raise SystemExit(main())
