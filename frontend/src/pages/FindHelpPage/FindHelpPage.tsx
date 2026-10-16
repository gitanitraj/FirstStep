import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import ContentCard from '../../components/ContentCard/ContentCard';
import type { FindHelpPage as PageData } from '../../types/api';
import styles from './FindHelpPage.module.css';

/**
 * Find Help — orientation, not a database.
 *
 * **The page answers one question: where should I start?** It hands off and
 * never filters in place, because the three destinations answer different
 * questions:
 *
 * <pre>
 *   Find Help          where should I start?
 *   Category page      what is available for this need?
 *   Organization page  who can I contact?
 * </pre>
 *
 * Selecting a category LEAVES this page for /category/:key, where the resident
 * gets every content form for that need — resources, flyers, FAQs, expert
 * answers, articles and updates. Filtering here instead would trap them inside
 * whichever table Find Help happened to query, which is the failure this whole
 * structure is designed against.
 *
 * **Three levels of engagement, in the order a resident needs them:** know what
 * you need → choose a category; need a starting point → "Open to everyone";
 * already know the organization → search the directory. The directory is LAST
 * deliberately, because asking someone in difficulty to scan 63 organizations
 * before offering a way in is the alphabetical directory this page was scoped
 * against.
 */
export default function FindHelpPage() {
  const { t } = useI18n();
  const [page, setPage] = useState<PageData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  useEffect(() => {
    apiGet<PageData>('/api/find-help')
      .then(setPage)
      .catch((e: Error) => setError(e.message));
  }, []);

  // Client-side because the whole directory is already here — 63 entries is a
  // filter, not a search backend.
  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!page) return [];
    if (!q) return page.directory;
    return page.directory.filter(
      (o) =>
        o.name.toLowerCase().includes(q) ||
        o.services.some((s) => (s.title ?? '').toLowerCase().includes(q)),
    );
  }, [page, query]);

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <header className={styles.head}>
          <h1 className={styles.title}>{t('findHelp.title')}</h1>
          <p className={styles.intro}>{t('findHelp.intro')}</p>
        </header>

        {error && <p className={styles.placeholder} role="alert">{error}</p>}
        {!error && page === null && (
          <p className={styles.placeholder} role="status">{t('common.loading')}</p>
        )}

        {page && (
          <>
            <section className={styles.section} aria-labelledby="gateways-title">
              <h2 id="gateways-title" className={styles.sectionTitle}>
                {t('findHelp.gateways')}
              </h2>
              <ul className={styles.gateways}>
                {page.gateways.map((g) =>
                  g.available && g.destination ? (
                    <li key={g.key}>
                      <Link className={styles.gateway} to={g.destination}>
                        {g.icon && <span aria-hidden="true">{g.icon}</span>}
                        <span>{g.label}</span>
                      </Link>
                    </li>
                  ) : (
                    /* Rendered, not hidden. The data populates the experience; it
                       does not decide the information architecture. Saying "coming
                       soon" is honest — removing the door would quietly shrink the
                       site's navigation to match a thin table. */
                    <li key={g.key}>
                      <span className={`${styles.gateway} ${styles.unavailable}`} aria-disabled="true">
                        {g.icon && <span aria-hidden="true">{g.icon}</span>}
                        <span>{g.label}</span>
                        <span className={styles.soon}>{t('findHelp.comingSoon')}</span>
                      </span>
                    </li>
                  ),
                )}
              </ul>
            </section>

            {page.openToEveryone.length > 0 && (
              <section className={styles.section} aria-labelledby="open-title">
                {/* The heading STATES THE SELECTION RULE, so the label is the
                    disclosure. Nothing here is "featured" or "popular" — First
                    Step has no usage data, and resources carry no date. */}
                <h2 id="open-title" className={styles.sectionTitle}>
                  {t('findHelp.openToEveryone')}
                </h2>
                <p className={styles.sectionNote}>{t('findHelp.openToEveryoneNote')}</p>
                <ul className={styles.cards}>
                  {page.openToEveryone.map((item) => (
                    <ContentCard key={item.id} item={item} />
                  ))}
                </ul>
              </section>
            )}

            <section className={styles.section} aria-labelledby="directory-title">
              <h2 id="directory-title" className={styles.sectionTitle}>
                {t('findHelp.directory')}
              </h2>
              <p className={styles.sectionNote}>{t('findHelp.directoryNote')}</p>

              <label className={styles.searchLabel} htmlFor="org-search">
                {t('findHelp.searchLabel')}
              </label>
              <input
                id="org-search"
                className={styles.search}
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t('findHelp.searchPlaceholder')}
              />

              <p className={styles.count} role="status">
                {visible.length} {t('findHelp.organizations')}
              </p>

              <ul className={styles.directory}>
                {visible.map((org) => (
                  <li key={org.slug} className={styles.entry}>
                    <Link className={styles.entryName} to={`/organization/${org.slug}`}>
                      {org.name}
                    </Link>
                    {org.services.length > 0 && (
                      <ul className={styles.services}>
                        {org.services.map((s) => (
                          <li key={`${s.title ?? ''}${s.category ?? ''}${s.city ?? ''}`} className={styles.service}>
                            {/* Title is null when a service has no name distinct
                                from its provider — resources derive their title
                                from the organization, so rendering it here would
                                repeat the heading above. The meta line identifies
                                the service instead.

                                The phone belongs to THIS service and is never
                                presented as the organization's line. */}
                            {s.title && <span className={styles.serviceTitle}>{s.title}</span>}
                            <span className={styles.serviceMeta}>
                              {s.category && <span>{s.category}</span>}
                              {s.city && <span> · {s.city}</span>}
                              {s.phone && <span> · {s.phone}</span>}
                            </span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          </>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
