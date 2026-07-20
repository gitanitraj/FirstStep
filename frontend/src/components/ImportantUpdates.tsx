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
export default function ImportantUpdates() {
  const [updates, setUpdates] = useState<UpdateItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Serialized snapshot of the last applied feed, used to skip no-op updates.
  const lastSerialized = useRef<string | null>(null);

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

    load();
    const id = setInterval(load, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
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
            <li className="update-item" key={`${u.type}-${u.id}`}>
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
