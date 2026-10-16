/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/Sidebar.tsx
 * Homepage-redesign Step 4 (AppLayout + Sidebar). See references/decisions.md
 * Decision 017. Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The left-hand category filter of the redesigned homepage. It fetches the
 *   live category list and renders one checkbox per category, each showing the
 *   category's resource count.
 *
 * WHY IT LOOKS THE WAY IT DOES (the two Step-4 scope decisions)
 *   1. LOCAL-TOGGLE ONLY. The checked state lives entirely in this component
 *      (`checked`, a Set of category keys). Clicking a box flips it visibly but
 *      changes nothing elsewhere — there is no content to filter yet (that's
 *      Steps 5–6) and the *shared* filter context is explicitly Step 7. Wiring
 *      a React Context now would be an abstraction with no consumer, which the
 *      project's "Simplicity First" rule forbids.
 *   2. SELF-CONTAINED FETCH. The component fetches its own data rather than
 *      receiving it as a prop. With a single consumer this is the simplest
 *      thing that works; if Step 7 needs the category list in more than one
 *      place, the fetch can be lifted then (YAGNI until then).
 * ============================================================================= */

// React hooks: useState for local component state, useEffect for the one-time
// data fetch on mount.
import { useEffect, useState } from 'react';

// apiGet<T> — the typed wrapper from Decision 016 that unwraps the backend's
// ApiResponse<T> envelope (success/data/errorMessage) and throws on failure.
// Reused here instead of a raw fetch(), exactly as the roadmap intends.
import { apiGet } from '../api/client';

// CategorySummary — existing shared type (key/label/icon/resourceCount + the
// latestItems/latestPolicyUpdate fields Step 5 will use). No new type needed.
import type { CategorySummary } from '../types/api';

/**
 * Category filter sidebar (Step 4). Fetches the live category list and renders a
 * checkbox per category with its resource count. Checked state is local-only —
 * toggling drives nothing downstream yet; real filtering is wired to content in
 * Step 7 (shared filter context).
 */
export default function Sidebar() {
  // Three independent pieces of state:
  //  - categories: null until the fetch resolves (drives the loading message).
  //  - error: the message string if the fetch/envelope failed (role="alert").
  //  - checked: the Set of category keys the user has ticked. A Set (not an
  //    array) gives O(1) has()/add()/delete() and clean membership semantics.
  const [categories, setCategories] = useState<CategorySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checked, setChecked] = useState<Set<string>>(new Set());

  // Fetch once on mount. Empty dependency array => runs a single time. Mirrors
  // the loading/error handling the Step-3 App.tsx used, moved here now that the
  // Sidebar owns the category data.
  useEffect(() => {
    apiGet<CategorySummary[]>('/api/categories')
      .then(setCategories)
      .catch((err: Error) => setError(err.message));
  }, []);

  // Immutable toggle: build a NEW Set from the previous one, then add/remove the
  // key. Returning a fresh object is required for React to detect the change and
  // re-render — mutating `prev` in place would not trigger an update.
  function toggle(key: string) {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  return (
    // <aside> is the correct landmark element for complementary sidebar content.
    <aside className="home-sidebar">
      <div className="sidebar-section">
        <h2 className="sidebar-title">Categories</h2>

        {/* Error and loading are mutually exclusive with the list. role="alert"
            makes the error announced by screen readers. */}
        {error && <p role="alert">Error: {error}</p>}
        {!error && !categories && <p>Loading categories…</p>}

        {/* Optional chaining: nothing renders until `categories` is populated. */}
        {categories?.map((c) => (
          // The <label> wraps the input and its text so the whole row is
          // clickable, and htmlFor/id ties label→input explicitly for a11y and
          // for Testing Library's getByLabelText.
          <label className="category-checkbox" htmlFor={`cat-${c.key}`} key={c.key}>
            <input
              type="checkbox"
              id={`cat-${c.key}`}
              checked={checked.has(c.key)} // controlled input driven by our Set
              onChange={() => toggle(c.key)}
            />
            {/* The emoji icon is decorative; aria-hidden keeps it out of the
                accessible name so the checkbox reads as just its label+count. */}
            <span aria-hidden="true">{c.icon}</span>
            <span>{c.label}</span>
            {/* margin-left:auto (in CSS) pushes the count to the right edge. */}
            <span className="category-count">{c.resourceCount}</span>
          </label>
        ))}
      </div>
    </aside>
  );
}
