import { useI18n } from '../../i18n/I18nProvider';
import type { ContentItem } from '../../types/api';
import styles from './NoticeGallery.module.css';

interface Props {
  items: ContentItem[];
}

/**
 * The flyer gallery — the one place Community Notices departs from the generic
 * card list, and the only place it should.
 *
 * **The image IS the content.** A flyer is a poster: the design, the dates, the
 * phone number and the languages it is printed in are all on the image. Rendering
 * a flyer as a title and a summary throws away the thing worth browsing, which is
 * why this view exists at all.
 *
 * **A GRID, not a carousel.** A carousel is a preview device — it asks a visitor
 * to scroll sideways through an unknown number of items before they know whether
 * any of it is relevant. That is right on the homepage and wrong on the
 * destination, where someone has already said "show me the flyers". The homepage
 * keeps its carousel; browsing gets a grid.
 *
 * **`object-fit: contain`**, per Decision 044 — `cover` cropped
 * "FREE LEGAL HELP IS AVAILABLE FOR DELAWARE RENTERS" mid-line. Letterboxing
 * costs a little polish and keeps the poster readable.
 *
 * It stays part of the Community Notices page system: same route, same nav, same
 * data shape. Only the presentation differs, and only because the content does.
 */
export default function NoticeGallery({ items }: Props) {
  const { t } = useI18n();

  return (
    <ul className={styles.gallery}>
      {items.map((item) => (
        <li key={item.id} className={styles.card}>
          {item.imageUrl ? (
            <img className={styles.image} src={item.imageUrl} alt={item.title} loading="lazy" />
          ) : (
            /* A notice can be a flyer by contentType and still have no poster —
               an authored meeting notice, say. Falling back keeps it in the
               gallery rather than silently dropping it. */
            <div className={styles.noImage} aria-hidden="true">
              <span>{t('notices.noImage')}</span>
            </div>
          )}
          <div className={styles.body}>
            <p className={styles.title}>{item.title}</p>
            <p className={styles.meta}>
              {item.organization && <span>{item.organization}</span>}
              {item.organization && item.date && <span> · </span>}
              {item.date && <span>{item.date}</span>}
            </p>
          </div>
        </li>
      ))}
    </ul>
  );
}
