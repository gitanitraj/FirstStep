/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/FirstStepOriginals/
 *   FirstStepOriginals.tsx + FirstStepOriginals.module.css
 *   ALSO covers MissionCards/ and SiteFooter/ (small presentational siblings).
 * Slice H. See references/decisions.md Decision 042.
 * Keep this mirror in sync whenever the production files change.
 * ============================================================================= */

import { useI18n } from '../../i18n/I18nProvider';
import type { ContentItem } from '../../types/api';
import styles from './FirstStepOriginals.module.css';

/** A front door shows a taste, not a feed — the same rule as FLYER_LIMIT. */
const ORIGINALS_LIMIT = 4;

export default function FirstStepOriginals({ originals }: { originals: ContentItem[] | null }) {
  const { t } = useI18n();
  const preview = originals?.slice(0, ORIGINALS_LIMIT) ?? null;

  return (
    <section className={styles.section} aria-labelledby="originals-title">
      {/* The masthead block stays WHITE against the orange body — the same move
          a magazine makes to separate a section's nameplate from its contents. */}
      <div className={styles.head}>
        <h2 id="originals-title" className={styles.title}>{t('home.originals')}</h2>
        <p className={styles.intro}>{t('home.originalsIntro')}</p>
      </div>

      {preview === null ? (
        <p className={styles.placeholder}>{t('common.loading')}</p>
      ) : preview.length === 0 ? (
        <p className={styles.placeholder}>{t('home.originalsEmpty')}</p>
      ) : (
        <ul className={styles.list}>
          {preview.map((item) => (
            <li key={item.id} className={styles.item}>
              <h3 className={styles.itemTitle}>{item.title}</h3>
              {item.summary && <p className={styles.summary}>{item.summary}</p>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// =============================================================================
// SECTION 1 — "ORIGINALS" IS A ContentSource QUESTION
// =============================================================================
// The component displays CivicContent First Step produced ITSELF. Membership is
// decided server-side by `contentSource.id === "first-step"` — never by a new
// ContentType, never by a new domain class (Decision 041).
//
// The data proves the distinction is real rather than invented:
//
//     faq.json            contentSource.name = "First Step"
//     expert-answers.json contentSource.name = "Delaware Volunteer Legal Services"
//     BOTH                contentType        = EXPERT
//
// Same kind of content, different producer. "We made this" vs "we publish this".
// A ContentType called ORIGINAL would have DESTROYED that distinction by
// conflating format with provenance — and ContentSource already existed to hold
// it; its `id` field was simply null in every record until this slice.
//
// SECTION 2 — WHY IT RENDERS CONTENT AND NOT A LIST OF KINDS
// =============================================================================
// The spec listed "potential content: Community Briefings, YouTube, FAQs, Data
// Stories, Articles, Newsletters". Only FAQs exist as data today.
//
// Rendering all six as pathway cards would put five dead links on the front
// door. So the section renders WHAT EXISTS — six curated FAQs — and the other
// five are future content, not future UI.
//
// The extensibility is already built and costs nothing: when briefings or data
// stories arrive carrying contentSource.id = "first-step", they appear here with
// NO CODE CHANGE. That is the payoff of identifying the section by provenance
// rather than by enumerating types.
//
// SECTION 3 — THREE STATES, NOT TWO
// =============================================================================
//   null       -> "Loading…"            payload not yet arrived
//   []         -> "Nothing published…"  First Step has published nothing
//   items      -> the list
//
// The middle case is real: it is what the section shows if the FAQ provenance is
// ever un-set. "Nothing here" and "still loading" must never look alike — the
// same rule CategoryController applies to 404-vs-empty.
//
// SECTION 4 — THE PANEL, AND WHY IT LOOKS THE WAY IT DOES
// =============================================================================
// A 380px FIXED sidebar track (HomePage.module.css), styled as a magazine
// nameplate over a body:
//
//   dark green 4px top rule   the brand mark on the panel
//   WHITE masthead block      heading + intro, separated from the contents
//   orange wash body          --surface-brand-wash, rgba(224,123,57,.55)
//
// Every one of those colours was chosen by MEASUREMENT, not by eye:
//
//   · A green wash was tried first and rejected on looks. At 8% it composited to
//     khaki (#e9e0c9) over a ground this warm and read as no change at all.
//   · SOLID --accent-color as the body would drop the summary text to 2.57:1 — a
//     flat WCAG AA failure. The 55% wash keeps item titles at 9.1:1.
//   · `.summary` uses --text-on-wash (#443f3a), not --text-secondary, which
//     measures 3.97:1 there and fails. 5.42:1 with the darker value.
//   · `.title` uses --accent-ink (#8f4009), not --accent-color: the fill orange
//     is 2.26:1 as small uppercase text. The BINDING constraint was the wash,
//     not white — deepening the panel is what forced the readable-accent token.
//   · Item dividers are rgba(28,25,23,.15). --border-color is a warm cream and
//     measures 1.43:1 against the orange — it disappears entirely.
//
// `overflow: hidden` on the section is load-bearing: without it the white
// masthead squares off the panel's rounded top corners.
//
// TOP ALIGNMENT: `margin-top: var(--sidebar-top-offset, 0)`, set by the page
// layout so this panel's top lines up with the first ROW OF CARDS rather than
// with the heading beside it. The fallback of 0 means the component still stands
// alone anywhere else — it does not need to know a heading exists next to it.
//
// The column is still deliberately LIGHTER than CommunityResources: ruled rows
// rather than icon cards. Finding help is the primary job; Originals is reading
// alongside it, and equal weight would imply equal urgency to someone who
// arrived needing rent assistance.
//
// ORIGINALS_LIMIT = 4 caps the preview, for the same reason FLYER_LIMIT caps the
// flyers — and because six unclamped entries ran ~270px past the cards beside
// them and left a hole in the main column. ⚠️ With no Originals destination page
// yet, this cap is the ONLY thing hiding entries; it needs a "see all" target
// when real briefings and data stories land.
//
// =============================================================================
// APPENDIX A — MissionCards (components/MissionCards/)
// =============================================================================
// Discover · Connect · Stay Informed. Takes NO PROPS and queries nothing, which
// is the whole point: these are UX PATHWAYS, NOT DOMAIN CONCEPTS, so the
// homepage payload has no field for them.
//
// They are deliberately EQUAL IN IMPORTANCE — one grid track each, no featured
// variant, no accent on one. A visitor who does not yet know what they need must
// not be steered, and that restraint is enforced in the CSS (the high-contrast
// block keeps all three actions identical too).
//
// `.action` uses `margin-top: auto` so all three buttons align on a shared
// baseline regardless of how long the copy runs — otherwise the longest card
// would push its button down and imply a hierarchy the design denies.
//
// Two of the three destinations are stubs on the day this ships. Expected: the
// front door composes, then G and H's follow-on work fill in behind it.
//
// =============================================================================
// APPENDIX B — SiteFooter (components/SiteFooter/)
// =============================================================================
// ⚠️ THE CONTENT IS MOCK. v1 had no footer, so there was nothing to carry over.
// The STRUCTURE is the deliverable — brand line, quick links, contact, a
// verification note, attribution — so real copy drops into the same slots.
//
// Mock values are written to be OBVIOUSLY placeholder (`hello@example.org`, a
// "PLACEHOLDER CONTACT" label in accent colour) rather than plausible. A
// believable-but-fake phone number on a civic resource site is a number a
// resident in difficulty would actually try to call. Fake data that looks real
// is worse than fake data that looks fake.
//
// The verification line is NOT filler. 04-editorial-principles.md requires that
// First Step "encourage residents to connect with the originating organization
// rather than replacing it", and the footer is where that belongs on every page.
// It should survive the mock-content replacement.
// =============================================================================
