import { useI18n } from '../../i18n/I18nProvider';
import { CONTENT_TYPE_LABEL } from '../../i18n/contentTypeLabel';
import type { ContentItem } from '../../types/api';
import styles from './ContentCard.module.css';

/**
 * ONE card design for every kind of CivicContent — Decision 021's "one
 * consistent card design that labels content type and source".
 *
 * It renders a normalized {@link ContentItem} and shows only the fields that are
 * present, so a resource (organization, city, cost, urgency) and a flyer (date)
 * use the same markup without either branching on type. The only thing that
 * varies by type is the BADGE, and that is a label lookup rather than a layout
 * decision.
 *
 * FIRST COMPONENT BUILT ON THE CSS MODULES CONVENTION (Decision 039): styles are
 * co-located and locally scoped, so `.card` here can never collide with anything
 * — including the CSS of a component deleted three slices ago, which is what
 * caused the two bugs that motivated the convention.
 *
 * Every colour is a token, so the high-contrast theme applies for free and this
 * file needs no `:global()` at all.
 */
export default function ContentCard({ item }: { item: ContentItem }) {
  const { t } = useI18n();

  // Many directory records name the resource after its provider ("American Red
  // Cross" / "American Red Cross"), so repeating it below the title wastes a
  // line on every card. Show the organization only when it adds something.
  const provider = item.organization !== item.title ? item.organization : null;

  // "standard" is the ABSENCE of urgency, not a level of it. Rendering it as a
  // chip gives every ordinary resource a badge that means nothing — the same
  // check ImportantUpdates already makes.
  const urgency = item.urgency && item.urgency.toLowerCase() !== 'standard' ? item.urgency : null;

  return (
    <li className={styles.card}>
      <div className={styles.head}>
        <span className={`${styles.badge} ${styles[`badge${item.contentType}`] ?? ''}`}>
          {t(CONTENT_TYPE_LABEL[item.contentType])}
        </span>
        {/* Link out when the provider gave us somewhere to go. Editorial
            standard: connect residents to the originating organization rather
            than replacing it. */}
        {item.url ? (
          <a className={styles.title} href={item.url} target="_blank" rel="noopener noreferrer">
            {item.title}
          </a>
        ) : (
          <span className={styles.title}>{item.title}</span>
        )}
      </div>

      {(provider || item.location) && (
        <p className={styles.provider}>
          {provider && <span>{provider}</span>}
          {provider && item.location && <span aria-hidden="true"> · </span>}
          {item.location && <span>{item.location}</span>}
        </p>
      )}

      {item.summary && <p className={styles.summary}>{item.summary}</p>}

      {(item.cost || urgency || item.date) && (
        <p className={styles.facts}>
          {item.cost && <span className={styles.cost}>{item.cost}</span>}
          {urgency && <span className={styles.urgency}>{urgency}</span>}
          {item.date && <span className={styles.date}>{item.date}</span>}
        </p>
      )}
    </li>
  );
}
