import { useI18n } from '../i18n/I18nProvider';
import type { FlyerCard } from '../types/api';

/**
 * Community Information — timely community info outside traditional resource
 * directories. Its primary component is the flyer carousel: a horizontal,
 * scroll-snap strip of flyer cards (image + caption). Data comes from GET
 * /api/home `communityFlyers` (imageUrl already resolved + encoded server-side),
 * so this component just displays. No auto-advance — the user explores by
 * scrolling, "without overwhelming".
 */
export default function CommunityInformation({ flyers }: { flyers: FlyerCard[] | null }) {
  const { t } = useI18n();

  return (
    <section className="community-info" aria-labelledby="community-title">
      <h2 id="community-title" className="community-title">
        {t('section.community')}
      </h2>

      {(!flyers || flyers.length === 0) && (
        <p className="section-placeholder">{t('common.comingSoon')}</p>
      )}

      {flyers && flyers.length > 0 && (
        <ul className="flyer-carousel" aria-label={t('section.community')}>
          {flyers.map((flyer) => (
            <li className="flyer-card" key={flyer.imageUrl}>
              <img className="flyer-card-image" src={flyer.imageUrl} alt={flyer.title} loading="lazy" />
              <div className="flyer-card-body">
                <p className="flyer-card-title">{flyer.title}</p>
                <p className="flyer-card-meta">
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
