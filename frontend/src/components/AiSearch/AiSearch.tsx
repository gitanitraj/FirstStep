import { useEffect, useRef, useState } from 'react';
import { apiPost } from '../../api/client';
import { useI18n } from '../../i18n/I18nProvider';
import AiResultCard from '../AiResultCard';
import type { DecisionRequest, DecisionResponse } from '../../types/api';
import styles from './AiSearch.module.css';

type Status = 'idle' | 'loading' | 'done' | 'error';

/**
 * The homepage's AI Guidance entry point — "What do you need help with today?"
 *
 * This is the starting point for the visitor who ALREADY KNOWS what they need
 * (Slice H's "intentional discovery"). The rest of the homepage serves the
 * visitor who does not.
 *
 * The search logic moved here from `UtilityBar`, which Slice H retired: the
 * original First Step put a question at the top of the page rather than a search
 * box in a strip, and a persistent input competing with a dedicated section is
 * the kind of duplication the front door is supposed to avoid. The header now
 * carries a banner that points HERE instead.
 *
 * `id="ai-search"` is that banner's target, so it is part of the contract.
 */
export default function AiSearch() {
  const { t } = useI18n();
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  async function submit() {
    const userQuery = query.trim();
    if (!userQuery || status === 'loading') {
      return;
    }
    setStatus('loading');
    setOpen(true);
    // A plain "what do you need" question — no urgency/category chips. Those
    // belonged to the retired hero widget, and the front door stays simple.
    const payload: DecisionRequest = { userQuery, urgent: false, preferredCategories: [] };
    try {
      const data = await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload);
      setResult(data);
      setStatus('done');
    } catch {
      setStatus('error');
    }
  }

  // Dismiss on Escape. Unlike the old dropdown there is no click-outside
  // handler: this panel sits in the page flow rather than floating over it, so
  // clicking elsewhere is not an attempt to get rid of it.
  useEffect(() => {
    if (!open) {
      return;
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  return (
    <section id="ai-search" className={styles.section} aria-labelledby="ai-search-title">
      <h2 id="ai-search-title" className={styles.title}>
        {t('home.aiTitle')}
      </h2>

      <form
        className={styles.form}
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <label className="visually-hidden" htmlFor="ai-search-input">
          {t('search.placeholder')}
        </label>
        <input
          id="ai-search-input"
          className={styles.input}
          type="search"
          placeholder={t('search.placeholder')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" className={styles.submit}>
          {t('home.aiSubmit')}
        </button>
      </form>

      {open && status !== 'idle' && (
        <div
          className={styles.panel}
          ref={panelRef}
          role="region"
          aria-live="polite"
          aria-label={t('search.resultsLabel')}
        >
          <button
            type="button"
            className={styles.close}
            aria-label={t('search.close')}
            onClick={() => setOpen(false)}
          >
            ×
          </button>
          {status === 'loading' && <p className={styles.status}>{t('search.thinking')}</p>}
          {status === 'error' && (
            <p className={styles.status} role="alert">
              {t('search.error')}
            </p>
          )}
          {status === 'done' && result && <AiResultCard result={result} />}
        </div>
      )}
    </section>
  );
}
