# Actual Housing candidate audit

Captured fixed Clock: 2026-10-16T16:00:00Z. Candidate state is scoped to Wilmington; original files remain hash-identical.

## All controlled Topics

|Topic|Current authored Help count|Exact actionable IDs|Distinct Topic proxy coverage|Direct / recent context|Newness|Featured|
|---|---|---|---|---|---|---|
|Transitional Housing|11|HA-002, HA-003, HA-005, HA-010, HA-012, HA-015, HA-019, HA-020, HA-028, HA-030, HA-037|1|0 / 0|unknown|yes|
|Emergency Shelter|9|HA-001, HA-006, HA-009, HA-016, HA-017, HA-018, HA-029, HA-031, HA-033|1|0 / 0|unknown|yes|
|Homeownership|4|HA-011, HA-013, HA-021, HA-022|1|0 / 0|unknown|yes|
|Rental Assistance|4|HA-008, HA-023, HA-026, HA-034|1|0 / 0|unknown|yes|
|Sober Living|3|HA-014, HA-024, HA-025|1|0 / 0|unknown|no|
|Senior Housing|2|HA-027, HA-036|1|0 / 0|unknown|no|
|Youth Housing|2|HA-007, HA-032|1|0 / 0|unknown|no|
|Public Housing|1|HA-035|1|0 / 0|unknown|no|
|Eviction Prevention|0|none|1|0 / 0|2026-08-12|no|

Eviction Prevention has an IM-06 lower-level image proxy on FL-002. Its zero Help IDs exclude it from featured Topics. That proxy does not admit an IM-07 VISUAL item: FL-002 has no helpAccess and is rejected before item role assignment. Its only admitted item is contextual OR-003 as FALLBACK.

## Stored versus scoped current/admitted records

Rows include non-Wilmington Housing, withheld Articles and Category-only Expert/FAQ; none is silently added to the36 Help IDs. Source columns identify producer provenance, not PROVIDER/HOST relationships. Resource catalog newness remains unknown even when source-derived createdDate exists. Full raw fields are in stored-housing-facts.json; actual cards/provenance/evidence/limitations are in real-catalog-before.json.

|ID|Origin|Community|Topic / authored proof|Editorial state / public status|Explicit urgency|Scoped Help / item admission|Actual item roles|Producer|Exclusion or role limitation|
|---|---|---|---|---|---|---|---|---|
|SD-048|app/data/resources.communities.json|bear-de|Sober Living / True|ACTIVE / True|standard|False / False|not admitted|dscyf-directory|different_community|
|SD-047|app/data/resources.communities.json|middletown-de|Rental Assistance / True|ACTIVE / True|emergency|False / False|not admitted|dscyf-directory|different_community|
|SD-049|app/data/resources.communities.json|new-castle-de|Emergency Shelter / True|ACTIVE / True|emergency|False / False|not admitted|dscyf-directory|different_community|
|SD-051|app/data/resources.communities.json|new-castle-de|Public Housing / True|ACTIVE / True|standard|False / False|not admitted|dscyf-directory|different_community|
|SD-052|app/data/resources.communities.json|newark-de|Public Housing / True|ACTIVE / True|standard|False / False|not admitted|dscyf-directory|different_community|
|SD-053|app/data/resources.communities.json|newark-de|Public Housing / True|ACTIVE / True|standard|False / False|not admitted|dscyf-directory|different_community|
|SD-050|app/data/resources.communities.json|newark-de|Rental Assistance / True|ACTIVE / True|emergency|False / False|not admitted|dscyf-directory|different_community|
|HA-004|app/data/resources.json|newport-de|Rental Assistance / True|ACTIVE / True|emergency|False / False|not admitted|dscyf-directory|different_community|
|EA-001|app/data/expert-answers.json|wilmington-de|Category only / False|ACTIVE / True|absent|False / False|not admitted|de-volunteer-legal-services|no_matching_authored_topic_proof|
|EA-005|app/data/expert-answers.json|wilmington-de|Category only / False|ACTIVE / True|absent|False / False|not admitted|de-volunteer-legal-services|no_matching_authored_topic_proof|
|FAQ-002|app/data/faq.json|wilmington-de|Category only / False|ACTIVE / True|absent|False / False|not admitted|first-step|no_matching_authored_topic_proof|
|NP-001|app/data/news.json|wilmington-de|Category only / False|EXPIRED / True|time-limited|False / False|not admitted|city-of-wilmington|not_current_editorial_inventory; no_matching_authored_topic_proof|
|NP-002|app/data/news.json|wilmington-de|Category only / False|EXPIRED / True|time-limited|False / False|not admitted|de-housing-authority|not_current_editorial_inventory; no_matching_authored_topic_proof|
|NP-004|app/data/news.json|wilmington-de|Category only / False|ACTIVE / True|standard|False / False|not admitted|de-dhss|no_matching_authored_topic_proof|
|NP-005|app/data/news.json|wilmington-de|Category only / False|ACTIVE / True|standard|False / False|not admitted|west-end-neighborhood-house|no_matching_authored_topic_proof|
|NP-006|app/data/news.json|wilmington-de|Category only / False|EXPIRED / True|time-limited|False / False|not admitted|wilmington-housing-authority|not_current_editorial_inventory; no_matching_authored_topic_proof|
|NP-009|app/data/news.json|wilmington-de|Category only / False|EXPIRED / True|standard|False / False|not admitted|west-end-neighborhood-house|not_current_editorial_inventory; no_matching_authored_topic_proof|
|HA-001|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-001; no_authentic_catalog_date:HA-001; no_positive_resident_timely_evidence:HA-001; no_positive_durable_evidence:HA-001; no_positive_visual_media_source_evidence:HA-001|
|HA-006|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-006; no_authentic_catalog_date:HA-006; no_positive_resident_timely_evidence:HA-006; no_positive_durable_evidence:HA-006; no_positive_visual_media_source_evidence:HA-006|
|HA-009|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-009; no_authentic_catalog_date:HA-009; no_positive_resident_timely_evidence:HA-009; no_positive_durable_evidence:HA-009; no_positive_visual_media_source_evidence:HA-009|
|HA-016|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-016; no_positive_resident_timely_evidence:HA-016; no_positive_visual_media_source_evidence:HA-016|
|HA-017|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-017; no_authentic_catalog_date:HA-017; no_positive_resident_timely_evidence:HA-017; no_positive_durable_evidence:HA-017; no_positive_visual_media_source_evidence:HA-017|
|HA-018|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-018; no_authentic_catalog_date:HA-018; no_positive_resident_timely_evidence:HA-018; no_positive_durable_evidence:HA-018; no_positive_visual_media_source_evidence:HA-018|
|HA-029|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-029; no_authentic_catalog_date:HA-029; no_positive_resident_timely_evidence:HA-029; no_positive_durable_evidence:HA-029; no_positive_visual_media_source_evidence:HA-029|
|HA-031|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-031; no_authentic_catalog_date:HA-031; no_positive_resident_timely_evidence:HA-031; no_positive_durable_evidence:HA-031; no_positive_visual_media_source_evidence:HA-031|
|HA-033|app/data/resources.json|wilmington-de|Emergency Shelter / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-033; no_authentic_catalog_date:HA-033; no_positive_resident_timely_evidence:HA-033; no_positive_durable_evidence:HA-033; no_positive_visual_media_source_evidence:HA-033|
|FL-002|app/data/flyers.json|wilmington-de|Eviction Prevention / True|ACTIVE / True|absent|False / False|not admitted|community-legal-aid|flyer_help_access_required|
|OR-001|app/data/articles.json|wilmington-de|Eviction Prevention / True|ACTIVE / True|absent|False / False|not admitted|first-step|article_not_approved|
|OR-003|app/data/articles.json|wilmington-de|Eviction Prevention / True|ACTIVE / True|absent|False / True|no positive role|first-step|no_positive_resident_timely_evidence:OR-003; no_positive_durable_evidence:OR-003; no_positive_visual_media_source_evidence:OR-003|
|HA-011|app/data/resources.json|wilmington-de|Homeownership / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-011; no_positive_resident_timely_evidence:HA-011; no_positive_visual_media_source_evidence:HA-011|
|HA-013|app/data/resources.json|wilmington-de|Homeownership / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-013; no_positive_resident_timely_evidence:HA-013; no_positive_visual_media_source_evidence:HA-013|
|HA-021|app/data/resources.json|wilmington-de|Homeownership / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-021; no_positive_resident_timely_evidence:HA-021; no_positive_visual_media_source_evidence:HA-021|
|HA-022|app/data/resources.json|wilmington-de|Homeownership / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-022; no_positive_resident_timely_evidence:HA-022; no_positive_visual_media_source_evidence:HA-022|
|HA-035|app/data/resources.json|wilmington-de|Public Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-035; no_positive_resident_timely_evidence:HA-035; no_positive_visual_media_source_evidence:HA-035|
|HA-008|app/data/resources.json|wilmington-de|Rental Assistance / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-008; no_positive_resident_timely_evidence:HA-008; no_positive_visual_media_source_evidence:HA-008|
|HA-023|app/data/resources.json|wilmington-de|Rental Assistance / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-023; no_authentic_catalog_date:HA-023; no_positive_resident_timely_evidence:HA-023; no_positive_durable_evidence:HA-023; no_positive_visual_media_source_evidence:HA-023|
|HA-026|app/data/resources.json|wilmington-de|Rental Assistance / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-026; no_authentic_catalog_date:HA-026; no_positive_resident_timely_evidence:HA-026; no_positive_durable_evidence:HA-026; no_positive_visual_media_source_evidence:HA-026|
|HA-034|app/data/resources.json|wilmington-de|Rental Assistance / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-034; no_positive_resident_timely_evidence:HA-034; no_positive_visual_media_source_evidence:HA-034|
|HA-027|app/data/resources.json|wilmington-de|Senior Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-027; no_positive_resident_timely_evidence:HA-027; no_positive_visual_media_source_evidence:HA-027|
|HA-036|app/data/resources.json|wilmington-de|Senior Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-036; no_positive_resident_timely_evidence:HA-036; no_positive_visual_media_source_evidence:HA-036|
|HA-014|app/data/resources.json|wilmington-de|Sober Living / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-014; no_positive_resident_timely_evidence:HA-014; no_positive_visual_media_source_evidence:HA-014|
|HA-024|app/data/resources.json|wilmington-de|Sober Living / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-024; no_positive_resident_timely_evidence:HA-024; no_positive_visual_media_source_evidence:HA-024|
|HA-025|app/data/resources.json|wilmington-de|Sober Living / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-025; no_positive_resident_timely_evidence:HA-025; no_positive_visual_media_source_evidence:HA-025|
|HA-002|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-002; no_positive_resident_timely_evidence:HA-002; no_positive_visual_media_source_evidence:HA-002|
|HA-003|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-003; no_positive_resident_timely_evidence:HA-003; no_positive_visual_media_source_evidence:HA-003|
|HA-005|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-005; no_positive_resident_timely_evidence:HA-005; no_positive_visual_media_source_evidence:HA-005|
|HA-010|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-010; no_positive_resident_timely_evidence:HA-010; no_positive_visual_media_source_evidence:HA-010|
|HA-012|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-012; no_positive_resident_timely_evidence:HA-012; no_positive_visual_media_source_evidence:HA-012|
|HA-015|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-015; no_positive_resident_timely_evidence:HA-015; no_positive_visual_media_source_evidence:HA-015|
|HA-019|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-019; no_positive_resident_timely_evidence:HA-019; no_positive_visual_media_source_evidence:HA-019|
|HA-020|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-020; no_positive_resident_timely_evidence:HA-020; no_positive_visual_media_source_evidence:HA-020|
|HA-028|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-028; no_positive_resident_timely_evidence:HA-028; no_positive_visual_media_source_evidence:HA-028|
|HA-030|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-030; no_positive_resident_timely_evidence:HA-030; no_positive_visual_media_source_evidence:HA-030|
|HA-037|app/data/resources.json|wilmington-de|Transitional Housing / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-037; no_authentic_catalog_date:HA-037; no_positive_resident_timely_evidence:HA-037; no_positive_durable_evidence:HA-037; no_positive_visual_media_source_evidence:HA-037|
|HA-007|app/data/resources.json|wilmington-de|Youth Housing / True|ACTIVE / True|emergency|True / True|no positive role|dscyf-directory|no_explicit_standard_urgency:HA-007; no_authentic_catalog_date:HA-007; no_positive_resident_timely_evidence:HA-007; no_positive_durable_evidence:HA-007; no_positive_visual_media_source_evidence:HA-007|
|HA-032|app/data/resources.json|wilmington-de|Youth Housing / True|ACTIVE / True|standard|True / True|DURABLE|dscyf-directory|no_authentic_catalog_date:HA-032; no_positive_resident_timely_evidence:HA-032; no_positive_visual_media_source_evidence:HA-032|

## Actual winners, next eligible rivals and collision stages

All real admitted actionable Housing candidates have no genuine catalog-newness date. DURABLE has no applicable time discriminator; its exact standard-urgency peers tie on relevance and unknown newness. Fallback compares remaining Help first, then genuine newness, then canonical ID. Next-rival IDs below are computed only for these equal-substantive real groups, not as a replacement production comparator.

|Topic|Position|Winner|Next eligible rival|Comparator stage|
|---|---|---|---|---|
|Emergency Shelter|DURABLE|HA-016|none|SOLE_FEASIBLE_CANDIDATE|
|Emergency Shelter|FALLBACK|HA-001|HA-006|STABLE_CANONICAL_ID|
|Emergency Shelter|FALLBACK|HA-006|HA-009|STABLE_CANONICAL_ID|
|Transitional Housing|DURABLE|HA-002|HA-003|STABLE_CANONICAL_ID|
|Transitional Housing|FALLBACK|HA-003|HA-005|STABLE_CANONICAL_ID|
|Transitional Housing|FALLBACK|HA-005|HA-010|STABLE_CANONICAL_ID|
|Sober Living|DURABLE|HA-014|HA-024|STABLE_CANONICAL_ID|
|Sober Living|FALLBACK|HA-024|HA-025|STABLE_CANONICAL_ID|
|Sober Living|FALLBACK|HA-025|none|SOLE_FEASIBLE_CANDIDATE|
|Rental Assistance|DURABLE|HA-008|HA-034|STABLE_CANONICAL_ID|
|Rental Assistance|FALLBACK|HA-023|HA-026|STABLE_CANONICAL_ID|
|Rental Assistance|FALLBACK|HA-026|HA-034|STABLE_CANONICAL_ID|
|Public Housing|DURABLE|HA-035|none|SOLE_FEASIBLE_CANDIDATE|
|Homeownership|DURABLE|HA-011|HA-013|STABLE_CANONICAL_ID|
|Homeownership|FALLBACK|HA-013|HA-021|STABLE_CANONICAL_ID|
|Homeownership|FALLBACK|HA-021|HA-022|STABLE_CANONICAL_ID|
|Senior Housing|DURABLE|HA-027|HA-036|STABLE_CANONICAL_ID|
|Senior Housing|FALLBACK|HA-036|none|SOLE_FEASIBLE_CANDIDATE|
|Youth Housing|DURABLE|HA-032|none|SOLE_FEASIBLE_CANDIDATE|
|Youth Housing|FALLBACK|HA-007|none|SOLE_FEASIBLE_CANDIDATE|
|Eviction Prevention|FALLBACK|OR-003|none|SOLE_FEASIBLE_CANDIDATE|
