/**
 * Expert vertical slice — two CivicContent subtypes: ExpertAnswer
 * (individual Q&A from a named expert/session, first real adopter of
 * shared/model/Contact) and FAQ (simpler curated Q&A, optionally linked
 * back to a source ExpertAnswer). Backed by static app/data/
 * expert-answers.json and app/data/faq.json, mirroring how the Flyer
 * slice loads its data. GET /api/expert-answers and GET /api/faqs (plus
 * /{id} variants). Not yet wired into Search or the category taxonomy —
 * see references/decisions.md's Decision 015.
 */
package org.firststep.backend.expert;
