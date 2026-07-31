package org.firststep.backend.category.model;

// =============================================================================
// WHAT THIS RECORD DOES
// =============================================================================
// CategoryDefinition is one canonical category — its key, display label, icon,
// the source values that map onto it, and the topics beneath it.
//
// WHAT CHANGED IN SLICE F1 (Decision 032): this record used to BE the
// vocabulary. It held a `public static final List<CategoryDefinition> ALL`
// listing all ten categories inline in Java, hand-mirrored against
// app/data/taxonomy.json. That constant is GONE. The file is now the single
// source of truth, TaxonomyService loads it, and this record is purely the
// shape Jackson binds each entry to.
//
// The class went from "the vocabulary, plus a shape" to just "the shape". That
// is the entire point of the change.
// =============================================================================

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One canonical category, deserialized from {@code app/data/taxonomy.json}.
 *
 * <p><b>This used to be the vocabulary itself</b> — a hardcoded {@code ALL}
 * constant listing all ten categories, hand-mirrored against taxonomy.json and
 * free to drift from it. Slice F1 deleted that constant and made the file the
 * single source of truth; this record is now purely the shape Jackson binds it
 * to, and {@link org.firststep.backend.category.service.TaxonomyService} owns
 * loading it. Any service that classifies or validates CivicContent asks the
 * TaxonomyService rather than carrying its own copy.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code matchCategories} — raw source-data category strings (DSCYF
 *       directory vocabulary, e.g. "Housing Assistance") that map onto this
 *       display category.</li>
 *   <li>{@code matchCategoryTags} — canonical editorial {@code category_tags}
 *       that associate a CivicContent item with this category. These are the
 *       display labels only. The old "Healthcare" alias is GONE: RSS now emits
 *       canonical values at the source, so downstream lists no longer widen to
 *       absorb upstream drift.</li>
 *   <li>{@code subcategories} — the canonical topics beneath this category.</li>
 * </ul>
 *
 * <p>{@code includesFlyers} is also gone. It was a hardcoded boolean that let
 * Community Events sweep in every flyer regardless of what the flyer was about —
 * the last place where content reached a category by special case rather than by
 * editorial classification. Flyers now carry their own {@code category_tags}.
 *
 * <p>Slice F2 added the classification vocabulary: {@code keywords} (terms and
 * phrases that are evidence FOR this category in free text) and the optional
 * {@code subcategoryKeywords} map. Both are read by
 * {@code shared.classification.CategoryClassifier}. Note the three vocabularies
 * are distinct and none substitutes for another — {@code matchCategories} is
 * upstream source vocabulary matched exactly, {@code matchCategoryTags} is
 * canonical editorial classification, {@code keywords} is probabilistic evidence
 * used only when nothing has classified the item already.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryDefinition(
        String key,
        String label,
        String icon,
        List<String> matchCategories,
        List<String> matchCategoryTags,
        List<String> keywords,
        Map<String, List<String>> subcategoryKeywords,
        List<String> subcategories
) {

    /**
     * Null-safe accessors — taxonomy.json is hand-authored and both keyword
     * fields are optional, so a category may legitimately omit them. Callers
     * should never have to null-check a vocabulary.
     */
    public List<String> keywordsOrEmpty() {
        return keywords == null ? List.of() : keywords;
    }

    public List<String> subcategoryKeywordsFor(String subcategory) {
        if (subcategoryKeywords == null) {
            return List.of();
        }
        return subcategoryKeywords.getOrDefault(subcategory, List.of());
    }
}

// =============================================================================
// SECTION 1 — THE FIELDS, AND WHY THERE ARE TWO MATCH LISTS
// =============================================================================
//   key                Stable identifier used in URLs (/category/housing) and
//                      as the join key everywhere. Never displayed.
//   label              What a resident reads. "Furniture & Household".
//   icon               Emoji shown beside the label. NEW to this record in F1 —
//                      it moved out of the deleted Java constant into
//                      taxonomy.json.
//   matchCategories    RAW SOURCE strings. DSCYF directory vocabulary —
//                      "Housing Assistance", "Recreational", "Before/After
//                      School Care". These are what the upstream data actually
//                      says, and they are neither canonical nor user-facing.
//   matchCategoryTags  CANONICAL editorial category_tags. What a CivicContent
//                      item carries to say which category it belongs to.
//   subcategories      The canonical topics beneath this category. NEW to this
//                      record in F1 — the Java constant never had these; only
//                      the JSON file did, which is why the backend could not
//                      offer a topic level at all before now.
//
// Two match lists exist because two different KINDS of value arrive:
// upstream-source vocabulary that must be translated (matchCategories), and
// editorial classification that is already canonical (matchCategoryTags). One
// combined list would lose that distinction and make it impossible to tell
// which values are ours and which are a vendor's.
//
// The long-term direction is that matchCategories shrinks: once Slice F2's
// classifier normalizes raw source categories into canonical categoryTags at
// load, resources classify the same way everything else does, and
// matchCategories becomes provenance rather than routing.
//
// SECTION 2 — WHY THE "Healthcare" ALIAS WAS REMOVED
// -----------------------------------------------------------------------------
// Decision 031 set health's matchCategoryTags to ["Health", "Healthcare"]. The
// alias existed for one reason: the RSS legislation classifier emitted
// "Healthcare" where the taxonomy said "Health", and adding the alias made the
// mismatch go away.
//
// Decision 032 removed it and fixes the mismatch at the source instead. The
// reasoning, worth keeping:
//
//     Absorbing upstream drift downstream is compounding interest on a
//     vocabulary. Each new source adds its own synonyms; the match lists grow
//     to document the history of every integration; and "canonical" quietly
//     comes to mean "whatever anyone has ever emitted".
//
// The invariant now is that every producer — Resources, News, RSS/Laws, Flyers,
// Expert content — emits canonical taxonomy values, and these lists stay
// narrow. TaxonomyService matches case-insensitively (a casing slip is a typo)
// but not fuzzily (a different word is a different vocabulary).
//
// SECTION 3 — WHY includesFlyers WAS DELETED
// -----------------------------------------------------------------------------
// The record used to carry a sixth component, `boolean includesFlyers`, true
// only for community-events. CategoryService read it like this:
//
//     List<Flyer> matchedFlyers = definition.includesFlyers() ? flyers : List.of();
//
// Every flyer, into Community Events, regardless of subject. It was the last
// place in the system where content reached a category by SPECIAL CASE rather
// than by editorial classification — and it produced visibly wrong results:
//
//   FL-002  "Know Your Rights: Eviction Prevention Info Session"  -> Community Events
//   FL-006  "Free Community Health Fair"                          -> Community Events
//   FL-007  "Free Furniture Giveaway"                             -> Community Events
//
// A tenant looking for eviction help under Housing found nothing. The flyer was
// filed under a category chosen by a boolean, not by what it was about.
//
// Flyers now carry their own category_tags and subcategory in flyers.json, so
// they classify like every other CivicContent type and the boolean has nothing
// left to do. FL-002 correctly reaches BOTH Housing and Legal; Community Events
// keeps only FL-001, the youth program that actually belongs there. Live counts
// moved accordingly: community-events 60 -> 54, housing 44 -> 45, legal 3 -> 5,
// health 32 -> 33, furniture-household 6 -> 7, community-support 58 -> 61.
//
// LESSON: a boolean on a definition that grants blanket membership is almost
// always a missing field on the CONTENT. The fix is to let the content say what
// it is, not to let the container say what it will accept.
//
// SECTION 4 — WHY A RECORD, AND WHY @JsonIgnoreProperties
// -----------------------------------------------------------------------------
// A record because a category definition is immutable value data with no
// behavior: Jackson binds records via their canonical constructor, and
// immutability means a service cannot accidentally mutate shared vocabulary at
// runtime. (Note the List components are not defensively copied — Jackson hands
// in ArrayLists. Nothing mutates them today; if that ever changes, List.copyOf
// in a compact constructor is the fix.)
//
// @JsonIgnoreProperties(ignoreUnknown = true) is NEW and load-bearing now that
// the record is deserialized rather than hand-constructed: taxonomy.json is an
// editorial artifact that will gain fields (a description, a hero image, an
// ordering hint) before this record does. Without it, adding any key to the file
// crashes startup — a data file should not be able to break the backend by
// gaining a field.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - TaxonomyService deserializes app/data/taxonomy.json into a List of these
//   and is the only thing that constructs them.
// - CategoryService iterates taxonomyService.getCategories(), matching
//   resources on matchCategories and news/flyers on matchCategoryTags.
// - CategorySummary (the DTO) carries key/label/icon out to the frontend.
// - Slice F3's NavigationService reads subcategories() for topic structure and
//   cross-checks it against navigation.json's groups.
// - validate_navigation.py enforces the same subcategories from the Python
//   side, so navigation.json can never name a topic this record doesn't have.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Keep ALL and add subcategories to it (the minimal change). Rejected — see
//   TaxonomyService_annotated.java Section 1; it preserves the drift problem
//   and makes a fifth hand-maintained copy of the vocabulary.
// - Put `icon` in navigation.json instead, on the theory that an icon is
//   presentation and taxonomy.json is the domain model. Architecturally purer,
//   but navigation.json's schema is groups-only and Decision 029 established
//   "absent from that file = flat topic list" as a meaningful invariant. Listing
//   all ten categories there just to carry icons would break that invariant and
//   force a validate_navigation.py change. One field in taxonomy.json was the
//   cheaper honest option; the impurity is recorded here rather than hidden.
// - A CategoryDefinition interface with a JSON-backed implementation, so tests
//   could substitute a fake vocabulary. Rejected: the tests deliberately run
//   against the REAL taxonomy so they fail when the file drifts. A fake would
//   remove exactly the signal the tests exist to give.
// =============================================================================

// =============================================================================
// SLICE F2 UPDATE (Decision 033) — A THIRD VOCABULARY
// =============================================================================
// Two components were added: `keywords` (terms and phrases that are evidence FOR
// this category in free text) and the optional `subcategoryKeywords` map.
//
// This record now carries THREE vocabularies, and conflating any two of them
// would undo a previous slice's work:
//
//   matchCategories     Upstream SOURCE vocabulary. Matched EXACTLY. Curated,
//                       deterministic, 100% coverage of the DSCYF directory.
//                       Tier 1 of CategoryClassifier.
//   matchCategoryTags   Canonical EDITORIAL classification. What a CivicContent
//                       item carries to say where it belongs.
//   keywords            Probabilistic EVIDENCE. Used only when nothing has
//                       classified the item already. Tier 2.
//
// The distinction that matters most: matchCategories is what a VENDOR calls a
// category; keywords are what a DOCUMENT says when it is about one. Merging them
// into a single "things that mean housing" list would lose the difference
// between a deterministic mapping and a guess, and tier 1 would stop being
// trustworthy.
//
// keywordsOrEmpty() and subcategoryKeywordsFor() are null-safe because
// taxonomy.json is hand-authored and both fields are optional — a category may
// legitimately have no keywords (as all ten did before F2). No caller should
// have to null-check a vocabulary.
//
// As of F2, `subcategoryKeywords` is authored for NO category. The schema and
// the code path exist so topic-level classification is purely additive later;
// until then CategoryClassifier declines to guess a topic. See
// CategoryClassifier_annotated.java Section 3 for why that reticence is
// deliberate rather than unfinished.
