package org.firststep.backend.category.selection;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.firststep.backend.category.selection.FeaturedRoleQualification.Role;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.stereotype.Service;

/**
 * IM-07 internal item selection. Reuses current authored inventory and existing card projections.
 * No controller exposes diagnostics; IM-08 must deliberately project cards into its public contract.
 * Maximum matching precedes lexicographic TIMELY/DURABLE/VISUAL assignment, never a score.
 */
@Service
public class FeaturedItemSelector {
    public enum PositionType { TIMELY, DURABLE, VISUAL, FALLBACK }
    public record Provenance(String communityId, String sourceId, String sourceName,
            String sourceType, String sourceUrl, String originalImage) {}
    public record Candidate(ContentItem card, Provenance provenance, boolean actionable,
            Set<Role> roles, LocalDate meaningfulDate, LocalDate catalogOrApprovalDate,
            List<String> evidence, List<String> limitations) {
        public Candidate {
            roles = java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(roles));
            evidence = List.copyOf(evidence); limitations = List.copyOf(limitations);
        }
        public String id() { return card.id(); }
    }
    public record Position(PositionType type, Candidate item, String tieStage) {}
    public record Diagnostic(String code, String communityId, String categoryKey, String topic,
            List<String> itemIds, String explanation) {
        public Diagnostic { itemIds = List.copyOf(itemIds); }
    }
    public record Selection(String communityId, String categoryKey, String topic, LocalDate selectionDate,
            List<Position> positions, List<Candidate> candidates, List<Diagnostic> diagnostics) {
        public Selection {
            positions = List.copyOf(positions); candidates = List.copyOf(candidates); diagnostics = List.copyOf(diagnostics);
        }
    }

    private final TaxonomyService taxonomy;
    private final CategoryContentService content;
    private final FeaturedTopicInventory inventory;
    private final Clock clock;
    private final EditorialExpiryConfiguration dates;

    public FeaturedItemSelector(TaxonomyService taxonomy, CategoryContentService content,
            FeaturedTopicInventory inventory, Clock clock, EditorialExpiryConfiguration dates) {
        this.taxonomy = taxonomy; this.content = content; this.inventory = inventory;
        this.clock = clock; this.dates = dates;
    }

    /** Accepts a controlled Topic label or its existing canonical route slug. */
    public Selection select(String communityId, String categoryKey, String topic) {
        if (communityId == null || communityId.isBlank()) throw new IllegalArgumentException("Explicit Community required");
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(
                dates.getCommunityZones().getOrDefault(communityId, dates.getDefaultZone()))));
        var category = taxonomy.findByKey(categoryKey).orElse(null);
        String canonical = category == null || topic == null ? null : category.subcategories().stream()
                .filter(t -> t.equalsIgnoreCase(topic) || TaxonomyService.topicSlug(t).equals(topic)).findFirst().orElse(null);
        if (canonical == null) return new Selection(communityId, categoryKey, topic, today,
                List.of(), List.of(), List.of(new Diagnostic("INVALID_SCOPE", communityId, categoryKey,
                        topic, List.of(), "Unknown Category or unrelated/unknown Topic")));

        var snapshot = inventory.current(communityId, category);
        Map<String, ContentItem> help = new TreeMap<>();
        content.helpFor(categoryKey, canonical, communityId).forEach(card -> help.putIfAbsent(card.id(), card));
        Map<String, ContentItem> projected = new TreeMap<>(help);
        content.forTopic(category, canonical, communityId).forEach(card -> projected.putIfAbsent(card.id(), card));
        content.collectionsFor(categoryKey, communityId).forEach(collection ->
                collection.items().forEach(card -> projected.putIfAbsent(card.id(), card)));
        List<Candidate> candidates = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (String limitation : snapshot.classificationLimitations()) diagnostics.add(new Diagnostic(
                "CLASSIFICATION_EVIDENCE_LIMITATION", communityId, categoryKey, canonical, List.of(), limitation));
        for (var item : snapshot.items().stream().sorted(Comparator.comparing(item -> item.id)).toList()) {
            if (!canonical.equalsIgnoreCase(item.subcategory)) continue;
            ContentItem card = projected.get(item.id);
            if (card == null) continue; // Metadata cannot create a new resident projection.
            boolean actionable = help.containsKey(item.id);
            if (item instanceof Flyer && (!actionable || !"ACTIONABLE_HELP_ACCESS".equals(item.helpAccess))) {
                diagnostics.add(new Diagnostic("FLYER_HELP_ACCESS_REQUIRED", communityId, categoryKey, canonical,
                        List.of(item.id), "Context-only Flyer remains in browse; no IM-07 role or fallback admission"));
                continue;
            }
            var facts = FeaturedRoleQualification.assess(item, today);
            Set<Role> roles = new HashSet<>(facts.roles());
            List<String> evidence = new ArrayList<>();
            List<String> limitations = new ArrayList<>(facts.limitations());
            LocalDate meaningful = null;
            if (facts.directAction() && actionable && item instanceof Flyer flyer) {
                meaningful = LocalDate.parse(flyer.eventDate); evidence.add("structured_action_event_local_0_30");
            } else if (facts.recentContext()) {
                meaningful = LocalDate.parse(item.publishDate); evidence.add("authored_law_policy_local_minus30_0");
            } else {
                roles.remove(Role.TIMELY); limitations.add("no_positive_resident_timely_evidence:" + item.id);
            }
            if (roles.contains(Role.DURABLE)) evidence.add("current_resource_explicit_standard_urgency_proxy");
            else limitations.add("no_positive_durable_evidence:" + item.id);
            String originalImage = item instanceof Flyer flyer ? flyer.image : null;
            var source = item.contentSource;
            boolean visual = item instanceof Flyer && item.contentType == ContentType.FLYER && actionable
                    && originalImage != null && !originalImage.isBlank()
                    && card.imageUrl() != null && !card.imageUrl().isBlank()
                    && source != null && source.id != null && !source.id.isBlank()
                    && safeVisualSource(source.url);
            roles.remove(Role.VISUAL);
            if (visual) { roles.add(Role.VISUAL); evidence.add("actionable_flyer_original_media_and_source"); }
            else limitations.add("no_positive_visual_media_source_evidence:" + item.id);
            var provenance = new Provenance(item.communityId, source == null ? null : source.id,
                    source == null ? null : source.name, source == null ? null : source.type,
                    source == null ? null : source.url, originalImage);
            candidates.add(new Candidate(card, provenance, actionable, roles, meaningful,
                    facts.catalogOrApprovalDate(), evidence, limitations));
        }
        return order(communityId, categoryKey, canonical, today, candidates, diagnostics);
    }

    /** IM-11: executable/relative text is not safe original source evidence for VISUAL. */
    private static boolean safeVisualSource(String value) {
        if (value == null) return false;
        try {
            var uri = java.net.URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /** Pure assignment seam for synthetic collision fixtures; production callers use select. */
    static Selection order(String community, String category, String topic, LocalDate today,
            List<Candidate> input, List<Diagnostic> initialDiagnostics) {
        Map<String, Candidate> distinct = new TreeMap<>();
        for (Candidate candidate : input) distinct.putIfAbsent(candidate.id(), candidate);
        List<Candidate> candidates = List.copyOf(distinct.values());
        List<Diagnostic> diagnostics = new ArrayList<>(initialDiagnostics);
        List<Position> positions = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int target = coverage(candidates, 0, used);
        for (int i = 0; i < Role.values().length; i++) {
            Role role = Role.values()[i];
            List<Candidate> feasible = new ArrayList<>();
            for (Candidate candidate : candidates) {
                if (!used.contains(candidate.id()) && candidate.roles().contains(role)) {
                    Set<String> excluded = new HashSet<>(used); excluded.add(candidate.id());
                    if (positions.size() + 1 + coverage(candidates, i + 1, excluded) == target) feasible.add(candidate);
                }
            }
            feasible.sort(substantive(role).thenComparing(Candidate::id));
            if (feasible.isEmpty()) {
                diagnostics.add(new Diagnostic("MISSING_ROLE", community, category, topic, List.of(),
                        role + ": no distinct candidate in the chosen maximum-coverage assignment"));
                continue;
            }
            Candidate winner = feasible.get(0);
            positions.add(new Position(PositionType.valueOf(role.name()), winner,
                    explain(winner, feasible, substantive(role), role)));
            used.add(winner.id());
            technicalDiagnostic(diagnostics, community, category, topic, role.name(), winner, feasible, substantive(role));
            if (feasible.size() < candidates.stream().filter(c -> c.roles().contains(role)).count())
                diagnostics.add(new Diagnostic("COVERAGE_COLLISION", community, category, topic,
                        List.of(winner.id()), role + ": other assignments excluded to preserve distinct-role coverage/identity"));
        }
        var fallbackComparator = Comparator.comparing(Candidate::actionable).reversed()
                .thenComparing(Candidate::catalogOrApprovalDate, Comparator.nullsLast(Comparator.reverseOrder()));
        List<Candidate> remaining = candidates.stream().filter(c -> !used.contains(c.id()))
                .sorted(fallbackComparator.thenComparing(Candidate::id)).toList();
        for (int i = 0; i < remaining.size() && positions.size() < 3; i++) {
            Candidate winner = remaining.get(i);
            var alternatives = remaining.subList(i, remaining.size());
            positions.add(new Position(PositionType.FALLBACK, winner,
                    explain(winner, alternatives, fallbackComparator, null)));
            technicalDiagnostic(diagnostics, community, category, topic, "FALLBACK", winner, alternatives, fallbackComparator);
        }
        for (Candidate candidate : candidates) if (!candidate.limitations().isEmpty()) diagnostics.add(new Diagnostic(
                "METADATA_LIMITATION", community, category, topic, List.of(candidate.id()), String.join(";", candidate.limitations())));
        return new Selection(community, category, topic, today, positions, candidates, diagnostics);
    }

    private static Comparator<Candidate> substantive(Role role) {
        Comparator<Candidate> result = Comparator.comparing(Candidate::actionable).reversed();
        // Context dates descend, direct dates ascend: relevance groups are compared first.
        if (role == Role.TIMELY) result = (a, b) -> {
            int relevance = Boolean.compare(b.actionable(), a.actionable());
            if (relevance != 0) return relevance;
            return (a.actionable() ? Comparator.<LocalDate>nullsLast(Comparator.naturalOrder())
                    : Comparator.<LocalDate>nullsLast(Comparator.reverseOrder()))
                    .compare(a.meaningfulDate(), b.meaningfulDate());
        };
        return result.thenComparing(Candidate::catalogOrApprovalDate, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static String explain(Candidate winner, List<Candidate> alternatives, Comparator<Candidate> comparator, Role role) {
        if (alternatives.size() == 1) return "SOLE_FEASIBLE_CANDIDATE";
        Candidate other = alternatives.get(1);
        if (comparator.compare(winner, other) == 0) return "STABLE_CANONICAL_ID";
        if (winner.actionable() != other.actionable()) return "ROLE_RELEVANCE_HELP_FIRST";
        if (role == Role.TIMELY && !java.util.Objects.equals(winner.meaningfulDate(), other.meaningfulDate()))
            return "MEANINGFUL_TEMPORAL_DISTINCTION";
        return "CATALOG_OR_APPROVAL_NEWNESS";
    }

    private static void technicalDiagnostic(List<Diagnostic> diagnostics, String community, String category,
            String topic, String position, Candidate winner, List<Candidate> alternatives, Comparator<Candidate> comparator) {
        List<String> tied = alternatives.stream().filter(c -> comparator.compare(winner, c) == 0).map(Candidate::id).sorted().toList();
        if (tied.size() > 1) diagnostics.add(new Diagnostic("STABLE_ITEM_ID_FALLBACK", community, category, topic,
                tied, position + ": materially equal feasible candidates; canonical String.compareTo identity order"));
    }

    /** Three-role bipartite augmenting paths: O(roles squared * inventory), no inventory truncation. */
    private static int coverage(List<Candidate> candidates, int start, Set<String> excluded) {
        Map<String, Role> matched = new TreeMap<>();
        int count = 0;
        for (int i = start; i < Role.values().length; i++)
            if (augment(Role.values()[i], candidates, excluded, matched, new HashSet<>())) count++;
        return count;
    }

    private static boolean augment(Role role, List<Candidate> candidates, Set<String> excluded,
            Map<String, Role> matched, Set<String> visited) {
        for (Candidate candidate : candidates) if (!excluded.contains(candidate.id())
                && candidate.roles().contains(role) && visited.add(candidate.id())) {
            Role previous = matched.get(candidate.id());
            if (previous == null || augment(previous, candidates, excluded, matched, visited)) {
                matched.put(candidate.id(), role); return true;
            }
        }
        return false;
    }
}
