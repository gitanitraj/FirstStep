# =============================================================================
# ANNOTATED REFERENCE — data-cleaning/scripts/enrich_resources.py (D0.2).
# Mirrors the production script; keep in sync. See references/decisions.md
# Decision 027 and the D0 plan (glowing-sauteeing-finch.md).
# =============================================================================
#
# WHAT THIS IS
#   The AI enrichment tool for the 171 "structurally mapped" community records in
#   app/data/resources.communities.json. Those records carry a raw `category` but
#   no `subcategory`, `cost`, `urgency`, or `tags` (Decision 013: the raw DSCYF
#   directory has no source data for them). This script proposes those fields with
#   Claude, CONSTRAINED to the canonical vocabulary in app/data/taxonomy.json
#   (D0.1), so the Category Hub (F) can group records and the navigation generator
#   (D0.4) has tags to organize.
#
# WHY TWO PHASES (propose -> review -> apply)
#   The model must NEVER edit the data file directly. `propose` calls the API and
#   writes a proposals file a human reviews (correct a value, or set approved:false
#   to drop a record). `apply` reads that file and merges ONLY approved entries,
#   and only into EMPTY fields — so it can be re-run and never clobbers a hand-set
#   value. This mirrors the clean_csv/normalize scripts' "propose, human confirms"
#   posture and satisfies the plan's "never overwrites directly".
#
# WHY CONSTRAINED OUTPUT (the key design choice)
#   Subcategory is the gating canonical field, so the model is given a per-category
#   JSON-schema *enum* of only that display category's allowed subcategories
#   (output_config.format). It literally cannot invent a subcategory outside the
#   taxonomy. This is why taxonomy.json is the single source of truth: the same
#   file the validator enforces is the vocabulary the enricher assigns from.
#
# WHY RAW HTTP (no anthropic SDK)
#   The user declined to install the anthropic SDK. The Messages API is just an
#   HTTPS endpoint, so the script POSTs to it with the Python STANDARD LIBRARY
#   (urllib) — zero third-party dependencies. Structured outputs are a request
#   parameter, so output_config.format works identically over raw HTTP; auth is the
#   x-api-key header from ANTHROPIC_API_KEY. (Per the claude-api skill, raw HTTP is
#   the sanctioned path when the user explicitly declines the SDK.)
#
# =============================================================================

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional

# Paths are repo-root-relative (run from the repo root, like the other scripts).
# proposals/ is the review surface — a git-tracked, diffable artifact, NOT throwaway.
TAXONOMY_FILE = Path("app/data/taxonomy.json")
RESOURCES_FILE = Path("app/data/resources.communities.json")
FLYERS_FILE = Path("app/data/flyers.json")
PROPOSALS_DIR = Path("data-cleaning/proposals")
RESOURCES_PROPOSALS = PROPOSALS_DIR / "resources.communities.proposals.json"
FLYERS_PROPOSALS = PROPOSALS_DIR / "flyers.proposals.json"

MODEL = "claude-opus-4-8"  # per the claude-api skill default; classification needs no thinking
API_URL = "https://api.anthropic.com/v1/messages"
ANTHROPIC_VERSION = "2023-06-01"

# cost/urgency mirror the validator's VALID_COST/VALID_URGENCY. "unknown" is an
# enum escape hatch so the model isn't forced to guess a cost the directory omits;
# _fill_empty() drops "unknown" rather than writing it.
VALID_COST = ["free", "low-cost", "unknown"]
VALID_URGENCY = ["emergency", "time-limited", "standard"]


# -----------------------------------------------------------------------------
# TAXONOMY LOADER — turns taxonomy.json into two lookups:
#   raw_to_display[raw_category] -> {key,label,subcategories}  (mirrors
#     CategoryDefinition.java's matchCategories mapping, so a raw source category
#     like "Mental Health" resolves to the "health" display category + its subs)
#   all_subcategories -> the full de-duplicated topic vocabulary (44), used to
#     constrain flyer tags.
# -----------------------------------------------------------------------------
def load_taxonomy() -> Dict[str, Any]:
    data = json.loads(TAXONOMY_FILE.read_text(encoding="utf-8"))
    raw_to_display: Dict[str, Dict[str, Any]] = {}
    all_subcategories: List[str] = []
    for cat in data["categories"]:
        display = {
            "key": cat["key"],
            "label": cat["label"],
            "subcategories": cat.get("subcategories", []),
        }
        for raw in cat.get("matchCategories", []):
            raw_to_display[raw] = display
        for sub in cat.get("subcategories", []):
            if sub not in all_subcategories:  # preserve order, drop dupes (Eviction
                all_subcategories.append(sub)   # Prevention appears in housing+legal)
    return {"raw_to_display": raw_to_display, "all_subcategories": all_subcategories}


# -----------------------------------------------------------------------------
# JSON I/O — save_json matches normalize_resources.py exactly (indent=2,
# ensure_ascii=False, trailing newline) so diffs on the data files stay minimal.
# records_of() tolerates both shapes: the data files wrap rows in {records:[...]},
# a bare list is also accepted.
# -----------------------------------------------------------------------------
def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def records_of(payload: Any) -> List[Dict[str, Any]]:
    if isinstance(payload, dict) and isinstance(payload.get("records"), list):
        return payload["records"]
    if isinstance(payload, list):
        return payload
    raise ValueError("Expected a list or a {records:[...]} object.")


# -----------------------------------------------------------------------------
# CLAUDE API — one constrained classification per record, over raw HTTPS (urllib).
#   require_api_key(): reads ANTHROPIC_API_KEY; exits with a clear message if unset.
#     Only `propose` calls it; `apply` never touches the API.
#   call_claude(): POSTs the request body (x-api-key + anthropic-version headers),
#     surfaces HTTP/network errors as a clean exit, and returns the first text
#     block — which output_config.format guarantees is valid JSON.
#   resource_schema(): builds the per-category output schema. When the display
#     category has subcategories, `subcategory` is a required enum of exactly those;
#     when it has none (e.g. utilities), the field is omitted entirely.
#   classify_resource(): builds the request body with output_config.format and
#     json.loads the returned text. No thinking (constrained classification against
#     a small enum is simple), max_tokens small.
# -----------------------------------------------------------------------------
def require_api_key() -> str:
    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        sys.exit("ANTHROPIC_API_KEY is not set. Export it before running `propose`.")
    return key


def call_claude(body: Dict[str, Any], api_key: str) -> str:
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
    properties: Dict[str, Any] = {
        "tags": {"type": "array", "items": {"type": "string"},
                 "description": "1-4 short resident-facing topic tags."},
        "cost": {"type": "string", "enum": VALID_COST},
        "urgency": {"type": "string", "enum": VALID_URGENCY},
        "rationale": {"type": "string"},
    }
    required = ["tags", "cost", "urgency", "rationale"]
    if allowed_subcategories:
        properties["subcategory"] = {"type": "string", "enum": allowed_subcategories}
        required.insert(0, "subcategory")
    # additionalProperties:false + required-everything is mandatory for structured
    # outputs; guarantees tool_use.input validates exactly.
    return {"type": "object", "properties": properties, "required": required,
            "additionalProperties": False}


def classify_resource(api_key: str, record: Dict[str, Any], display: Dict[str, Any]) -> Dict[str, Any]:
    allowed = display["subcategories"]
    subline = (
        f'Pick exactly ONE subcategory from this allowed list for the '
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
        "Then give 1-4 short topic tags, a best-effort cost (free / low-cost / "
        "unknown when the directory does not say), and an urgency (emergency for "
        "crisis services, time-limited for seasonal or deadline-bound programs, "
        "otherwise standard)."
    )
    body = {
        "model": MODEL,
        "max_tokens": 1024,
        "output_config": {"format": {"type": "json_schema", "schema": resource_schema(allowed)}},
        "messages": [{"role": "user", "content": prompt}],
    }
    return json.loads(call_claude(body, api_key))


# Flyer pass: flyers are content-agnostic CivicContent. Their tags are constrained
# to the FULL subcategory vocabulary (a topic == a subcategory name), so a tagged
# flyer surfaces under the same navigation topic as a directory service.
def flyer_schema(all_subcategories: List[str]) -> Dict[str, Any]:
    return {
        "type": "object",
        "properties": {
            "tags": {"type": "array",
                     "items": {"type": "string", "enum": all_subcategories}},
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


# -----------------------------------------------------------------------------
# RULES PROPOSER (--method rules; the DEFAULT, no API). Added when the user
# declined to fund an API key. Two tiers:
#   RAW_DEFAULT[raw_category] -> subcategory  — for raw source categories that ARE
#     a subcategory (e.g. "Support Group" -> "Support Groups"). This alone covers
#     all of community-support at ~100%, because the DSCYF directory's raw category
#     is already the subcategory for those rows.
#   SUBCATEGORY_KEYWORDS[subcategory] -> [keywords]  — a lexicon for the broad
#     categories (Recreational, Mental Health, Housing, Food, Employment, Legal)
#     where the raw category doesn't pin a subcategory. Insertion order = priority
#     (specific subcategories first) so ties break toward the specific one.
# classify_resource_rules(): score allowed subcategories by keyword hits; if none
#   hit, fall back to RAW_DEFAULT; if still nothing, subcategory=None (flagged for
#   manual fill in review). Result shape is IDENTICAL to the AI path, so review and
#   apply are method-agnostic. On the 171 records this hit 169/171 (98%).
# The lexicon/maps are omitted from this annotated mirror for brevity — see the
# production script for the full keyword lists. classify_flyer_rules() scores the
# full vocabulary and takes the top 2; flyers describe intent not services, so its
# output is lower-confidence and always human-reviewed (FL-003, a volunteer flyer,
# keyword-matched "food banks/shelters" and had to be corrected by hand).
# -----------------------------------------------------------------------------
# RAW_DEFAULT = { "Support Group": "Support Groups", ... }              (see source)
# SUBCATEGORY_KEYWORDS = { "Crisis Services": ["crisis", "24 hour", ...], ... }
# def _detect_cost / _detect_urgency / _population_tags — best-effort heuristics.
# def classify_resource_rules(record, display) -> {subcategory, tags, cost, urgency, rationale}
# def classify_flyer_rules(flyer, all_subcategories) -> {tags, rationale}


# -----------------------------------------------------------------------------
# PROPOSE — iterate records, classify with the chosen method, write the review
# file. Never touches the data file. `method` picks the proposer: "rules" (offline,
# default) or "ai" (require_api_key() first). --category filters to one display key;
# --limit caps a trial run. Records whose raw category isn't in the taxonomy are
# reported, not guessed. Each proposal carries method + approved:True for veto.
# -----------------------------------------------------------------------------
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


# -----------------------------------------------------------------------------
# APPLY — merge approved proposals. _fill_empty() is the safety valve: it writes a
# field ONLY if currently empty (None/""/[]) and the proposed value is meaningful
# (not None/""/[]/"unknown"). So apply is idempotent, never clobbers a curated
# value, and drops the "unknown" cost sentinel. --category scopes an apply to one
# display key, matching the per-category propose/review/apply cadence of D0.3.
# -----------------------------------------------------------------------------
def _fill_empty(record: Dict[str, Any], field: str, value: Any) -> bool:
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
    by_id = {r.get("id"): r for r in records}  # match proposals to rows by id

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

    save_json(RESOURCES_FILE, payload)  # write back the WHOLE payload (meta + records)
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
        # UNION into existing tags (flyers already carry a hand-set tag or two) —
        # additive, so re-running is safe and hand tags survive.
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


# -----------------------------------------------------------------------------
# CLI — positional action (propose|apply) + --task (resources|flyers) + --method
# (rules default | ai). --category and --limit apply to the resources task. apply
# is method-agnostic (reads whatever proposals exist), so it takes no --method.
# -----------------------------------------------------------------------------
def main() -> int:
    parser = argparse.ArgumentParser(description="Enrich community resources / flyers (rules by default; --method ai uses the Claude API).")
    parser.add_argument("action", choices=["propose", "apply"])
    parser.add_argument("--task", choices=["resources", "flyers"], default="resources")
    parser.add_argument("--method", choices=["rules", "ai"], default="rules",
                        help="rules = offline keyword/raw-category proposer (no API); ai = Claude API.")
    parser.add_argument("--category", help="Display-category key filter (resources only).")
    parser.add_argument("--limit", type=int, help="Cap the number of records processed.")
    args = parser.parse_args()

    if args.task == "flyers":
        return propose_flyers(args.limit, args.method) if args.action == "propose" else apply_flyers()
    if args.action == "propose":
        return propose_resources(args.category, args.limit, args.method)
    return apply_resources(args.category)


if __name__ == "__main__":
    raise SystemExit(main())
