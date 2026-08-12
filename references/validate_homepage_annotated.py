"""
=============================================================================
ANNOTATED REFERENCE — data-cleaning/scripts/validate_homepage.py
Slice H. See references/decisions.md Decision 042.
Keep this mirror in sync whenever the production script changes.
=============================================================================

WHAT THIS SCRIPT IS
    The gate for app/data/homepage.json, the third authored presentation
    artifact in the project. It follows validate_navigation.py's shape exactly:
    load taxonomy.json, check the presentation file against it, print a report,
    exit non-zero on error.

WHY IT EARNS ITS PLACE
    Three of its four rules are ordinary schema checks. The fourth is the one
    that matters — it is the automated defence of Decision 041's central
    architectural claim. See SECTION 2.
=============================================================================
"""

# ---- Config -----------------------------------------------------------------
# INPUT_FILE    = app/data/homepage.json
# TAXONOMY_FILE = app/data/taxonomy.json
# CATEGORY  = "category"   DISCOVERY = "discovery"

# =============================================================================
# SECTION 1 — THE FOUR RULES
# =============================================================================
#
#   kind must be "category" or "discovery"
#       The kind is what records whether a pathway is taxonomy or a query. A
#       missing or misspelled kind would make PathwayService silently treat the
#       entry as a category and try to resolve it.
#
#   a category pathway MUST exist in taxonomy.json
#       Catches "hosuing". PathwayService skips an unknown key rather than
#       throwing (a presentation file must not take the app down), so without
#       this gate a typo removes a pathway from the homepage and nothing
#       anywhere reports it.
#
#   a category pathway MUST NOT author its own label or icon
#       Anti-drift. Labels are resolved from the taxonomy at load time; a label
#       authored here would be a second copy of the vocabulary, and renaming a
#       category would silently leave the homepage showing the old name. This is
#       the same class of bug Decision 032 removed by deleting
#       CategoryDefinition.ALL — worth blocking before it can be reintroduced.
#
#   a discovery pathway MUST author its own label and icon
#       It has no taxonomy row to resolve one from. PathwayService deliberately
#       does NOT invent a label from the key, because guessing would hide the
#       omission from this check.
#
# =============================================================================
# SECTION 2 — THE RULE WITH TEETH
# =============================================================================
#
#     if kind == DISCOVERY and key in taxonomy:
#         error("'{key}' is a discovery pathway but ALSO a category …
#                A facet must not be promoted into the vocabulary — it answers
#                'who is this relevant to?', which the taxonomy never asked.")
#
# THIS IS THE ONE TO UNDERSTAND. Everything else here is hygiene; this rule is
# architecture.
#
# The failure it prevents is not a typo — it is a REASONABLE-LOOKING DECISION
# made under time pressure. Someone needs the Seniors pathway to "work like the
# others", notices that adding `seniors` to taxonomy.json would make
# /category/seniors resolve, and does it. Nothing breaks. Tests pass. The
# homepage looks better. And First Step now has a category that answers "who is
# this for?" sitting alongside categories that answer "what is this about?",
# which is the exact conflation Decision 041 was written to prevent and which
# 01-domain-model.md's three-question test exists to detect.
#
# The build cannot notice that. This rule can.
#
# It is negative-tested: pointing the discovery entry at an existing category key
# makes the validator exit 1 with that message.
#
# =============================================================================
# SECTION 3 — WHAT IT REPORTS BUT DOES NOT FAIL ON
# =============================================================================
# Categories absent from the homepage are listed as INFORMATIONAL:
#
#     clothing, community-events, community-support, utilities
#
# They are not errors. The homepage is a curated front door, not an index — a
# validator that demanded full coverage would be enforcing the opposite of the
# slice's design. But an editor should be able to SEE what they left out, so the
# omission is surfaced without being punished. (Contrast validate_navigation.py,
# where an unplaced topic IS an error: navigation must reach every topic, since
# an unreachable topic is a bug rather than a choice.)
#
# =============================================================================
# SECTION 4 — THE COST, RECORDED HONESTLY
# =============================================================================
# This is the FIFTH separate taxonomy loader in the codebase — after
# validate_schema.py, validate_news.py, validate_navigation.py, and Java's
# TaxonomyService. Extracting a shared loader is tech-debt item 1, deferred by
# the user with a clear instruction: raise it, do not start it.
#
# Raised here rather than fixed. Following the established pattern kept this
# script reviewable next to its three siblings; unifying five loaders mid-slice
# would have mixed an architectural refactor into functional work, which is the
# specific thing Decision 031 forbids.
# =============================================================================
