import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import ContentCard from '../../components/ContentCard/ContentCard';
import NoticeGallery from '../../components/NoticeGallery/NoticeGallery';
import type { CommunityNoticesPage as PageData, NoticeView } from '../../types/api';
import styles from './CommunityNoticesPage.module.css';

/** The four discovery views, in nav order. OVERVIEW is the page they sit on. */
const VIEWS: { key: string; view: NoticeView; icon: string }[] = [
  { key: 'events', view: 'EVENTS', icon: '📅' },
  { key: 'meetings', view: 'MEETINGS', icon: '🏛️' },
  { key: 'announcements', view: 'ANNOUNCEMENTS', icon: '📢' },
  { key: 'flyers', view: 'FLYERS', icon: '🖼️' },
];

/**
 * ONE page architecture, five routes.
 *
 * <pre>
 *   /community-notices                 overview — cards + a preview of each
 *   /community-notices/events          the same page, events view
 *   /community-notices/meetings        …
 *   /community-notices/announcements   …
 *   /community-notices/flyers          …
 * </pre>
 *
 * **THE URL IS THE SOURCE OF TRUTH.** The active view comes from `useParams`,
 * never from component state — the same pattern `CategoryPage` and `TopicPage`
 * already use. That is what makes every one of the five routes work when typed,
 * bookmarked, shared or reached with the browser's back button, without the
 * visitor having to pass through the landing route first.
 *
 * **Community-produced information is not the same thing as community
 * resources.** A resource is a service a resident can use; a notice is something
 * an organisation is telling the neighbourhood. Everything here is scoped
 * server-side to the community sector.
 *
 * **The four views are LENSES, not buckets.** A health-fair flyer carries kind
 * `event` and appears in both Events and Flyers, because "what is happening?" and
 * "what posters are up?" are different questions about the same item.
 *
 * **The landing route is a destination, not a redirect.** It answers "what kinds
 * of community information can I find here?" with counts and a real sample of
 * each, then the four views answer "show me only this kind."
 */
export default function CommunityNoticesPage() {
  const { view: viewParam } = useParams<{ view?: string }>();
  const { t } = useI18n();
  const [page, setPage] = useState<PageData | null>(null);
  const [error, setError] = useState<string | null>(null);

  const activeKey = viewParam ?? 'overview';

  useEffect(() => {
    setPage(null);
    setError(null);
    const path = viewParam ? `/api/community-notices/${viewParam}` : '/api/community-notices';
    apiGet<PageData>(path)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [viewParam]);

  const isOverview = activeKey === 'overview';

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        <header className={styles.header}>
          <h1 className={styles.title}>{t('notices.title')}</h1>
          <p className={styles.intro}>
            {isOverview ? t('notices.intro') : t(`notices.${activeKey}.intro`)}
          </p>
        </header>

        {/* The four cards render on EVERY route — they are the page's navigation,
            not the landing page's content. Counts ride on every response so they
            never fill in after the page has drawn. */}
        <nav className={styles.views} aria-label={t('notices.viewsLabel')}>
          {VIEWS.map((v) => (
            <Link
              key={v.key}
              to={`/community-notices/${v.key}`}
              className={activeKey === v.key ? `${styles.viewCard} ${styles.active}` : styles.viewCard}
              aria-current={activeKey === v.key ? 'page' : undefined}
            >
              <span className={styles.viewIcon} aria-hidden="true">
                {v.icon}
              </span>
              <span className={styles.viewLabel}>{t(`notices.${v.key}.label`)}</span>
              <span className={styles.viewCount}>{page?.counts?.[v.view] ?? '—'}</span>
            </Link>
          ))}
        </nav>

        {error && (
          <p className={styles.placeholder} role="alert">
            {error}
          </p>
        )}
        {!error && page === null && <p className={styles.placeholder}>{t('common.loading')}</p>}

        {/* OVERVIEW — a real sample of each view, so the landing route earns its
            place rather than routing onward. */}
        {page && isOverview && (
          <div className={styles.previews}>
            {page.previews.map((preview) => {
              const meta = VIEWS.find((v) => v.view === preview.view);
              if (!meta || preview.items.length === 0) {
                return null;
              }
              return (
                <section key={preview.view} className={styles.preview} aria-labelledby={`pv-${meta.key}`}>
                  <div className={styles.previewHead}>
                    <h2 id={`pv-${meta.key}`} className={styles.previewTitle}>
                      {t(`notices.${meta.key}.label`)}
                    </h2>
                    <Link className={styles.previewMore} to={`/community-notices/${meta.key}`}>
                      {t('notices.seeAll')} ({preview.count})
                    </Link>
                  </div>
                  {meta.view === 'FLYERS' ? (
                    <NoticeGallery items={preview.items} />
                  ) : (
                    <ul className={styles.list}>
                      {preview.items.map((item) => (
                        <ContentCard key={item.id} item={item} />
                      ))}
                    </ul>
                  )}
                </section>
              );
            })}
          </div>
        )}

        {/* A VIEW. Flyers gets a gallery because the image IS the content; the
            other three are the same generic card list used elsewhere. */}
        {page && !isOverview && page.items.length === 0 && (
          <p className={styles.placeholder}>{t(`notices.${activeKey}.empty`)}</p>
        )}
        {page && !isOverview && page.items.length > 0 && (
          activeKey === 'flyers' ? (
            <div className={styles.viewBody}>
              <NoticeGallery items={page.items} />
            </div>
          ) : (
            <ul className={`${styles.viewBody} ${styles.list}`}>
              {page.items.map((item) => (
                <ContentCard key={item.id} item={item} />
              ))}
            </ul>
          )
        )}
      </main>
      <SiteFooter />
    </>
  );
}
