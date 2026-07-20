import type { CategorySummary } from '../types/api';

/**
 * Step 5c — the CategoryPreviewList. Renders one card per category showing its
 * resource count, the latest policy update (if any), and a few recent items.
 * Data comes from GET /api/home (categories), so this component just displays.
 *
 * The Browse control is intentionally inert this step — result pages and routes
 * arrive in Step 6, at which point it becomes a real link.
 */
export default function CategoryPreviewList({ categories }: { categories: CategorySummary[] }) {
  return (
    <section className="category-previews" aria-labelledby="previews-title">
      <h3 id="previews-title" className="previews-title">
        Browse by category
      </h3>
      <div className="category-preview-grid">
        {categories.map((c) => (
          <article className="category-preview" key={c.key}>
            <header className="category-preview-header">
              <span className="category-preview-icon" aria-hidden="true">
                {c.icon}
              </span>
              <div>
                <h4 className="category-preview-label">{c.label}</h4>
                <p className="category-preview-count">{c.resourceCount} resources</p>
              </div>
            </header>

            {c.latestPolicyUpdate && (
              <p className="category-preview-policy">📢 {c.latestPolicyUpdate.title}</p>
            )}

            {c.latestItems.length > 0 && (
              <ul className="category-preview-items">
                {c.latestItems.map((item) => (
                  <li key={item.content.id}>{item.content.title}</li>
                ))}
              </ul>
            )}

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
