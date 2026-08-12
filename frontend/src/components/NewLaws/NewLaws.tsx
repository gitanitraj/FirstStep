import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { LawItem } from '../../types/api';
import styles from './NewLaws.module.css';

const ROTATE_MS = 5000;

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

/**
 * "New Laws in Delaware" — the retained V2 RSS/news scroll, and the homepage's
 * answer to PASSIVE DISCOVERY.
 *
 * Not every visitor arrives with a defined need. This section exists so someone
 * who came looking for nothing in particular still leaves knowing something that
 * may affect their housing, health, employment or benefits. That framing is why
 * it is a TEASER, not a news ticker: one law at a time, a list of what else
 * lives behind the link, and a route to the full Latest Updates page.
 *
 * **The box does not resize as it rotates.** Titles vary from a few words to a
 * full House Joint Resolution, and a box that grew and shrank every five seconds
 * would shove the rest of the page around. Every title is rendered into one grid
 * cell with only the current one visible, so the cell measures the tallest of
 * them at whatever width it has — no hard-coded line count to re-measure when
 * the copy or the layout changes.
 *
 * Was `ImportantChanges` (Slice H) and `DelawareLawsFeature` before that; the
 * name now matches what the section actually lists.
 */
const CHANGE_TYPES = ['legislation', 'policy', 'benefits', 'deadlines', 'announcements'];

export default function NewLaws({ laws }: { laws: LawItem[] | null }) {
  const { t } = useI18n();
  const [index, setIndex] = useState(0);
  const count = laws?.length ?? 0;

  useEffect(() => {
    if (count < 2 || prefersReducedMotion()) {
      return;
    }
    const id = setInterval(() => setIndex((i) => (i + 1) % count), ROTATE_MS);
    return () => clearInterval(id);
  }, [count]);

  const safeIndex = count > 0 ? index % count : 0;
  const bill = laws && count > 0 ? laws[safeIndex] : null;

  return (
    <section className={styles.section} aria-labelledby="laws-title">
      <div className={styles.head}>
        <h2 id="laws-title" className={styles.title}>
          {t('section.laws')}
        </h2>
        <Link className={styles.more} to="/updates">
          {t('mission.informed.action')}
        </Link>
      </div>

      <div className={styles.rotator}>
        {/* EVERY title is rendered, stacked in one grid cell; only the current
            one is visible. The cell therefore sizes itself to the TALLEST title
            in the feed, at whatever width the box happens to be — so the box
            never resizes as it rotates, and no hard-coded line count has to be
            re-measured when the copy or the layout changes.
            `visibility: hidden` keeps the hidden titles out of the a11y tree. */}
        <div className={styles.billStack}>
          {bill ? (
            (laws ?? []).map((law, i) => (
              <p
                key={i === safeIndex ? `active-${i}` : i}
                className={i === safeIndex ? `${styles.bill} ${styles.billActive}` : styles.bill}
              >
                {law.url ? (
                  <a href={law.url} target="_blank" rel="noopener noreferrer">
                    {law.title}
                  </a>
                ) : (
                  law.title
                )}
                {law.date && <span className={styles.billDate}> · {law.date}</span>}
              </p>
            ))
          ) : (
            <p className={`${styles.bill} ${styles.billActive}`}>
              <span className={styles.placeholder}>{t('common.comingSoon')}</span>
            </p>
          )}
        </div>

        {count > 1 && (
          <div className={styles.dots}>
            {(laws ?? []).map((_, i) => (
              <button
                key={i}
                type="button"
                className={i === safeIndex ? `${styles.dot} ${styles.active}` : styles.dot}
                aria-label={`${i + 1} / ${count}`}
                aria-current={i === safeIndex}
                onClick={() => setIndex(i)}
              />
            ))}
          </div>
        )}
      </div>

      {/* What a visitor will find if they follow the link — the teaser's whole
          job. Static copy, not derived from the feed: it describes the Updates
          page's remit, which does not change when the feed does. */}
      <ul className={styles.types}>
        {CHANGE_TYPES.map((type) => (
          <li key={type} className={styles.type}>
            {t(`changes.${type}`)}
          </li>
        ))}
      </ul>
    </section>
  );
}
