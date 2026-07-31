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
