/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CategoryPreviewList.tsx
 * Homepage-redesign Step 5c. See references/decisions.md Decision 020.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The "Browse by category" grid at the bottom of the homepage: one card per
 *   category showing its resource count, the latest policy update (if any), and
 *   a few recent item titles. It's a PURE display component — it takes the
 *   category data as a prop (sourced from GET /api/home by MainContent) and
 *   fetches nothing itself. Textbook "frontend displays."
 *
 * WHY THE BROWSE BUTTON IS DISABLED
 *   Clicking through to a category's full listing needs result pages + routes,
 *   which are Step 6. Rather than ship a button that silently does nothing, it's
 *   `disabled` with a "coming soon" title — an honest not-yet-wired affordance,
 *   the same stance taken for the Step-4 sidebar checkboxes. In Step 6 it becomes
 *   a real <Link>.
 *
 * CSS NAMING
 *   Uses `.category-preview*` / `.previews-*` — deliberately NOT
 *   `.category-group-header`, which already exists with a different meaning in
 *   the backend demo's styles.css (flagged across Decisions 018–020).
 * ============================================================================= */

import type { CategorySummary } from '../types/api';

export default function CategoryPreviewList({ categories }: { categories: CategorySummary[] }) {
  return (
    <section className="category-previews" aria-labelledby="previews-title">
      <h3 id="previews-title" className="previews-title">
        Browse by category
      </h3>
      {/* Responsive auto-fill grid (see index.css) — cards reflow by width. */}
      <div className="category-preview-grid">
        {categories.map((c) => (
          <article className="category-preview" key={c.key}>
            <header className="category-preview-header">
              {/* Emoji icon is decorative → aria-hidden so it's not read out. */}
              <span className="category-preview-icon" aria-hidden="true">
                {c.icon}
              </span>
              <div>
                <h4 className="category-preview-label">{c.label}</h4>
                <p className="category-preview-count">{c.resourceCount} resources</p>
              </div>
            </header>

            {/* latestPolicyUpdate is a nullable NewsItem-ish summary; render a
                highlighted line only when present (some categories have none). */}
            {c.latestPolicyUpdate && (
              <p className="category-preview-policy">📢 {c.latestPolicyUpdate.title}</p>
            )}

            {/* Up to 3 recent items (backend-capped). content.id is stable key. */}
            {c.latestItems.length > 0 && (
              <ul className="category-preview-items">
                {c.latestItems.map((item) => (
                  <li key={item.content.id}>{item.content.title}</li>
                ))}
              </ul>
            )}

            {/* Inert until Step 6 routes exist (see header note). */}
            <button
              type="button"
              className="category-browse"
              disabled
              title="Full listings coming soon"
            >
              Browse {c.label}
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}
