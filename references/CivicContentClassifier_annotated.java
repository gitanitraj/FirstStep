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

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CivicContentClassifier is the ONE entry point every ingestion path calls to
// normalize content into the canonical taxonomy. Six callers use it:
// JsonResourceRepository, JsonNewsRepository, JsonFlyerRepository,
// JsonExpertAnswerRepository, JsonFaqRepository and RssFeedService.
//
// The point of Slice F2 was a shared ENGINE, not six fixed callers. Before it,
// exactly one source classified anything (RssFeedService, with its own private
// keyword tables and its own private vocabulary) and resources were translated
// at query time inside CategoryService. Two classifiers, neither reusable,
// disagreeing about what a category even was.
// =============================================================================

// =============================================================================
// SECTION 1 — THE POLICY THIS CLASS EXISTS TO ENFORCE
// =============================================================================
// The engine could have been written without this class: each repository could
// call CategoryClassifier directly. It exists because a POLICY has to live
// somewhere, and a policy re-implemented in six places is a policy that will be
// wrong in at least one of them.
//
//     The classifier only classifies when editorial classification is absent.
//     Hand-authored editorial classifications are authoritative and IMMUTABLE
//     DURING INGESTION. Automated classification exists SOLELY to normalize
//     unclassified content.
//
// Three deliberate precisions in that wording:
//
// 1. "immutable DURING INGESTION" — not "immutable". Editorial classification
//    can and should change; an editor edits the data file. What is forbidden is
//    the PIPELINE mutating it. Without this, F2 would silently overwrite the
//    flyer and curated-news classification F1 hand-authored, and the failure
//    would be invisible until someone noticed a category page looked wrong.
//
// 2. "SOLELY to normalize unclassified content" — this bounds the classifier's
//    PURPOSE, not just its behavior. It rules out a tempting future feature: a
//    classifier that "improves" or second-guesses an editor's placement. It has
//    no such mandate.
//
// 3. PER FIELD, not per item. Curated news item NP-001 carries category_tags but
//    no subcategory. Per-item logic would skip it entirely and leave it
//    permanently topic-less; per-field, its present categoryTags are untouchable
//    while its absent subcategory stays eligible. This distinction is subtle
//    enough to be worth the test that locks it in
//    (shouldApplyThePolicyPerFieldNotPerItem).
//
// THE COROLLARY, which is what makes the system predictable:
//
//     Changes to how content is classified must result from intentional
//     editorial decisions, never from classifier behavior.
//
// This was verified empirically, not just asserted. Midway through F2 the
// keyword vocabulary was tuned (legal and community-support gained terms) and
// the live category counts did not move by a single record — 238 before, 238
// after — because every editorially-placed item was immutable. Only previously
// unclassified RSS bills moved, 134 -> 175 of 428. That is the invariant
// demonstrating itself.
//
// =============================================================================
// SECTION 2 — WHY THE TAG MERGE IS ASYMMETRIC WITH THE EDITORIAL FIELDS
// =============================================================================
// Note the deliberate asymmetry in classify():
//
//     categoryTags / subcategory   filled ONLY when absent  (replace-never)
//     tags                         MERGED with what is there (additive-always)
//
// That is not an inconsistency. Editorial classification is a PLACEMENT — an
// item has one home, and a second opinion is a conflict. Descriptive tags are
// METADATA — more of them is strictly better for search, and a hand-authored tag
// and a machine-derived tag do not compete. The two fields answer different
// questions, so they get different merge rules.
//
// =============================================================================
// SECTION 3 — WHY TEXT EXTRACTION IS A TYPE SWITCH (and why that is acceptable)
// =============================================================================
// classifiableText() is the only place in the engine that branches on content
// type, which sits uneasily beside the CivicContent contract's whole purpose of
// removing per-type branching. It is justified because the thing that varies is
// genuinely type-specific: a Resource keeps its prose in description/population/
// notes, a NewsItem in body/whyItMatters, an ExpertAnswer in question/answer.
// No amount of contract design makes those the same field.
//
// The alternative — a classifiableText() method on each model class — was
// rejected because the model classes are deliberately dumb data carriers.
// Giving them behavior for one consumer's convenience inverts that, and the next
// consumer would want its own method.
//
// Written as an instanceof chain rather than switch-over-patterns because the
// project targets Java 17 (maven.compiler.release); pattern matching in switch
// is 21+. A future type matching nothing still gets title + summary, which is
// the sensible floor rather than an exception.
//
// =============================================================================
// SECTION 4 — WHY THE COUNTERS EXIST
// =============================================================================
// summary() reports what a load actually produced. It exists because the keyword
// vocabulary is hand-authored and therefore WRONG in ways nobody can predict
// from reading it — the only way to tune it is to see what it does to real data.
// The first run on live legislation classified 134 of 428 bills; inspecting the
// most common words among the UNCLASSIFIED titles showed "court" appearing six
// times (a legal keyword that scored 1, below MIN_SCORE) and a cluster of
// education/student/school terms that community-support had no vocabulary for at
// all. That diagnosis came from the counters, not from re-reading the list.
//
// AtomicInteger rather than plain int because repositories load on
// ApplicationReadyEvent and there is no guarantee they are all on one thread.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CategoryClassifier does the actual category/subcategory determination.
// - TagClassifier merges evidence into descriptive tags.
// - All five Json*Repository classes and RssFeedService call classify() during
//   their load, which is what "classification happens at ingestion" means
//   concretely.
// - CategoryService no longer classifies anything: by the time content reaches
//   it, every item already carries canonical categoryTags.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Persist classification back into the data files (the way D0.3's enrichment
//   did), so it is reviewable in git. Rejected: it creates a second write path,
//   and re-running after a keyword change becomes a data migration that can
//   silently clobber hand-authored editorial classification — the one thing the
//   policy above forbids. Runtime classification means a vocabulary change takes
//   effect on restart with nothing to migrate.
// - Let each repository call CategoryClassifier directly, without this facade.
//   Rejected: see Section 1 — the policy would be re-implemented six times.
// - Have this class return a new object rather than mutating in place. Cleaner
//   functionally, but every caller would then have to rebuild its list, and the
//   models are mutable public-field POJOs throughout; a lone immutable seam here
//   would be inconsistent without being safer.
