/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/AiSearch/
 *   AiSearch.tsx + AiSearch.module.css, plus components/AiResultCard.tsx
 * Slice B originally, as the Utility Bar dropdown (Decision 026).
 * MOVED and RESHAPED in Slice H (042); ⚠️ REMOVED FROM THE PAGE in 044.
 * Keep this mirror in sync whenever the production files change.
 * =============================================================================
 *
 * ⚠️ THIS COMPONENT IS NOT RENDERED ANYWHERE. It is retained, with
 * AiResultCard and POST /api/decide, for whenever AI is decided — see the
 * Version 3 backlog. Nothing imports it, so nothing here is live.
 *
 * WHAT IT WAS
 *   The homepage's AI Guidance entry point — "What do you need help with today?"
 *   Type a need, submit, and the answer appears in a panel below the form.
 *
 * WHY IT WAS PULLED (Decision 044)
 *   It was powered by an Ollama agent that is no longer wired in. A prominent
 *   "ask us anything" box on a civic service homepage is a PROMISE, and a
 *   resident in difficulty is the wrong person to disappoint — **an entry point
 *   that cannot answer is worse than none.** The header banner that pointed at
 *   it went too, for the same reason.
 *
 * WHO IT SERVES
 *   The visitor who ALREADY KNOWS what they need ("I need housing help", "where
 *   can I find food?"). Slice H calls this INTENTIONAL DISCOVERY. The rest of
 *   the homepage serves the visitor who does not — see NewLaws.
 * ============================================================================= */

import { useEffect, useRef, useState } from 'react';
import { apiPost } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import AiResultCard from '../AiResultCard';
import type { DecisionRequest, DecisionResponse } from '../../types/api';
import styles from './AiSearch.module.css';

type Status = 'idle' | 'loading' | 'done' | 'error';

export default function AiSearch() {
  const { t } = useI18n();
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [open, setOpen] = useState(false);

  async function submit() {
    const userQuery = query.trim();
    if (!userQuery || status === 'loading') {
      return;                       // guards empty input AND double-submit
    }
    setStatus('loading');
    setOpen(true);
    const payload: DecisionRequest = { userQuery, urgent: false, preferredCategories: [] };
    try {
      const data = await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload);
      setResult(data);
      setStatus('done');
    } catch {
      setStatus('error');
    }
  }

  // Escape only — see SECTION 2.
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  return (
    <section id="ai-search" className={styles.section} aria-labelledby="ai-search-title">
      <h2 id="ai-search-title" className={styles.title}>{t('home.aiTitle')}</h2>

      <form className={styles.form} onSubmit={(e) => { e.preventDefault(); submit(); }}>
        <label className="visually-hidden" htmlFor="ai-search-input">{t('search.placeholder')}</label>
        <input id="ai-search-input" className={styles.input} type="search" … />
        <button type="submit" className={styles.submit}>{t('home.aiSubmit')}</button>
      </form>

      {open && status !== 'idle' && (
        <div className={styles.panel} role="region" aria-live="polite" aria-label={t('search.resultsLabel')}>
          <button type="button" className={styles.close} aria-label={t('search.close')} … >×</button>
          {status === 'loading' && <p className={styles.status}>{t('search.thinking')}</p>}
          {status === 'error' && <p className={styles.status} role="alert">{t('search.error')}</p>}
          {status === 'done' && result && <AiResultCard result={result} />}
        </div>
      )}
    </section>
  );
}

// =============================================================================
// SECTION 1 — WHY IT MOVED OUT OF THE UTILITY BAR
// =============================================================================
// Slice B put this search in a sticky strip at the top of every page, "always
// available without dominating". Slice H made it a dedicated homepage section
// and deleted the strip.
//
// The original First Step opened with a QUESTION — `.hero-title`, "What do you
// need help with today?" — not a search box. That framing does more work than it
// looks: a search box asks the resident to already know the right words, while a
// question invites them to describe a situation. For someone in difficulty, the
// second is a much lower barrier.
//
// Keeping both would have put two search affordances on one screen competing for
// the same intent. So the header keeps only a BANNER pointing here (v1's
// `.ai-banner-header` did exactly that), and the input lives in one place.
//
// The cost, stated plainly: category and topic pages no longer carry a search
// box. The banner navigates home to this section instead — which is what v1 did
// when it switched to the AI Guidance screen.
//
// SECTION 2 — WHY THERE IS NO CLICK-OUTSIDE HANDLER ANYMORE
// =============================================================================
// The Slice B version was a DROPDOWN floating over the page, so it needed both
// Escape and a document-level `mousedown` listener to dismiss it — a floating
// layer that ignores clicks elsewhere feels broken.
//
// This panel sits IN THE PAGE FLOW. Clicking elsewhere on the page is not an
// attempt to get rid of it, so the listener was removed rather than carried over.
// Escape and the close button remain. Carrying it over would have been the easy
// choice and would have made the panel vanish while a resident tried to select
// the answer text.
//
// SECTION 3 — THE #ai-search ANCHOR IS A CONTRACT
// =============================================================================
// SiteHeader's banner targets `#ai-search` (`href="#ai-search"` on the homepage,
// `to="/#ai-search"` elsewhere). That id is therefore part of this component's
// public surface, not an implementation detail.
//
// A test asserts the anchor exists — a contract between two components deserves
// an assertion rather than a comment, because renaming the id would silently
// break the header with no other failure anywhere.
//
// SECTION 4 — ACCESSIBILITY
// =============================================================================
// - A real <form> with a submit button, so Enter works natively rather than
//   through a keydown handler (the Slice B version listened for Enter manually).
// - `aria-live="polite"` on the panel announces the answer without stealing focus.
// - The input has a `visually-hidden` <label>; the placeholder is not a label.
// - `role="alert"` on the error path so a failure is announced immediately.
//
// SECTION 5 — THE AI IS STILL A STUB
// =============================================================================
// No Spring AI provider is on the classpath, so POST /api/decide returns a
// graceful degraded 200. AiResultCard detects that shape and shows a friendly
// "temporarily unavailable" line instead of leaking the provider's dev message
// (the Slice 5a fix, still load-bearing). Unchanged by this slice — AiResultCard
// was reused as-is and still uses global `.ai-result-*` classes in index.css.
// =============================================================================
