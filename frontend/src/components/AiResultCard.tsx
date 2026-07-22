import { useI18n } from '../i18n/I18nProvider';
import type { DecisionResponse } from '../types/api';

/**
 * Renders a DecisionResponse from POST /api/decide. Extracted from the Step-5a
 * hero widget when the AI search moved to the Utility Bar (Slice B).
 *
 * The AI provider is a stub, so /api/decide returns a graceful DEGRADED body
 * (empty steps/citations). We detect the provider-unavailable case (its notes are
 * prefixed "AI call failed") and show a friendly line instead of leaking the raw
 * internal error; a legitimate "no matches" notes is shown as-is. Once a provider
 * is wired in, the full answer/steps/citations render with no change here.
 */
export default function AiResultCard({ result }: { result: DecisionResponse }) {
  const { t } = useI18n();

  const degraded = result.steps.length === 0 && result.citations.length === 0;
  const providerUnavailable = degraded && result.notes.startsWith('AI call failed');

  if (degraded) {
    return (
      <p className="ai-result-unavailable">
        {providerUnavailable || !result.notes ? t('search.unavailable') : result.notes}
      </p>
    );
  }

  return (
    <div className="ai-result">
      <h3 className="ai-result-title">{result.answerTitle}</h3>
      <ol className="ai-result-steps">
        {result.steps.map((step) => (
          <li key={step.order}>
            <strong>{step.title}</strong>
            {step.action && <span> — {step.action}</span>}
            {step.why && <p className="ai-result-why">{step.why}</p>}
          </li>
        ))}
      </ol>
      {result.citations.length > 0 && (
        <p className="ai-result-sources">
          {t('search.sources')}: {result.citations.map((c) => c.label).join(', ')}
        </p>
      )}
    </div>
  );
}
