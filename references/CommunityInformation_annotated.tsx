/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CommunityInformation/
 *   CommunityInformation.tsx + CommunityInformation.module.css
 * Slice E originally (Decision 025); REWRITTEN in Slice H (Decision 042);
 * RENAMED and re-pointed in Slice J (Decision 046).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage's preview of community-produced information: a heading, one
 *   link out, and three flyer images. That is the whole section.
 *
 * NOTE ON THE FILE NAME
 *   The component is still called CommunityInformation; the SECTION is called
 *   Community Notices. The file was left alone deliberately — renaming it would
 *   have churned imports, the CSS Module, the test file and the mirror in a diff
 *   whose actual content is three strings. Recorded here so the mismatch reads
 *   as a decision rather than an oversight; it is cheap debt to pay off in
 *   whichever slice next has reason to open this file.
 *
 * WHY IT IS THE MOST INTERESTING SECTION IN THE SLICE
 *   It is where a documented data-model gap turned out not to be a problem at
 *   all — twice over, and the second time by deleting the thing that solved it
 *   the first time. Then Slice J answered it properly. See Section 1.
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

/**
 * Community Notices — the homepage's preview of community-produced information.
 *
 * RENAMED from "Community Information" in Slice J, and the rename carries
 * meaning: **community-produced information is not the same thing as community
 * resources.** The row above this one (Community Resources) is services a
 * resident can USE; this is what organisations are TELLING the neighbourhood.
 * Two different questions, and the labels now say so.
 *
 * It links to /community-notices, where Events · Meetings · Announcements ·
 * Flyers are four discovery views over the same content.
 *
 * **Flyers, and nothing else here.** An earlier draft also carried three pathway
 * cards (Upcoming Events · Meeting Notices · Announcements) above the images.
 * They were removed: the flyers ARE the community's own voice, they carry their
 * own images and dates, and three text links above them restated what the "See
 * all" link already offered. The section says more by showing less.
 *
 * That removal did not resurrect the data-model question those cards sidestepped.
 * Slice J has since ANSWERED it: a controlled kind vocabulary (event · meeting ·
 * announcement) in `tags`, and the four views live on the destination page where
 * they always belonged. This row still groups nothing — it previews, and links.
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
          {t('notices.title')}
        </h2>
        <Link className={styles.more} to="/community-notices">
          {t('notices.homeViewAll')}
        </Link>
      </div>
      <p className={styles.intro}>{t('notices.homeIntro')}</p>

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

// =============================================================================
// SECTION 1 — THE GAP THAT DISSOLVED, AND WAS LATER ANSWERED
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
// Slice H answered that with three LINKS — Events, Meetings, Announcements —
// rather than three filtered lists. The grouping question moved to the
// destination page, and no flyer metadata was needed.
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
// SLICE J CLOSED IT AT THE DESTINATION (046). The gap was real — it was just
// never the homepage's. Measured against actual data, two of the four views were
// not derivable from any existing field ("meeting" appeared in 0 of 21 records)
// and a third would have duplicated Flyers. The answer was a controlled kind
// vocabulary in `tags`, declared in taxonomy.json and consumed by
// CommunityNoticesService — editorial metadata the producer already knows, not a
// frontend-invented classification.
//
// Note where that vocabulary is NOT read: here. This component still groups
// nothing. The homepage was right to stay out of it, start to finish.
//
// A test pins the end state: the section exposes exactly ONE link, and it now
// goes to /community-notices. If a future change starts filtering here, or adds
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

// =============================================================================
// SECTION 5 — THE RENAME CARRIES MEANING (Slice J)
// =============================================================================
// "Community Information" became "Community Notices", and the link moved from
// /community to /community-notices.
//
// The rename is not cosmetic. The row directly above this one is Community
// RESOURCES — services a resident can USE. This row is what organizations are
// TELLING the neighborhood. Two genuinely different questions that had been
// sitting under two labels a resident could not tell apart, one of which
// ("Information") could plausibly have meant either.
//
// The link change is the other half: before Slice J, /community-notices existed
// as a URL with NO entry point anywhere in the product — reachable only by
// typing it. A destination nothing links to is not a destination. Slice I's own
// closing note flagged this as the first thing to fix, and this is the fix.
//
// Copy lives in the shared `notices.*` i18n namespace rather than a homepage-
// specific one, so the section heading and the destination's <h1> are the same
// string in both languages and cannot drift apart.
// =============================================================================
