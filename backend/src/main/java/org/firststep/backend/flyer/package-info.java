/**
 * Flyer vertical slice — static flyers.json + static images MVP.
 *
 * A Flyer is CivicContent plus organization, eventDate, location, and a
 * bare image filename. Backed by JsonFlyerRepository reading a static
 * app/data/flyers.json, mirroring ResourceService's loading mechanism —
 * see model/Flyer.java and repository/JsonFlyerRepository.java.
 *
 * Today's flyers.json data is manually authored metadata for the 7 real
 * flyer images at backend/src/main/resources/static/images/seasonal/
 * (still served today via resource/controller/ResourceController#
 * getSeasonalImages for the existing carousel, unrelated to this slice) —
 * not extracted via OCR/AI, which remains a future milestone. See
 * "Milestone 2" in docs/architecture/03-application-architecture.md and
 * references/decisions.md's Decision 011.
 */
package org.firststep.backend.flyer;
