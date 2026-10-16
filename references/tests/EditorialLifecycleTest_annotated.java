/*
 * IM-01 annotated test reference — full mirror of backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialLifecycleTest.java
 * WHAT: Lifecycle/projection regression evidence.
 * WHY: prove behavior using deterministic time and preserve unrelated test expectations.
 * INTERACTION: see individual test names and fixtures; only lifecycle dependencies were
 * added to existing service construction. No assertion was weakened to hide expiry failures.
 * ALTERNATIVE: machine-clock fixtures would make future runs depend on the date of execution.
 */
package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.firststep.backend.shared.lifecycle.EditorialLifecycle.State.*;
import static org.junit.jupiter.api.Assertions.*;

class EditorialLifecycleTest {
    private EditorialLifecycle at(String instant, EditorialExpiryConfiguration configuration) {
        return new EditorialLifecycle(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), configuration);
    }

    private Resource expiring(String date) {
        Resource item = new Resource();
        item.communityId = "wilmington-de";
        item.expirationDate = date;
        return item;
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-15T03:59:59Z, ACTIVE",
        "2026-10-15T04:00:00Z, APPROACHING_EXPIRY",
        "2026-11-14T05:00:00Z, APPROACHING_EXPIRY",
        "2026-11-15T04:59:59Z, APPROACHING_EXPIRY",
        "2026-11-15T05:00:00Z, EXPIRED"
    })
    void shouldUseThirtyCalendarDaysAndInclusiveNewYorkExpiry(String instant, EditorialLifecycle.State state) {
        var result = at(instant, new EditorialExpiryConfiguration()).assess(expiring("2026-11-14"));
        assertEquals(state, result.state());
        assertEquals(state != EXPIRED, result.editorialExpiryEligible());
        assertEquals(state != ACTIVE, result.editorialAttentionRequired());
    }

    @ParameterizedTest
    @CsvSource({
        "2026-03-08T06:59:59Z, APPROACHING_EXPIRY",
        "2026-03-08T07:00:00Z, APPROACHING_EXPIRY",
        "2026-03-09T03:59:59Z, APPROACHING_EXPIRY",
        "2026-03-09T04:00:00Z, EXPIRED"
    })
    void shouldPreserveLocalExpiryAcrossSpringDaylightSaving(String instant, EditorialLifecycle.State state) {
        assertEquals(state, at(instant, new EditorialExpiryConfiguration()).assess(expiring("2026-03-08")).state());
    }

    @ParameterizedTest
    @CsvSource({
        "2026-11-01T05:30:00Z, APPROACHING_EXPIRY",
        "2026-11-01T06:30:00Z, APPROACHING_EXPIRY",
        "2026-11-02T04:59:59Z, APPROACHING_EXPIRY",
        "2026-11-02T05:00:00Z, EXPIRED"
    })
    void shouldPreserveLocalExpiryAcrossAutumnDaylightSaving(String instant, EditorialLifecycle.State state) {
        assertEquals(state, at(instant, new EditorialExpiryConfiguration()).assess(expiring("2026-11-01")).state());
    }

    @Test
    void shouldResolveEachCommunityZoneWithoutChangingTheAlgorithm() {
        var configuration = new EditorialExpiryConfiguration();
        configuration.setCommunityZones(Map.of("test-community", "Asia/Tokyo"));
        var lifecycle = at("2026-11-15T02:00:00Z", configuration);
        Resource item = expiring("2026-11-14");
        assertEquals(APPROACHING_EXPIRY, lifecycle.assess(item).state());
        item.communityId = "test-community";
        assertEquals(EXPIRED, lifecycle.assess(item).state());
    }

    @Test
    void shouldUseConfiguredDeploymentZoneForUnlistedCommunities() {
        var configuration = new EditorialExpiryConfiguration();
        configuration.setDefaultZone("Pacific/Honolulu");
        Resource item = expiring("2026-11-14");
        item.communityId = "unlisted";
        assertEquals(APPROACHING_EXPIRY, at("2026-11-15T09:59:59Z", configuration).assess(item).state());
        assertEquals(EXPIRED, at("2026-11-15T10:00:00Z", configuration).assess(item).state());
    }

    @Test
    void shouldHonorConfiguredWindow() {
        var configuration = new EditorialExpiryConfiguration();
        configuration.setApproachingDays(7);
        assertEquals(ACTIVE, at("2026-11-06T17:00:00Z", configuration).assess(expiring("2026-11-14")).state());
        assertEquals(APPROACHING_EXPIRY, at("2026-11-07T17:00:00Z", configuration).assess(expiring("2026-11-14")).state());
    }

    static Stream<CivicContent> contentClasses() {
        NewsItem law = new NewsItem();
        law.contentType = ContentType.LAW;
        return Stream.of(new Resource(), new NewsItem(), new Flyer(), law, new FAQ(), new ExpertAnswer(), new Article());
    }

    @ParameterizedTest
    @MethodSource("contentClasses")
    void shouldAllowMissingExpiryForEveryContentClassWithoutInventingDates(CivicContent item) {
        item.verified = false;
        item.publishDate = "2000-01-01";
        item.createdDate = "2000-01-01";
        item.updatedDate = "2000-01-01";
        if (item instanceof Flyer flyer) flyer.eventDate = "2000-01-01";
        if (item instanceof ExpertAnswer answer) answer.sessionDate = "2000-01-01";
        var result = LifecycleFixture.fixed().assess(item);
        assertEquals(ACTIVE, result.state());
        assertTrue(result.editorialExpiryEligible());
        assertFalse(result.editorialAttentionRequired());
        assertNull(item.expirationDate);
        assertFalse(item.verified);
    }

    @ParameterizedTest
    @MethodSource("contentClasses")
    void shouldKeepStatusIndependentOfExpiryState(CivicContent item) {
        for (String status : List.of("inactive", "withdrawn", " INACTIVE ")) {
            item.status = status;
            assertEquals(ACTIVE, LifecycleFixture.fixed().assess(item).state());
            assertTrue(LifecycleFixture.fixed().assess(item).editorialExpiryEligible());
            assertFalse(LifecycleFixture.fixed().isCurrent(item));
        }
    }

    @Test
    void shouldNotSilentlyTreatMalformedEditorialExpiryAsMissing() {
        var exception = assertThrows(java.time.format.DateTimeParseException.class,
                () -> LifecycleFixture.fixed().assess(expiring("not-a-date")));
        assertEquals("not-a-date", exception.getParsedString());
    }
}
