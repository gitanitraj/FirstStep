package org.firststep.backend.flyer.dto;

/**
 * A display-ready flyer card for the homepage Community Information carousel.
 *
 * `imageUrl` is fully resolved server-side (the /images/seasonal/ path convention
 * + URL-encoding of the filename) so the frontend just renders it — the browser
 * never has to know where flyer images live or how to encode their names.
 */
public record FlyerCard(String imageUrl, String title, String organization, String eventDate) {
}
