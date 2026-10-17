package org.firststep.backend.category.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FirstRateNewsMembershipTest {
    @TempDir Path directory;
    @Test void t13ExactApprovalRequiresIdentityKindCommunityAndCategory() {
        var membership = new FirstRateNewsMembership("../app/data");
        ExpertAnswer answer = new ExpertAnswer(); answer.id = "EA-001"; answer.communityId = "wilmington-de";
        assertTrue(membership.admits(answer, "housing"));
        assertFalse(membership.admits(answer, "legal"));
        answer.communityId = "bear-de"; assertFalse(membership.admits(answer, "housing"));
        answer.communityId = "wilmington-de"; answer.contentType = ContentType.NEWS;
        assertFalse(membership.admits(answer, "housing"));
        FAQ wrongKind = new FAQ(); wrongKind.id = "EA-001"; wrongKind.communityId = "wilmington-de";
        assertFalse(membership.admits(wrongKind, "housing"));
    }
    @Test void t13DerivedFaqRequiresItsOwnApproval() {
        var membership = new FirstRateNewsMembership("../app/data");
        FAQ faq = new FAQ(); faq.id = "FAQ-002"; faq.communityId = "wilmington-de";
        assertTrue(membership.admits(faq, "housing"));
        faq.id = "FAQ-001"; assertFalse(membership.admits(faq, "housing"));
    }
    @Test void absentManifestFailsClosed() {
        var membership = new FirstRateNewsMembership(directory.toString());
        ExpertAnswer answer = new ExpertAnswer(); answer.id = "EA-001"; answer.communityId = "wilmington-de";
        assertFalse(membership.admits(answer, "housing"));
    }
    @Test void malformedManifestCannotSilentlyAdmitOrIgnoreDecisions() throws Exception {
        Files.writeString(directory.resolve("first-rate-news-membership.json"), "[invalid");
        var error = assertThrows(IllegalStateException.class, () -> new FirstRateNewsMembership(directory.toString()));
        assertEquals("Cannot read First Rate News admissions", error.getMessage());
    }
    @Test void unsupportedRecordKindCannotBecomeAnAdmission() throws Exception {
        Files.writeString(directory.resolve("first-rate-news-membership.json"),
                "[{\"recordKind\":\"Article\",\"recordId\":\"OR-001\",\"communityId\":\"wilmington-de\",\"categoryKey\":\"housing\"}]");
        var error = assertThrows(IllegalStateException.class, () -> new FirstRateNewsMembership(directory.toString()));
        assertEquals("Invalid First Rate News admission", error.getMessage());
    }
}
