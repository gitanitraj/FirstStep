import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, apiGet } from '../../api/client';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import CategoryBrowse from '../../components/CategoryBrowse';
import ContentCollection from '../../components/ContentCollection/ContentCollection';
import ContentCard from '../../components/ContentCard/ContentCard';
import { useI18n } from '../../i18n/I18nProvider';
import type { CategoryPage as CategoryPagePayload, ContentType, ContextLink } from '../../types/api';
import styles from './CategoryPage.module.css';

/** IM-08 composes server-selected content; semantic positions never become inferred UI badges. */
const TYPE_ORDER: ContentType[] = ['RESOURCE', 'LAW', 'NEWS', 'EXPERT', 'FLYER'];
const INFORMATION = [
  ['policy-info', 'collection.policyInfo'], ['law', 'collection.law'],
  ['flyers', 'collection.flyers'], ['first-rate-news', 'collection.firstRateNews'],
] as const;

export default function CategoryPage() {
  const { key } = useParams<{ key: string }>();
  const { t } = useI18n();
  const [search, setSearch] = useSearchParams();
  const community = search.get('communityId');
  const query = community ? `?${new URLSearchParams({ communityId: community })}` : '';
  const [page, setPage] = useState<CategoryPagePayload | null>(null);
  const [error, setError] = useState<'missing' | 'network' | null>(null);
  const [activeCollection, setActiveCollection] = useState('');
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    let active = true;
    setPage(null); setError(null); setActiveCollection('');
    apiGet<CategoryPagePayload>(`/api/category/${key}${query}`)
      .then((result) => {
        if (!active) return;
        setPage(result);
        // The configured server default is made explicit in shareable navigation/history.
        if (!community && result.destination?.communityId) {
          const next = new URLSearchParams(search); next.set('communityId', result.destination.communityId);
          setSearch(next, { replace: true });
        }
      })
      .catch((err: Error) => { if (active) setError(err instanceof ApiError && err.code === 'NOT_FOUND' ? 'missing' : 'network'); });
    return () => { active = false; };
  }, [key, query]); // Only route scope drives retrieval.
  useEffect(() => { if (page) heading.current?.focus(); }, [page]);

  const help = page?.collections.find(c => c.key === 'get-help');
  const information = page?.collections.filter(c => c.layer === 'INFORMATION') ?? [];
  const supporting = page?.collections.filter(c => c.layer === 'SUPPORTING') ?? [];
  const choice = activeCollection || information[0]?.key || 'policy-info';
  const selected = information.find(c => c.key === choice);
  const allHelp = page ? `/category/${page.metadata.key}/help${query}` : '';
  const allLabel = page ? `${t('help.all')} ${page.metadata.label} ${t('help.help')}` : '';
  const featured = page?.destination?.featuredTopics ?? [];
  // An approved Article collection owns internal reading navigation; no source URL is invented.
  const contextual = selected?.key === 'first-rate-news' ? { ...selected, items: selected.items.map(item => ({
    ...item, url: `/originals/${encodeURIComponent(item.id)}${query}`,
  })) } : selected;

  const links = (items: ContextLink[]) => items.length ? <ul className={styles.headlines}>
    {items.map(item => <li key={item.id}>{item.href.startsWith('/originals/')
      ? <Link to={item.href}>{item.title}</Link>
      : <a href={item.href} target="_blank" rel="noopener noreferrer">{item.title}</a>}</li>)}
  </ul> : <p className={styles.note}>{t('destination.noLinks')}</p>;

  return <>
    <SiteHeader />
    <main className={styles.body}>
      {error && <section className="stub-page" role="alert">
        <h1>{t(error === 'missing' ? 'category.notFound' : 'help.error')}</h1>
        <Link to="/" className="stub-back">{t('stub.back')}</Link>
      </section>}
      {!error && !page && <p className="section-placeholder" role="status">{t('common.loading')}</p>}
      {!error && page && <>
        <header className={styles.header}>
          <nav className={styles.crumbs} aria-label={t('topic.breadcrumb')}>
            <Link className={styles.crumb} to={`/${query}`}>{t('stub.back')}</Link>
          </nav>
          <h1 ref={heading} tabIndex={-1} className={styles.title}>
            <span className={styles.icon} aria-hidden="true">{page.metadata.icon}</span>{page.metadata.label}
          </h1>
          <p className={styles.note}>{t('destination.intro')}</p>
          <p className={styles.counts}>{TYPE_ORDER.filter(type => page.metadata.countsByType[type]).map((type, i) => {
            const n = page.metadata.countsByType[type] as number;
            return <span key={type}>{i > 0 && <span aria-hidden="true"> · </span>}
              {n} {t(`contentType.${type.toLowerCase()}.${n === 1 ? 'one' : 'plural'}`)}</span>;
          })}</p>
          <Link className={styles.quickHelp} to={allHelp}>{t('destination.quickHelp')} {page.metadata.label}</Link>
        </header>

        <section aria-labelledby="featured-title">
          <h2 id="featured-title" className={styles.sectionTitle}>{t('destination.featured')}</h2>
          {featured.length ? <div className={styles.featured}>
            {featured.map(topic => <section key={topic.slug} className={styles.topic} aria-labelledby={`featured-${topic.slug}`}>
              <h3 className={styles.topicTitle} id={`featured-${topic.slug}`}><Link to={`/category/${page.metadata.key}/${topic.slug}${query}`}>{topic.name}</Link></h3>
              <ul className={styles.cards}>{topic.items.map(position =>
                <ContentCard key={position.item.id} item={position.item} href={position.href} />)}</ul>
            </section>)}
          </div> : <p role="status" className={styles.note}>{t('destination.noFeatured')}</p>}
        </section>

        <div className={styles.layout}>
          <div className={styles.primary}>
            {help ? <ContentCollection key={`${key}${query}-help`} collection={help} headingLevel={2}
              moreHref={allHelp} moreLabel={allLabel} /> : <section aria-labelledby="help-title">
              <h2 id="help-title" className={styles.sectionTitle}>{t('collection.getHelp')}</h2>
              <p role="status">{t('help.empty')}</p><Link to={allHelp}>{allLabel}</Link>
            </section>}
            <details className={styles.browse}>
              <summary>{t('destination.browse')}</summary>
              <CategoryBrowse categoryKey={page.metadata.key} query={query} groups={page.groups} topics={page.topics} />
            </details>

            <section className={styles.whatToKnow} aria-labelledby="what-to-know-title">
              <h2 id="what-to-know-title" className={styles.sectionTitle}>{t('destination.whatToKnow')}</h2>
              <nav className={styles.switcher} aria-label={t('destination.collections')}>
                {INFORMATION.map(([id, label]) => <button key={id} type="button" aria-pressed={choice === id}
                  aria-controls="active-collection" onClick={() => setActiveCollection(id)}>{t(label)}</button>)}
              </nav>
              <div id="active-collection" aria-live="polite">
                {contextual ? <ContentCollection key={`${key}${query}${choice}`} collection={contextual} headingLevel={3} previewCount={2} />
                  : <p role="status">{t('destination.emptyCollection')}</p>}
              </div>
              {supporting.length > 0 && <section className={styles.supporting} aria-labelledby="supporting-title">
                <h3 id="supporting-title" className="visually-hidden">{t('destination.supporting')}</h3>
                {supporting.map(collection => <ContentCollection key={`${key}${query}${collection.key}`}
                  collection={collection} headingLevel={4} previewCount={2} />)}
              </section>}
            </section>

            <section className={styles.organizations} aria-labelledby="organizations-title">
              <h2 id="organizations-title" className={styles.sectionTitle}>{t('section.organizations')}</h2>
              <p className={styles.note}>{t('destination.organizationsNote')}</p>
              {page.organizations.length ? <ul>{page.organizations.map(org => <li key={org.slug}>
                <Link to={`/organization/${org.slug}${query}`}>{org.name}</Link>
              </li>)}</ul> : <p role="status">{t('destination.noOrganizations')}</p>}
            </section>
          </div>
          <aside className={styles.rail} aria-labelledby="context-title">
            <h2 id="context-title" className={styles.sectionTitle}>{t('destination.rail')}</h2>
            <section><h3 className={styles.railTitle}>{t('collection.firstRateNews')}</h3>{links(page.destination?.headlines ?? [])}</section>
            <section><h3 className={styles.railTitle}>{t('destination.expertLinks')}</h3>{links(page.destination?.expertLinks ?? [])}</section>
            <section><h3 className={styles.railTitle}>{t('section.laws')}</h3>{links(page.destination?.laws ?? [])}</section>
          </aside>
        </div>
      </>}
    </main>
    <SiteFooter />
  </>;
}
