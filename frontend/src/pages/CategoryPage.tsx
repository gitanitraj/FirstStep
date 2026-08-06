import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../api/client';
import UtilityBar from '../components/UtilityBar';
import SiteHero from '../components/SiteHero';
import CategoryUpdates from '../components/CategoryUpdates';
import CategoryBrowse from '../components/CategoryBrowse';
import { useI18n } from '../i18n/I18nProvider';
import type { CategoryPage as CategoryPagePayload, ContentType } from '../types/api';

/**
 * A category landing page (Slice F5b), rendering the GET /api/category/{key}
 * aggregate built in F5a. Three pillars, in the order a resident needs them:
 *
 *   Stay Informed  what has changed      → CategoryUpdates
 *   Discover       what is available     → CategoryBrowse
 *   Connect        who to contact        → organizations (below)
 *
 * ONE request, and the page only displays. Every count, filter and sort already
 * happened server-side — the browser never fetches all resources and filters
 * them, which is the client-side business logic the BFF principle removed.
 *
 * The frame (Utility Bar + Hero) renders immediately so the page is never blank
 * while the payload loads, matching HomePage and StubPage.
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

  return (
    <>
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
