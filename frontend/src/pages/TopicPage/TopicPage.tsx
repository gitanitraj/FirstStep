import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import ContentCard from '../../components/ContentCard/ContentCard';
import { useI18n } from '../../i18n/I18nProvider';
import type { TopicPage as TopicPagePayload } from '../../types/api';
import styles from './TopicPage.module.css';

/**
 * The fourth level of the navigation hierarchy (Slice F6):
 * Category → topic group → topic → <b>CivicContent</b>. This is where the
 * content itself is finally listed.
 *
 * Deliberately simpler than CategoryPage. A category page answers three
 * questions and needs three sections; a topic page answers one — "what is
 * available under this topic?" — so it is a breadcrumb, a header and a list.
 *
 * Only resources and flyers appear, because they are the only content types
 * carrying a subcategory. That is the same fact that made the category page an
 * aggregate, seen from the other side.
 */
export default function TopicPage() {
  const { key, topic } = useParams<{ key: string; topic: string }>();
  const { t } = useI18n();
  const [page, setPage] = useState<TopicPagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setPage(null);
    setError(null);
    apiGet<TopicPagePayload>(`/api/category/${key}/${topic}`)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [key, topic]);

  return (
    <>
      <SiteHeader />
      <main className="home-body">
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
