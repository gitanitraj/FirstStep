import { useEffect, useState } from 'react';
import { apiGet } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import UpdateGroup from '../../components/UpdateGroup/UpdateGroup';
import type { UpdatesPage as UpdatesPageData } from '../../types/api';
import styles from './UpdatesPage.module.css';

interface Props {
  /** Registry sector key — `government` or `community`. */
  sector: 'government' | 'community';
}

/**
 * ONE page component serving BOTH updates destinations:
 *
 * <pre>
 *   /updates            sector="government"   Latest Updates
 *   /community-notices  sector="community"    Community Notices
 * </pre>
 *
 * They differ only in **who published the content** — the shape is identical, so
 * a second component would have been the same file with different copy. The
 * copy is keyed off the sector.
 *
 * The distinction is the point of the pages, not an implementation detail: a
 * church offering a free meal and a state agency changing SNAP eligibility are
 * both "notices", and collapsing them would flatten the thing a resident most
 * needs to know — who is telling me this, and what does that imply about it?
 *
 * **Empty groups cannot appear here.** The server never builds one, so this
 * component has no guard for it (Decision 045).
 */
export default function UpdatesPage({ sector }: Props) {
  const { t } = useI18n();
  const [page, setPage] = useState<UpdatesPageData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setPage(null);
    setError(null);
    apiGet<UpdatesPageData>(`/api/updates/${sector}`)
      .then(setPage)
      .catch((err: Error) => setError(err.message));
  }, [sector]);

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        <header className={styles.header}>
          <h1 className={styles.title}>{t(`updates.${sector}.title`)}</h1>
          <p className={styles.intro}>{t(`updates.${sector}.intro`)}</p>
          {page && (
            <p className={styles.count}>
              {page.totalCount} {t(page.totalCount === 1 ? 'updates.item.one' : 'updates.item.plural')}
            </p>
          )}
        </header>

        {error && (
          <p className={styles.placeholder} role="alert">
            {error}
          </p>
        )}

        {!error && page === null && <p className={styles.placeholder}>{t('common.loading')}</p>}

        {page && page.groups.length === 0 && (
          <p className={styles.placeholder}>{t(`updates.${sector}.empty`)}</p>
        )}

        {page && page.groups.length > 0 && (
          <div className={styles.groups}>
            {page.groups.map((group) => (
              <UpdateGroup key={group.contentType} group={group} />
            ))}
          </div>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
