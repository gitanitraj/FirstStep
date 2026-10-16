/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CategoryUpdates.tsx
 * Slice F5b. See references/decisions.md Decision 037 (and 036 for the aggregate).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   "Stay Informed" — the category page's what-has-changed feed: news, signed
 *   legislation, flyers and expert answers, newest first, capped at 6 server-side.
 *
 * WHY IT EXISTS AT ALL (the F5a argument, made visible)
 *   Every item in this feed carries a CATEGORY and NO SUBCATEGORY, so no topic
 *   tile can reach it — 193 of 429 classified items (Decision 036). The
 *   classifier will not invent a topic for them, so the page reaches them by
 *   COMPOSITION instead. This section IS that composition. Delete it and half of
 *   most categories becomes unreachable: utilities would go back to being an
 *   entirely empty page.
 *
 * THE ONE RULE THAT MATTERS HERE
 *   BADGES ARE DERIVED FROM `contentType`, NEVER FROM `type`. See the long note
 *   at the bottom — this is the first place the Decision 036 exit criterion is
 *   actually honoured, not just written down.
 * ============================================================================= */

import { useI18n } from '../i18n/I18nProvider';
import { CONTENT_TYPE_LABEL } from '../i18n/contentTypeLabel';
import type { UpdateItem } from '../types/api';

// The label map moved to i18n/contentTypeLabel.ts in Slice F6, once ContentCard
// became a second consumer — an abstraction earning its name on the second use,
// the same rule the backend services follow. Still exhaustively keyed by
// ContentType there, so a new content type without a label fails the build.

interface Props {
  updates: UpdateItem[];
  lastUpdated: string | null;
}

export default function CategoryUpdates({ updates, lastUpdated }: Props) {
  const { t } = useI18n();

  return (
    <section className="category-section category-updates" aria-labelledby="category-updates-title">
      <div className="category-section-head">
        <h2 id="category-updates-title" className="category-section-title">
          {t('category.updates')}
        </h2>
        {/* Safe to display: lastUpdated comes from EDITORIAL dates (bill signed,
            news published, expert spoke), never Resource.updatedDate — that one is
            a load-date proxy and showing it would imply a freshness guarantee the
            data cannot back. A category holding only resources shows nothing here. */}
        {lastUpdated && (
          <span className="category-updated">
            {t('category.latest')} {lastUpdated}
          </span>
        )}
      </div>

      {updates.length === 0 ? (
        <p className="section-placeholder">{t('category.noUpdates')}</p>
      ) : (
        <ul className="category-update-list">
          {updates.map((u) => (
            // Keyed on contentType+id rather than id alone: ids are unique per
            // source, not globally, so a flyer and a news item could collide.
            <li className="category-update" key={`${u.contentType}-${u.id}`}>
              <div className="category-update-head">
                <span className={`category-badge badge-${u.contentType.toLowerCase()}`}>
                  {t(CONTENT_TYPE_LABEL[u.contentType])}
                </span>
                {/* Link out only when the source gave us a URL (news and laws do;
                    flyers and expert answers do not). Editorial standard:
                    encourage residents to reach the originating organization
                    rather than replacing it. */}
                {u.url ? (
                  <a
                    className="category-update-title"
                    href={u.url}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {u.title}
                  </a>
                ) : (
                  <span className="category-update-title">{u.title}</span>
                )}
              </div>
              {u.summary && <p className="category-update-summary">{u.summary}</p>}
              <p className="category-update-meta">
                {u.source && <span>{u.source}</span>}
                {u.source && u.date && <span> · </span>}
                {u.date && <span>{u.date}</span>}
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// =============================================================================
// contentType, NOT type — THE EXIT CRITERION IN PRACTICE
// =============================================================================
// UpdateItem carries BOTH fields today. `type` is the legacy string
// ("news" | "flyer" | "expert"); `contentType` is the CivicContent enum
// (RESOURCE · NEWS · LAW · FLYER · EXPERT).
//
// READING `type` HERE WOULD BE A BUG, not a style choice. It reports "news" for
// BOTH curated news and signed legislation, so this feed would render
//
//     [News] Relating to Rent Increases.        <- actually a signed law
//     [News] SRAP waitlist opens
//
// and a resident could not tell a change in the law from an announcement. That
// conflation is the entire reason contentType was added in F5a.
//
// It is also the Decision 036 exit criterion honoured at its first opportunity:
//
//     Slice H retires UpdateItem.type. contentType becomes the SINGLE semantic
//     identifier for CivicContent. Any presentation labels or badges are derived
//     from contentType by the frontend.
//
// Because this component never touches `type`, Slice H's deletion of that field
// will not require a single edit here. The label map above IS the "derived by the
// frontend" half of the criterion — the backend sends what a thing IS, and the
// display string is chosen in the UI where it belongs, translated per locale.
//
// Pinned by two tests that would fail if someone "simplified" this back to
// `type`: shouldBadgeALawAndCuratedNewsDifferently, and CategoryPage.test's
// fixture where type and contentType DELIBERATELY DISAGREE.
// =============================================================================

// =============================================================================
// ACCESSIBILITY NOTES
// =============================================================================
// - The badge is TEXT, not a colour swatch. In high contrast every badge
//   collapses to the same black-on-yellow (see index.css), so the LABEL carries
//   the distinction and the colour is decoration. A colour-only indicator would
//   have become unreadable in exactly the theme that needs it most.
// - `aria-labelledby` ties the section landmark to its visible heading, matching
//   every other section in the app.
// - External links get rel="noopener noreferrer" with target="_blank".
// =============================================================================
