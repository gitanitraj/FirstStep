package org.firststep.backend.search.dto;

import org.firststep.backend.shared.model.CivicContent;

public record SearchResult(String type, int score, CivicContent content) {
}
