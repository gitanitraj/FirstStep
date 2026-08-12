import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { FlyerCard } from '../../types/api';
import styles from './CommunityInformation.module.css';

interface Props {
  flyers: FlyerCard[] | null;
}

/** How many flyer images the homepage shows. The rest live on the Community page. */
const FLYER_LIMIT = 3;

/**
 * Community Information — information originating from the community.
 *
 * **Flyers, and nothing else.** An earlier draft also carried three pathway
 * cards (Upcoming Events · Meeting Notices · Announcements) above the images.
 * They were removed: the flyers ARE the community's own voice, they carry their
 * own images and dates, and three text links above them restated what the "See
 * all" link already offered. The section says more by showing less.
 *
 * That removal did not resurrect the data-model question those cards sidestepped
 * — nothing here groups flyers by kind, so no flyer metadata is needed. The
 * grouping question still belongs to the Community destination page.
 *
 * `imageUrl` is already resolved and URL-encoded server-side, so this component
 * only displays.
 */
export default function CommunityInformation({ flyers }: Props) {
  const { t } = useI18n();
  const preview = flyers?.slice(0, FLYER_LIMIT) ?? [];

  return (
    <section className={styles.section} aria-labelledby="community-title">
      <div className={styles.head}>
        <h2 id="community-title" className={styles.title}>
          {t('section.community')}
        </h2>
        <Link className={styles.more} to="/community">
          {t('community.viewAll')}
        </Link>
      </div>
      <p className={styles.intro}>{t('community.intro')}</p>

      {preview.length > 0 && (
        <ul className={styles.flyers}>
          {preview.map((flyer) => (
            <li className={styles.flyerCard} key={flyer.imageUrl}>
              <img className={styles.flyerImage} src={flyer.imageUrl} alt={flyer.title} loading="lazy" />
              <div className={styles.flyerBody}>
                <p className={styles.flyerTitle}>{flyer.title}</p>
                <p className={styles.flyerMeta}>
                  {flyer.organization && <span>{flyer.organization}</span>}
                  {flyer.organization && flyer.eventDate && <span> · </span>}
                  {flyer.eventDate && <span>{flyer.eventDate}</span>}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
