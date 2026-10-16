package org.firststep.backend.shared.model;

/**
 * What KIND of civic content this is — the answer to the CivicContent contract's
 * first question ("What kind of content is this?").
 *
 * <p><b>Content type is not a category.</b> It determines how an item is
 * PRESENTED (which card treatment, which type indicator, which detail view), never
 * WHERE it appears in the navigation hierarchy. Placement is driven solely by
 * editorial classification — {@code categoryTags} + {@code subcategory}. A
 * legislative item classified into Housing appears under Housing like any other
 * content, while still rendering with the Law treatment.
 *
 * <p>{@link #LAW} is deliberately a content type rather than a category. RSS
 * legislation classifies into the canonical taxonomy the same way every other
 * source does; keeping LAW here is what preserves the dedicated News/Policy/Law
 * experience without introducing a parallel editorial vocabulary.
 */
public enum ContentType {
    RESOURCE,
    NEWS,
    FLYER,
    LAW,
    EXPERT
}
