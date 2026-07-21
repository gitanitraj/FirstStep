/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/ResourceDiscovery.tsx (Slice D)
 * (+ note on HomePage's role as the /api/home fetcher.)
 * See references/decisions.md Decision 023. Keep in sync with the sources.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage's primary navigation area: two columns — curated Organizations
 *   (left) and resource Categories (right) — separated by a subtle divider. It's
 *   a PURE display component: it takes organizations + categories as PROPS (from
 *   GET /api/home) and renders two lists of <Link>s. All aggregation/ranking
 *   happened server-side (OrganizationService); the browser stitches nothing.
 *
 * WHERE THE DATA COMES FROM (HomePage, Slice D)
 *   Slice A removed MainContent (the old /api/home fetcher), so HomePage now owns
 *   the single BFF request: `apiGet<HomePayload>('/api/home')` on mount, then
 *   passes `home.organizations` + `home.categories` (+ error) down here. The
 *   frame (Utility Bar + Hero) renders instantly; only this section waits on the
 *   payload. The Laws feature (C) and Community carousel (E) will read the same
 *   payload later.
 *
 * NAVIGATION
 *   Each org links to /organization/{slug}, each category to /category/{key}.
 *   Those routes render StubPage for now — real Organization pages are Slice G,
 *   Category pages Slice F.
 * ============================================================================= */

import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';
import type { CategorySummary, OrgSummary } from '../types/api';

interface Props {
  organizations: OrgSummary[] | null; // null = not loaded yet
  categories: CategorySummary[] | null;
  error: string | null;
}

export default function ResourceDiscovery({ organizations, categories, error }: Props) {
  const { t } = useI18n();
  // Loading = no error AND nothing arrived yet. (Section titles are translated;
  // org/category NAMES come from data and are not translated.)
  const loading = !error && organizations === null && categories === null;

  return (
    <section className="discovery" aria-labelledby="discovery-title">
      {/* Visually-hidden section label for the landmark; the visible column
          headings below carry the on-screen structure. */}
      <h2 id="discovery-title" className="visually-hidden">
        {t('section.categories')}
      </h2>

      {error && <p role="alert">Error: {error}</p>}
      {loading && <p className="section-placeholder">{t('common.comingSoon')}</p>}

      {!error && !loading && (
        <div className="discovery-columns">
          {/* LEFT — curated organizations */}
          <div className="discovery-col discovery-orgs">
            <h3 className="discovery-col-title">{t('section.organizations')}</h3>
            <ul className="discovery-list">
              {organizations?.map((org) => (
                <li key={org.slug}>
                  <Link className="discovery-item" to={`/organization/${org.slug}`}>
                    <span className="discovery-item-name">{org.name}</span>
                    <span className="discovery-item-count">{org.resourceCount}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="discovery-divider" aria-hidden="true" />

          {/* RIGHT — resource categories (the existing 10-category taxonomy) */}
          <div className="discovery-col discovery-categories">
            <h3 className="discovery-col-title">{t('section.categories')}</h3>
            <ul className="discovery-list">
              {categories?.map((cat) => (
                <li key={cat.key}>
                  <Link className="discovery-item" to={`/category/${cat.key}`}>
                    <span className="discovery-item-icon" aria-hidden="true">{cat.icon}</span>
                    <span className="discovery-item-name">{cat.label}</span>
                    <span className="discovery-item-count">{cat.resourceCount}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </section>
  );
}
