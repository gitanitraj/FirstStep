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
