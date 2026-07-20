/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/MainContent.tsx
 * Homepage-redesign Step 5c (was a thin placeholder in 5a/5b). See
 * references/decisions.md Decision 020. Keep this mirror in sync with the source.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage main column AND its single data-loader. In 5c it became the
 *   orchestrator of the "one request" architecture: it makes ONE GET /api/home
 *   call on mount and hands each slice of the payload to the right child.
 *
 * THE KEY DESIGN CHOICE (Decision 020): HERO INSTANT, DATA SECTIONS DEFERRED
 *   - <HeroGuidance> is ALWAYS rendered, immediately, with `aiConfig={home?.aiConfig
 *     ?? null}`. Before /api/home resolves that's null, so the hero falls back to
 *     its own DEFAULT_AI_CONFIG — the primary call-to-action never waits on the
 *     network. When the payload arrives, the real backend aiConfig takes over.
 *   - <ImportantUpdates> and <CategoryPreviewList> render only AFTER `home` is
 *     set. That's what lets ImportantUpdates be SEEDED (initialUpdates=home.updates)
 *     and therefore skip its own mount fetch — no duplicate /api/updates request on
 *     load. It still polls /api/updates afterwards for live refresh.
 *   - On error, the hero still works (default config) and a message stands in for
 *     the data sections. The page is never fully dead.
 *
 * WHY NOT ALSO FEED THE SIDEBAR FROM /api/home?
 *   The Sidebar (separate subtree under AppLayout) still calls /api/categories on
 *   its own. Lifting that into a shared fetch is Step 7 (shared context); doing it
 *   now would mean reworking finished Step-4 components. Accepted cost: categories
 *   load twice until then.
 * ============================================================================= */

import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import type { HomePayload } from '../types/api';
import HeroGuidance from './HeroGuidance';
import ImportantUpdates from './ImportantUpdates';
import CategoryPreviewList from './CategoryPreviewList';

export default function MainContent() {
  const [home, setHome] = useState<HomePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  // The single homepage request. Reuses apiGet's envelope unwrap.
  useEffect(() => {
    apiGet<HomePayload>('/api/home')
      .then(setHome)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <>
      {/* Always present; hero uses default config until aiConfig arrives. */}
      <HeroGuidance aiConfig={home?.aiConfig ?? null} />

      {/* Data-section states: error / loading / loaded. */}
      {error && <p role="alert">Couldn’t load homepage content: {error}</p>}
      {!error && !home && <p className="section-placeholder">Loading updates…</p>}
      {home && (
        <>
          {/* Seeded → no mount fetch; ImportantUpdates just polls afterwards. */}
          <ImportantUpdates initialUpdates={home.updates} />
          <CategoryPreviewList categories={home.categories} />
        </>
      )}
    </>
  );
}
