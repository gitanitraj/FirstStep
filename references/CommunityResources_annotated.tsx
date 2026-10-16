/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CommunityResources/
 *   CommunityResources.tsx + CommunityResources.module.css
 * Slice H. See references/decisions.md Decision 042.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage's LEFT column: seven icon cards, each a discovery pathway into
 *   existing CivicContent. The most recognisably "original First Step" section
 *   on the page — v1's whole homepage was a short list of large, obvious,
 *   icon-led choices, and this is that, widened from four to seven.
 *
 * WHY IT MATTERS ARCHITECTURALLY
 *   This is where Decision 041's central distinction becomes something a user
 *   can click. Six pathways are categories; one is not. The component routes
 *   them differently, and that difference is the guardrail made executable.
 * ============================================================================= */

import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { ResourcePathway } from '../../types/api';
import styles from './CommunityResources.module.css';

interface Props {
  pathways: ResourcePathway[] | null;
}

export default function CommunityResources({ pathways }: Props) {
  const { t } = useI18n();

  // THE LOAD-BEARING THREE LINES OF THIS SLICE.
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
                <span className={styles.icon} aria-hidden="true">{pathway.icon}</span>
                <span className={styles.label}>{pathway.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// =============================================================================
// SECTION 1 — WHY `kind` DECIDES THE URL
// =============================================================================
//     category  → /category/{key}    a canonical taxonomy category
//     discovery → /discover/{key}    a controlled query over CivicContent metadata
//
// Seniors is a legitimate resident discovery need, so it belongs on this list.
// It is NOT a category, and it must never become one: a category answers "what
// is this ABOUT?", and Seniors answers "who is this RELEVANT TO?" — a question
// the taxonomy never asked (01-domain-model.md's three-question test).
//
// Routing it to /category/seniors would assert a taxonomy entry that does not
// and must not exist. The page would 404 today, and the "obvious fix" would be
// to add Seniors to taxonomy.json — which is exactly the failure Decision 041
// exists to prevent. Splitting the URL namespace makes the wrong fix unappealing
// and the right one obvious.
//
// THIS IS PINNED BY A TEST, deliberately phrased as the thing that must not
// happen: "should route a discovery pathway away from the category namespace."
// If someone simplifies `href` to a single template string, that test fails.
//
// SECTION 2 — WHY THE LIST IS A PROP, NOT DERIVED
// =============================================================================
// The seven pathways and their order come from app/data/homepage.json via
// PathwayService. The component does not know which categories exist, does not
// sort, and does not decide what to show. It cannot: which seven of ten
// categories a resident sees first is an editorial judgement, and one of the
// seven is not a category at all.
//
// The upside is that reordering the homepage is a data edit, not a deploy.
//
// The taxonomy still owns the vocabulary — a category pathway's label and icon
// are RESOLVED from taxonomy.json server-side, never authored twice. So the
// strings this component renders for Housing are the same strings the category
// page renders, by construction rather than by discipline.
//
// SECTION 3 — THE TWO EMPTY STATES ARE DIFFERENT
// =============================================================================
//   pathways === null   -> "Loading…"      the payload has not arrived
//   pathways.length 0   -> heading only    homepage.json is missing/empty
//
// The second is the UI half of PathwayService's degrade-don't-throw contract: a
// missing presentation file empties one column rather than taking the site down.
// Both are tested, because "nothing here yet" and "still loading" must not look
// alike — the same principle as CategoryController's 404-vs-empty rule.
//
// SECTION 3b — THE ARROW IS AN AFFORDANCE, AND IT IS aria-hidden
// =============================================================================
// Each card ends with an arrow inside a circle. A bordered box does not, on its
// own, say "this is clickable" — especially to a resident who is not a confident
// web user, which is a large share of this audience. The arrow says it, and it
// leans into the card's hover so the two read as one gesture.
//
// It is `aria-hidden` because the WHOLE CARD is the link and the label already
// names the destination. Announcing "Housing, arrow" adds a word and no
// information. Decoration that is visible to the eye and invisible to a screen
// reader is exactly what aria-hidden is for.
//
// SECTION 4 — STYLING NOTES
// =============================================================================
// Co-located CSS Module per the standing convention. The grid is
// `repeat(auto-fill, minmax(300px, 1fr))` so it fills whatever width the main
// column has rather than committing to a column count — the page is full width
// (043), so that width varies a great deal between a laptop and a wide monitor.
//
// The icon is aria-hidden for the same reason as the arrow: the label already
// names the pathway, so a screen reader would otherwise announce "house emoji
// Housing".
//
// The high-contrast block uses :global(body.high-contrast) — the ONE sanctioned
// escape hatch. It is needed because `body.high-contrast a` has specificity
// (0,1,2) and a CSS Module class has (0,1,0), so without an explicit rule every
// card label turns yellow and the hover state collapses into a no-op. That is
// the same specificity trap Decision 039 documented, seen from the other side:
// there, deleting a rule let the generic link colour win; here, omitting one
// would.
// =============================================================================
