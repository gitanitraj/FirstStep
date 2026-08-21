/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/NoticeGallery/NoticeGallery.tsx
 * Slice J. See references/decisions.md Decision 046 and Decision 044.
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * The flyer view's presentation: a responsive grid of poster images with a title
 * and a line of metadata beneath each.
 *
 * WHY IT EXISTS AT ALL — the justification for departing from ContentCard
 * ----------------------------------------------------------------------
 * THE IMAGE IS THE CONTENT. A flyer is a poster: the design, the dates, the
 * phone number, the languages it is printed in are all ON the image and nowhere
 * in the record. Rendering a flyer as a title and a two-line summary throws away
 * the only thing worth browsing. That is a property of the content, not a
 * preference for variety — which is the bar a presentation departure has to
 * clear under Decision 041.
 *
 * Note what this component does NOT do: it introduces no domain concept, reads
 * no field ContentItem did not already carry, and has no knowledge of notice
 * kinds. It is presentation, and only presentation.
 *
 * A GRID, NOT A CAROUSEL — and the homepage keeps its carousel
 * ------------------------------------------------------------
 * A carousel is a PREVIEW device. It asks a visitor to scroll sideways through
 * an unknown number of items before they can tell whether any of it is relevant,
 * which is right on a homepage where nobody asked for flyers yet. It is wrong on
 * a destination someone reached BY asking for flyers: they want to see how many
 * there are and scan them all. Same content, different question, different
 * control.
 *
 * object-fit: contain (Decision 044)
 * ----------------------------------
 * `cover` fills the tile neatly and cropped "FREE LEGAL HELP IS AVAILABLE FOR
 * DELAWARE RENTERS" mid-line on a real flyer. Letterboxing costs a little polish
 * and keeps the poster readable. On a civic-information site that trade is not
 * close.
 *
 * THE MISSING-POSTER FALLBACK
 * ---------------------------
 * A notice can be a flyer by contentType and still carry no image — an authored
 * meeting notice, for instance. It falls back to a labeled tile rather than
 * being dropped, because the alternative is a count that says 5 above a grid
 * showing 4. `aria-hidden` on the placeholder: the title beside it already names
 * the item, so announcing "No poster" would add noise, not information.
 *
 * WHY imageUrl IS ALREADY RESOLVED
 * --------------------------------
 * FlyerService.imageUrlFor owns the path-and-encoding rule server-side, so this
 * component never builds a URL. A second encoder in the client would be a second
 * place to get it wrong.
 * ============================================================================= */

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
