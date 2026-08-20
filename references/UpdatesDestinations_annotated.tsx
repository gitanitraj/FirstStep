/* =============================================================================
 * ANNOTATED REFERENCE — the UPDATES DESTINATIONS (Slice I).
 * Groups the whole vertical into one learning file:
 *   backend  updates/dto/{UpdatesPage,UpdateGroup}.java
 *            updates/controller/UpdatesController.java
 *            updates/service/UpdatesService#getBySector, #getPage
 *   frontend pages/UpdatesPage/UpdatesPage.tsx
 *            components/UpdateGroup/UpdateGroup.tsx
 * See references/decisions.md Decision 045.
 * =============================================================================
 *
 * WHAT THIS IS
 *   Two destination pages that differ only in who published the content:
 *
 *     /updates            sector=government   Latest Updates
 *     /community-notices  sector=community    Community Notices
 *
 * WHY IT MATTERS ARCHITECTURALLY
 *   This is where Decision 041's amendment gets exercised for the first time:
 *   presentation grouping CivicContent by controlled metadata, without inventing
 *   a single domain concept to do it. See PART 3.
 * ============================================================================= */

// =============================================================================
// PART 1 — ONE ENDPOINT, ONE PAGE COMPONENT, TWO ROUTES
// =============================================================================
//
//   GET /api/updates/{sector}  ->  UpdatesPage { sector, totalCount, groups[] }
//
// Both pages have the SAME SHAPE, because the only thing separating them is the
// producer. A second DTO would have been the same fields under a different name,
// and a second React page would have been the same file with different copy.
//
// So the difference lives in a PARAMETER — and the copy is keyed off it:
//
//     t(`updates.${sector}.title`)   t(`updates.${sector}.intro`)
//
// That is not merely DRY. The distinction is the POINT of the pages: a church
// offering a free meal and a state agency changing SNAP eligibility are both
// "notices", and collapsing them would flatten the thing a resident most needs to
// know — who is telling me this, and what does that imply about it? Keeping the
// sector as data rather than as duplicated code keeps that idea in one place.
//
// An unrecognised sector is a 404, matching CategoryController: a sector that
// EXISTS and is empty and a sector that DOES NOT EXIST are different facts and
// must not look alike.

export default function UpdatesPage({ sector }: { sector: 'government' | 'community' }) {
  const { t } = useI18n();
  const [page, setPage] = useState<UpdatesPageData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setPage(null);                  // clears between sectors — see PART 5
    setError(null);
    apiGet<UpdatesPageData>(`/api/updates/${sector}`).then(setPage).catch(e => setError(e.message));
  }, [sector]);

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        <header className={styles.header}>
          <h1>{t(`updates.${sector}.title`)}</h1>
          <p>{t(`updates.${sector}.intro`)}</p>
          {page && <p>{page.totalCount} {t(page.totalCount === 1 ? 'updates.item.one' : 'updates.item.plural')}</p>}
        </header>
        …loading / error / empty…
        {page && page.groups.length > 0 && (
          <div className={styles.groups}>
            {page.groups.map(group => <UpdateGroup key={group.contentType} group={group} />)}
          </div>
        )}
      </main>
      <SiteFooter />
    </>
  );
}

// =============================================================================
// PART 2 — SECTOR FOLLOWS THE PRODUCER (UpdatesService#getBySector)
// =============================================================================
// The service walks the same four sources it always has and asks ONE question of
// each item: is this producer in this sector?
//
//     contentSources.isInSector(item.contentSource, sector)
//
// It never branches on contentType. Wilmington Housing Authority publishes both a
// news item and a flyer and both are government — the counter-example that made
// a "flyers are community" rule impossible. VERIFIED ON LIVE DATA rather than
// fixtures, in both directions (Ministry of Caring's news AND flyer are both
// community).
//
// Unresolvable ids are excluded, never guessed: isInSector returns false for
// EVERY sector, so such an item appears on neither page while remaining valid
// CivicContent everywhere else (the failure boundary — see
// ContentSourceService_annotated.java SECTION 3).
//
// No cap. This is a destination, not the front door's teaser.

// =============================================================================
// PART 3 — THE GROUPING, AND THE CONSTRAINTS THAT MAKE IT LEGITIMATE
// =============================================================================
// Decision 041 said presentation may not create domain concepts to satisfy a
// display need. Latest Updates exposed a case that rule was too broad to express,
// and Decision 045 amended it:
//
//     Presentation may GROUP existing CivicContent by controlled metadata when
//     that grouping represents a meaningful user-facing discovery model — using
//     GENERIC components, never one per metadata value, and never rendering an
//     empty group.
//
// The distinction that makes it safe: grouping READS metadata the domain already
// owns. It adds no type, no field, no class. The violation would not be the
// grouping — it would be a `LawGroup` component beside a `NewsGroup` component,
// enumerating the metadata in code.
//
// FOUR CONSTRAINTS, AND WHERE EACH IS ENFORCED:
//
//   1. contentType stays existing domain metadata     — nothing new was added
//   2. ONE generic component renders every group      — UpdateGroup.tsx, below
//   3. groups are generated, never enumerated         — UpdatesService#getPage
//   4. empty groups are never rendered                — guaranteed SERVER-SIDE
//
// (3) and (4) are the same line of code. getPage walks the ITEMS and creates a
// group the first time it meets a type:
//
//     for (UpdateItem item : items)
//         byType.computeIfAbsent(item.contentType(), t -> new ArrayList<>()).add(item);
//
// so a sector with no laws produces no LAW group. The frontend therefore has NO
// GUARD for empty groups — it cannot receive one. That is worth preserving: a
// rule enforced by the shape of the data cannot be forgotten by the next person
// to touch the component.
//
// VISIBLE IN PRODUCTION: Community Notices has no Laws group at all, because
// laws only come from the legislature, which is government.
//
// Group ORDER follows the ContentType enum's declaration order — stable, so a
// page does not reshuffle itself when the newest item changes type. Item order
// WITHIN a group stays reverse-chronological, which is the ordering that carries
// meaning.

export default function UpdateGroup({ group }: { group: UpdateGroupData }) {
  const { t } = useI18n();
  const label = t(`${CONTENT_TYPE_LABEL[group.contentType]}.plural`);
  return (
    <section className={styles.group} aria-labelledby={`group-${group.contentType}`}>
      <div className={styles.head}>
        <h2 id={`group-${group.contentType}`} className={styles.title}>{label}</h2>
        <span className={styles.count}>{group.count}</span>
      </div>
      <ul className={styles.list}>{/* …one <li> per item… */}</ul>
    </section>
  );
}

// -----------------------------------------------------------------------------
// PART 3b — THE LABEL, AND A SMALL TYPOGRAPHY TRAP
// -----------------------------------------------------------------------------
// The heading reuses CONTENT_TYPE_LABEL — the exhaustive Record<ContentType, …>
// that FAILS THE BUILD if a type has no label, so a new ContentType cannot reach
// a resident as `undefined`.
//
// Those values are LOWERCASE ("news items", "laws") because they were written for
// mid-sentence counts: "12 laws". As a heading that reads wrong, so the CSS
// capitalises with `.title::first-letter`.
//
// NOT `text-transform: capitalize` — that would render the Spanish
// "respuestas de expertos" as "Respuestas De Expertos". The tests assert the
// ACCESSIBLE name, which CSS does not change, so they expect lowercase.
//
// `count` comes from the server rather than `items.length` so a future capped
// group cannot make the heading disagree with the list beneath it.

// =============================================================================
// PART 4 — WHAT WAS DELETED HERE: UpdateItem.type
// =============================================================================
// Decision 036's exit criterion, closed by Decision 045. Not deprecated, not
// unused — ABSENT from the model, services, serialized payload, Java fixtures,
// frontend types, components and tests.
//
// It reported "news" for BOTH curated news and signed legislation, so a resident
// could not tell a change in the law from an announcement. contentType is now the
// sole semantic identifier in the updates pipeline.
//
// ONE TEST LOST ITS POINT, worth recording: CategoryUpdates.test used to render a
// RESOURCE carrying `type: 'news'` — a decoy proving the badge read the right
// field. That disagreement is NO LONGER EXPRESSIBLE, because there is one
// identifier. The test kept its name and lost its decoy.
//
// Three lookalike fields survive and are NOT this one: NewsItem.type,
// contentSource.type, SearchResult.type.

// =============================================================================
// PART 5 — SMALL THINGS THAT ARE DELIBERATE
// =============================================================================
// - `setPage(null)` at the top of the effect. Both routes render the SAME
//   component instance, so navigating between them re-runs the effect without
//   unmounting; without this the new sector briefly shows the old sector's
//   groups.
// - `key={group.contentType}` — stable across refetches, unlike an index.
// - Layout mirrors the homepage (contained at --page-max, inset by
//   --page-gutter) so a resident moving between them does not feel the page
//   change shape underneath them.
// - UpdateGroup's high-contrast block exists for the Decision 039 specificity
//   trap: `body.high-contrast a` (0,1,2) beats a CSS Module class (0,1,0), so
//   without an explicit rule every linked title turns yellow and the hover state
//   collapses.
// =============================================================================
