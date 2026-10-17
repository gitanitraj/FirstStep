package org.firststep.backend.category.selection;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;

/** IM-06 approved existing-field proxies only; not IM-07 item selection or role fallback. */
public final class FeaturedRoleQualification {
    private FeaturedRoleQualification() {}

    public enum Role { TIMELY, DURABLE, VISUAL }

    public record Facts(Set<Role> roles, boolean directAction, boolean recentContext,
                        LocalDate catalogOrApprovalDate, List<String> limitations) {}

    /** Caller supplies an already current, explicitly Category/Topic-scoped item. */
    public static Facts assess(CivicContent item, LocalDate today) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        List<String> limitations = new ArrayList<>();
        boolean direct = false;
        boolean context = false;
        if (item instanceof Resource resource) {
            if ("standard".equals(resource.urgency)) roles.add(Role.DURABLE);
            else limitations.add("no_explicit_standard_urgency:" + item.id);
            // The Resource loader copies source retrieved into createdDate: not catalog insertion.
            limitations.add("no_authentic_catalog_date:" + item.id);
        }
        if (item instanceof Flyer flyer && item.contentType == ContentType.FLYER) {
            if (flyer.image != null && !flyer.image.isBlank()) roles.add(Role.VISUAL);
            LocalDate event = date(flyer.eventDate);
            direct = "ACTIONABLE_HELP_ACCESS".equals(flyer.helpAccess)
                    && event != null && !event.isBefore(today) && !event.isAfter(today.plusDays(30));
            if (direct) roles.add(Role.TIMELY);
            if (event == null) limitations.add("missing_or_invalid_event_date:" + item.id);
        }
        if (item instanceof NewsItem news) {
            LocalDate published = date(item.publishDate);
            context = (item.contentType == ContentType.LAW || "policy-update".equals(news.type))
                    && published != null && !published.isAfter(today) && !published.isBefore(today.minusDays(30));
            if (context) roles.add(Role.TIMELY);
            if (published == null) limitations.add("missing_or_invalid_publication_date:" + item.id);
            // Curated News and RSS both copy publication into createdDate; never newness evidence.
            limitations.add("no_authentic_catalog_date:" + item.id);
        }
        LocalDate newness = null;
        if (item instanceof Flyer || item instanceof Article || item instanceof FAQ || item instanceof ExpertAnswer) {
            newness = notFuture(date(item.createdDate), today);
            if (item instanceof Article article && article.isPublishable()) {
                LocalDate approved = notFuture(date(article.editorialReview.reviewedDate), today);
                if (approved != null && (newness == null || approved.isAfter(newness))) newness = approved;
            }
            if (newness == null) limitations.add("no_authentic_catalog_or_approval_date:" + item.id);
        }
        return new Facts(java.util.Collections.unmodifiableSet(EnumSet.copyOf(roles)), direct, context, newness, List.copyOf(limitations));
    }

    private static LocalDate notFuture(LocalDate value, LocalDate today) {
        return value == null || value.isAfter(today) ? null : value;
    }

    private static LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ignored) { return null; }
    }

    /** Maximum matching cardinality, not role count: an ID cannot occupy two simultaneous roles. */
    public static int maximumDistinctCoverage(Map<String, Set<Role>> candidates) {
        return match(candidates, 0, new HashSet<>());
    }

    private static int match(Map<String, Set<Role>> candidates, int role, Set<String> used) {
        if (role == Role.values().length) return 0;
        int best = match(candidates, role + 1, used);
        for (var entry : candidates.entrySet()) {
            if (entry.getValue().contains(Role.values()[role]) && used.add(entry.getKey())) {
                best = Math.max(best, 1 + match(candidates, role + 1, used));
                used.remove(entry.getKey());
            }
        }
        return best;
    }
}
