import { useEffect, useState } from 'react';
import { apiGet } from './api/client';
import type { CategorySummary } from './types/api';

export default function App() {
  const [categories, setCategories] = useState<CategorySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<CategorySummary[]>('/api/categories')
      .then(setCategories)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <div>
      <h1>First Step</h1>
      <p>React frontend scaffold — Step 3 of the homepage redesign roadmap.</p>
      {error && <p role="alert">Error: {error}</p>}
      {!error && !categories && <p>Loading categories…</p>}
      {categories && (
        <ul>
          {categories.map((c) => (
            <li key={c.key}>
              {c.icon} {c.label} — {c.resourceCount} resources
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
