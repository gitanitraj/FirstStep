package org.firststep.backend.shared.classification;

import java.util.List;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CivicContentClassifierTest {

    private CivicContentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = ClassifierFixture.real();
    }

    // =========================================================================
    // THE CLASSIFICATION POLICY — the load-bearing tests
    //
    //   The classifier only classifies when editorial classification is absent.
    //   Hand-authored editorial classifications are authoritative and immutable
    //   during ingestion. Automated classification exists solely to normalize
    //   unclassified content.
    // =========================================================================

    @Test
    void shouldNotOverwriteHandAuthoredEditorialClassification() {
        // Without this rule, F2 silently destroys the flyer and news
        // classification F1 hand-authored. FL-002's text is full of housing
        // keywords, but an editor placed it under Housing AND Legal.
        Flyer flyer = new Flyer();
        flyer.title = "Know Your Rights: Eviction Prevention Info Session";
        flyer.summary = "Tenant rights, eviction defenses and emergency rental assistance.";
        flyer.categoryTags = List.of("Housing", "Legal");
        flyer.subcategory = "Eviction Prevention";

        classifier.classify(flyer);

        assertEquals(List.of("Housing", "Legal"), flyer.categoryTags);
        assertEquals("Eviction Prevention", flyer.subcategory);
    }

    @Test
    void shouldApplyThePolicyPerFieldNotPerItem() {
        // NP-001's shape: editorial category_tags but no subcategory. Per-item
        // logic would skip it entirely and leave it permanently topic-less.
        NewsItem news = new NewsItem();
        news.title = "Emergency rental assistance applications open";
        news.summary = "Eviction, tenant and landlord guidance for renters.";
        news.categoryTags = List.of("Housing");
        news.subcategory = null;

        classifier.classify(news);

        // The present field is untouched...
        assertEquals(List.of("Housing"), news.categoryTags);
        // ...and the absent one was eligible to be filled (null today only
        // because no subcategoryKeywords are authored yet — see F2 scope).
        assertNull(news.subcategory);
    }

    @Test
    void shouldNormalizeUnclassifiedContent() {
        NewsItem bill = new NewsItem();
        bill.title = "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS AND LANDLORDS";
        bill.summary = "Rental housing protections.";

        classifier.classify(bill);

        assertEquals(List.of("Housing"), bill.categoryTags);
    }

    @Test
    void shouldTreatEmptyCategoryTagsAsUnclassified() {
        // An empty list is absence, not a decision — otherwise an item that
        // once failed classification could never be classified again.
        NewsItem bill = new NewsItem();
        bill.title = "AN ACT RELATING TO MEDICAID AND CLINIC ACCESS";
        bill.categoryTags = List.of();

        classifier.classify(bill);

        assertTrue(bill.categoryTags.contains("Health"));
    }

    @Test
    void shouldLeaveContentUnclassifiedWhenThereIsNoEvidence() {
        NewsItem bill = new NewsItem();
        bill.title = "AN ACT CONCERNING THE STATE FLAG DESIGN";

        classifier.classify(bill);

        assertTrue(bill.categoryTags == null || bill.categoryTags.isEmpty());
    }

    @Test
    void shouldTolerateNullItem() {
        classifier.classify(null);
    }

    // ---- Resources: source vocabulary + untouched subcategory ---------------

    /** A resource as JsonResourceRepository produces one: raw category PLUS the
     *  provider id, without which no source adapter can know whose vocabulary
     *  "Housing Assistance" is. */
    private Resource dscyfResource(String rawCategory) {
        Resource r = new Resource();
        r.category = rawCategory;
        r.contentSource = new ContentSource();
        r.contentSource.id = "dscyf-directory";
        return r;
    }

    @Test
    void shouldClassifyResourceFromItsRawSourceCategory() {
        Resource resource = dscyfResource("Housing Assistance");
        resource.subcategory = "Emergency Shelter";
        resource.title = "Sunday Breakfast Mission";

        classifier.classify(resource);

        assertEquals(List.of("Housing"), resource.categoryTags);
        assertEquals("Emergency Shelter", resource.subcategory);
    }

    @Test
    void shouldNotLeakUpstreamSourceVocabularyIntoDescriptiveTags() {
        // A deterministic source-category match must contribute NO evidence:
        // "Housing Assistance" is DSCYF's word for a category, and pushing it
        // into tags would both put a category name in the descriptive field and
        // pollute search with upstream vocabulary. Provenance stays on
        // Resource.category, where it belongs.
        Resource resource = dscyfResource("Housing Assistance");
        resource.title = "Sunday Breakfast Mission";
        resource.tags = List.of("emergency");

        classifier.classify(resource);

        assertEquals(List.of("emergency"), resource.tags);
    }

    @Test
    void shouldClassifyResourceWhoseSourceCategoryIsUnknownUsingItsText() {
        Resource resource = dscyfResource("Unmapped Vendor Category");
        resource.subcategory = "Food Pantry";
        resource.title = "Neighborhood food pantry";
        resource.description = "Groceries and meals for families facing hunger.";

        classifier.classify(resource);

        assertTrue(resource.categoryTags.contains("Food"));
    }

    // ---- Descriptive tags are additive -------------------------------------

    @Test
    void shouldAddEvidenceTermsToDescriptiveTagsWithoutReplacingAuthoredOnes() {
        NewsItem bill = new NewsItem();
        bill.title = "AN ACT RELATING TO EVICTION AND TENANT PROTECTIONS";
        bill.tags = List.of("hand-authored");

        classifier.classify(bill);

        assertEquals("hand-authored", bill.tags.get(0));
        assertTrue(bill.tags.size() > 1);
    }

    @Test
    void shouldNotPutCategoryNamesIntoDescriptiveTags() {
        // Tags describe what content is ABOUT; category names describe where it
        // BELONGS. Mixing them is the conflation F1 removed.
        NewsItem bill = new NewsItem();
        bill.title = "AN ACT RELATING TO EVICTION AND TENANT PROTECTIONS";

        classifier.classify(bill);

        assertTrue(bill.categoryTags.contains("Housing"));
        assertTrue(bill.tags.stream().noneMatch(t -> t.equalsIgnoreCase("Housing")));
    }

    // ---- Text extraction ---------------------------------------------------

    @Test
    void shouldReadClassifiableTextFromEachContentTypesOwnFields() {
        ExpertAnswer expert = new ExpertAnswer();
        expert.question = "How do I apply for utility assistance?";
        expert.answer = "Contact LIHEAP about your electric and heating bill.";

        String text = CivicContentClassifier.classifiableText(expert);

        assertTrue(text.contains("LIHEAP"));
        assertTrue(text.contains("utility assistance"));
    }

    @Test
    void shouldClassifyExpertContentThroughTheSamePathAsEverythingElse() {
        ExpertAnswer expert = new ExpertAnswer();
        expert.question = "How do I get help with my electric bill?";
        expert.answer = "LIHEAP and weatherization cover heating and energy costs.";

        classifier.classify(expert);

        assertTrue(expert.categoryTags.contains("Utilities"));
    }

    @Test
    void shouldReportASummaryOfWhatWasClassified() {
        NewsItem classified = new NewsItem();
        classified.title = "AN ACT RELATING TO EVICTION AND TENANT PROTECTIONS";
        NewsItem editorial = new NewsItem();
        editorial.categoryTags = List.of("Food");
        editorial.subcategory = "Food Pantry";

        classifier.classify(classified);
        classifier.classify(editorial);

        String summary = classifier.summary();
        assertTrue(summary.contains("1 already editorially classified"));
        assertTrue(summary.contains("1 normalized by classifier"));
    }
}
