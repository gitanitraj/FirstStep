/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/ContentCard/ContentCard.tsx
 * Slice F6. See references/decisions.md Decision 040 (CSS convention = 039).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   ONE card design for every kind of CivicContent — Decision 021's "one
 *   consistent card design that labels content type and source", finally built.
 *
 * WHY IT IS THE FIRST CSS MODULES COMPONENT
 *   Decision 039 set the convention; this is its first application. Styles are
 *   co-located and locally scoped, so `.card` here CANNOT collide with anything
 *   — including the CSS of a component deleted three slices ago, which is what
 *   caused the two bugs that motivated the whole exercise. Verified live: the
 *   rendered class is `_card_1kq6h_8`, not `card`.
 *
 * AND WHY IT NEEDS NO :global()
 *   Every colour is a token. High contrast is token redefinition (039 Phase 2),
 *   so this component is themed for free — measured live at rgb(0,0,0) surface
 *   and rgb(255,255,0) border with zero theme rules of its own. That is the
 *   payoff of Phase 2 demonstrated on the first component to depend on it.
 * ============================================================================= */

import { useI18n } from '../../i18n/I18nProvider';
import { CONTENT_TYPE_LABEL } from '../../i18n/contentTypeLabel';
import type { ContentItem } from '../../types/api';
import styles from './ContentCard.module.css';

export default function ContentCard({ item }: { item: ContentItem }) {
  const { t } = useI18n();

  // Many directory records name the resource after its provider ("American Red
  // Cross" / "American Red Cross"), so repeating it below the title wastes a
  // line on every card. Show the organization only when it adds something.
  const provider = item.organization !== item.title ? item.organization : null;

  // "standard" is the ABSENCE of urgency, not a level of it. Rendering it as a
  // chip gives every ordinary resource a badge that means nothing — the same
  // check ImportantUpdates already makes.
  const urgency = item.urgency && item.urgency.toLowerCase() !== 'standard' ? item.urgency : null;

  return (
    <li className={styles.card}>
      <div className={styles.head}>
        {/* The per-type modifier is COMPOSED from the enum value, so adding a
            content type needs one CSS rule and no branch here. The `?? ''`
            guards a type that has no modifier rule yet. */}
        <span className={`${styles.badge} ${styles[`badge${item.contentType}`] ?? ''}`}>
          {t(CONTENT_TYPE_LABEL[item.contentType])}
        </span>
        {/* Link out when the provider gave us somewhere to go. Editorial
            standard: connect residents to the originating organization rather
            than replacing it. */}
        {item.url ? (
          <a className={styles.title} href={item.url} target="_blank" rel="noopener noreferrer">
            {item.title}
          </a>
        ) : (
          <span className={styles.title}>{item.title}</span>
        )}
      </div>

      {(provider || item.location) && (
        <p className={styles.provider}>
          {provider && <span>{provider}</span>}
          {provider && item.location && <span aria-hidden="true"> · </span>}
          {item.location && <span>{item.location}</span>}
        </p>
      )}

      {item.summary && <p className={styles.summary}>{item.summary}</p>}

      {(item.cost || urgency || item.date) && (
        <p className={styles.facts}>
          {item.cost && <span className={styles.cost}>{item.cost}</span>}
          {urgency && <span className={styles.urgency}>{urgency}</span>}
          {item.date && <span className={styles.date}>{item.date}</span>}
        </p>
      )}
    </li>
  );
}

// =============================================================================
// ONE MARKUP, NO PER-TYPE BRANCHING
// =============================================================================
// A resource has an organization, a city, a cost and an urgency. A flyer has a
// date. A law has a URL. The card renders the SAME elements for all of them and
// simply omits what is null — there is no `if (contentType === 'FLYER')`
// anywhere, and there must not be.
//
// That is only possible because ContentItem is normalized SERVER-side. The
// browser never asks "what kind of object is this?" before it can read a title,
// which is the same rule UpdateItem follows for the updates feeds and the reason
// the BFF exists at all.
//
// The one thing that DOES vary by type is the badge, and even that is a label
// lookup plus a colour modifier — data, not control flow.
// =============================================================================

// =============================================================================
// TWO DEFECTS CAUGHT IN LIVE VERIFICATION, NOT BY TESTS
// =============================================================================
// Both were visible on EVERY card of the first real page render, and both are in
// this component's own logic rather than in the data or the backend:
//
//   1. TITLE/ORGANIZATION DUPLICATION. Directory records frequently name a
//      resource after its provider, so ten cards each read "American Red Cross"
//      then "American Red Cross · Wilmington". Fixed by suppressing the
//      organization when it equals the title.
//
//   2. "STANDARD" URGENCY AS A CHIP. `urgency: "standard"` means ordinary, so
//      every non-urgent resource wore a badge announcing it was not urgent.
//      ImportantUpdates already skipped it; this now matches.
//
// Neither is a rendering error — the DOM was exactly what the code asked for.
// They are JUDGEMENT errors, and only looking at the page surfaces those. Worth
// remembering when deciding whether a screenshot pass is optional.
//
// Both are now pinned by tests, so the judgement survives the next refactor.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Renders shared/dto/ContentItem, produced by TopicPageService.
// - Uses i18n/contentTypeLabel.ts, extracted in this slice once a SECOND
//   component needed the map (CategoryUpdates was the first) — the same
//   "earn it on the second use" rule the backend services follow.
// - Renders as an <li>: it is always a list item, and the parent owns the <ul>.
//   That keeps list semantics correct for assistive tech rather than nesting a
//   list inside each card.
// - Styles: ./ContentCard.module.css, locally scoped, tokens only.
//
// LIKELY NEXT CONSUMERS: the topic page uses it today. Search results, the
// Important Notices page (H) and the Front Door's Latest Updates are the obvious
// next ones — at which point CategoryUpdates should render ContentCard too, and
// UpdateItem/ContentItem should converge alongside the Slice H `type` removal.
// =============================================================================
