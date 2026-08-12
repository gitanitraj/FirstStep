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

export default function FirstStepOriginals({ originals }: { originals: ContentItem[] | null }) {
  const { t } = useI18n();

  return (
    <section className={styles.section} aria-labelledby="originals-title">
      <h2 id="originals-title" className={styles.title}>{t('home.originals')}</h2>
      <p className={styles.intro}>{t('home.originalsIntro')}</p>

      {originals === null ? (
        <p className={styles.placeholder}>{t('common.loading')}</p>
      ) : originals.length === 0 ? (
        <p className={styles.placeholder}>{t('home.originalsEmpty')}</p>
      ) : (
        <ul className={styles.list}>
          {originals.map((item) => (
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
// SECTION 4 — VISUAL WEIGHT IS A DECISION
// =============================================================================
// The right column is deliberately LIGHTER than CommunityResources: plain
// bordered items rather than icon cards, and a narrower grid track (1fr against
// 1.35fr in HomePage.module.css). Finding help is the primary job; Originals is
// discovery alongside it. Equal weight would imply equal urgency to someone who
// arrived needing rent assistance.
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
