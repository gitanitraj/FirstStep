#!/usr/bin/env python3
"""
Validates app/data/organizations.json — the organization registry — and every
EXPLICIT provider reference into it, across the whole served corpus.

WHY IT MATTERS
    Organizations used to have no record: identity was a display string, and the
    URL was slugify(that string). The registry replaced that with stable IDs
    (Decision 049), and this script is what keeps the two sides in step.

    Unlike ContentSource, an unresolvable organization_id is NOT tolerated at
    runtime with a log line — a resource whose organization cannot be resolved
    still renders, but its provider link would point nowhere. This gate is where
    that is caught.
"""
import json
import re
import sys
from pathlib import Path

REGISTRY = Path("app/data/organizations.json")

# EXPLICIT provider references, and only those.
#
# The registry's first version checked resources.json alone, which let the
# registry look complete while eight organizations named by flyers and expert
# answers had no record at all. The check was not wrong — it was too narrow, and
# narrowness is invisible: a validator that passes tells you nothing about what
# it never looked at.
#
# What belongs here is the question "does the schema explicitly say this content
# is PROVIDED BY an organization?":
#
#   resources[].organization             the service provider
#   flyers[].organization                the organization running the thing
#   expert-answers[].expert_organization the answering organization
#
# What is deliberately absent:
#
#   news[].source_id      PRODUCER identity — who published the item, which is
#                         ContentSource's question and answered by its own
#                         registry. A city publishing a notice about rental
#                         assistance is not thereby a provider to contact.
#   *.contentSource       same, everywhere it appears.
#   free text             descriptions, summaries and bodies name organizations
#                         constantly and incidentally. Inferring identity from
#                         prose would manufacture records from mentions.
PROVIDER_SOURCES = [
    (Path("app/data/resources.json"), "resources", "organization"),
    (Path("app/data/flyers.json"), "flyers", "organization"),
    (Path("app/data/expert-answers.json"), "expert-answers", "expert_organization"),
]
# Supplementary imported rows may omit a provider ID. Any supplied reference must resolve.
# This checks identity only: a Flyer organizer or Expert affiliation is not a typed PROVIDER/HOST.
OPTIONAL_REFERENCE_SOURCES = [
    (Path("app/data/resources.communities.json"), "resources.communities", "organization"),
]
ID_RE = re.compile(r"^ORG-\d{3}$")
SLUG_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


def records(path, key):
    data = json.loads(path.read_text())
    if isinstance(data, list):
        return data
    if key in data:
        return data[key]
    return data[next(k for k, v in data.items() if isinstance(v, list))]


def main():
    strict = "--strict" in sys.argv
    errors, warnings = [], []

    orgs = records(REGISTRY, "organizations")

    # Every piece of content whose schema names a providing organization.
    referring = []
    for path, label, name_field in PROVIDER_SOURCES:
        for r in records(path, label):
            referring.append((label, r.get("id"), r.get("organization_id"), r.get(name_field)))

    for path, label, name_field in OPTIONAL_REFERENCE_SOURCES:
        if path.exists():
            for r in records(path, label):
                if r.get("organization_id"):
                    referring.append((label, r.get("id"), r["organization_id"], r.get(name_field)))

    seen_ids, seen_slugs, seen_names = {}, {}, {}
    for i, o in enumerate(orgs):
        oid, name, slug = o.get("id"), o.get("name"), o.get("slug")

        # RULE 1 — a well-formed, unique, stable ID. This is the identity;
        # everything else about an organization may change.
        if not oid or not ID_RE.match(str(oid)):
            errors.append(f"[{i}] id {oid!r} malformed. Expected ORG-### (e.g. ORG-001).")
        elif oid in seen_ids:
            errors.append(f"[{i}] duplicate id {oid!r}, already used by {seen_ids[oid]!r}. "
                          f"Two organizations sharing an ID means resources silently "
                          f"attach to whichever loads second.")
        else:
            seen_ids[oid] = name

        # RULE 2 — a unique, URL-safe slug. Distinct from the ID: this is public,
        # and it changes when the name does.
        if not slug or not SLUG_RE.match(str(slug)):
            errors.append(f"[{i}] slug {slug!r} is not URL-safe. Expected lowercase "
                          f"alphanumerics separated by single hyphens.")
        elif slug in seen_slugs:
            errors.append(f"[{i}] duplicate slug {slug!r}, already used by "
                          f"{seen_slugs[slug]!r}. Two organizations cannot share a URL — "
                          f"one of them becomes unreachable.")
        else:
            seen_slugs[slug] = name

        # RULE 3 — a name, and only one organization per name. The name is what a
        # resident reads; two identical names are indistinguishable on screen.
        if not name or not str(name).strip():
            errors.append(f"[{i}] {oid} has no name. Nothing would render.")
        elif name in seen_names:
            errors.append(f"[{i}] duplicate name {name!r} ({oid} and {seen_names[name]}).")
        else:
            seen_names[name] = oid

    # RULE 4 — every EXPLICIT provider reference resolves, in every content type.
    for label, rid, ref, name in referring:
        if not ref:
            errors.append(f"{label} {rid} names organization {name!r} but has no "
                          f"organization_id. It would render with a provider the "
                          f"registry has never heard of.")
        elif ref not in seen_ids:
            errors.append(f"{label} {rid} references unknown organization_id {ref!r}.")

    # RULE 5 — MIGRATION COMPLETENESS, not a domain invariant.
    #
    # Every registry entry should currently correspond to at least one resource,
    # because the registry was GENERATED from the resources in this migration.
    # An orphan therefore means the migration dropped something, which is worth
    # catching now.
    #
    # It is NOT a permanent rule that an organization may never exist without a
    # served resource — a real registry will legitimately hold organizations
    # whose listings lapsed, or that were added ahead of their services. When
    # that day comes this rule is retired, not argued with.
    referenced = {ref for _, _, ref, _ in referring}
    for oid, name in seen_ids.items():
        if oid not in referenced:
            warnings.append(f"{oid} ({name}) is referenced by no content. Expected "
                            f"during this migration only — see RULE 5.")

    print(f"  organizations       : {len(orgs)}")
    print(f"  provider references : {len(referring)} across "
          f"{len({l for l, _, _, _ in referring})} content types")
    print(f"  with org-level website: {sum(1 for o in orgs if o.get('website'))}")
    for w in warnings:
        print(f"  WARN  {w}")
    for e in errors:
        print(f"  ERROR {e}")

    if errors or (strict and warnings):
        print(f"\n  FAILED — {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1
    print(f"\n  OK — {len(warnings)} warning(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
