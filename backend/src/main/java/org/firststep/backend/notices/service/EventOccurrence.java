package org.firststep.backend.notices.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Collectors;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.springframework.stereotype.Service;

/** P5-R5b: occurrence eligibility only; never changes editorial lifecycle or stored content. */
@Service
public class EventOccurrence {
    public enum State { CURRENT_OR_UPCOMING, CONCLUDED, UNKNOWN }
    public record Assessment(State state, String diagnostic) {}
    private final Clock clock;
    private final ZoneId defaultZone;
    private final Map<String, ZoneId> zones;

    public EventOccurrence(Clock clock, EditorialExpiryConfiguration configuration) {
        this.clock = clock;
        defaultZone = ZoneId.of(configuration.getDefaultZone());
        zones = configuration.getCommunityZones().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> ZoneId.of(e.getValue())));
    }

    private ZoneId zone(CivicContent item) {
        return item.communityId == null ? defaultZone : zones.getOrDefault(item.communityId, defaultZone);
    }
    private static String start(CivicContent item) {
        return item instanceof Flyer f ? f.startsAt : item instanceof NewsItem n ? n.startsAt : null;
    }
    private static String end(CivicContent item) {
        return item instanceof Flyer f ? f.endsAt : item instanceof NewsItem n ? n.endsAt : null;
    }
    private static Instant timestamp(String value) { return OffsetDateTime.parse(value).toInstant(); }

    public Assessment assess(CivicContent item) {
        Instant start = null, end = null;
        LocalDate date = null;
        try { if (start(item) != null) start = timestamp(start(item)); }
        catch (DateTimeParseException e) { return new Assessment(State.UNKNOWN, "invalid_starts_at"); }
        try { if (end(item) != null) end = timestamp(end(item)); }
        catch (DateTimeParseException e) { return new Assessment(State.UNKNOWN, "invalid_ends_at"); }
        try { if (item instanceof Flyer f && f.eventDate != null) date = LocalDate.parse(f.eventDate); }
        catch (DateTimeParseException e) { return new Assessment(State.UNKNOWN, "invalid_event_date"); }
        if (start != null && end != null && end.isBefore(start))
            return new Assessment(State.UNKNOWN, "end_before_start");
        if (end != null) return new Assessment(clock.instant().isAfter(end) ? State.CONCLUDED : State.CURRENT_OR_UPCOMING, "explicit_end");
        LocalDate localDate = start != null ? start.atZone(zone(item)).toLocalDate() : date;
        if (localDate == null) return new Assessment(State.UNKNOWN, "missing_occurrence");
        return new Assessment(LocalDate.now(clock.withZone(zone(item))).isAfter(localDate)
                ? State.CONCLUDED : State.CURRENT_OR_UPCOMING, start != null ? "start_local_date" : "event_local_date");
    }

    /** Only called for assessed eligible Events; order by instant, not lexical offset text. */
    public Instant orderTime(CivicContent item) {
        if (start(item) != null) return timestamp(start(item));
        if (item instanceof Flyer f && f.eventDate != null) return LocalDate.parse(f.eventDate).atStartOfDay(zone(item)).toInstant();
        return timestamp(end(item));
    }

    /** Verbatim occurrence facts; publication is never an event-date fallback. */
    public String displayDate(CivicContent item) {
        String from = start(item) != null ? start(item) : item instanceof Flyer f ? f.eventDate : null;
        if (from == null) return null; // An end alone does not establish a start label.
        return end(item) == null ? from : from + " – " + end(item);
    }
}
