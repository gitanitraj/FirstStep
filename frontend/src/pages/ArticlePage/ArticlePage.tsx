import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiGet } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import SiteHeader from '../../components/SiteHeader/SiteHeader';
import SiteFooter from '../../components/SiteFooter/SiteFooter';
import type { ArticleDetail } from '../../types/api';
import styles from './ArticlePage.module.css';

/** Blank-line-separated plain text. See the note on body structure below. */
function paragraphsOf(body: string | null): string[] {
  return (body ?? '')
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean);
}

/**
 * The reading surface for a First Step Original article.
 *
 * <pre>
 *   /originals/:id
 * </pre>
 *
 * **The first page in the product that displays content First Step HOSTS.**
 * Every other surface either links out to the producing organization or shows a
 * summary — which is why this page has no "read more at…" link. There is nowhere
 * else to send the reader.
 *
 * **It consumes `ArticleDetail`, never `ContentItem`.** Two shapes because there
 * are two jobs: the card shape is shared by topic pages, search and notices, and
 * widening it with a body would widen every one of them.
 *
 * **Nothing here knows about editorial review.** No status, no flags, no
 * dispositions, no `generatedBy` — the server's public record has no component
 * for them, and this client infers nothing from their absence. What a reader is
 * told about authorship is the `byline`; whether AI assisted is the separate,
 * authored `disclosure`.
 *
 * **A 404 is an ordinary not-found.** The page does not inspect the error to
 * work out whether an article is unapproved or simply does not exist — the
 * backend deliberately makes those indistinguishable, and re-deriving the
 * difference here would defeat that.
 */
export default function ArticlePage() {
  const { id } = useParams<{ id: string }>();
  const { t } = useI18n();
  const [article, setArticle] = useState<ArticleDetail | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setArticle(null);
    setFailed(false);
    apiGet<ArticleDetail>(`/api/originals/${id}`)
      .then(setArticle)
      // One failure state, deliberately. The only error this endpoint produces
      // is a 404, and branching on the message would mean reading a distinction
      // the server exists to withhold.
      .catch(() => setFailed(true));
  }, [id]);

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        {failed && (
          <div className={styles.placeholder} role="alert">
            <h1 className={styles.notFoundTitle}>{t('originals.notFound.title')}</h1>
            <p>{t('originals.notFound.body')}</p>
            <Link className={styles.back} to="/">
              {t('originals.notFound.back')}
            </Link>
          </div>
        )}

        {!failed && article === null && (
          <p className={styles.placeholder} role="status">
            {t('common.loading')}
          </p>
        )}

        {article && (
          <article className={styles.article} aria-labelledby="article-title">
            <header className={styles.head}>
              {/* Category labels only. The contract carries labels, not the URL
                  keys a link would need, and this codebase never derives one
                  from the other client-side. */}
              {article.categoryTags && article.categoryTags.length > 0 && (
                <p className={styles.kicker}>
                  {article.categoryTags.join(' · ')}
                  {article.subcategory && ` · ${article.subcategory}`}
                </p>
              )}

              <h1 id="article-title" className={styles.title}>
                {article.title}
              </h1>

              {article.summary && <p className={styles.lede}>{article.summary}</p>}

              <p className={styles.meta}>
                {article.byline && (
                  <span>
                    {t('originals.publishedBy')} {article.byline}
                  </span>
                )}
                {article.publishDate && (
                  <>
                    {article.byline && <span aria-hidden="true"> · </span>}
                    <time dateTime={article.publishDate}>{article.publishDate}</time>
                  </>
                )}
                {article.updatedDate && article.updatedDate !== article.publishDate && (
                  <>
                    <span aria-hidden="true"> · </span>
                    <span>
                      {t('originals.updated')}{' '}
                      <time dateTime={article.updatedDate}>{article.updatedDate}</time>
                    </span>
                  </>
                )}
              </p>

              {/* A controlled key resolved to standard wording — the client
                  renders what it is given and infers nothing about how the
                  article was written. */}
              {article.disclosure && (
                <p className={styles.disclosure}>{t(`originals.disclosure.${article.disclosure}`)}</p>
              )}
            </header>

            {article.whyItMatters && (
              <aside className={styles.why} aria-labelledby="why-title">
                <h2 id="why-title" className={styles.whyTitle}>
                  {t('originals.whyItMatters')}
                </h2>
                <p className={styles.whyBody}>{article.whyItMatters}</p>
              </aside>
            )}

            {/* Body is plain text with blank-line paragraph breaks. Richer
                structure (headings, lists) is a CONTENT MODEL question — whether
                `body` becomes markdown — not something to infer here by pattern
                matching prose. */}
            <div className={styles.body}>
              {paragraphsOf(article.body).map((paragraph, i) => (
                <p key={i}>{paragraph}</p>
              ))}
            </div>
          </article>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
