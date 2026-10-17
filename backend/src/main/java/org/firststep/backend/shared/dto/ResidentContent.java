package org.firststep.backend.shared.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonValue;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Contact;
import org.firststep.backend.shared.model.Phone;
import org.firststep.backend.shared.model.Website;
import java.util.List;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.dto.NewsSummary;

/** Explicit resident allowlist for the legacy Resource/Expert APIs, never a raw model serializer. */
public record ResidentContent(@JsonValue Map<String, Object> fields) {
    public ResidentContent { fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields)); }
    public record Source(String id, String name, String type, String url) {}
    public static Source source(ContentSource s) {
        return s == null ? null : new Source(s.id, s.name, s.type, safeUrl(s.url));
    }
    public static String safeUrl(String value) {
        if (value == null) return null;
        try {
            var uri = java.net.URI.create(value);
            return uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme())) ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
    public record Place(String label, String address, String city, String state, String zip, Boolean confidential) {
        static Place from(Location p) {
            boolean hidden = Boolean.TRUE.equals(p.confidential);
            return new Place(p.label, hidden ? null : p.address, p.city, p.state, hidden ? null : p.zip, p.confidential);
        }
    }
    public record PublicPhone(String number, String label) {}
    public record Site(String url, String label) {}
    public record PublicContact(List<PublicPhone> phones, List<Site> websites, String email) {}
    private static List<PublicPhone> phones(List<Phone> values) {
        return values == null ? null : values.stream().map(p -> new PublicPhone(p.number, p.label)).toList();
    }
    private static List<Site> sites(List<Website> values) {
        return values == null ? null : values.stream().map(w -> new Site(safeUrl(w.url), w.label)).toList();
    }
    private static PublicContact contact(Contact c) {
        return c == null ? null : new PublicContact(phones(c.phones), sites(c.websites), c.email);
    }
    public static Object from(CivicContent c) {
        if (c instanceof NewsItem n) return NewsSummary.from(n);
        var m = new LinkedHashMap<String, Object>();
        m.put("id", c.id); m.put("title", c.title); m.put("summary", c.summary);
        m.put("contentType", c.contentType); m.put("communityId", c.communityId);
        m.put("category_tags", c.categoryTags); m.put("subcategory", c.subcategory);
        m.put("tags", c.tags); m.put("publishDate", c.publishDate);
        m.put("contentSource", source(c.contentSource));
        if (c instanceof Resource r) {
            m.put("category", r.category); m.put("organization", r.organization);
            m.put("organization_id", r.organizationId); m.put("parent_organization", r.parentOrganization);
            m.put("description", r.description); m.put("population", r.population);
            m.put("eligibility", r.eligibility); m.put("eligibility_age_min", r.eligibilityAgeMin);
            m.put("eligibility_age_max", r.eligibilityAgeMax); m.put("eligibility_gender", r.eligibilityGender);
            m.put("locations", r.locations == null ? null : r.locations.stream().map(Place::from).toList());
            m.put("phones", phones(r.phones)); m.put("websites", sites(r.websites));
            m.put("county", r.county); m.put("access_mode", r.accessMode);
            m.put("cost", r.cost); m.put("urgency", r.urgency); m.put("notes", r.notes);
        } else if (c instanceof FAQ f) {
            m.put("question", f.question); m.put("answer", f.answer);
            m.put("source_expert_answer_id", f.sourceExpertAnswerId);
        } else if (c instanceof ExpertAnswer e) {
            m.put("question", e.question); m.put("answer", e.answer);
            m.put("expert_name", e.expertName); m.put("expert_credentials", e.expertCredentials);
            m.put("expert_organization", e.expertOrganization); m.put("expert_contact", contact(e.expertContact));
            m.put("session_date", e.sessionDate);
        } else {
            throw new IllegalArgumentException("Unsupported resident content projection");
        }
        return new ResidentContent(m);
    }
}
