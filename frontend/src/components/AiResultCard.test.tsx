import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { I18nProvider } from '../i18n/I18nProvider';
import AiResultCard from './AiResultCard';
import type { DecisionResponse } from '../types/api';

function renderCard(result: DecisionResponse) {
  return render(
    <I18nProvider>
      <AiResultCard result={result} />
    </I18nProvider>,
  );
}

describe('AiResultCard', () => {
  it('shows a friendly notice (not the raw error) when the AI provider is unavailable', () => {
    renderCard({
      answerTitle: 'Unable to generate guidance',
      steps: [],
      citations: [],
      notes: 'AI call failed: No AI provider is configured (no ChatClient.Builder bean available).',
    });

    expect(screen.getByText(/temporarily unavailable/i)).toBeInTheDocument();
    expect(screen.queryByText(/ChatClient\.Builder/)).not.toBeInTheDocument();
  });

  it("shows the AI's own notes for a legitimate no-match answer", () => {
    renderCard({
      answerTitle: 'No close matches',
      steps: [],
      citations: [],
      notes: 'I could not find resources matching that request near Wilmington.',
    });

    expect(screen.getByText(/could not find resources/i)).toBeInTheDocument();
  });

  it('renders the answer title, steps, and sources for a full response', () => {
    renderCard({
      answerTitle: 'Here is how to find rental help',
      steps: [{ order: 1, title: 'Call the housing line', action: 'Dial 211', why: 'They triage requests' }],
      citations: [{ sourceType: 'resource', id: 'HA-001', label: 'Red Cross', contentSource: null }],
      notes: '',
    });

    expect(screen.getByRole('heading', { name: 'Here is how to find rental help' })).toBeInTheDocument();
    expect(screen.getByText(/Call the housing line/)).toBeInTheDocument();
    expect(screen.getByText(/Red Cross/)).toBeInTheDocument();
  });
});
