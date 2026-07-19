import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import type { CategorySummary } from '../types/api';

/**
 * Category filter sidebar (Step 4). Fetches the live category list and renders a
 * checkbox per category with its resource count. Checked state is local-only —
 * toggling drives nothing downstream yet; real filtering is wired to content in
 * Step 7 (shared filter context).
 */
export default function Sidebar() {
  const [categories, setCategories] = useState<CategorySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checked, setChecked] = useState<Set<string>>(new Set());

  useEffect(() => {
    apiGet<CategorySummary[]>('/api/categories')
      .then(setCategories)
      .catch((err: Error) => setError(err.message));
  }, []);

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
    <aside className="home-sidebar">
      <div className="sidebar-section">
        <h2 className="sidebar-title">Categories</h2>
        {error && <p role="alert">Error: {error}</p>}
        {!error && !categories && <p>Loading categories…</p>}
        {categories?.map((c) => (
          <label className="category-checkbox" htmlFor={`cat-${c.key}`} key={c.key}>
            <input
              type="checkbox"
              id={`cat-${c.key}`}
              checked={checked.has(c.key)}
              onChange={() => toggle(c.key)}
            />
            <span aria-hidden="true">{c.icon}</span>
            <span>{c.label}</span>
            <span className="category-count">{c.resourceCount}</span>
          </label>
        ))}
      </div>
    </aside>
  );
}
