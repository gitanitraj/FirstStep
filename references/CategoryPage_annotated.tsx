/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/pages/CategoryPage.tsx
 * Slice F5b. See references/decisions.md Decision 037 (aggregate = 036).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS PAGE IS
 *   The category landing page at /category/:key, rendering the three-pillar
 *   aggregate built in F5a. It replaced a StubPage.
 *
 * THE THREE PILLARS, in the order a resident needs them
 *   Stay Informed  what has changed    -> CategoryUpdates
 *   Discover       what is available   -> CategoryBrowse
 *   Connect        who to contact      -> organizations (inline below)
 *
 * ONE REQUEST, AND THE PAGE ONLY DISPLAYS. Every count, filter and sort already
 * happened server-side. The browser never fetches all resources and filters them
 * client-side — that is the pattern the BFF principle removed from the old
 * app.js (Decisions 019/020), and a category page is exactly where it would
 * otherwise creep back in.
 * ============================================================================= */

import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import CategoryBrowse from '../../components/CategoryBrowse';
import ContentCollection from '../../components/ContentCollection/ContentCollection';
import { useI18n } from '../../i18n/I18nProvider';
import type { CategoryPage as CategoryPagePayload, ContentType } from '../../types/api';
import styles from './CategoryPage.module.css';

/**
 * The category destination (Decision 051) at /category/:key.
 *
 * CATEGORY IS THE SCOPE. DESTINATION IS THE EXPERIENCE. The three pillars this
 * page shipped with — Discover, Connect, Stay Informed — correctly described what
 * the page KNOWS and poorly described what a resident WANTS. Nobody arrives at
 * Housing thinking "I would like to Discover"; they think "I need help paying my
 * rent." The sequence below answers resident jobs instead:
 *
 *   orientation      what is this?              header
 *   topic discovery  where do I need help?      CategoryBrowse — NAVIGATES
 *   get help         help me act                ACTION collections
 *   what to know     help me understand         INFORMATION + SUPPORTING collections
 *   organizations    who is connected to these? conditional, secondary
 *
 * THE PAGE COMPOSES; THE PAYLOAD PROJECTS. Collections arrive already selected,
 * ordered and counted, so this file decides placement and nothing else. Order
 * comes from the payload — it is the destination composition, declared server-side
 * — and this page groups by layer rather than reordering.
 *
 * ONE request, and the page only displays. Every count, filter and sort already
 * happened server-side; the browser never fetches all resources and filters them.
 *
 * `page.updates` is deliberately NOT rendered. Policy Info and Law present that
 * same content as purposeful collections. The field keeps its semantics for other
 * consumers — a pillar is retired by a deliberate act, never by leaving a silent
 * duplicate on the page (Decision 051).
 */
const TYPE_ORDER: ContentType[] = ['RESOURCE', 'LAW', 'NEWS', 'EXPERT', 'FLYER'];

export default function CategoryPage() {
  const { key } = useParams<{ key: string }>();
  const { t } = useI18n();
  const [page, setPage] = useState<CategoryPagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Reset on key change so navigating between categories doesn't briefly show
    // the previous category's content under the new one's name.
    setPage(null);
    setError(null);
    apiGet<CategoryPagePayload>(`/api/category/${key}`)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [key]);

  const action = page?.collections.filter((c) => c.layer === 'ACTION') ?? [];
  const information = page?.collections.filter((c) => c.layer === 'INFORMATION') ?? [];
  const supporting = page?.collections.filter((c) => c.layer === 'SUPPORTING') ?? [];

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        {/* An unknown key 404s from the API. Show a way out rather than the raw
            "Category not found: x" — a typo'd URL is a user event, not an error. */}
        {error && (
          <section className="stub-page" role="alert">
            <h2>{t('category.notFound')}</h2>
            <Link to="/" className="stub-back">
              {t('stub.back')}
            </Link>
          </section>
        )}

        {!error && !page && <p className="section-placeholder">{t('common.loading')}</p>}

        {!error && page && (
          <>
            <header className={styles.header}>
              <nav className={styles.crumbs} aria-label={t('topic.breadcrumb')}>
                <Link className={styles.crumb} to="/">
                  {t('stub.back')}
                </Link>
              </nav>
              <h1 className={styles.title}>
                <span className={styles.icon} aria-hidden="true">
                  {page.metadata.icon}
                </span>
                {page.metadata.label}
              </h1>
              {/* The per-type breakdown is what makes the halves legible together:
                  a category with 44 resources and 20 laws is telling a resident
                  that acting and understanding find different things. */}
              <p className={styles.counts}>
                {TYPE_ORDER.filter((type) => page.metadata.countsByType[type]).map((type, i) => {
                  const n = page.metadata.countsByType[type] as number;
                  // Separate singular/plural keys rather than appending "s" —
                  // "1 flyers" is wrong in English and no suffix rule survives
                  // Spanish ("ley" → "leyes").
                  const suffix = n === 1 ? 'one' : 'plural';
                  return (
                    <span key={type}>
                      {i > 0 && <span aria-hidden="true"> · </span>}
                      {n} {t(`contentType.${type.toLowerCase()}.${suffix}`)}
                    </span>
                  );
                })}
              </p>
            </header>

            <CategoryBrowse
              categoryKey={page.metadata.key}
              groups={page.groups}
              topics={page.topics}
            />

            {/* Layer 1 — a PEER of What to Know, not a child of topic discovery.
                It carries no wrapping section of its own, so the collection's own
                heading must sit at h2 for the document outline to say so. */}
            {action.map((collection) => (
              <ContentCollection key={collection.key} collection={collection} headingLevel={2} />
            ))}

            {/* Layer 2. The wrapper earns its heading because it introduces
                several collections that answer one question together. */}
            {information.length + supporting.length > 0 && (
              <section className={styles.whatToKnow} aria-labelledby="what-to-know-title">
                <h2 id="what-to-know-title" className={styles.sectionTitle}>
                  {t('destination.whatToKnow')}
                </h2>
                <div className={styles.collections}>
                  {information.map((collection) => (
                    <ContentCollection
                      key={collection.key}
                      collection={collection}
                      headingLevel={3}
                    />
                  ))}
                </div>
                {/* Supporting content, SUBORDINATE to the primary collections
                    rather than beside them. The nested section gives them a
                    parent heading so they are h4s under it, instead of reading
                    as peers of Policy Info and Law.

                    The heading is visually hidden for now: the architecture
                    requires the RELATIONSHIP to be expressed semantically, while
                    the visible label for this group is a Pass 2 decision. Pass 2
                    can reveal it without changing any structure. */}
                {supporting.length > 0 && (
                  <section className={styles.supporting} aria-labelledby="supporting-title">
                    <h3 id="supporting-title" className="visually-hidden">
                      {t('destination.supporting')}
                    </h3>
                    {supporting.map((collection) => (
                      <ContentCollection
                        key={collection.key}
                        collection={collection}
                        headingLevel={4}
                      />
                    ))}
                  </section>
                )}
              </section>
            )}

            {/* LAYER 3 IS DEFERRED FOR THIS PAGE, AND THAT IS WHY NOTHING
                RENDERS HERE (Decision 051).

                `page.organizations` is still in the payload and
                OrganizationService is untouched — the capability is preserved,
                only its category-page PRESENTATION is deferred. The registry
                cannot yet express the relationship it would show: 37 distinct
                organizations across 44 Housing resources, essentially 1:1, with
                program-shaped names, and `resource.title = resource.organization`
                means a row here renders the same words as its Get Help card.

                Layer 3 returns when the organization/resource relationship
                distinguishes who PROVIDES from what is OFFERED. Organizations
                remain reachable at /organization/:slug and through Find Help. */}
          </>
        )}
      </main>
      <SiteFooter />
    </>
  );
}

// =============================================================================
// WHY ORGANIZATIONS RENDER INLINE BUT UPDATES AND BROWSE ARE COMPONENTS
// =============================================================================
// Two extracted components, not three. The split is by whether there is real
// rendering LOGIC, not by counting sections:
//
//   CategoryUpdates  owns a contentType -> label map and the link-out rule
//   CategoryBrowse   owns the grouped/flat branch and the empty-topics case
//   organizations    is a list of links, ~12 lines, reusing .discovery-item
//
// A third component would be an abstraction over a single use with no logic
// inside it — "no abstractions for single-use code". Reusing the homepage's
// .discovery-list / .discovery-item classes is also deliberate: an organization
// row on the homepage and on a category page are the same affordance, and
// sharing the CSS keeps them from drifting apart.
//
// The empty guard matters: `utilities` has zero resources, so it has zero
// organizations, and an "Organizations" heading over an empty list would look
// broken rather than informative.
// =============================================================================

// =============================================================================
// 404 HANDLING — A TYPO IS A USER EVENT, NOT AN ERROR
// =============================================================================
// GET /api/category/nope returns 404 with errorMessage "Category not found:
// nope", which apiGet throws. Rendering that string would leak backend phrasing
// and an internal key into the UI, and give the resident nothing to do.
//
// Instead: a translated line and a link home, inside role="alert" so assistive
// tech announces it. The test asserts BOTH that the friendly message appears and
// that the raw backend message does NOT.
//
// This is the client half of the endpoint's own rule — never let "nothing here"
// and "no such thing" look alike. The server refuses to invent an empty page for
// an unknown key; the client refuses to render a broken-looking one.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
//   GET /api/category/{key}
//     -> CategoryPageService (backend aggregate)
//        -> NavigationService + UpdatesService + OrganizationService
//   CategoryPage (this file)   owns the request, params, loading and error state
//     -> CategoryUpdates       props only, no fetching
//     -> CategoryBrowse        props only, no fetching
//     -> organizations inline
//
// Routing: App.tsx maps /category/:key here, and ALSO declares
// /category/:key/:topic -> StubPage so the topic links this page renders resolve
// to "Coming soon" instead of falling through to the not-found route. That is
// the precedent Slice D set when it linked categories to a declared stub; F6
// replaces the stub with the real topic page.
//
// SpaWebConfig's depth-agnostic fallback already serves these two-segment deep
// links — verified by hard-reloading /app-next/category/housing.
// =============================================================================

// =============================================================================
// DECISION 051 — WHY THE PILLARS WENT AND WHAT REPLACED THEM
// =============================================================================
// The pillars correctly described what the page KNOWS and poorly described what
// a resident WANTS. Nobody arrives at Housing thinking "I would like to
// Discover." They think "I need help paying my rent."
//
// The page groups collections BY LAYER but never reorders them: order IS the
// destination composition, declared server-side. This file decides placement and
// nothing else, which keeps composition in one place instead of two.
//
// `page.updates` is deliberately not rendered — see CategoryPageService's mirror
// for why the field nonetheless stays in the payload.
//
// CategoryUpdates is now rendered by nothing. It is NOT deleted: "do not delete
// existing aggregate capabilities simply because the visual treatment changes",
// and an orphaned component is reported rather than removed on the way past.
//
// THE CSS RATCHET: this migration moved .category-header, .category-title,
// .category-icon and .category-counts out of the index.css quarantine, taking it
// from 50 selectors to 47. .category-section and the .category-update family
// stay — CategoryBrowse and CategoryUpdates still use them, and a component
// migrates when a slice touches it, not when a neighbour does.
// =============================================================================

// =============================================================================
// THREE PASS 1 IMPLEMENTATION CORRECTIONS (audit follow-up)
// =============================================================================
// An audit of the RENDERED structure against the settled hierarchy found three
// discrepancies. None reopened the architecture; all three were implementation
// drift away from decisions already taken.
//
// 1. ORGANIZATIONS RENDERED THOUGH LAYER 3 IS DEFERRED. The section was carried
//    over from the pre-destination page and never reconciled against the
//    deferral recorded in Decision 051. Removed. The capability is untouched —
//    `page.organizations` still arrives, OrganizationService is unchanged, and
//    organizations remain reachable at /organization/:slug and through Find
//    Help. Only the category-page PRESENTATION is deferred, which is what the
//    decision actually said.
//
//    Its CSS was deleted with it rather than left behind. Dead CSS outliving its
//    component is how a later component silently inherits styles it never asked
//    for — the exact bug class that produced the index.css quarantine rule.
//
// 2. GET HELP WAS AN ORPHANED H3. It is a PEER of What to Know, not a child of
//    topic discovery, so it now renders at h2 via the collection component's
//    headingLevel prop.
//
// 3. SUPPORTING CONTENT READ AS PEER CONTENT. FAQs and Expert Q&A were h3
//    siblings of Policy Info and Law. They now sit at h4 inside a nested section
//    with its own h3, so the document outline states the subordination the
//    payload's `layer` field already encodes.
//
//    That h3 is VISUALLY HIDDEN on purpose. The architecture requires the
//    RELATIONSHIP to be expressed semantically now; the visible label for the
//    group is a Pass 2 decision. Pass 2 can reveal it by deleting one class,
//    with no structural change.
// =============================================================================
