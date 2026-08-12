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
              <h3 className={styles.itemTitle}>{item.title}</h3>
              {item.summary && <p className={styles.summary}>{item.summary}</p>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
