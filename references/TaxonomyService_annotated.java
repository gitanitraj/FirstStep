// =============================================================================
// ANNOTATED REFERENCE — backend/.../category/service/TaxonomyService.java
// Slice F originally; `noticeKinds` added in Slice J (Decision 046).
// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// TaxonomyService loads app/data/taxonomy.json at startup and is the ONE place
// the backend gets its controlled vocabulary. Every service that classifies or
// validates CivicContent asks this class instead of carrying its own copy.
//
// It holds TWO vocabularies now:
//   categories   the ten editorial categories and their subcategories
//   noticeKinds  event / meeting / announcement (Slice J)
//
// It also owns the small operations that belong to the vocabulary itself:
// matching an item's editorial category_tags against a category, listing the
// distinct topics, converting topics to and from URL slugs, and resolving the
// single notice kind carried by a set of tags.
// =============================================================================

package org.firststep.backend.category.service;

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

/**
 * Loads {@code app/data/taxonomy.json} and serves the canonical category /
 * subcategory vocabulary to every service that needs it.
 *
 * <p><b>Why this class exists.</b> taxonomy.json has described itself as the
 * single source of truth since Decision 027, but no Java code ever read it — the
 * backend kept a hand-mirrored copy in {@code CategoryDefinition.ALL}, and the
 * Python validators read the file. Two copies of a vocabulary that must agree is
 * a drift bug waiting to happen, and Decisions 030/031 each spent effort keeping
 * them in sync by hand. Now there is one copy, and Java reads it.
 *
 * <p><b>Why the constructor loads, not an event listener.</b> The other JSON
 * loaders in this codebase (JsonResourceRepository, JsonNewsRepository) load on
 * {@code ApplicationReadyEvent}, which is fine for content but wrong for
 * vocabulary: those repositories will eventually need the taxonomy to normalize
 * what they load, so it must already be in memory when they run. Loading in the
 * constructor makes the vocabulary available to any bean that injects this one,
 * which is exactly the guarantee a source of truth should give.
 *
 * <p>Resolution order matches the repositories: the external file at
 * {@code app.data.dir} first (that is what Docker mounts at /data), then the
 * classpath. Unlike the repositories, a missing taxonomy is FATAL rather than an
 * empty list — the application cannot classify anything without it, and failing
 * at startup is far cheaper to diagnose than ten silently empty categories.
 */
@Service
public class TaxonomyService {

    /** Wrapper matching taxonomy.json's top level; version/note/source are metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Taxonomy(List<CategoryDefinition> categories, List<String> noticeKinds) {
    }

    private final List<CategoryDefinition> categories;
    private final List<String> noticeKinds;

    public TaxonomyService(@Value("${app.data.dir:app/data}") String dataDir) {
        Taxonomy taxonomy = load(dataDir);
        this.categories = taxonomy.categories();
        this.noticeKinds = taxonomy.noticeKinds() == null ? List.of() : List.copyOf(taxonomy.noticeKinds());
        System.out.println("Loaded taxonomy (" + categories.size() + " categories, "
                + allSubcategories().size() + " distinct subcategories, "
                + noticeKinds.size() + " notice kinds)");
    }

    /**
     * The controlled vocabulary for WHAT KIND of community notice an item is —
     * {@code event}, {@code meeting}, {@code announcement}. Carried in a record's
     * existing {@code tags}, so this adds no field and no ContentType.
     *
     * <p><b>Three kinds, not four.</b> "Flyers" is not a kind — it is
     * {@code contentType}, an axis that already exists. A flyer advertising a
     * health fair carries kind {@code event} and appears in BOTH views, because
     * they are lenses over the same content rather than exclusive buckets.
     *
     * <p>It lives in taxonomy.json rather than a new file because that artifact
     * already declares itself the single source of truth for controlled
     * vocabulary — a seventh data file would have brought a seventh loader and a
     * seventh validator with it.
     */
    public List<String> getNoticeKinds() {
        return noticeKinds;
    }

    /** True if {@code tag} is a declared notice kind. Case-insensitive, like category matching. */
    public boolean isNoticeKind(String tag) {
        return tag != null && noticeKinds.stream().anyMatch(k -> k.equalsIgnoreCase(tag));
    }

    /**
     * The single notice kind carried by these tags, or empty.
     *
     * <p>Empty when there is none AND when there is more than one: two kinds is an
     * authoring error the validator blocks, and guessing which one wins would hide
     * it. The same never-guess rule ContentSourceService applies to an unknown
     * producer (Decision 045).
     */
    public Optional<String> noticeKindOf(List<String> tags) {
        if (tags == null) {
            return Optional.empty();
        }
        List<String> found = tags.stream().filter(this::isNoticeKind)
                .map(t -> t.toLowerCase(Locale.ROOT)).distinct().toList();
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }

    /** All categories, in the file's authored order — that order is the display order. */
    public List<CategoryDefinition> getCategories() {
        return categories;
    }

    public Optional<CategoryDefinition> findByKey(String key) {
        return categories.stream().filter(c -> c.key().equals(key)).findFirst();
    }

    /**
     * The category an item's editorial {@code category_tags} place it in. Matching
     * is case-insensitive on the canonical label, which tolerates casing slips in
     * authored data without tolerating a different vocabulary.
     */
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

    /** Every distinct subcategory across all categories. A topic may appear under several. */
    public Set<String> allSubcategories() {
        Set<String> topics = new LinkedHashSet<>();
        categories.forEach(c -> topics.addAll(c.subcategories()));
        return topics;
    }

    /** True if {@code topic} is a declared subcategory of the given category. */
    public boolean isTopicOf(String categoryKey, String topic) {
        return findByKey(categoryKey)
                .map(c -> c.subcategories().stream().anyMatch(s -> s.equalsIgnoreCase(topic)))
                .orElse(false);
    }

    /**
     * Slug used in topic URLs — "Child Care &amp; Early Learning" →
     * "child-care-early-learning". Same rule as OrganizationService.slugify, kept
     * local rather than shared: one is a topic slug, one an organization slug, and
     * coupling them would mean a change to either affects the other's URLs.
     */
    public static String topicSlug(String topic) {
        String slug = topic.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-+)|(-+$)", "");
    }

    /** Resolve a URL slug back to the canonical topic name within a category. */
    public Optional<String> findTopicBySlug(String categoryKey, String slug) {
        return findByKey(categoryKey).flatMap(c -> c.subcategories().stream()
                .filter(s -> topicSlug(s).equals(slug))
                .findFirst());
    }

    private static Taxonomy load(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        Path external = Path.of(dataDir, "taxonomy.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), Taxonomy.class);
            }
            try (InputStream in = TaxonomyService.class.getResourceAsStream("/taxonomy.json")) {
                if (in == null) {
                    throw new IllegalStateException(
                            "taxonomy.json not found at " + external.toAbsolutePath() + " or on the classpath");
                }
                return mapper.readValue(in, Taxonomy.class);
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

// =============================================================================
// SECTION 6 — WHY noticeKinds LIVES HERE (Slice J, Decision 046)
// =============================================================================
// Community Notices needed a controlled vocabulary of three values to
// distinguish events, meetings and announcements. The options were:
//
//   1. A new app/data/notice-kinds.json with its own loader and validator.
//   2. A Java enum NoticeKind.
//   3. One more array in taxonomy.json, read by this class.
//
// (1) was rejected on cost: a seventh data file brings a seventh loader, a
// seventh validator and a seventh thing to keep in sync, to hold three strings.
//
// (2) was rejected on ownership. These are EDITORIAL values — the people who
// author flyers decide whether a notice is a meeting. Vocabulary that content
// authors own belongs in the data file they can edit, not in a Java enum that
// requires a deployment to extend. Contrast ContentType, which IS an enum
// precisely because it changes the code that renders it.
//
// (3) is what taxonomy.json already claims to be: "the single source of truth
// for controlled vocabulary". It was already loaded at startup, already
// validated, already the file every classifier consults. Adding one array was
// the change that introduced no new machinery at all.
//
// WHY THE FIELD IS DEFENSIVE ABOUT NULL
// -------------------------------------
//     this.noticeKinds = taxonomy.noticeKinds() == null ? List.of() : …
//
// The categories field is NOT defensive — a taxonomy with no categories is a
// broken deployment and should fail loudly. noticeKinds is different: it was
// added to an existing file format, so an older taxonomy.json without the key is
// a valid state during a rollback, and the correct behavior is "no notice kinds
// exist" (every kind view empties), not a startup crash that takes the whole
// site down over a feature it predates.
//
// =============================================================================
// SECTION 7 — noticeKindOf RETURNS EMPTY FOR TWO KINDS, NOT THE FIRST ONE
// =============================================================================
//     return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
//
// Read that carefully: it returns empty for ZERO matches AND for MORE THAN ONE.
// Returning the first match would have been the natural-looking implementation
// and would have been wrong.
//
// Two kinds on one record is an authoring error the validator blocks. If one
// silently won here, the record would look CORRECTLY FILED on whichever page it
// landed on — a bug with no symptom, discoverable only by someone noticing an
// item missing from the page they expected. Excluding it instead makes the error
// visible as an absence, and the validator names it at author time.
//
// This is the same never-guess rule ContentSourceService applies to an
// unresolvable producer (Decision 045), and it is worth stating as a general
// principle: **when the data is ambiguous, an implementation that picks one
// answer converts a data error into a silent behavior error.**
//
// Case-insensitive matching matches matchesCategoryTags — tolerate a casing slip
// in authored data, do not tolerate a different vocabulary.
//
// =============================================================================
// SECTION 8 — WHY isNoticeKind IS PUBLIC AS WELL AS noticeKindOf
// =============================================================================
// noticeKindOf answers "which kind is this item?" and is what
// CommunityNoticesService calls. isNoticeKind answers "is this string a kind at
// all?" — a different question, needed for validation rather than selection.
// One is a lookup, the other is membership; folding them together would have
// meant callers wrapping a single tag in a List to ask about it.
// =============================================================================
