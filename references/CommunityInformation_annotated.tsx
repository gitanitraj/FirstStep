/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CommunityInformation.tsx (Slice E)
 * (+ note on the backend FlyerService.getCarouselCards it consumes.)
 * See references/decisions.md Decision 025. Keep this mirror in sync.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage's bottom "Community Information" section — a flyer CAROUSEL: a
 *   horizontal, scroll-snap strip of flyer cards (image + caption). Pure display:
 *   it takes FlyerCard[] as a prop (from GET /api/home `communityFlyers`) and
 *   renders it. No auto-advance — the user explores by scrolling, "without
 *   overwhelming".
 *
 * WHERE imageUrl COMES FROM (backend, FlyerService.getCarouselCards)
 *   The flyer records store a BARE image filename (e.g. "Health Fair.jpg"). The
 *   seasonal images serve at /images/seasonal/<file>, but ONLY URL-encoded
 *   (spaces → %20). So the BACKEND resolves + encodes the full imageUrl
 *   ("/images/seasonal/" + UriUtils.encodePathSegment(image)); the frontend never
 *   knows the path convention or does encoding. It also filters image-less flyers
 *   and sorts soonest-event-first. Classic backend-aggregates / frontend-displays.
 *
 * WHY A SCROLL STRIP (not a sliding auto-carousel)
 *   Fixed-width cards (240px) in an overflow-x:auto + scroll-snap <ul> means the
 *   NEXT card peeks at the right edge — a clear affordance that there's more to
 *   scroll — while staying simple, keyboard/touch friendly, and free of the a11y
 *   pitfalls of auto-moving content.
 * ============================================================================= */

import { useI18n } from '../i18n/I18nProvider';
import type { FlyerCard } from '../types/api';

export default function CommunityInformation({ flyers }: { flyers: FlyerCard[] | null }) {
  const { t } = useI18n();

  return (
    <section className="community-info" aria-labelledby="community-title">
      <h2 id="community-title" className="community-title">
        {t('section.community')}
      </h2>

      {/* null (loading) or empty → placeholder. */}
      {(!flyers || flyers.length === 0) && (
        <p className="section-placeholder">{t('common.comingSoon')}</p>
      )}

      {flyers && flyers.length > 0 && (
        // <ul> is the scroll container; each <li> is a snap target.
        <ul className="flyer-carousel" aria-label={t('section.community')}>
          {flyers.map((flyer) => (
            <li className="flyer-card" key={flyer.imageUrl}>
              {/* alt = the flyer title (meaningful); lazy-load offscreen cards. */}
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
