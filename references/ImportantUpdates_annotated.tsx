/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/ImportantUpdates.tsx
 * Homepage-redesign Step 5b. See references/decisions.md Decision 019.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage "Important Updates" section. It renders the server-aggregated
 *   News + Flyers feed from GET /api/updates and LIVE-REFRESHES it on a timer,
 *   with no manual page reload — the app's first polling component.
 *
 * TWO IDEAS THAT MATTER HERE
 *   1. POLLING + TEARDOWN. A single useEffect starts a setInterval and returns a
 *      cleanup that clearInterval()s it. This is the first interval/teardown in
 *      the codebase (Sidebar's effect was a one-shot fetch with no cleanup). The
 *      `cancelled` flag guards against a late fetch resolving after unmount.
 *   2. CHANGE-DIFFING. The backend RSS feed only changes ~hourly, so most 5-min
 *      polls return identical data. Calling setState every poll would re-render
 *      the whole list needlessly (flicker risk). Instead we keep the last applied
 *      payload SERIALIZED in a ref and only setState when the new payload differs.
 *      Proven by a React.Profiler commit-count unit test: an identical poll adds
 *      no commit; a changed poll does.
 * ============================================================================= */

import { useEffect, useRef, useState } from 'react';
import { apiGet } from '../api/client';
import type { UpdateItem } from '../types/api';

// 5 minutes. The backend refreshes RSS at most hourly (see RssFeedService), so
// this is frequent enough to surface changes promptly without wasteful polling.
const POLL_INTERVAL_MS = 5 * 60 * 1000;

export default function ImportantUpdates() {
  const [updates, setUpdates] = useState<UpdateItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  // The last feed we actually applied, serialized. A ref (not state) because
  // changing it must NOT itself trigger a render — it's bookkeeping for the diff.
  const lastSerialized = useRef<string | null>(null);

  useEffect(() => {
    // Guards a slow fetch that resolves after the component unmounts (or the
    // effect re-runs) — without this, setState-after-unmount would warn/leak.
    let cancelled = false;

    async function load() {
      try {
        const data = await apiGet<UpdateItem[]>('/api/updates');
        if (cancelled) {
          return;
        }
        // THE DIFF: only commit when the payload actually changed. JSON.stringify
        // is a cheap deep-equality proxy for a small (<=8) flat list.
        const serialized = JSON.stringify(data);
        if (serialized !== lastSerialized.current) {
          lastSerialized.current = serialized;
          setUpdates(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError((err as Error).message);
        }
      }
    }

    load(); // initial fetch on mount
    const id = setInterval(load, POLL_INTERVAL_MS); // then poll
    // Cleanup: stop the timer and ignore any in-flight response.
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []); // empty deps — set up once, tear down on unmount

  return (
    <section className="updates-panel" aria-labelledby="updates-title">
      <h3 id="updates-title" className="updates-title">
        Important Updates
      </h3>
      {/* Mutually exclusive states: error / loading / empty / list. */}
      {error && <p role="alert">Error: {error}</p>}
      {!error && !updates && <p>Loading updates…</p>}
      {updates && updates.length === 0 && <p>No updates right now.</p>}
      {updates && updates.length > 0 && (
        <ul className="updates-list">
          {updates.map((u) => (
            // Key combines type + id because a news item and a flyer could
            // theoretically share an id across the two sources.
            <li className="update-item" key={`${u.type}-${u.id}`}>
              <div className="update-item-head">
                <span className="update-item-title">{u.title}</span>
                {/* Only show an urgency badge for genuinely elevated news;
                    "standard" and flyers (null urgency) get none. */}
                {u.urgency && u.urgency !== 'standard' && (
                  <span className="update-urgency">{u.urgency}</span>
                )}
              </div>
              {/* Summary is CSS line-clamped to 2 lines (see index.css) — some
                  RSS items carry a whole bill body as their summary. */}
              {u.summary && <p className="update-item-summary">{u.summary}</p>}
              <p className="update-item-meta">
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
