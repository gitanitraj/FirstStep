import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import ContentCard from '../../components/ContentCard/ContentCard';
import type { OrganizationPage as OrgPage } from '../../types/api';
import styles from './OrganizationPage.module.css';

/**
 * One organization and what it offers — the third step of the Front Door's
 * Connect pathway (directory → organization → that organization's resources).
 *
 * **The URL carries a slug; identity is an ID.** The server resolves one to the
 * other. This component never builds a slug and never reasons about names.
 *
 * **An organization-level website is often absent, and that is correct.** Only a
 * site the source data explicitly labels as the organization's is promoted to
 * this level; program pages stay on the resource that runs them. Where nothing
 * is genuinely established, the page shows nothing rather than presenting a
 * plausible-looking link as authoritative — a resident acting on a wrong number
 * or address is worse off than one who reads on to the service below.
 *
 * **Service contact lives on the cards.** Each resource carries its own website
 * and city through the shared ContentCard, which is where those facts are true.
 */
export default function OrganizationPage() {
  const { slug } = useParams<{ slug: string }>();
  const { t } = useI18n();
  const [search] = useSearchParams();
  const community = search.get('communityId');
  const query = community ? `?${new URLSearchParams({ communityId: community })}` : '';
  const [org, setOrg] = useState<OrgPage | null>(null);
  const [failed, setFailed] = useState(false);
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    setOrg(null);
    setFailed(false);
    apiGet<OrgPage>(`/api/organization/${slug}${query}`)
      .then(setOrg)
      .catch(() => setFailed(true));
  }, [slug, query]);

  useEffect(() => { if (org || failed) heading.current?.focus(); }, [org, failed]);

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        {failed && (
          <div className={styles.placeholder} role="alert">
            <h1 ref={heading} tabIndex={-1} className={styles.notFoundTitle}>{t('organization.notFound.title')}</h1>
            <p>{t('organization.notFound.body')}</p>
            <Link className={styles.back} to={`/${query}`}>
              {t('organization.notFound.back')}
            </Link>
          </div>
        )}

        {!failed && org === null && (
          <p className={styles.placeholder} role="status">
            {t('common.loading')}
          </p>
        )}

        {org && (
          <>
            <header className={styles.head}>
              <p className={styles.kicker}>{t('organization.kicker')}</p>
              <h1 ref={heading} tabIndex={-1} className={styles.title}>{org.name}</h1>
              {org.website && (
                <p className={styles.website}>
                  <a href={org.website} target="_blank" rel="noreferrer">
                    {t('organization.visitWebsite')}
                  </a>
                </p>
              )}
            </header>

            <section aria-labelledby="org-resources">
              <h2 id="org-resources" className={styles.sectionTitle}>
                {t('organization.resources')}
              </h2>
              {org.resources.length === 0 ? (
                /* A registered organization with nothing listed is a different
                   fact from an organization that does not exist — the first
                   renders, the second 404s. */
                <p className={styles.placeholder}>{t('organization.noResources')}</p>
              ) : (
                <ul className={styles.list}>
                  {org.resources.map((item) => (
                    <ContentCard key={item.id} item={item} />
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
