/* IM-06 annotated reference.
 * What: scoped current inventory, authored classification proof, approved role facts and lexicographic Topic selection.
 * Why: retain identity and all seven P5 stages without accepting keyword inference or counting one ID in two roles.
 * Interaction: current services and helpFor supply eligibility; metadata proof establishes stored classification;
 * qualification and maximum matching feed ordered internal evidence and nonblocking diagnostics.
 * Alternatives rejected: raw-record eligibility, inferred Topic relevance, persisted scores/flags,
 * retrieval as catalog newness, final item selection and public featured UI owned by IM-07/08.
 */
package org.firststep.backend.category.selection;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.firststep.backend.category.selection.FeaturedRoleQualification.Role;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.springframework.stereotype.Service;

/**
 * IM-06 internal callable contract. No endpoint/serialization into resident responses.
 * Evidence and diagnostics are bounded read-only stewardship output, not item winners.
 */
@Service
public class FeaturedTopicSelector {
    public record TopicEvidence(String key, String name, int actionableCount, int distinctRoleCoverage,
            int directActionCount, int recentContextCount, LocalDate catalogOrApprovalNewness,
            List<String> actionableIds, Map<String, Set<Role>> availableRoles,
            List<String> directActionIds, List<String> contextIds, List<String> metadataLimitations) {}
    public record Diagnostic(String code, String communityId, String categoryKey,
            List<String> topicKeys, String explanation) {}
    public record Selection(String communityId, String categoryKey, LocalDate selectionDate,
            List<TopicEvidence> selected, List<TopicEvidence> candidates, List<Diagnostic> diagnostics) {}

    private static final Comparator<TopicEvidence> SUBSTANTIVE =
            Comparator.comparingInt(TopicEvidence::actionableCount).reversed()
                    .thenComparing(Comparator.comparingInt(TopicEvidence::distinctRoleCoverage).reversed())
                    .thenComparing(Comparator.comparingInt(TopicEvidence::directActionCount).reversed())
                    .thenComparing(Comparator.comparingInt(TopicEvidence::recentContextCount).reversed())
                    .thenComparing(TopicEvidence::catalogOrApprovalNewness,
                            Comparator.nullsLast(Comparator.reverseOrder()));
    private final TaxonomyService taxonomy;
    private final CategoryContentService content;
    private final FeaturedTopicInventory inventory;
    private final Clock clock;
    private final EditorialExpiryConfiguration dates;

    public FeaturedTopicSelector(TaxonomyService taxonomy, CategoryContentService content,
            FeaturedTopicInventory inventory, Clock clock, EditorialExpiryConfiguration dates) {
        this.taxonomy = taxonomy; this.content = content; this.inventory = inventory;
        this.clock = clock; this.dates = dates;
    }

    /** Explicit catalog partition required; historical unscoped browse is not a user-selection policy. */
    public Selection select(String communityId, String categoryKey) {
        if (communityId == null || communityId.isBlank()) throw new IllegalArgumentException("Explicit Community required");
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(
                dates.getCommunityZones().getOrDefault(communityId, dates.getDefaultZone()))));
        var category = taxonomy.findByKey(categoryKey).orElse(null);
        if (category == null) return new Selection(communityId, categoryKey, today, List.of(), List.of(), List.of());
        var snapshot = inventory.current(communityId, category);
        var current = snapshot.items();
        List<TopicEvidence> candidates = new ArrayList<>();
        for (String topic : category.subcategories()) {
            Set<String> provenIds = current.stream().filter(item -> topic.equalsIgnoreCase(item.subcategory))
                    .map(item -> item.id).collect(java.util.stream.Collectors.toSet());
            List<String> helpIds = content.helpFor(categoryKey, topic, communityId).stream()
                    .map(ContentItem::id).filter(provenIds::contains).distinct().sorted().toList();
            Map<String, Set<Role>> roles = new TreeMap<>();
            Set<String> direct = new TreeSet<>();
            Set<String> context = new TreeSet<>();
            Set<String> limitations = new TreeSet<>();
            LocalDate newness = null;
            for (var item : current) {
                if (!topic.equalsIgnoreCase(item.subcategory)) continue;
                var facts = FeaturedRoleQualification.assess(item, today);
                if (!facts.roles().isEmpty()) roles.put(item.id, facts.roles());
                // The exact semantic and window cannot bypass the authoritative Get Help join.
                if (facts.directAction() && helpIds.contains(item.id)) direct.add(item.id);
                if (facts.recentContext()) context.add(item.id);
                limitations.addAll(facts.limitations());
                LocalDate date = facts.catalogOrApprovalDate();
                if (date != null && (newness == null || date.isAfter(newness))) newness = date;
            }
            candidates.add(new TopicEvidence(TaxonomyService.topicSlug(topic), topic, helpIds.size(),
                    FeaturedRoleQualification.maximumDistinctCoverage(roles), direct.size(), context.size(),
                    newness, helpIds, java.util.Collections.unmodifiableMap(new TreeMap<>(roles)),
                    List.copyOf(direct), List.copyOf(context), List.copyOf(limitations)));
        }
        Selection ordered = order(communityId, categoryKey, today, candidates);
        if (snapshot.classificationLimitations().isEmpty()) return ordered;
        List<Diagnostic> diagnostics = new ArrayList<>(ordered.diagnostics());
        diagnostics.add(new Diagnostic("CLASSIFICATION_EVIDENCE_LIMITATION", communityId, categoryKey,
                List.of(), String.join(";", snapshot.classificationLimitations())));
        return new Selection(communityId, categoryKey, today, ordered.selected(), ordered.candidates(), List.copyOf(diagnostics));
    }

    /** Package-private for isolated hierarchy fixtures; production callers always use select(). */
    static Selection order(String communityId, String categoryKey, LocalDate today, List<TopicEvidence> input) {
        List<TopicEvidence> candidates = input.stream()
                .sorted(SUBSTANTIVE.thenComparing(TopicEvidence::key)).toList();
        List<TopicEvidence> qualifying = candidates.stream().filter(topic -> topic.actionableCount() > 0).toList();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (int i = 0; i < qualifying.size();) {
            int end = i + 1;
            while (end < qualifying.size() && SUBSTANTIVE.compare(qualifying.get(i), qualifying.get(end)) == 0) end++;
            if (end - i > 1) diagnostics.add(new Diagnostic("STABLE_TOPIC_KEY_FALLBACK", communityId,
                    categoryKey, qualifying.subList(i, end).stream().map(TopicEvidence::key).toList(),
                    "Equal actionable count, distinct role coverage, direct actions, recent context and catalog/approval newness; stable key resolves order."));
            i = end;
        }
        return new Selection(communityId, categoryKey, today, qualifying.stream().limit(4).toList(),
                candidates, List.copyOf(diagnostics));
    }
}
