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
import { apiGet } from '../api/client';
import UtilityBar from '../components/UtilityBar';
import SiteHero from '../components/SiteHero';
import CategoryUpdates from '../components/CategoryUpdates';
import CategoryBrowse from '../components/CategoryBrowse';
import { useI18n } from '../i18n/I18nProvider';
import type { CategoryPage as CategoryPagePayload, ContentType } from '../types/api';

// Display order for the count line. NOT the enum's declaration order: resources
// lead because they are what most residents came for, and the rest follow by how
// much they typically contribute. Enum order stays stable for the API's sake
// (it drives EnumMap iteration in countsByType); presentation order lives here.
const TYPE_ORDER: ContentType[] = ['RESOURCE', 'LAW', 'NEWS', 'EXPERT', 'FLYER'];

export default function CategoryPage() {
  const { key } = useParams<{ key: string }>();
  const { t } = useI18n();
  const [page, setPage] = useState<CategoryPagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Reset on key change so navigating between categories doesn't briefly show
    // the previous category's content under the new one's name. React reuses the
    // component instance across param changes, so without this the stale payload
    // would render for one frame under the new heading.
    setPage(null);
    setError(null);
    apiGet<CategoryPagePayload>(`/api/category/${key}`)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [key]);

  return (
    <>
      {/* Frame first, so the page is never blank while the payload loads —
          same order as HomePage and StubPage. */}
      <UtilityBar />
      <SiteHero />
      <main className="home-body">
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

        {!error && !page && <p className="section-placeholder">{t('common.comingSoon')}</p>}

        {!error && page && (
          <>
            <header className="category-header">
              <h1 className="category-title">
                <span className="category-icon" aria-hidden="true">
                  {page.metadata.icon}
                </span>
                {page.metadata.label}
              </h1>
              {/* The per-type breakdown is what makes the two halves legible
                  together: a category with 44 resources and 20 laws is telling a
                  resident that browsing and staying informed find different things. */}
              <p className="category-counts">
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

            <CategoryUpdates updates={page.updates} lastUpdated={page.metadata.lastUpdated} />

            <CategoryBrowse
              categoryKey={page.metadata.key}
              groups={page.groups}
              topics={page.topics}
            />

            {page.organizations.length > 0 && (
              <section
                className="category-section category-orgs"
                aria-labelledby="category-orgs-title"
              >
                <h2 id="category-orgs-title" className="category-section-title">
                  {t('section.organizations')}
                </h2>
                <ul className="discovery-list">
                  {page.organizations.map((org) => (
                    <li key={org.slug}>
                      <Link className="discovery-item" to={`/organization/${org.slug}`}>
                        <span className="discovery-item-name">{org.name}</span>
                        <span className="discovery-item-count">{org.resourceCount}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </main>
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
