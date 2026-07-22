import { useEffect, useRef, useState } from 'react';
import { apiPost } from '../api/client';
import { useI18n } from '../i18n/I18nProvider';
import { useHighContrast } from '../hooks/useHighContrast';
import type { DecisionRequest, DecisionResponse } from '../types/api';
import AiResultCard from './AiResultCard';

type Status = 'idle' | 'loading' | 'done' | 'error';

/**
 * The Utility Bar: a narrow strip across the very top of every page.
 * - Left: reserved space for future social-media icons.
 * - Center: the always-available AI search. Submitting (Enter) posts to
 *   /api/decide and shows the answer in a dismissible dropdown panel — kept out
 *   of the page flow so the search stays available "without dominating".
 *   Canned/degraded responses are expected until an AI provider is wired in.
 * - Right: accessibility controls — language toggle (ES/EN) and high contrast.
 */
export default function UtilityBar() {
  const { lang, setLang, t } = useI18n();
  const { highContrast, toggle } = useHighContrast();
  const otherLang = lang === 'en' ? 'es' : 'en';

  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [open, setOpen] = useState(false);
  const centerRef = useRef<HTMLDivElement>(null);

  async function submit() {
    const userQuery = query.trim();
    if (!userQuery || status === 'loading') {
      return;
    }
    setStatus('loading');
    setOpen(true);
    // The Utility Bar search is a plain "what do you need" box — no urgency/
    // category chips (those lived in the old hero widget).
    const payload: DecisionRequest = { userQuery, urgent: false, preferredCategories: [] };
    try {
      const data = await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload);
      setResult(data);
      setStatus('done');
    } catch {
      setStatus('error');
    }
  }

  // Dismiss the panel on Escape or a click outside the search area.
  useEffect(() => {
    if (!open) {
      return;
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    }
    function onPointer(e: MouseEvent) {
      if (centerRef.current && !centerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onPointer);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('mousedown', onPointer);
    };
  }, [open]);

  return (
    <div className="utility-bar">
      <div className="utility-slot utility-left" aria-hidden="true">
        {/* Social media icons — reserved for a later slice. */}
      </div>

      <div className="utility-center" ref={centerRef}>
        <label className="visually-hidden" htmlFor="ai-search">
          {t('search.placeholder')}
        </label>
        <input
          id="ai-search"
          className="ai-search-input"
          type="search"
          placeholder={t('search.placeholder')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              submit();
            }
          }}
        />

        {open && status !== 'idle' && (
          <div
            className="ai-search-panel"
            role="region"
            aria-live="polite"
            aria-label={t('search.resultsLabel')}
          >
            <button
              type="button"
              className="ai-search-close"
              aria-label={t('search.close')}
              onClick={() => setOpen(false)}
            >
              ×
            </button>
            {status === 'loading' && <p className="ai-result-loading">{t('search.thinking')}</p>}
            {status === 'error' && (
              <p className="ai-result-unavailable" role="alert">
                {t('search.error')}
              </p>
            )}
            {status === 'done' && result && <AiResultCard result={result} />}
          </div>
        )}
      </div>

      <div className="utility-slot utility-right">
        <button
          type="button"
          className="utility-button"
          onClick={() => setLang(otherLang)}
          aria-label={t('a11y.language')}
          title={t('a11y.language')}
        >
          {otherLang.toUpperCase()}
        </button>
        <button
          type="button"
          className="utility-button"
          onClick={toggle}
          aria-pressed={highContrast}
          aria-label={t('a11y.contrast')}
          title={t('a11y.contrast')}
        >
          ◐
        </button>
      </div>
    </div>
  );
}
