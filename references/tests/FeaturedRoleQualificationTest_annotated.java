/* IM-06 annotated reference.
 * What: scoped current inventory, authored classification proof, approved role facts and lexicographic Topic selection.
 * Why: retain identity and all seven P5 stages without accepting keyword inference or counting one ID in two roles.
 * Interaction: current services and helpFor supply eligibility; metadata proof establishes stored classification;
 * qualification and maximum matching feed ordered internal evidence and nonblocking diagnostics.
 * Alternatives rejected: raw-record eligibility, inferred Topic relevance, persisted scores/flags,
 * retrieval as catalog newness, final item selection and public featured UI owned by IM-07/08.
 */
package org.firststep.backend.category.selection;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.firststep.backend.category.selection.FeaturedRoleQualification.Role;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeaturedRoleQualificationTest {
    private final LocalDate today = LocalDate.of(2026, 10, 16);

    @Test void durableRequiresExplicitStandardServiceProxyNotSimplyUndatedOrEmergency() {
        Resource r = new Resource();
        assertFalse(FeaturedRoleQualification.assess(r, today).roles().contains(Role.DURABLE));
        r.urgency = "emergency";
        assertFalse(FeaturedRoleQualification.assess(r, today).roles().contains(Role.DURABLE));
        r.urgency = "Standard";
        assertFalse(FeaturedRoleQualification.assess(r, today).roles().contains(Role.DURABLE));
        r.urgency = "standard";
        assertTrue(FeaturedRoleQualification.assess(r, today).roles().contains(Role.DURABLE));
    }

    @Test void directWindowIncludesTodayAndDay30ButNotYesterdayOrDay31() {
        Flyer f = new Flyer(); f.helpAccess = "ACTIONABLE_HELP_ACCESS";
        for (int offset : List.of(-1, 0, 30, 31)) {
            f.eventDate = today.plusDays(offset).toString();
            assertEquals(offset >= 0 && offset <= 30, FeaturedRoleQualification.assess(f, today).directAction());
        }
    }

    @Test void directNeedsExactSignalAndStructuredDateNotTitlePublishOrExpiry() {
        Flyer f = new Flyer(); f.title = "Apply now deadline 2026-10-17";
        f.publishDate = "2026-10-17"; f.expirationDate = "2026-10-17";
        f.helpAccess = "ACTIONABLE_HELP_ACCESS";
        for (String badDate : new String[]{null, "", "2026-02-30", "tomorrow"}) {
            f.eventDate = badDate;
            assertFalse(FeaturedRoleQualification.assess(f, today).directAction());
        }
        f.eventDate = today.toString(); f.helpAccess = "actionable_help_access";
        assertFalse(FeaturedRoleQualification.assess(f, today).directAction());
    }

    @Test void contextualWindowIncludesTodayAndDayMinus30OnlyForLawOrPolicyType() {
        NewsItem n = new NewsItem(); n.contentType = ContentType.LAW;
        for (int offset : List.of(-31, -30, 0, 1)) {
            n.publishDate = today.plusDays(offset).toString();
            assertEquals(offset >= -30 && offset <= 0, FeaturedRoleQualification.assess(n, today).recentContext());
        }
        n.publishDate = today.toString(); n.contentType = ContentType.NEWS;
        assertFalse(FeaturedRoleQualification.assess(n, today).recentContext());
        n.type = "policy-update";
        assertTrue(FeaturedRoleQualification.assess(n, today).recentContext());
        n.publishDate = "invalid";
        assertFalse(FeaturedRoleQualification.assess(n, today).recentContext());
    }

    @Test void aSingleMultiRoleFlyerCannotFillTwoSlotsButIndependentTimelyAllowsBoth() {
        assertEquals(1, FeaturedRoleQualification.maximumDistinctCoverage(Map.of("flyer", Set.of(Role.TIMELY, Role.VISUAL))));
        assertEquals(2, FeaturedRoleQualification.maximumDistinctCoverage(Map.of("flyer", Set.of(Role.TIMELY, Role.VISUAL),
                "other", Set.of(Role.TIMELY))));
        assertEquals(3, FeaturedRoleQualification.maximumDistinctCoverage(Map.of("flyer", Set.of(Role.TIMELY, Role.VISUAL),
                "other", Set.of(Role.TIMELY), "service", Set.of(Role.DURABLE))));
    }

    @Test void currentVisualDoesNotAutomaticallyEstablishActionableOrDurable() {
        Flyer f = new Flyer(); f.image = "source.png"; f.eventDate = today.toString();
        var facts = FeaturedRoleQualification.assess(f, today);
        assertEquals(Set.of(Role.VISUAL), facts.roles());
        assertFalse(facts.directAction());
        f.image = "";
        assertTrue(FeaturedRoleQualification.assess(f, today).roles().isEmpty());
    }

    @Test void loaderDerivedSourceDatesAndEditorialExpiryNeverBecomeCatalogNewness() {
        Resource r = new Resource(); r.createdDate = "2026-10-15"; r.updatedDate = "2026-10-16";
        r.expirationDate = "2026-10-17";
        assertNull(FeaturedRoleQualification.assess(r, today).catalogOrApprovalDate());
        assertFalse(FeaturedRoleQualification.assess(r, today).roles().contains(Role.TIMELY));
        NewsItem n = new NewsItem(); n.createdDate = "2026-10-15"; n.publishDate = "2026-10-15";
        assertNull(FeaturedRoleQualification.assess(n, today).catalogOrApprovalDate());
    }

    @Test void actualFlyerCreationAndApprovedReviewSupportNewnessWithoutFutureInvention() {
        Flyer f = new Flyer(); f.createdDate = "2026-10-15"; f.eventDate = "2026-10-16";
        assertEquals(today.minusDays(1), FeaturedRoleQualification.assess(f, today).catalogOrApprovalDate());
        f.createdDate = "2026-10-17";
        assertNull(FeaturedRoleQualification.assess(f, today).catalogOrApprovalDate());
        Article a = new Article(); a.createdDate = "2026-10-01"; a.editorialReview = new EditorialReview();
        a.editorialReview.status = "approved"; a.editorialReview.reviewedDate = "2026-10-15";
        assertEquals(today.minusDays(1), FeaturedRoleQualification.assess(a, today).catalogOrApprovalDate());
        a.editorialReview.status = "draft";
        assertEquals(today.minusDays(15), FeaturedRoleQualification.assess(a, today).catalogOrApprovalDate());
    }
}
