import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, apiGet } from '../../api/client';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import ContentCollection from '../../components/ContentCollection/ContentCollection';
import { useI18n } from '../../i18n/I18nProvider';
import type { TopicPage as TopicPagePayload } from '../../types/api';
import styles from './TopicPage.module.css';

/** IM-05: the established Topic URL also reserves /help for the full Category help inventory. */
export default function TopicPage() {
  const { key, topic } = useParams<{ key: string; topic: string }>();
  const [search] = useSearchParams();
  const community = search.get('communityId');
  const query = community ? `?${new URLSearchParams({ communityId: community })}` : '';
  const allHelp = topic === 'help';
  const { t } = useI18n();
  const [page, setPage] = useState<TopicPagePayload | null>(null);
  const [error, setError] = useState<'missing' | 'network' | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    let active = true;
    setPage(null);
    setError(null);
    apiGet<TopicPagePayload>(`/api/category/${key}/${topic}${query}`)
      .then((result) => { if (active) setPage(result); })
      .catch((err: Error) => {
        if (active) setError(err instanceof ApiError && err.code === 'NOT_FOUND' ? 'missing' : 'network');
      });
    return () => { active = false; };
  }, [key, topic, query]);

  useEffect(() => { if (page || error) heading.current?.focus(); }, [page, error]);

  const allLabel = page ? `${t('help.all')} ${page.metadata.categoryLabel} ${t('help.help')}` : '';
  return (
    <>
      <SiteHeader />
      <main className="home-body">
        {error && (
          <section className="stub-page" role="alert">
            <h1 ref={heading} tabIndex={-1}>{t(error === 'missing' ? 'topic.notFound' : 'help.error')}</h1>
            <Link to={`/category/${key}${query}`} className="stub-back">{t('topic.backToCategory')}</Link>
          </section>
        )}
        {!error && !page && <p className="section-placeholder" role="status">{t('common.loading')}</p>}
        {!error && page && (
          <>
            <header className={styles.header}>
              <nav className={styles.crumbs} aria-label={t('topic.breadcrumb')}>
                <Link to={`/category/${page.metadata.categoryKey}${query}`} className={styles.crumb}>
                  {page.metadata.categoryLabel}
                </Link>
                {!allHelp && <> / <Link to={`/category/${page.metadata.categoryKey}/help${query}`} className={styles.crumb}>{allLabel}</Link></>}
              </nav>
              <h1 ref={heading} tabIndex={-1} className={styles.title}>{allHelp ? allLabel : page.metadata.name}</h1>
              <p className={styles.count}>{page.metadata.totalCount} {t(page.metadata.totalCount === 1 ? 'help.result.one' : 'help.results')}</p>
            </header>
            {allHelp && (page.topics?.length ?? 0) > 0 && (
              <nav className={styles.topics} aria-label={t('help.narrow')}>
                <details>
                  <summary>{t('help.narrow')}</summary>
                <ul>
                  {page.topics?.map((choice) => (
                    <li key={choice.slug}>
                      <Link to={`/category/${page.metadata.categoryKey}/${choice.slug}${query}`}>
                        {choice.name} ({choice.count})
                      </Link>
                    </li>
                  ))}
                </ul>
                </details>
              </nav>
            )}
            {page.items.length === 0 ? (
              <p className="section-placeholder" role="status">{t('help.empty')}</p>
            ) : (
              <ContentCollection key={`${key}/${topic}${query}`} collection={{
                key: 'help-browse', labelKey: 'collection.getHelp', layer: 'ACTION',
                count: page.metadata.totalCount, items: page.items,
              }} headingLevel={2} />
            )}
          </>
        )}
      </main>
    </>
  );
}
