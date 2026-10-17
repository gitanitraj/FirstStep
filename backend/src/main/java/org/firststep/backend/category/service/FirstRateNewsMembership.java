package org.firststep.backend.category.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Exact editorial admissions, independent of lifecycle, taxonomy and Article publication. */
@Service
public class FirstRateNewsMembership {
    private record Admission(String recordKind, String recordId, String communityId, String categoryKey) {}
    private final List<Admission> admissions;

    public FirstRateNewsMembership(@Value("${app.data.dir:app/data}") String dataDir) {
        Path file = Path.of(dataDir, "first-rate-news-membership.json");
        try {
            admissions = Files.exists(file)
                    ? List.copyOf(new ObjectMapper().readValue(file.toFile(), new TypeReference<List<Admission>>() {}))
                    : List.of();
            for (Admission entry : admissions) {
                if (!("FAQ".equals(entry.recordKind()) || "ExpertAnswer".equals(entry.recordKind()))
                        || entry.recordId() == null || entry.recordId().isBlank()
                        || entry.communityId() == null || entry.communityId().isBlank()
                        || entry.categoryKey() == null || entry.categoryKey().isBlank()) {
                    throw new IllegalStateException("Invalid First Rate News admission");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read First Rate News admissions", e);
        }
    }

    /** Admission never authorizes a new identity, ownership or classification. */
    public boolean admits(CivicContent item, String categoryKey) {
        if (item.contentType != ContentType.EXPERT) return false;
        String kind = item instanceof FAQ ? "FAQ" : item instanceof ExpertAnswer ? "ExpertAnswer" : null;
        return admissions.stream().anyMatch(entry -> entry.recordKind().equals(kind)
                && entry.recordId().equals(item.id) && entry.communityId().equals(item.communityId)
                && entry.categoryKey().equals(categoryKey));
    }
}
