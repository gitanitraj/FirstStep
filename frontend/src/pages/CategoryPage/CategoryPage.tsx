import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
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
  const [search] = useSearchParams();
  const community = search.get('communityId');
  const query = community ? `?${new URLSearchParams({ communityId: community })}` : '';
  const [page, setPage] = useState<CategoryPagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Reset on key change so navigating between categories doesn't briefly show
    // the previous category's content under the new one's name.
    let active = true;
    setPage(null);
    setError(null);
    apiGet<CategoryPagePayload>(`/api/category/${key}${query}`)
      .then((result) => { if (active) setPage(result); })
      .catch((err: Error) => { if (active) setError(err.message); });
    return () => { active = false; };
  }, [key, query]);

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

            <Link className="discovery-item" to={`/category/${page.metadata.key}/help${query}`}>
              {t('help.all')} {page.metadata.label} {t('help.help')}
            </Link>
            <CategoryBrowse
              categoryKey={page.metadata.key}
              query={query}
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
