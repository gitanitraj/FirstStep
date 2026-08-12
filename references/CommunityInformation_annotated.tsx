/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CommunityInformation/
 *   CommunityInformation.tsx + CommunityInformation.module.css
 * Slice E originally (Decision 025); REWRITTEN in Slice H (Decision 042).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   Information originating from the community: a heading, one link out, and
 *   three flyer images. That is the whole section.
 *
 * WHY IT IS THE MOST INTERESTING SECTION IN THE SLICE
 *   It is where a documented data-model gap turned out not to be a problem at
 *   all — twice over, and the second time by deleting the thing that solved it
 *   the first time. See Section 1.
 * ============================================================================= */

import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { FlyerCard } from '../../types/api';
import styles from './CommunityInformation.module.css';

interface Props {
  flyers: FlyerCard[] | null;
}

/** How many flyer images the homepage shows. The rest live on the Community page. */
const FLYER_LIMIT = 3;

export default function CommunityInformation({ flyers }: Props) {
  const { t } = useI18n();
  const preview = flyers?.slice(0, FLYER_LIMIT) ?? [];

  return (
    <section className={styles.section} aria-labelledby="community-title">
      <div className={styles.head}>
        <h2 id="community-title" className={styles.title}>{t('section.community')}</h2>
        <Link className={styles.more} to="/community">{t('community.viewAll')}</Link>
      </div>
      <p className={styles.intro}>{t('community.intro')}</p>

      {preview.length > 0 && (
        <ul className={styles.flyers}>
          {preview.map((flyer) => (
            <li className={styles.flyerCard} key={flyer.imageUrl}>
              <img className={styles.flyerImage} src={flyer.imageUrl} alt={flyer.title} loading="lazy" />
              <div className={styles.flyerBody}>
                <p className={styles.flyerTitle}>{flyer.title}</p>
                <p className={styles.flyerMeta}>…organization · eventDate…</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// =============================================================================
// SECTION 1 — THE GAP THAT DISSOLVED
// =============================================================================
// The Front Door scoping pass (Decision 041) recorded gap 4:
//
//     Community Information's four groups have no distinguishing metadata. All
//     seven flyers are contentSource.type: "manual". Nothing separates a meeting
//     from an announcement.
//
// The obvious readings were both bad. Four new ContentTypes would take the enum
// from 5 to 8 values answering CivicContent's six questions identically — types
// distinguished by nothing the system can act on. Reaching for `subcategory`
// would put a display concern inside editorial classification, which is the
// `includesFlyers` mistake in a new costume (F1 spent a slice removing that one).
//
// THE HOMEPAGE DOES NOT NEED TO GROUP ANYTHING. It needs to offer a way in.
//
// Slice H answered that with three LINKS to /community — Events, Meetings,
// Announcements — rather than three filtered lists. The grouping question moved
// to the destination page, and no flyer metadata was needed.
//
// THEN THE THREE LINKS WENT TOO (043). Seeing them rendered, they were three
// text links stacked above a "See all community information" link that already
// went to the same page — the same destination offered four times. The flyers
// are the community's own voice, and they carry their own images and dates.
//
// The section now says more by showing less, and the underlying point survives
// unchanged: it still groups nothing, so it still needs no flyer metadata. The
// gap did not reopen when the cards were removed, which is a decent test that
// the original reasoning was about the SHAPE of the page rather than about those
// three particular cards.
//
// That is "complexity belongs behind the front door" paying for itself in a way
// worth remembering: **a data-model gap can be a symptom of asking a page to do
// a destination's job.** Before adding a field to satisfy a screen, check whether
// the screen should have been asking.
//
// A test now pins the END state instead: the section exposes exactly ONE link,
// and it goes to /community. If a future change starts filtering here, or adds
// a fourth route to the same page, that count is where it surfaces.
//
// =============================================================================
// SECTION 2 — WHY THE CAROUSEL BECAME A CAPPED PREVIEW
// =============================================================================
// Slice E built a horizontal scroll-snap carousel of every flyer. Slice H shows
// THREE, in a plain grid.
//
// A carousel is a browsing affordance, and browsing is a destination-page job.
// On a front door it asks the visitor to work — scroll sideways through an
// unknown number of items — before they know whether any of it is relevant. The
// capped preview says "this exists, here is a taste, the rest is one click away".
//
// FLYER_LIMIT is a named constant rather than an inline `.slice(0, 3)` because
// it encodes an editorial decision about the homepage's density, and a test
// asserts it holds when more flyers are available.
//
// `object-fit: contain`, NOT `cover` (043). These are POSTERS — the words are
// the content. `cover` cropped "FREE LEGAL HELP IS AVAILABLE FOR DELAWARE
// RENTERS" mid-line and cut both edges off another flyer. Letterboxing costs a
// little polish and keeps the flyer readable, which is the entire reason for
// showing an image rather than a title.
//
// `imageUrl` is already resolved and URL-encoded SERVER-side (Decision 025 —
// seasonal images only serve at that static path when %20-encoded), so this
// component only displays. That has not changed and must not: doing it here
// would put a serving detail in the browser.
//
// =============================================================================
// SECTION 3 — WHAT SURVIVES WHEN DATA DOES NOT
// =============================================================================
// The heading, intro and "See all" link render unconditionally; only the flyer
// list is guarded. A section whose entire value disappears when a feed is empty
// is a fragile section, and this one's remaining value is the way in.
//
// Tested explicitly with `flyers={null}`: heading present, no list items.
//
// =============================================================================
// SECTION 4 — MIGRATION NOTE
// =============================================================================
// Moved from a flat `components/CommunityInformation.tsx` with global
// `.community-*` / `.flyer-*` rules in index.css to a co-located CSS Module,
// because this slice was rewriting it anyway. That is the standing rule working
// as designed — components migrate when a slice touches them, never in a
// churn-only diff. Its old rules left index.css as part of the same change.
// =============================================================================
