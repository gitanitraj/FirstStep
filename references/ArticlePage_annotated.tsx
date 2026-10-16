/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/pages/ArticlePage/ArticlePage.tsx
 *   + ArticlePage.module.css
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: the reading surface for a First Step Original, at /originals/:id.
 *
 * WHY IT IS DIFFERENT FROM EVERY OTHER PAGE IN THE PRODUCT
 * --------------------------------------------------------
 * It is the FIRST page that displays content First Step HOSTS. Every other
 * surface either links out to the producing organization or shows a summary —
 * which is why this page has no "read more at..." link. There is nowhere else to
 * send the reader, and that single fact is what made both a reading surface and
 * a review gate necessary.
 *
 * IT CONSUMES ArticleDetail, NEVER ContentItem
 * --------------------------------------------
 * ContentItem is the CARD shape, shared by topic pages, search, notices and the
 * homepage. Adding `body` to it would have put a full article on every card
 * payload in the product. Two shapes because there are two jobs — and because a
 * shared shape widens every surface at once when you widen it.
 *
 * WHAT THIS COMPONENT CANNOT DO, BY CONSTRUCTION
 * ----------------------------------------------
 * It cannot leak editorial state, because the contract it consumes has no
 * component carrying any: no status, no flags, no dispositions, no reviewer, no
 * generatedBy, no `verified`. It also does not INFER any of them — there is no
 * "if this looks AI-written" logic anywhere. What a reader is told about
 * authorship is the byline; whether AI assisted is the separate authored
 * disclosure.
 *
 * THE 404 IS DELIBERATELY INCURIOUS
 * ---------------------------------
 * One catch, one failure state, no inspection of the error. The backend makes an
 * unapproved article and a nonexistent one indistinguishable ON PURPOSE — same
 * status, same message — and any branching here would re-derive the distinction
 * the server exists to withhold.
 *
 * It also does NOT display the server's message. "Article not found: OR-001" is
 * English-only and echoes the id back at the reader; the page shows its own
 * translated copy plus a way home. A test asserts the id never appears.
 *
 * WHY CATEGORY LABELS ARE NOT LINKS
 * ---------------------------------
 * The contract carries category LABELS ("Housing", "Furniture & Household") and
 * not the URL KEYS a link needs ("housing", "furniture-household"). Slugifying
 * client-side would duplicate a server-side rule in a second place, and this
 * codebase never does it — CategoryBrowse and CommunityResources both RECEIVE
 * keys from the server.
 *
 * The alternative was widening the contract, which was out of scope. Recorded as
 * a limitation rather than solved with a guess: breadcrumb links need category
 * keys in ArticleDetail.
 *
 * BODY IS PLAIN TEXT, SPLIT ON BLANK LINES
 * ----------------------------------------
 * paragraphsOf() does exactly one thing: split on blank lines. It deliberately
 * does NOT interpret ALL-CAPS lines as headings or "- " lines as list items,
 * even though the Rent Escrow draft contains both.
 *
 * That would be a markdown-lite parser inferring structure from prose, and it
 * would misfire the first time an article legitimately shouted a word. Whether
 * `body` should carry structure at all is a CONTENT MODEL question — should it
 * be markdown? — and belongs in a decision, not in a regex here.
 *
 * ACCESSIBILITY NOTES
 * -------------------
 * <article aria-labelledby="article-title">   the page's content region, named
 *                                             by its own h1
 * <aside aria-labelledby="why-title">         "Why it matters" is complementary
 *                                             to the article, not part of its
 *                                             narrative — a different question
 *                                             from the reporting
 * <time dateTime>                             machine-readable dates
 * role="status" on loading                    announced politely, not urgently
 * role="alert" on not-found                   announced immediately; it is the
 *                                             whole content of the page
 *
 * The homepage card wraps the WHOLE card in the link rather than just the title:
 * a link covering only a heading gives a small target and reads as two unrelated
 * things when the summary belongs to it.
 *
 * CSS: THE MEASURE IS THE WHOLE POINT
 * -----------------------------------
 * --page-max is 1600px, which is right for browse layouts and unreadable for
 * prose. The article is capped at 68ch — roughly 70 characters a line. This is
 * the one page in the product that deliberately ignores the page-width token,
 * and that is not an oversight.
 *
 * The disclosure is a bordered note rather than fine print. Burying a
 * transparency statement in small grey text defeats the reason for having one.
 *
 * A BUG FOUND ONLY BY LOOKING AT THE OTHER THEME
 * ----------------------------------------------
 * The first version styled the h1 and both headings with:
 *
 *     color: var(--brand-green, #1a5c38);
 *
 * `--brand-green` IS NOT DEFINED ANYWHERE in tokens.css. Every one of those
 * rules therefore fell silently through to the hardcoded fallback, which looked
 * perfectly correct on the cream page and rendered DARK GREEN ON BLACK in high
 * contrast — close to unreadable, on the page's most important text.
 *
 * Two lessons, both cheap to state and expensive to rediscover:
 *
 *   A FALLBACK LITERAL DEFEATS THEMING PRECISELY WHEN THEMING MATTERS. The
 *   fallback exists to make a missing token harmless, and its effect here was to
 *   make a missing token INVISIBLE — a rule that never participates in the
 *   theme, with no error anywhere.
 *
 *   TESTS AND THE DEFAULT THEME BOTH PASSED. 87 frontend tests were green and
 *   the light-theme screenshot looked right. The only thing that caught it was
 *   rendering the high-contrast page and reading it.
 *
 * The correct token is --primary-color — but that was only HALF the fix, and the
 * measurement said so: after switching, the ratio was still 2.63:1.
 *
 * --primary-color is DELIBERATELY NOT FLIPPED by the high-contrast theme. It is
 * overloaded as both a surface colour (the utility bar's background, wants #000)
 * and an ink colour (headings, want #ff0), and no single value satisfies both.
 * styles/themes.css says so explicitly and tracks the token split separately.
 *
 * The convention is therefore that EVERY COMPONENT USING IT AS INK DECLARES ITS
 * OWN `:global(body.high-contrast)` RULES — which CommunityNoticesPage does and
 * this file originally did not. The omitted block is the actual bug; the
 * undefined token merely hid it.
 *
 * Measured after: 2.63:1 -> 19.56:1.
 *
 * WHY THIS WAS FOUND LATE, worth remembering: the light theme looked right, all
 * 87 frontend tests passed, and tsc was clean. Nothing in the toolchain can see
 * a colour contrast failure. Only rendering the other theme and MEASURING it
 * caught this — which is why the project's rule is that contrast is measured,
 * never eyeballed.
 * ============================================================================= */

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
