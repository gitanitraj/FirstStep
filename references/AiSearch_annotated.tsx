/* =============================================================================
 * ANNOTATED REFERENCE — Utility Bar AI search (Slice B).
 * Groups components/UtilityBar.tsx (the search + dropdown) and
 * components/AiResultCard.tsx (the result rendering). See decisions.md Decision 026.
 * Keep in sync with the sources.
 * =============================================================================
 *
 * WHAT THIS IS
 *   The always-available AI search in the Utility Bar. Type a need, press Enter,
 *   and the answer appears in a dismissible dropdown panel — kept out of page flow
 *   so the search never "dominates the page". Replaces the old Step-5a hero widget
 *   (HeroGuidance, now deleted); its result-rendering logic lives on in AiResultCard.
 *
 * THE AI IS A STUB (canned responses are fine at this stage)
 *   No model provider is wired, so POST /api/decide returns a graceful degraded
 *   body (title, empty steps/citations, notes "AI call failed: …"). AiResultCard
 *   detects that and shows a friendly "temporarily unavailable" line rather than
 *   the raw internal error. When a provider is added, full answers render unchanged.
 * ============================================================================= */

import { useEffect, useRef, useState } from 'react';
import { apiPost } from '../api/client';
import { useI18n } from '../i18n/I18nProvider';
import type { DecisionRequest, DecisionResponse } from '../types/api';

/* ---------------------------------------------------------------------------
 * components/AiResultCard.tsx — pure presentation of a DecisionResponse.
 * degraded = no steps AND no citations. providerUnavailable narrows that to the
 * stub error (notes starts "AI call failed") — only THAT is replaced with the
 * friendly canned line; a real "no matches" notes is shown as-is. Otherwise the
 * answerTitle + ordered steps (title — action, muted why) + comma-joined sources.
 * ------------------------------------------------------------------------- */
export function AiResultCardSketch({ result }: { result: DecisionResponse }) {
  const { t } = useI18n();
  const degraded = result.steps.length === 0 && result.citations.length === 0;
  const providerUnavailable = degraded && result.notes.startsWith('AI call failed');
  if (degraded) {
    return <p>{providerUnavailable || !result.notes ? t('search.unavailable') : result.notes}</p>;
  }
  return <div>{/* answerTitle, steps, sources */}</div>;
}

/* ---------------------------------------------------------------------------
 * components/UtilityBar.tsx — owns the search state + dropdown.
 * ------------------------------------------------------------------------- */
export function UtilityBarSearchSketch() {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [open, setOpen] = useState(false);
  const centerRef = useRef<HTMLDivElement>(null);

  // Submit: a PLAIN query — urgent:false, preferredCategories:[] (the old hero's
  // urgency/category chips are gone). Opens the panel immediately (loading), then
  // fills it. On throw → status 'error' (a generic message, not the raw error).
  async function submit() {
    const userQuery = query.trim();
    if (!userQuery || status === 'loading') return;
    setStatus('loading');
    setOpen(true);
    const payload: DecisionRequest = { userQuery, urgent: false, preferredCategories: [] };
    try {
      setResult(await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload));
      setStatus('done');
    } catch {
      setStatus('error');
    }
  }

  // Dismiss on Escape or a click outside the search area. Listeners are attached
  // ONLY while the panel is open (and torn down in the cleanup) — no always-on
  // global handlers. centerRef wraps BOTH the input and the panel, so clicks on
  // either don't count as "outside".
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    const onPointer = (e: MouseEvent) =>
      centerRef.current && !centerRef.current.contains(e.target as Node) && setOpen(false);
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onPointer);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('mousedown', onPointer);
    };
  }, [open]);

  // Render sketch: <div className="utility-center" ref={centerRef}> holds the
  // <input> and, when `open && status !== 'idle'`, the absolutely-positioned
  // <div className="ai-search-panel" role="region" aria-live="polite"> with a
  // close button + (loading | error | <AiResultCard/>). `.utility-center` is
  // position:relative so the panel anchors under the centered search.
  return null;
}
