/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/pages/TopicPage/TopicPage.tsx
 * Slice F6. See references/decisions.md Decision 040.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS PAGE IS
 *   The fourth level of the navigation hierarchy, and the end of it:
 *   Category -> topic group -> topic -> CivicContent. The content is finally
 *   listed here.
 *
 * WHY IT IS SIMPLER THAN CategoryPage
 *   A category page answers THREE questions (Discover / Connect / Stay Informed)
 *   and needs three sections. A topic page answers ONE — "what is available under
 *   this topic?" — so it is a breadcrumb, a header and a list. Resisting the urge
 *   to give it sections it does not need is the point.
 *
 *   Only resources and flyers appear, because they are the only content types
 *   carrying a subcategory. Same fact that made the category page an aggregate,
 *   from the other side.
 * ============================================================================= */

import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import UtilityBar from '../../components/UtilityBar';
import SiteHero from '../../components/SiteHero';
import ContentCard from '../../components/ContentCard/ContentCard';
import { useI18n } from '../../i18n/I18nProvider';
import type { TopicPage as TopicPagePayload } from '../../types/api';
import styles from './TopicPage.module.css';

export default function TopicPage() {
  const { key, topic } = useParams<{ key: string; topic: string }>();
  const { t } = useI18n();
  const [page, setPage] = useState<TopicPagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Reset on param change so navigating between sibling topics never shows the
    // previous topic's items under the new heading for a frame.
    setPage(null);
    setError(null);
    apiGet<TopicPagePayload>(`/api/category/${key}/${topic}`)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [key, topic]);

  return (
    <>
      {/* Frame first, so the page is never blank while the payload loads. */}
      <UtilityBar />
      <SiteHero />
      <main className="home-body">
        {/* An unknown topic 404s. Send the resident UP one level rather than home:
            they were browsing a category and a bad topic slug should not cost
            them that context. */}
        {error && (
          <section className="stub-page" role="alert">
            <h2>{t('topic.notFound')}</h2>
            <Link to={`/category/${key}`} className="stub-back">
              {t('topic.backToCategory')}
            </Link>
          </section>
        )}

        {!error && !page && <p className="section-placeholder">{t('common.comingSoon')}</p>}

        {!error && page && (
          <>
            <header className={styles.header}>
              {/* Breadcrumb. The payload carries the category's LABEL so this
                  renders "Housing" rather than the "housing" key from the URL,
                  without a second request. */}
              <nav className={styles.crumbs} aria-label={t('topic.breadcrumb')}>
                <Link to={`/category/${page.metadata.categoryKey}`} className={styles.crumb}>
                  <span aria-hidden="true">{page.metadata.categoryIcon}</span>{' '}
                  {page.metadata.categoryLabel}
                </Link>
                <span aria-hidden="true"> › </span>
                <span>{page.metadata.name}</span>
              </nav>
              <h1 className={styles.title}>{page.metadata.name}</h1>
              <p className={styles.count}>
                {page.metadata.totalCount}{' '}
                {t(page.metadata.totalCount === 1 ? 'topic.item.one' : 'topic.item.plural')}
              </p>
            </header>

            {page.items.length === 0 ? (
              // A declared topic with nothing in it is a real state, not an
              // error — validate_navigation.py exists to surface exactly this.
              <p className="section-placeholder">{t('topic.empty')}</p>
            ) : (
              <ul className={styles.list}>
                {page.items.map((item) => (
                  <ContentCard key={`${item.contentType}-${item.id}`} item={item} />
                ))}
              </ul>
            )}
          </>
        )}
      </main>
    </>
  );
}

// =============================================================================
// THE BREADCRUMB IS A BFF DECISION, NOT A UI FLOURISH
// =============================================================================
// The client knows the URL it asked for, but "housing" is a KEY, not a display
// name. Without categoryLabel in the payload this page would either render the
// raw key ("housing › Emergency Shelter") or fetch the category page just to
// learn its label — a second request for one string.
//
// TopicMetadata therefore carries categoryKey + categoryLabel + categoryIcon.
// That is the BFF principle applied to navigation: the server sends the page
// everything the page renders.
//
// A TEST TRAP WORTH REMEMBERING: the frame also renders a "Housing Assistance"
// primary-nav link, so an unscoped getByRole('link', {name: /Housing/}) matches
// THAT one first — it failed exactly this way before being scoped to the
// breadcrumb landmark. The same mistake then bit the live browser probe. When a
// page reuses a word the frame also uses, scope the query to the landmark.
// =============================================================================

// =============================================================================
// CSS MODULES, SECOND APPLICATION
// =============================================================================
// Co-located as pages/TopicPage/{TopicPage.tsx, TopicPage.module.css} per
// Decision 039. Class names are short and semantic (.header, .crumbs, .title,
// .list) because the filename already namespaces them.
//
// NOTE WHAT IS STILL GLOBAL HERE: `home-body`, `stub-page`, `stub-back` and
// `section-placeholder`. Those are legacy globals in the index.css quarantine,
// shared across several components, and Phase 3's rule is that existing styles
// move when a slice touches THEM — not when a new page happens to use them.
// Migrating them here would have meant either duplicating them into this module
// or refactoring four other components, neither of which belongs in F6.
//
// F6 added ZERO rules to index.css. The ratchet held at 91 selectors.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
//   GET /api/category/{key}/{topic}
//     -> TopicPageService (taxonomy lookup + editorial filter + normalize)
//   TopicPage (this file)  owns the request, params, loading and error state
//     -> ContentCard       props only, one per item
//
// Reached from CategoryBrowse's topic links, which have pointed at this route
// since F5b — they resolved to a StubPage until this slice replaced it.
// =============================================================================
