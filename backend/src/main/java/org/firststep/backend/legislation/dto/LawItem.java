package org.firststep.backend.legislation.dto;

/**
 * A recently signed Delaware bill for the homepage "New Delaware Laws" rotator
 * (and, later, the Important Notices page's legislation column). Display-ready:
 * the frontend just renders it.
 */
public record LawItem(String title, String url, String date) {
}
