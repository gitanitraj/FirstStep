import { useState } from 'react';
import { apiPost } from '../api/client';
import type { DecisionRequest, DecisionResponse } from '../types/api';

type Status = 'idle' | 'loading' | 'done' | 'error';

// The bias chips beneath the question box. `value` is what gets sent in
// DecisionRequest.preferredCategories; `urgent` is handled separately.
const CATEGORY_CHIPS: { value: string; label: string }[] = [
  { value: 'housing', label: '🏠 Housing' },
  { value: 'essentials', label: '🛒 Essentials' },
];

/**
 * Step 5a — the merged Hero + AI guidance widget. A single hero card carrying
 * the AI question flow inline (question box + urgency/category chips + submit),
 * with the decision result rendered below.
 *
 * NOTE: POST /api/decide is a stub until a model provider is wired in — it
 * returns 200 with a graceful degraded body (empty steps/citations). We detect
 * that and show an honest "temporarily unavailable" notice rather than a blank
 * card.
 */
export default function HeroGuidance() {
  const [query, setQuery] = useState('');
  const [urgent, setUrgent] = useState(false);
  const [preferred, setPreferred] = useState<Set<string>>(new Set());
  const [status, setStatus] = useState<Status>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  function togglePreferred(value: string) {
    setPreferred((prev) => {
      const next = new Set(prev);
      if (next.has(value)) {
        next.delete(value);
      } else {
        next.add(value);
      }
      return next;
    });
  }

  async function submit() {
    const userQuery = query.trim();
    if (!userQuery || status === 'loading') {
      return;
    }
    setStatus('loading');
    setError(null);
    const payload: DecisionRequest = {
      userQuery,
      urgent,
      preferredCategories: [...preferred],
    };
    try {
      const data = await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload);
      setResult(data);
      setStatus('done');
    } catch (err) {
      setError((err as Error).message);
      setStatus('error');
    }
  }

  // The stub fallback returns a title but no steps and no citations. Treat that
  // as a "no actionable guidance" case.
  const degraded =
    result !== null && result.steps.length === 0 && result.citations.length === 0;

  // Distinguish the provider-unavailable stub (DecisionAgentService prefixes its
  // catch-block notes with "AI call failed:") from a legitimate "no matches
  // found" answer whose `notes` is genuinely user-facing. Only the former leaks
  // internal detail, so we replace it with a friendly line; the latter is shown.
  const providerUnavailable = degraded && result.notes.startsWith('AI call failed');

  return (
    <section className="hero-section" aria-labelledby="hero-title">
      <h2 id="hero-title" className="hero-title">
        What do you need help with today?
      </h2>
      <p className="hero-subtitle">
        Ask in your own words, or browse categories to find housing, essentials,
        and local support.
      </p>

      <div className="hero-ai-form">
        <label className="visually-hidden" htmlFor="ai-question">
          Describe what you need
        </label>
        <textarea
          id="ai-question"
          className="hero-ai-input"
          rows={2}
          placeholder="E.g., I need rental help near Wilmington for seniors"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              submit();
            }
          }}
        />

        <div className="hero-ai-chips">
          <button
            type="button"
            className={`hero-chip${urgent ? ' active' : ''}`}
            aria-pressed={urgent}
            onClick={() => setUrgent((u) => !u)}
          >
            🚨 Urgent
          </button>
          {CATEGORY_CHIPS.map((chip) => (
            <button
              key={chip.value}
              type="button"
              className={`hero-chip${preferred.has(chip.value) ? ' active' : ''}`}
              aria-pressed={preferred.has(chip.value)}
              onClick={() => togglePreferred(chip.value)}
            >
              {chip.label}
            </button>
          ))}
          <button
            type="button"
            className="hero-ai-submit"
            onClick={submit}
            disabled={status === 'loading'}
          >
            {status === 'loading' ? 'Thinking…' : 'Get Help'}
          </button>
        </div>
      </div>

      {status === 'error' && (
        <div className="hero-ai-result" role="alert">
          Something went wrong: {error}
        </div>
      )}

      {status === 'done' && result && (
        <div className="hero-ai-result">
          {degraded ? (
            <p className="hero-ai-unavailable">
              {providerUnavailable || !result.notes
                ? 'AI guidance is temporarily unavailable — try browsing categories below.'
                : result.notes}
            </p>
          ) : (
            <>
              <h3 className="hero-ai-answer-title">{result.answerTitle}</h3>
              <ol className="hero-ai-steps">
                {result.steps.map((step) => (
                  <li key={step.order}>
                    <strong>{step.title}</strong>
                    {step.action && <span> — {step.action}</span>}
                    {step.why && <p className="hero-ai-why">{step.why}</p>}
                  </li>
                ))}
              </ol>
              {result.citations.length > 0 && (
                <p className="hero-ai-citations">
                  Sources: {result.citations.map((c) => c.label).join(', ')}
                </p>
              )}
            </>
          )}
        </div>
      )}
    </section>
  );
}
