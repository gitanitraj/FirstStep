package org.firststep.backend.shared.classification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Assigns DESCRIPTIVE tags — the answer to "how can this be found?".
 *
 * <p><b>Tags are never category names.</b> That restriction is the whole reason
 * this is a separate class from {@link CategoryClassifier} rather than one method
 * returning both. Emitting "Housing" as a tag would recreate exactly the
 * conflation Slice F1 removed, where one field meant editorial classification for
 * one content type and search metadata for another. What gets emitted is the
 * matched EVIDENCE — "eviction", "voucher", "tenant" — which is genuinely
 * descriptive: it says what the content talks about, not where it belongs.
 *
 * <p>This is a strict improvement on what RSS did before, which set its
 * descriptive tags to the lowercased names of the matched category buckets
 * (["housing", "legal"]) — category names wearing a tag's clothes.
 *
 * <p>Tags are ADDITIVE. Hand-authored tags are never replaced, only extended,
 * because a human choosing search vocabulary knows something the classifier does
 * not. Both {@code SearchService} and {@code DecisionAgentService} already score
 * against {@code tags} via {@code TextScore.match}, so every tag added here
 * improves search and AI retrieval immediately.
 */
@Component
public class TagClassifier {

    /**
     * Union of existing tags and classification evidence, preserving order and
     * de-duplicating case-insensitively (a hand-authored "Eviction" should not
     * gain a machine "eviction" beside it).
     *
     * @param existing  hand-authored tags, may be null
     * @param evidence  keywords that matched during category classification
     */
    public List<String> mergeTags(List<String> existing, List<String> evidence) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> merged = new ArrayList<>();

        if (existing != null) {
            for (String tag : existing) {
                if (tag != null && !tag.isBlank() && seen.add(tag.toLowerCase(Locale.ROOT))) {
                    merged.add(tag);
                }
            }
        }
        for (String term : evidence) {
            if (term != null && !term.isBlank() && seen.add(term.toLowerCase(Locale.ROOT))) {
                merged.add(term);
            }
        }
        return merged;
    }
}

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// TagClassifier merges classification evidence into an item's DESCRIPTIVE tags —
// the answer to "how can this be found?".
// =============================================================================

// =============================================================================
// SECTION 1 — WHY IT IS A SEPARATE CLASS AT ALL
// =============================================================================
// This is thirty lines and one method. CategoryClassifier could have returned
// both the categories and the tags. It is separate because the SEPARATION is the
// point: the moment one class produces both, the temptation to emit a category
// name as a tag becomes a one-line change nobody reviews carefully.
//
// That is not hypothetical. It is exactly what RSS did before F2:
//
//     item.tags = cls.resourceTags;   // ["housing", "legal"] — lowercased
//                                     // names of the matched CATEGORY buckets
//
// Category names wearing a tag's clothes. Descriptive tags that described
// nothing except which category the item was already in.
//
// What gets emitted now is the matched EVIDENCE — "eviction", "voucher",
// "tenant" — which is genuinely descriptive: it says what the content talks
// about, not where it belongs. The guarantee is enforced end-to-end by
// CivicContentClassifierTest.shouldNotPutCategoryNamesIntoDescriptiveTags.
//
// =============================================================================
// SECTION 2 — WHY TAGS MERGE WHILE EDITORIAL FIELDS DO NOT
// =============================================================================
// CivicContentClassifier fills categoryTags/subcategory only when ABSENT, but
// always merges tags. The asymmetry is deliberate and follows from what each
// field is:
//
//   Editorial classification is a PLACEMENT. An item has one home. A second
//   opinion is a conflict, so the classifier defers to the editor entirely.
//
//   Descriptive tags are METADATA. More is strictly better for search, and a
//   hand-authored tag and a machine-derived tag do not compete for anything.
//
// Existing tags always come FIRST in the merged list, and dedupe is
// case-insensitive so a hand-authored "Eviction" does not acquire a machine
// "eviction" beside it. Order matters only cosmetically today, but a human's
// chosen vocabulary leading the list is the right default if anything ever
// truncates it.
//
// =============================================================================
// SECTION 3 — WHY THIS IMPROVES THINGS IMMEDIATELY
// =============================================================================
// SearchService and DecisionAgentService already score against `tags` via
// TextScore.match — three call sites between them, all predating F2. So every
// evidence term added here improves keyword search and AI retrieval with no
// further wiring. That is the payoff of the CivicContent contract: a new
// producer of a contract field benefits every existing consumer of it.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A controlled tag vocabulary (tags must come from an approved list).
//   Rejected: tags exist precisely to capture what the taxonomy does NOT — the
//   specific, the local, the not-yet-categorized. Controlling them would make
//   them a second taxonomy, which is what everything here is trying to prevent.
// - Extracting salient terms statistically (TF-IDF over the corpus) rather than
//   reusing classification evidence. More tags, better recall, and no
//   explanation for why any given tag appeared. Deferred with the relationship
//   graph, which is where that kind of analysis belongs.
// - Replacing rather than merging hand-authored tags. Never seriously
//   considered, but worth recording as forbidden: an editor's search vocabulary
//   encodes knowledge of how residents actually phrase things, which no keyword
//   table has.
