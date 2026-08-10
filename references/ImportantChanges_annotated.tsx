/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/DelawareLawsFeature.tsx (Slice C)
 * See references/decisions.md Decision 024. Keep this mirror in sync.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The "New Delaware Laws" feature below the Hero. It rotates ONE recently
 *   signed bill title into view at a time (7 most recent), reinforcing First
 *   Step as a central news point. Display-only: the bills come from GET /api/home
 *   `delawareLaws` (LegislationService aggregates them server-side from the
 *   GovernorSignedLegislation RSS feed).
 *
 * ACCESSIBILITY IS THE INTERESTING PART
 *   Auto-moving content is an a11y hazard, so:
 *   - It auto-advances every 5s ONLY when the user does NOT prefer reduced motion
 *     (and only when there are ≥2 bills). Reduced-motion users get a static first
 *     bill they can step through via the dots.
 *   - The fade animation is likewise disabled under prefers-reduced-motion (CSS).
 *   - Dot buttons give explicit manual control with aria-label / aria-current.
 * ============================================================================= */

import { useEffect, useState } from 'react';
import { useI18n } from '../i18n/I18nProvider';
import type { LawItem } from '../types/api';

const ROTATE_MS = 5000;

// Guarded matchMedia read — matchMedia is undefined in jsdom, so this returns
// false in tests (which is why the auto-rotate is testable with fake timers).
function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

export default function DelawareLawsFeature({ laws }: { laws: LawItem[] | null }) {
  const { t } = useI18n();
  const [index, setIndex] = useState(0);
  const count = laws?.length ?? 0;

  // Auto-rotate. Bails out (no interval) for <2 bills or reduced-motion users.
  // Depends on `count` so it re-arms if the bill list arrives/changes.
  useEffect(() => {
    if (count < 2 || prefersReducedMotion()) {
      return;
    }
    const id = setInterval(() => setIndex((i) => (i + 1) % count), ROTATE_MS);
    return () => clearInterval(id);
  }, [count]);

  // Derive a safe index instead of clamping state — `index % count` stays valid
  // even if the list shrank, without an extra effect.
  const safeIndex = count > 0 ? index % count : 0;
  const bill = laws && count > 0 ? laws[safeIndex] : null;

  return (
    <section className="laws-feature" aria-labelledby="laws-title">
      <h2 id="laws-title" className="laws-title">
        {t('section.laws')}
      </h2>

      {/* null (loading) or empty → placeholder. */}
      {!bill && <p className="section-placeholder">{t('common.comingSoon')}</p>}

      {bill && (
        <div className="laws-rotator">
          {/* key={safeIndex} remounts the <p> on each change so the fade-in
              keyframe replays. */}
          <p className="laws-bill" key={safeIndex}>
            {bill.url ? (
              <a href={bill.url} target="_blank" rel="noopener noreferrer">
                {bill.title}
              </a>
            ) : (
              bill.title
            )}
            {bill.date && <span className="laws-bill-date"> · {bill.date}</span>}
          </p>

          {/* One dot per bill; click to jump. (laws ?? []) satisfies the type —
              count>1 already implies laws is non-null. */}
          {count > 1 && (
            <div className="laws-dots" aria-label="Recent laws">
              {(laws ?? []).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`laws-dot${i === safeIndex ? ' active' : ''}`}
                  aria-label={`Law ${i + 1} of ${count}`}
                  aria-current={i === safeIndex}
                  onClick={() => setIndex(i)}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
