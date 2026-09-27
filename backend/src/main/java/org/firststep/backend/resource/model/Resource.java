package org.firststep.backend.resource.model;

import java.util.List;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Phone;
import org.firststep.backend.shared.model.Website;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A service listing. {@code subcategory} now lives on CivicContent (every content
 * type answers "what is it about?" the same way), so this class only keeps the
 * RAW source category string it arrived with — provenance from the DSCYF
 * directory, e.g. "Housing Assistance", "Recreational". Normalizing that raw
 * value into canonical {@code categoryTags} is the classifier's job (Slice F2);
 * until then CategoryService maps it through the taxonomy's matchCategories.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource extends CivicContent {

    public Resource() {
        this.contentType = ContentType.RESOURCE;
    }

    /** Raw source category as published by the directory — NOT the canonical taxonomy label. */
    public String category;
    /**
     * The resident-facing organization NAME. Still the thing a person reads, and
     * deliberately kept — but no longer the identity.
     */
    public String organization;

    /**
     * Canonical reference into the organization registry — {@code ORG-001}.
     *
     * <p>This, not {@link #organization} and never a slug, is how a resource says
     * who provides it. A display name can be corrected and a slug can change; an
     * ID cannot, which is what keeps a resource attached to its organization
     * across both (Decision 049).
     */
    @JsonProperty("organization_id")
    public String organizationId;
    @JsonProperty("parent_organization")
    public String parentOrganization;
    public String description;
    public String population;
    public String eligibility;
    @JsonProperty("eligibility_age_min")
    public Integer eligibilityAgeMin;
    @JsonProperty("eligibility_age_max")
    public Integer eligibilityAgeMax;
    @JsonProperty("eligibility_gender")
    public String eligibilityGender;
    public List<Location> locations;
    public List<Phone> phones;
    public List<Website> websites;
    public String county;
    @JsonProperty("access_mode")
    public List<String> accessMode;
    public String cost;
    public String urgency;
    public String notes;
}
