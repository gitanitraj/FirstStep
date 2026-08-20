/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../shared/service/ContentSourceService.java
 *   ALSO covers shared/model/Sector.java and app/data/content-sources.json.
 * Slice I. See references/decisions.md Decision 045.
 * Keep this mirror in sync whenever the production files change.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The producer registry. It answers two questions about a contentSource.id:
 *   what is this producer called, and what SECTOR are they?
 *
 * WHY IT HAD TO EXIST
 *   Latest Updates and Community Notices are split by WHO PUBLISHED the content,
 *   and nothing in the data said. See SECTION 1.
 *
 * THE PART TO READ IF YOU READ NOTHING ELSE
 *   SECTION 3 — the failure boundary. It is the decision most likely to be
 *   "corrected" later by someone who thinks failing fast is always safer.
 * ============================================================================= */

package org.firststep.backend.shared.service;

// imports elided — see production

@Service
public class ContentSourceService {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Registry(List<Producer> sources) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Producer(String id, String name, String sector, String feedUrl) { }

    private final Map<String, Producer> byId;
    /** Ids referenced by content but absent from the registry. Reported, never guessed. */
    private final Set<String> unknownIds = new LinkedHashSet<>();

    public ContentSourceService(@Value("${app.data.dir:app/data}") String dataDir) {
        this.byId = index(read(dataDir));
        System.out.println("Loaded content sources (" + byId.size() + " producers, "
                + feedUrls().size() + " with feeds)");
    }

    public Optional<String> nameOf(String id) { … }

    /** NORMALIZE STAGE — turns a reference into a display value. See SECTION 2. */
    public void resolveName(ContentSource contentSource) {
        if (contentSource == null || contentSource.name != null) return;
        nameOf(contentSource.id).ifPresent(name -> contentSource.name = name);
    }

    public Optional<Sector> sectorOf(String id) { … }

    /** False for EVERY sector when the id is absent or unknown. See SECTION 3. */
    public boolean isInSector(ContentSource contentSource, Sector sector) {
        if (contentSource == null) return false;
        return sectorOf(contentSource.id).filter(sector::equals).isPresent();
    }

    /** producerId -> feedUrl. RssFeedService reads its feed list from here. SECTION 4. */
    public Map<String, String> feedUrls() { … }

    @EventListener(ApplicationReadyEvent.class)
    public void reportUnknownIds() { … }   // the startup summary, SECTION 3c

    private Producer resolve(String id) {
        …
        if (producer == null && unknownIds.add(id)) {
            // Logged ONCE per distinct id — a broken feed would otherwise fill
            // the log with one repeated line and bury everything else.
            System.out.println("ERROR content-sources: unknown contentSource.id '" + id + "' …");
        }
        return producer;
    }
}

// =============================================================================
// SECTION 1 — WHY SECTOR COULD NOT BE DERIVED FROM ANYTHING THAT EXISTED
// =============================================================================
// Three candidates were checked against the real data before anything was built,
// and all three failed:
//
//   contentSource.type   encodes FORMAT — manual, expert-session, faq. Not sector.
//
//   contentType          THE COUNTER-EXAMPLE THAT SETTLED IT: Wilmington Housing
//                        Authority publishes BOTH a news item AND a flyer, and
//                        both are government. A rule of the form "flyers are
//                        community" would have been wrong — and wrong in a way
//                        that only appears when you look at the data.
//
//   contentSource.name   NOT A KEY. The same agency appeared as "Delaware DHSS"
//                        (news) and "Delaware Health and Social Services" (expert
//                        answers). RSS items took their name from feed.getTitle()
//                        AT RUNTIME, so it was not authored at all.
//
// Sector is a property of the PRODUCER. That is the whole finding, and everything
// else in this class follows from it.
//
// FOUR `type`-ish fields now exist. Only ONE was deleted (UpdateItem.type,
// Decision 045). Do not confuse them:
//
//   contentType          what the content IS            NEWS · LAW · FLYER · EXPERT
//   NewsItem.type        what KIND of news              deadline · policy-update
//   contentSource.type   ingestion format               manual · expert-session · faq
//   Sector               what kind of PRODUCER          government · community · first-step
//
// The registry field is called `sector` and NEVER `type`, precisely so it cannot
// be misread as describing the content.
//
// =============================================================================
// SECTION 2 — RECORDS REFERENCE, THE REGISTRY OWNS
// =============================================================================
// A record carries `id` and the things that are genuinely its own — this item's
// `url`, when it was `retrieved`, its ingestion `type`. It does NOT carry the
// producer's name.
//
// `resolveName()` fills that in at LOAD, from the repositories, which is the
// Normalize stage — the same place news.json's `headline` becomes `title`. Every
// downstream consumer then sees a resolved name without knowing the registry
// exists.
//
// THE PAYOFF IS VISIBLE ON THE PAGE: Latest Updates shows "Delaware Health and
// Social Services" for a record whose raw data said "Delaware DHSS". One producer,
// one identity. Two files holding the same producer name is the drift bug
// Decision 032 removed for category labels; this is that rule applied to
// provenance.
//
// =============================================================================
// SECTION 3 — THE FAILURE BOUNDARY
// =============================================================================
//
//     PROVENANCE RESOLUTION IS A CAPABILITY, NOT A VALIDITY GATE.
//
// An unknown contentSource.id means the item cannot participate in SECTOR-SCOPED
// VIEWS. It means nothing else.
//
// Such an item is still perfectly good CivicContent: it has a title, a summary, a
// category, a date. It is browsable, searchable, classifiable, and appears on its
// category and topic pages exactly as before. The only thing it cannot do is
// answer "which sector produced this?" — so it is absent only from the two views
// defined by that question.
//
// THIS CODEBASE ALREADY WORKED THIS WAY. Decision 036 established that content
// with a category but no `subcategory` is FULLY classified, not half classified —
// it simply cannot appear on a topic page, and the category page's updates feed
// reaches it instead. A missing optional dimension narrows WHERE content can
// appear; it never invalidates the content. Sector is the same shape of thing.
//
// -----------------------------------------------------------------------------
// SECTION 3b — WHY EXCLUDE-AND-LOG, NOT THROW-AT-STARTUP
// -----------------------------------------------------------------------------
// This is the decision most likely to be "fixed" later by someone who believes
// failing fast is always safer. Three arguments, the third decisive:
//
//   1. AVAILABILITY. A civic service must not go offline because one JSON record
//      has a typo. The people who pay for that outage are residents in
//      difficulty.
//
//   2. THE VALIDATOR IS THE REAL GATE. validate_content_sources.py blocks a bad
//      id at build time. This is defense in depth, and defense in depth must not
//      be the thing that breaks.
//
//   3. THROWING WOULD VIOLATE THE BOUNDARY ITSELF. Failing startup on an
//      unresolvable id makes provenance a GLOBAL VALIDITY REQUIREMENT for all
//      CivicContent — precisely what SECTION 3 forbids. Exclude-and-log is not a
//      weaker form of failing fast; it is the behaviour the architecture implies.
//
// Note the same reasoning makes a MISSING REGISTRY FILE non-fatal here, unlike
// TaxonomyService where a missing vocabulary IS fatal. Without the taxonomy
// nothing can be classified and every page is wrong; without this file every page
// except two keeps working.
//
// -----------------------------------------------------------------------------
// SECTION 3c — MAKING IT OBSERVABLE
// -----------------------------------------------------------------------------
// A single ERROR line in a boot log is easy to lose, and "excluded silently" is
// only acceptable if someone can see it. So there is a startup summary on
// ApplicationReadyEvent:
//
//     content-sources: 14 producers, all references resolved
//     content-sources: 14 producers, 1 UNRESOLVED reference(s) [de-department-of-education] …
//
// It runs on ApplicationReadyEvent rather than in the constructor because unknown
// ids are discovered as the REPOSITORIES resolve names during their own load — by
// the time the app is ready, every id referenced by content has been seen.
//
// DELIBERATELY NOT ON /api/health. That endpoint returns a bare "OK" and lives in
// ResourceController; reshaping a liveness probe to carry editorial diagnostics
// would give a resource controller a reason to know about content sources, and
// put provenance in the one endpoint that must stay trivial.
//
// VERIFIED LIVE, not assumed. A record was planted referencing
// "de-department-of-education": absent from both sector pages, STILL PRESENT on
// /api/category/housing and in /api/search, validator exit 1, summary naming it.
//
// =============================================================================
// SECTION 4 — RSS PROVENANCE IS CONFIGURATION
// =============================================================================
// A registry entry may carry `feedUrl`. RssFeedService reads its feed list from
// feedUrls() and stamps each fetched item with that entry's id.
//
// So a feed CANNOT BE ADDED WITHOUT DECLARING WHO PUBLISHES IT, and the item's
// provenance is known before the first byte is parsed. The previous design put
// URLs alone in `news.rss.urls` and let identity be guessed at parse time from
// feed.getTitle() — a value the upstream publisher can change at will.
//
// `news.rss.urls` was retired from application.properties. That is a
// DEPLOYMENT-VISIBLE change and is called out in Decision 045.
//
// =============================================================================
// APPENDIX — shared/model/Sector.java
// =============================================================================
//   GOVERNMENT   agencies, officials, programs        -> Latest Updates
//   COMMUNITY    churches, nonprofits, groups         -> Community Notices
//   FIRST_STEP   First Step's own editorial output    -> First Step Originals
//
// key()     -> "government" | "community" | "first-step"  (registry + URL spelling)
// fromKey() -> Optional, EMPTY for anything unrecognised.
//
// fromKey returning Optional rather than throwing or defaulting is the enum-level
// expression of SECTION 3: an unrecognised sector must never silently become one
// of the real ones. A misattributed notice is worse than a missing one.
// =============================================================================
