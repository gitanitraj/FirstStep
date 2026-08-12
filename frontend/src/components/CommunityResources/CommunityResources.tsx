import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { ResourcePathway } from '../../types/api';
import styles from './CommunityResources.module.css';

interface Props {
  pathways: ResourcePathway[] | null;
}

/**
 * The homepage's main column — discovery pathways into existing CivicContent.
 *
 * This is the section that is most recognisably the original First Step: icon
 * cards, one per pathway, and nothing else. Searching, filtering and browsing
 * all live on the destination page.
 *
 * THE ONE THING TO UNDERSTAND HERE is `kind`, which decides the destination:
 *
 *   category  → /category/{key}   a canonical taxonomy category (Slice F built
 *                                 these pages, so six of seven already work)
 *   discovery → /discover/{key}   a controlled query over existing CivicContent
 *                                 metadata — Seniors, and nothing else today
 *
 * Seniors is a legitimate resident discovery need, so it belongs on this list.
 * It is NOT a category and must never become one: it answers "who is this
 * relevant to?", which the taxonomy never asked (Decision 041). Routing it
 * differently is how that distinction stays true in the UI rather than only in
 * the documentation.
 *
 * The list is authored in `app/data/homepage.json`, not derived from the
 * taxonomy — the homepage shows a curated seven of ten categories, and which
 * seven is an editorial judgement. Every category stays reachable via Discover.
 */
export default function CommunityResources({ pathways }: Props) {
  const { t } = useI18n();

  const href = (pathway: ResourcePathway) =>
    pathway.kind === 'discovery' ? `/discover/${pathway.key}` : `/category/${pathway.key}`;

  return (
    <section className={styles.section} aria-labelledby="community-resources-title">
      <h2 id="community-resources-title" className={styles.title}>
        {t('home.communityResources')}
      </h2>

      {pathways === null ? (
        <p className={styles.placeholder}>{t('common.loading')}</p>
      ) : (
        <ul className={styles.grid}>
          {pathways.map((pathway) => (
            <li key={pathway.key}>
              <Link className={styles.card} to={href(pathway)}>
                <span className={styles.icon} aria-hidden="true">
                  {pathway.icon}
                </span>
                <span className={styles.label}>{pathway.label}</span>
                {/* Affordance only — the whole card is the link, and the label
                    already names the destination, so this is decorative and
                    hidden from assistive tech rather than announced as "arrow". */}
                <span className={styles.arrow} aria-hidden="true">
                  →
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
