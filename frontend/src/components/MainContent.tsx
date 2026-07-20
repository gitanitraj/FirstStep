import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import type { HomePayload } from '../types/api';
import HeroGuidance from './HeroGuidance';
import ImportantUpdates from './ImportantUpdates';
import CategoryPreviewList from './CategoryPreviewList';

/**
 * The homepage main column. Step 5c wires the single-request architecture: one
 * GET /api/home fetch on mount supplies the hero's config, the Important Updates
 * seed, and the category previews — the browser stitches nothing itself.
 *
 * The hero renders immediately (with its own default config) so the primary
 * call-to-action never waits on the network; once /api/home resolves its real
 * aiConfig takes over and the data sections mount seeded. Important Updates then
 * keeps itself fresh by polling /api/updates.
 */
export default function MainContent() {
  const [home, setHome] = useState<HomePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<HomePayload>('/api/home')
      .then(setHome)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <>
      <HeroGuidance aiConfig={home?.aiConfig ?? null} />
      {error && <p role="alert">Couldn’t load homepage content: {error}</p>}
      {!error && !home && <p className="section-placeholder">Loading updates…</p>}
      {home && (
        <>
          <ImportantUpdates initialUpdates={home.updates} />
          <CategoryPreviewList categories={home.categories} />
        </>
      )}
    </>
  );
}
