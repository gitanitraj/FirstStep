package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.OptionalLong;
import java.time.temporal.ChronoUnit;

import org.firststep.backend.shared.model.CivicContent;
import org.springframework.stereotype.Service;

/**
 * IM-01-PC-01: editorial expiry only; occurrence and publication are independent.
 * Date-only expiration is inclusive in the owning Community's configured zone.
 * No event, session, publication, retrieved or audit date participates here.
 */
@Service
public class EditorialLifecycle {
    public enum State { ACTIVE, APPROACHING_EXPIRY, EXPIRED }

    /** Reusable downstream contract; no lifecycle or eligibility value is persisted. */
    public record Assessment(State state, boolean editorialExpiryEligible, boolean editorialAttentionRequired) {}

    private final Clock clock;
    private final int approachingDays;
    private final ZoneId defaultZone;
    private final Map<String, ZoneId> communityZones;

    public EditorialLifecycle(Clock clock, EditorialExpiryConfiguration configuration) {
        this.clock = clock;
        this.approachingDays = configuration.getApproachingDays();
        if (approachingDays < 0) {
            throw new IllegalArgumentException("Editorial approaching-days must not be negative");
        }
        this.defaultZone = ZoneId.of(configuration.getDefaultZone());
        this.communityZones = configuration.getCommunityZones().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> ZoneId.of(entry.getValue())));
    }

    public Assessment assess(CivicContent item) {
        if (item.expirationDate == null || item.expirationDate.isBlank()) {
            return new Assessment(State.ACTIVE, true, false);
        }
        // Local calendar arithmetic, rather than 30 * 24 hours, preserves DST boundaries.
        LocalDate expiry = LocalDate.parse(item.expirationDate);
        LocalDate today = todayFor(item.communityId);
        State state = today.isAfter(expiry) ? State.EXPIRED
                : !today.isBefore(expiry.minusDays(approachingDays)) ? State.APPROACHING_EXPIRY
                : State.ACTIVE;
        return new Assessment(state, state != State.EXPIRED, state != State.ACTIVE);
    }

    /** Signed Community-calendar distance: zero on the inclusive expiry date; negative after it. */
    public OptionalLong daysUntilExpiry(CivicContent item) {
        if (item.expirationDate == null || item.expirationDate.isBlank()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(ChronoUnit.DAYS.between(todayFor(item.communityId),
                LocalDate.parse(item.expirationDate)));
    }

    private LocalDate todayFor(String communityId) {
        ZoneId zone = communityId == null ? defaultZone
                : communityZones.getOrDefault(communityId, defaultZone);
        return LocalDate.now(clock.withZone(zone));
    }

    /** Status is independent of editorial date state, including for historical links. */
    public boolean isPublicStatus(CivicContent item) {
        return item.status == null || !("inactive".equalsIgnoreCase(item.status.trim())
                || "withdrawn".equalsIgnoreCase(item.status.trim()));
    }

    /** Current inventory only. Callers must retain classification and article approval gates. */
    public boolean isCurrent(CivicContent item) {
        return isPublicStatus(item) && assess(item).editorialExpiryEligible();
    }
}
