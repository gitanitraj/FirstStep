/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/HeroGuidance.tsx
 * Homepage-redesign Step 5a (merged Hero + AI guidance widget).
 * See references/decisions.md Decision 018. Keep this mirror in sync whenever the
 * production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The top of the homepage main column: a single gradient "hero" card that both
 *   greets the user AND carries the AI question flow inline. It merges what were
 *   two separate blocks in the old static demo (a text-only hero + a lower AI
 *   section). The user types a need, optionally toggles bias chips, hits Get Help,
 *   and the decision result renders in a card below the form.
 *
 * THE ONE THING TO UNDERSTAND: THE AI IS A STUB
 *   POST /api/decide always returns HTTP 200, but until a Spring AI model-provider
 *   starter is added to the backend classpath, DecisionAgentService catches the
 *   "no provider" error and returns a DEGRADED body: a title, empty steps, empty
 *   citations, and `notes` prefixed "AI call failed: ...". This component is built
 *   to render that honestly — see the `providerUnavailable` logic below — and will
 *   automatically render real guidance once a provider is wired in. No frontend
 *   change will be needed then.
 * ============================================================================= */

import { useState } from 'react';

// apiPost<TReq,TRes> — added in 5a alongside apiGet; unwraps the ApiResponse<T>
// envelope and throws on !success. First (and only 5a) POST consumer.
import { apiPost } from '../api/client';

// DTO types mirroring the backend ai/dto records (added to types/api.ts in 5a).
import type { DecisionRequest, DecisionResponse } from '../types/api';

// A tiny state machine for the request lifecycle — clearer than juggling separate
// isLoading/hasError booleans.
type Status = 'idle' | 'loading' | 'done' | 'error';

// The two category-bias chips. `value` is exactly what the backend expects in
// DecisionRequest.preferredCategories (["housing","essentials"]). "Urgent" is a
// separate boolean flag, not a preferredCategory, so it is NOT in this list.
const CATEGORY_CHIPS: { value: string; label: string }[] = [
  { value: 'housing', label: '🏠 Housing' },
  { value: 'essentials', label: '🛒 Essentials' },
];

export default function HeroGuidance() {
  // Form state
  const [query, setQuery] = useState('');
  const [urgent, setUrgent] = useState(false);
  const [preferred, setPreferred] = useState<Set<string>>(new Set()); // Set = O(1) toggle
  // Request/result state
  const [status, setStatus] = useState<Status>('idle');
  const [result, setResult] = useState<DecisionResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Immutable Set toggle (same pattern as Sidebar's category checkboxes): copy,
  // mutate the copy, return it — React needs a new reference to re-render.
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
    // Guard: ignore empty submissions and double-submits while a request is in
    // flight (the button is also disabled, but Enter could still fire).
    if (!userQuery || status === 'loading') {
      return;
    }
    setStatus('loading');
    setError(null);
    const payload: DecisionRequest = {
      userQuery,
      urgent,
      preferredCategories: [...preferred], // Set → array for JSON
    };
    try {
      const data = await apiPost<DecisionRequest, DecisionResponse>('/api/decide', payload);
      setResult(data);
      setStatus('done');
    } catch (err) {
      // Network / envelope-failure path (distinct from the graceful degraded body,
      // which still arrives as a successful response).
      setError((err as Error).message);
      setStatus('error');
    }
  }

  // `degraded`: the response carried no actionable guidance (no steps, no
  // citations). True for BOTH the provider-unavailable stub AND a legitimate
  // "I looked but found nothing" answer.
  const degraded =
    result !== null && result.steps.length === 0 && result.citations.length === 0;

  // `providerUnavailable`: narrow the degraded case to specifically the stub
  // error. DecisionAgentService's catch block prefixes its notes with "AI call
  // failed:", so that prefix is our signal. WHY THIS MATTERS (Decision 018): the
  // raw notes for that case leak a developer message ("No ChatClient.Builder bean
  // available. Add a Spring AI model-provider starter…") — never show that to a
  // resident. A legitimate no-match answer, by contrast, has a genuinely
  // user-facing `notes` that we DO show. This distinction was a deliberate
  // deviation from the approved plan, made after live verification exposed the leak.
  const providerUnavailable = degraded && result.notes.startsWith('AI call failed');

  return (
    // aria-labelledby ties the section to its heading for screen readers.
    <section className="hero-section" aria-labelledby="hero-title">
      <h2 id="hero-title" className="hero-title">
        What do you need help with today?
      </h2>
      <p className="hero-subtitle">
        Ask in your own words, or browse categories to find housing, essentials,
        and local support.
      </p>

      <div className="hero-ai-form">
        {/* Visually-hidden label keeps the textarea accessible without cluttering
            the gradient hero with a visible label. */}
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
          // Enter submits; Shift+Enter inserts a newline (textarea, so multi-line
          // is possible). preventDefault stops Enter from adding a newline.
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              submit();
            }
          }}
        />

        <div className="hero-ai-chips">
          {/* Urgent is a standalone boolean toggle. aria-pressed exposes the
              on/off state to assistive tech; the `.active` class styles it. */}
          <button
            type="button"
            className={`hero-chip${urgent ? ' active' : ''}`}
            aria-pressed={urgent}
            onClick={() => setUrgent((u) => !u)}
          >
            🚨 Urgent
          </button>
          {/* Category chips toggle membership in the `preferred` Set. */}
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
          {/* Submit. Disabled + relabeled while loading to prevent double-submit
              and signal progress. margin-left:auto (CSS) pushes it to the right. */}
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

      {/* Network/throw path — role="alert" so screen readers announce it. */}
      {status === 'error' && (
        <div className="hero-ai-result" role="alert">
          Something went wrong: {error}
        </div>
      )}

      {/* Successful response (including the graceful degraded body). */}
      {status === 'done' && result && (
        <div className="hero-ai-result">
          {degraded ? (
            // Provider-unavailable OR empty notes → friendly canned line.
            // Legitimate no-match → show the AI's own user-facing notes.
            <p className="hero-ai-unavailable">
              {providerUnavailable || !result.notes
                ? 'AI guidance is temporarily unavailable — try browsing categories below.'
                : result.notes}
            </p>
          ) : (
            // Real guidance: title, ordered steps (title — action, muted why),
            // and a comma-joined source line. Fragment avoids an extra wrapper.
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
