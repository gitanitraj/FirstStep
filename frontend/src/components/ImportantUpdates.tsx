import { useEffect, useRef, useState } from 'react';
import { apiGet } from '../api/client';
import type { UpdateItem } from '../types/api';

// Live-refresh cadence. The backend RSS feed refreshes at most hourly, so a
// 5-minute client poll picks up changes promptly without hammering the server.
const POLL_INTERVAL_MS = 5 * 60 * 1000;

/**
 * Step 5b — the "Important Updates" section. Renders the server-aggregated
 * News + Flyers feed from GET /api/updates and live-refreshes it without a
 * manual page reload.
 *
 * Live-refresh: polls /api/updates every 5 minutes and only updates state when
 * the payload actually changed (change-diffing on the serialized list), so
 * unchanged polls cause no re-render or flicker.
 */
export default function ImportantUpdates({ initialUpdates }: { initialUpdates?: UpdateItem[] }) {
  // Seed from /api/home when provided (first paint with no extra request);
  // otherwise start empty and fetch /api/updates on mount (standalone use).
  const [updates, setUpdates] = useState<UpdateItem[] | null>(initialUpdates ?? null);
  const [error, setError] = useState<string | null>(null);
  // Serialized snapshot of the last applied feed, used to skip no-op updates.
  const lastSerialized = useRef<string | null>(
    initialUpdates !== undefined ? JSON.stringify(initialUpdates) : null,
  );

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const data = await apiGet<UpdateItem[]>('/api/updates');
        if (cancelled) {
          return;
        }
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

    // If seeded, skip the immediate fetch (the seed already covers first paint)
    // and let the interval poll for refreshes. If not seeded, fetch now.
    // Mount-once: the seed is a first-paint concern, so we don't re-run when the
    // prop reference changes (which would needlessly reset the poll timer).
    if (initialUpdates === undefined) {
      load();
    }
    const id = setInterval(load, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section className="updates-panel" aria-labelledby="updates-title">
      <h3 id="updates-title" className="updates-title">
        Important Updates
      </h3>
      {error && <p role="alert">Error: {error}</p>}
      {!error && !updates && <p>Loading updates…</p>}
      {updates && updates.length === 0 && <p>No updates right now.</p>}
      {updates && updates.length > 0 && (
        <ul className="updates-list">
          {updates.map((u) => (
            <li className="update-item" key={`${u.contentType}-${u.id}`}>
              <div className="update-item-head">
                <span className="update-item-title">{u.title}</span>
                {u.urgency && u.urgency !== 'standard' && (
                  <span className="update-urgency">{u.urgency}</span>
                )}
              </div>
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
