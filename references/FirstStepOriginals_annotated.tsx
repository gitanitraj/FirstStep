/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/FirstStepOriginals/
 *   FirstStepOriginals.tsx + FirstStepOriginals.module.css
 * Slice H originally (Decision 042); cards became links in Slice K (048).
 * =============================================================================
 *
 * WHAT IT IS: the homepage panel for content First Step produced itself,
 * identified by ContentSource rather than by a ContentType (Decision 041).
 *
 * SLICE K — THE CARDS NOW GO SOMEWHERE
 * ------------------------------------
 * Until the reading surface existed, an Originals card announced itself and
 * stopped. That was honest rather than broken — First Step HOSTS this content,
 * so unlike every other card in the product there was no originating
 * organization to send the reader to instead. It was also the clearest possible
 * argument that articles needed a detail page.
 *
 * WHY THE ROUTE IS DERIVED FROM THE ID
 * ------------------------------------
 * ContentItem.url is documented as "the provider's own site, never First
 * Step's". An Original has no provider site, so putting /originals/OR-003 in
 * that field would contradict its stated meaning for every other consumer of the
 * shared card shape. The route is built here instead: one component knows about
 * article routing, and the shared DTO keeps its semantics.
 *
 * WHY contentType AND NOT AN ID PREFIX
 * ------------------------------------
 * The first version tested `item.id.startsWith('OR-')`. That works today and is
 * wrong: it encodes a DATA-AUTHORING CONVENTION in the client, and would break
 * silently the day an id scheme changed — silently, because a non-matching id
 * simply renders unlinked, which looks like a design choice rather than a bug.
 *
 * contentType is a semantic field the server already sets: NEWS for articles,
 * EXPERT for FAQs. Verified against the live payload before switching.
 *
 * FAQs STAY UNLINKED, DELIBERATELY
 * --------------------------------
 * They have no reading surface because Decision 048 scopes the article pipeline
 * — and the editorial review gate — to ARTICLES. "First Step-created content"
 * and "First Step Original article" are different concepts; a two-sentence FAQ
 * answer needs neither a page nor a review.
 *
 * THE WHOLE CARD IS THE LINK
 * --------------------------
 * Not just the title. A link covering only a heading gives a small hit area and
 * reads as two unrelated things to a screen reader when the summary plainly
 * belongs to it. The underline moves to the title on hover and focus so the
 * affordance still reads as a headline link.
 * ============================================================================= */

import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { ContentItem } from '../../types/api';
import styles from './FirstStepOriginals.module.css';

interface Props {
  originals: ContentItem[] | null;
}

/**
 * The homepage's RIGHT column — CivicContent First Step produced itself.
 *
 * "Originals" is a **`ContentSource` distinction, not a ContentType and not a
 * domain class** (Decision 041). It describes WHO CREATED the content, which is
 * a question `ContentSource` already answers. The backend filters on
 * `contentSource.id === "first-step"`; this component just displays the result.
 *
 * That the distinction is real, not invented, is visible in the data: FAQs and
 * expert answers are BOTH `contentType: EXPERT`, but one carries
 * `contentSource.name = "First Step"` and the other "Delaware Volunteer Legal
 * Services". Same kind of content, different producer — exactly the distinction
 * a new ContentType would have destroyed.
 *
 * Today this is six curated FAQs. Community Briefings, YouTube, Data Stories,
 * articles and newsletters are future Originals; when they arrive they carry the
 * same ContentSource and appear here **with no code change**, which is the point
 * of identifying the section this way rather than enumerating types.
 */
/**
 * How many Originals the sidebar previews. The same reasoning as
 * CommunityInformation's FLYER_LIMIT: a front door shows a taste, not a feed.
 * It also keeps the sidebar close to the height of the cards beside it — six
 * entries ran ~270px past them and left a hole in the main column.
 *
 * When Originals has a destination page of its own, this gains a "see all" link.
 * Until then the cap is the only thing hiding entries, which is worth knowing.
 */
const ORIGINALS_LIMIT = 4;

export default function FirstStepOriginals({ originals }: Props) {
  const { t } = useI18n();
  const preview = originals?.slice(0, ORIGINALS_LIMIT) ?? null;

  return (
    <section className={styles.section} aria-labelledby="originals-title">
      {/* The masthead block stays WHITE against the orange body — the same move
          a magazine makes to separate a section's nameplate from its contents. */}
      <div className={styles.head}>
        <h2 id="originals-title" className={styles.title}>
          {t('home.originals')}
        </h2>
        <p className={styles.intro}>{t('home.originalsIntro')}</p>
      </div>

      {preview === null ? (
        <p className={styles.placeholder}>{t('common.loading')}</p>
      ) : preview.length === 0 ? (
        <p className={styles.placeholder}>{t('home.originalsEmpty')}</p>
      ) : (
        <ul className={styles.list}>
          {preview.map((item) => (
            <li key={item.id} className={styles.item}>
              {/* The route is derived from the id rather than read from a url
                  field: ContentItem.url means "the provider's own site, never
                  First Step's", and an Original has no provider site.

                  Articles are distinguished by contentType, not by an id prefix.
                  An `id.startsWith('OR-')` check would encode a data-authoring
                  convention in the client and break silently the day an id
                  scheme changed; contentType is a semantic field the server
                  already sets. FAQs (EXPERT) have no reading surface and stay
                  unlinked — Decision 048 scopes the article pipeline to
                  articles. */}
              {item.contentType === 'NEWS' ? (
                <Link className={styles.itemLink} to={`/originals/${item.id}`}>
                  <h3 className={styles.itemTitle}>{item.title}</h3>
                  {item.summary && <p className={styles.summary}>{item.summary}</p>}
                </Link>
              ) : (
                <>
                  <h3 className={styles.itemTitle}>{item.title}</h3>
                  {item.summary && <p className={styles.summary}>{item.summary}</p>}
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
