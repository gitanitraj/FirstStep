package org.firststep.backend.category.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// TaxonomyService loads app/data/taxonomy.json at startup and is the ONE place
// the backend gets its category and subcategory vocabulary. Every service that
// classifies or validates CivicContent asks this class instead of carrying its
// own copy of the ten categories.
//
// It also owns the small operations that belong to the vocabulary itself:
// matching an item's editorial category_tags against a category, listing the
// distinct topics, and converting topics to and from URL slugs.
// =============================================================================

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.firststep.backend.category.model.CategoryDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TaxonomyService {

    /** Wrapper matching taxonomy.json's top level; version/note/source are metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Taxonomy(List<CategoryDefinition> categories) {
    }

    private final List<CategoryDefinition> categories;

    public TaxonomyService(@Value("${app.data.dir:app/data}") String dataDir) {
        this.categories = load(dataDir);
        System.out.println("Loaded taxonomy (" + categories.size() + " categories, "
                + allSubcategories().size() + " distinct subcategories)");
    }

    public List<CategoryDefinition> getCategories() {
        return categories;
    }

    public Optional<CategoryDefinition> findByKey(String key) {
        return categories.stream().filter(c -> c.key().equals(key)).findFirst();
    }

    public boolean matchesCategoryTags(CategoryDefinition definition, List<String> categoryTags) {
        if (categoryTags == null) {
            return false;
        }
        for (String tag : categoryTags) {
            for (String match : definition.matchCategoryTags()) {
                if (match.equalsIgnoreCase(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> allSubcategories() {
        Set<String> topics = new LinkedHashSet<>();
        categories.forEach(c -> topics.addAll(c.subcategories()));
        return topics;
    }

    public boolean isTopicOf(String categoryKey, String topic) {
        return findByKey(categoryKey)
                .map(c -> c.subcategories().stream().anyMatch(s -> s.equalsIgnoreCase(topic)))
                .orElse(false);
    }

    public static String topicSlug(String topic) {
        String slug = topic.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-+)|(-+$)", "");
    }

    public Optional<String> findTopicBySlug(String categoryKey, String slug) {
        return findByKey(categoryKey).flatMap(c -> c.subcategories().stream()
                .filter(s -> topicSlug(s).equals(slug))
                .findFirst());
    }

    private static List<CategoryDefinition> load(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        Path external = Path.of(dataDir, "taxonomy.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), Taxonomy.class).categories();
            }
            try (InputStream in = TaxonomyService.class.getResourceAsStream("/taxonomy.json")) {
                if (in == null) {
                    throw new IllegalStateException(
                            "taxonomy.json not found at " + external.toAbsolutePath() + " or on the classpath");
                }
                return mapper.readValue(in, Taxonomy.class).categories();
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load taxonomy.json: " + e.getMessage(), e);
        }
    }
}

// =============================================================================
// SECTION 1 — WHY THIS CLASS HAD TO EXIST
// =============================================================================
// taxonomy.json has called itself "the single source of truth" since Decision
// 027. It wasn't. No Java code read it. The backend kept a parallel, hardcoded
// copy in CategoryDefinition.ALL — ten categories with their labels, icons,
// match rules — while the Python validators read the file. Two copies of a
// vocabulary that MUST agree.
//
// The cost was already being paid, visibly, in the decision log:
//   - Decision 030 discovered validate_news.py policing a stale 4-string
//     vocabulary while the 10-category taxonomy had moved on. It had been
//     failing silently.
//   - Decision 031 renamed matchNewsTags -> matchCategoryTags and had to change
//     BOTH the Java constant and the JSON file by hand, then verify they still
//     matched.
//
// Every one of those was a symptom of the same cause: a source of truth that
// nothing consumes is just a comment. Deleting CategoryDefinition.ALL and
// loading the file is what makes the claim true.
//
// SECTION 2 — WHY THE CONSTRUCTOR LOADS, NOT @EventListener
// -----------------------------------------------------------------------------
// The other JSON loaders here (JsonResourceRepository, JsonNewsRepository) load
// on ApplicationReadyEvent. This one deliberately does not, and the difference
// is not stylistic:
//
//     Content can load whenever. VOCABULARY must exist before anything that
//     uses it does.
//
// Those repositories will eventually normalize what they load THROUGH the
// taxonomy (Slice F2 does exactly this for raw resource categories). If the
// taxonomy also loaded on ApplicationReadyEvent, the ordering between two
// listeners would be unspecified — the classic "works until it doesn't"
// startup bug. Loading in the constructor makes it a Spring dependency: any
// bean that injects TaxonomyService is guaranteed a fully-loaded one, enforced
// by the container rather than by hope.
//
// SECTION 3 — WHY A MISSING FILE IS FATAL
// -----------------------------------------------------------------------------
// The repositories return an empty list when their file is missing, log it, and
// carry on. That is right for content: an app with no flyers is still an app.
// It is wrong for vocabulary. With no taxonomy, CategoryService produces ten
// categories that all count zero, /api/home returns a structurally valid
// payload full of nothing, and the homepage renders — empty, with no error
// anywhere. Someone would eventually trace it back to a mis-set APP_DATA_DIR.
//
// Throwing at startup costs one confusing failure once. Not throwing costs a
// silent, plausible-looking wrong answer indefinitely. The load() method's
// catch is written to re-throw IllegalStateException unwrapped so the "not
// found at <path>" message reaches the operator intact rather than being
// re-wrapped in a generic parse failure.
//
// SECTION 4 — WHY matchCategoryTags MATCHING IS CASE-INSENSITIVE BUT NOT FUZZY
// -----------------------------------------------------------------------------
// matchesCategoryTags() compares with equalsIgnoreCase. That tolerates "health"
// for "Health" — a casing slip in hand-authored data, where the author clearly
// meant the right category. It does NOT tolerate "Healthcare" for "Health":
// that is a different word, which means a different vocabulary.
//
// This is a deliberate reversal of Decision 031, which had ADDED "Healthcare"
// to health's match list so the taxonomy could absorb the RSS classifier's
// drifted labels. That direction is how vocabularies rot: each new upstream
// quirk widens the list, the lists stop describing the domain and start
// describing the history of integrations, and eventually nothing is canonical.
//
// The rule now runs the other way:
//
//     Normalize AT THE SOURCE. Every producer emits canonical values.
//     The taxonomy stays narrow and never absorbs drift.
//
// (As of F1 the RSS classifier still emits "Healthcare" and "Delaware
// Legislation". Nothing breaks — CategoryService reads curated news only, so
// those values reach no category page. Fixing the classifier is Slice F2, and
// TaxonomyServiceTest.shouldNotMatchDriftedVocabularyThatIsNotInTheTaxonomy
// locks in the narrow behavior so the widening cannot quietly return.)
//
// SECTION 5 — WHY topicSlug IS DUPLICATED FROM OrganizationService
// -----------------------------------------------------------------------------
// OrganizationService.slugify() is character-for-character the same three lines.
// Extracting a shared Slugs utility is the obvious DRY move and was not done.
//
// The two slugs serve different URL spaces: /organization/{slug} and
// /category/{key}/{topic}. Sharing the implementation would couple them, so a
// future change to one — say, transliterating accented characters for
// organization names — would silently change every topic URL too. URLs are a
// public contract; two six-line methods that happen to agree today are cheaper
// than one method that must serve two contracts forever.
//
// DRY applies to knowledge, not to characters. These are two pieces of
// knowledge that currently look alike.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CategoryDefinition is now a plain deserialization target (its ALL constant
//   is gone); this class owns the loading.
// - CategoryService injects this and iterates getCategories() instead of
//   CategoryDefinition.ALL, and calls matchesCategoryTags() for both news and
//   flyers.
// - Slice F3's NavigationService will use getCategories() + subcategories() for
//   topic structure, and topicSlug()/findTopicBySlug() for /category/{key}/{topic}
//   routing.
// - The Python validators (validate_schema, validate_news, validate_navigation,
//   enrich_resources) each read the same file independently. That is FOUR more
//   loaders, in another language, and remains open tech debt — but they now
//   agree with the backend by construction rather than by hand.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Keep CategoryDefinition.ALL and just add `subcategories` to it. Smallest
//   possible diff, no new I/O. Rejected: it makes a fifth hand-maintained copy
//   of the vocabulary and leaves the drift problem exactly where it was.
// - Derive topics from the loaded resources' distinct subcategory values rather
//   than from a declared vocabulary. Tempting (no file needed) but it means a
//   topic with zero content simply vanishes from the UI — Eviction Prevention
//   and all of Utilities would be invisible. That is precisely the
//   unreachability bug validate_navigation.py exists to catch.
// - Load taxonomy.json into a Spring @ConfigurationProperties bean. Idiomatic,
//   but it would require the file to live in the config namespace rather than
//   app/data/ alongside the other data artifacts, splitting the data directory
//   for no gain.
// - Cache/reload on file change. Not built: the taxonomy is a stable domain
//   model, and a restart is the honest way to adopt a vocabulary change.
// =============================================================================
