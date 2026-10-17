package org.firststep.backend.notices.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class EventOccurrenceTest {
    static EventOccurrence at(String time) {
        var configuration = new EditorialExpiryConfiguration();
        configuration.setCommunityZones(Map.of("east", "America/New_York", "west", "America/Los_Angeles"));
        return new EventOccurrence(Clock.fixed(Instant.parse(time), ZoneOffset.ofHours(9)), configuration);
    }
    @Test void t03ExplicitEndInclusiveToNanosecondAndOffsetsAreEquivalent() {
        Flyer f = new Flyer(); f.endsAt = "2026-10-17T16:00:00-04:00";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-10-17T20:00:00Z").assess(f).state());
        assertEquals(EventOccurrence.State.CONCLUDED, at("2026-10-17T20:00:00.000000001Z").assess(f).state());
        f.endsAt = "2026-10-17T20:00:00Z";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-10-17T20:00:00Z").assess(f).state());
    }
    @ParameterizedTest
    @CsvSource({
        "2026-03-08,2026-03-09T03:59:59.999999999Z,CURRENT_OR_UPCOMING",
        "2026-03-08,2026-03-09T04:00:00Z,CONCLUDED",
        "2026-11-01,2026-11-02T04:59:59.999999999Z,CURRENT_OR_UPCOMING",
        "2026-11-01,2026-11-02T05:00:00Z,CONCLUDED",
        "2026-10-17,2026-10-18T03:59:59.999999999Z,CURRENT_OR_UPCOMING",
        "2026-10-17,2026-10-18T04:00:00Z,CONCLUDED"})
    void t04t07DateOnlyLocalMidnightAndSpringFallDst(String date, String now, EventOccurrence.State state) {
        Flyer f = new Flyer(); f.communityId = "east"; f.eventDate = date;
        assertEquals(state, at(now).assess(f).state());
    }
    @Test void t05StartOnlyUsesOwningCommunityDateNotOffsetDateOrInventedDuration() {
        Flyer f = new Flyer(); f.communityId = "west"; f.startsAt = "2026-10-18T01:00:00Z";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-10-18T06:59:59.999999999Z").assess(f).state());
        assertEquals(EventOccurrence.State.CONCLUDED, at("2026-10-18T07:00:00Z").assess(f).state());
        assertEquals("2026-10-18T01:00:00Z", at("2026-10-18T05:00:00Z").displayDate(f));
    }
    @Test void t07IndependentCommunityZonesAndClockZoneCannotChangeOwnership() {
        Flyer f = new Flyer(); f.eventDate = "2026-10-17"; f.communityId = "east";
        assertEquals(EventOccurrence.State.CONCLUDED, at("2026-10-18T05:00:00Z").assess(f).state());
        f.communityId = "west";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-10-18T05:00:00Z").assess(f).state());
    }
    @Test void t06FutureOccurrenceRemainsUpcoming() {
        Flyer f = new Flyer(); f.startsAt = "2027-01-01T10:00:00-05:00";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-10-17T20:00:00Z").assess(f).state());
    }
    @ParameterizedTest
    @CsvSource({"2026-02-30,invalid_event_date", "Saturday,invalid_event_date", "'',invalid_event_date"})
    void t08MalformedDateNeverNormalizesOrUsesProse(String date, String reason) {
        Flyer f = new Flyer(); f.eventDate = date; f.title = "Tomorrow at 4"; f.publishDate = "2027-01-01";
        var result = at("2026-10-17T20:00:00Z").assess(f);
        assertEquals(EventOccurrence.State.UNKNOWN, result.state()); assertEquals(reason, result.diagnostic());
    }
    @Test void t08MissingDateCannotUsePublicationAuditExpiryOrMedia() {
        NewsItem n = new NewsItem(); n.publishDate = "2027-01-01"; n.updatedDate = "2027-01-01";
        n.expirationDate = "2027-12-31"; n.title = "Future event";
        assertEquals(new EventOccurrence.Assessment(EventOccurrence.State.UNKNOWN,"missing_occurrence"), at("2026-10-17T20:00:00Z").assess(n));
    }
    @Test void t08DateOnlyEndAndAmbiguousLocalTimestampAreDiagnosedWithoutInventingTime() {
        Flyer f = new Flyer(); f.endsAt = "2026-10-17";
        assertEquals("invalid_ends_at", at("2026-10-17T20:00:00Z").assess(f).diagnostic());
        f.endsAt = null; f.startsAt = "2026-11-01T01:30:00";
        assertEquals("invalid_starts_at", at("2026-10-17T20:00:00Z").assess(f).diagnostic());
        f.startsAt = "2026-03-08T02:30:00";
        assertEquals("invalid_starts_at", at("2026-10-17T20:00:00Z").assess(f).diagnostic());
    }
    @Test void t08EndBeforeStartIsAnExactDiagnostic() {
        Flyer f = new Flyer(); f.startsAt = "2026-10-17T20:00:00Z"; f.endsAt = "2026-10-17T19:00:00Z";
        assertEquals(new EventOccurrence.Assessment(EventOccurrence.State.UNKNOWN,"end_before_start"), at("2026-10-17T18:00:00Z").assess(f));
    }
    @Test void t07ExplicitFallFoldOffsetsAndSpringTransitionRemainInstants() {
        Flyer f = new Flyer(); f.startsAt = "2026-11-01T01:30:00-04:00"; f.endsAt = "2026-11-01T01:30:00-05:00";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-11-01T06:00:00Z").assess(f).state());
        assertEquals(EventOccurrence.State.CONCLUDED, at("2026-11-01T06:30:00.000000001Z").assess(f).state());
        f.startsAt = "2026-03-08T01:30:00-05:00"; f.endsAt = "2026-03-08T03:30:00-04:00";
        assertEquals(EventOccurrence.State.CURRENT_OR_UPCOMING, at("2026-03-08T07:30:00Z").assess(f).state());
        assertEquals(EventOccurrence.State.CONCLUDED, at("2026-03-08T07:30:00.000000001Z").assess(f).state());
    }
}
