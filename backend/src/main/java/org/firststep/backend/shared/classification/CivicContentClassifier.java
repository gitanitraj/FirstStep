package org.firststep.backend.shared.classification;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.CivicContent;
import org.springframework.stereotype.Component;

/**
 * The single entry point every ingestion path calls to normalize a piece of
 * content into the canonical taxonomy.
 *
 * <h2>The classification policy</h2>
 *
 * <blockquote>
 * The classifier only classifies when editorial classification is absent.
 * Hand-authored editorial classifications are authoritative and <b>immutable
 * during ingestion</b>. Automated classification exists <b>solely to normalize
 * unclassified content</b>.
 * </blockquote>
 *
 * <p>Three things that phrasing is careful about, and each matters:
 *
 * <p><b>"immutable during ingestion"</b> — not "immutable". Editorial
 * classification absolutely can change; an editor edits the data file and it
 * changes. What is forbidden is the PIPELINE mutating it. Without this rule F2
 * would silently overwrite the flyer and curated-news classification Slice F1
 * hand-authored, and nobody would notice until a category page looked wrong.
 *
 * <p><b>"solely to normalize unclassified content"</b> — this bounds the
 * classifier's purpose, ruling out a tempting future feature: a classifier that
 * "improves" or second-guesses an editor's placement. It has no such mandate.
 *
 * <p><b>The rule applies per FIELD, not per item.</b> A curated news item like
 * NP-001 carries {@code category_tags} but no {@code subcategory}. Per-item
 * logic would skip it entirely and leave it permanently topic-less; per-field,
 * its present {@code categoryTags} are untouchable while its absent
 * {@code subcategory} may be filled.
 *
 * <p>It follows that <b>changes to how content is classified must result from
 * intentional editorial decisions, never from classifier behavior</b>. Tuning the
 * keyword vocabulary may change what UNCLASSIFIED content normalizes to; it must
 * never move content an editor has already placed.
 */
@Component
public class CivicContentClassifier {

    private final CategoryClassifier categoryClassifier;
    private final TagClassifier tagClassifier;

    private final AtomicInteger classified = new AtomicInteger();
    private final AtomicInteger editorial = new AtomicInteger();
    private final AtomicInteger unclassified = new AtomicInteger();

    public CivicContentClassifier(CategoryClassifier categoryClassifier, TagClassifier tagClassifier) {
        this.categoryClassifier = categoryClassifier;
        this.tagClassifier = tagClassifier;
    }

    /**
     * Normalize one item in place. Safe to call on anything, including content
     * that is already fully classified — that is the common case, and it is a
     * no-op for the editorial fields by design.
     */
    public void classify(CivicContent item) {
        if (item == null) {
            return;
        }

        boolean needsCategory = isBlank(item.categoryTags);
        boolean needsSubcategory = item.subcategory == null || item.subcategory.isBlank();

        if (!needsCategory && !needsSubcategory) {
            editorial.incrementAndGet();
            return;
        }

        Classification result = categoryClassifier.classify(sourceCategoryOf(item), classifiableText(item));

        // PER-FIELD, and only into absent fields. Note the asymmetry with tags
        // below: editorial fields are filled only when empty, descriptive tags
        // are merged — because tags are additive metadata, not a placement.
        if (needsCategory && !result.categoryTags().isEmpty()) {
            item.categoryTags = result.categoryTags();
        }
        if (needsSubcategory && result.subcategory() != null) {
            item.subcategory = result.subcategory();
        }
        if (!result.evidence().isEmpty()) {
            item.tags = tagClassifier.mergeTags(item.tags, result.evidence());
        }

        if (isBlank(item.categoryTags)) {
            unclassified.incrementAndGet();
        } else {
            classified.incrementAndGet();
        }
    }

    /**
     * One-line summary of what a load produced, so the keyword vocabulary can be
     * tuned against real data rather than guessed at. Repositories call this
     * after loading; the counters are cumulative across all sources.
     */
    public String summary() {
        return "Classification: " + editorial.get() + " already editorially classified, "
                + classified.get() + " normalized by classifier, "
                + unclassified.get() + " left unclassified";
    }

    /**
     * Which fields hold classifiable prose, per content type.
     *
     * <p>This switch is the one legitimately type-specific thing in the engine —
     * every type stores its text somewhere different. It lives here rather than
     * as a {@code classifiableText()} method on each model because the model
     * classes are deliberately dumb data carriers; giving them behavior for the
     * classifier's benefit would invert that.
     *
     * <p>Note the raw {@code category} is included for Resources: it is real
     * descriptive signal even when Tier 1's exact match misses.
     *
     * <p>Written as an instanceof chain rather than a switch over patterns
     * because the project targets Java 17 ({@code maven.compiler.release}), and
     * pattern matching in switch is 21+. A future type that matches nothing still
     * gets title and summary, which is the sensible floor.
     */
    static String classifiableText(CivicContent item) {
        StringBuilder text = new StringBuilder();
        append(text, item.title);
        append(text, item.summary);

        if (item instanceof Resource r) {
            append(text, r.description);
            append(text, r.category);
            append(text, r.population);
            append(text, r.notes);
        } else if (item instanceof NewsItem n) {
            append(text, n.body);
            append(text, n.whyItMatters);
        } else if (item instanceof Flyer f) {
            append(text, f.organization);
        } else if (item instanceof ExpertAnswer e) {
            append(text, e.question);
            append(text, e.answer);
        } else if (item instanceof FAQ f) {
            append(text, f.question);
            append(text, f.answer);
        }
        return text.toString();
    }

    /** Only Resources carry an upstream source category; everything else returns null. */
    private static String sourceCategoryOf(CivicContent item) {
        return item instanceof Resource r ? r.category : null;
    }

    private static void append(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            text.append(value).append(' ');
        }
    }

    private static boolean isBlank(List<String> values) {
        return values == null || values.isEmpty();
    }
}
