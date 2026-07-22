import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n/I18nProvider';
import UtilityBar from './UtilityBar';

const degraded = {
  answerTitle: 'Unable to generate guidance',
  steps: [],
  citations: [],
  notes: 'AI call failed: No AI provider is configured.',
};

describe('UtilityBar AI search', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ success: true, data: degraded, errorCode: null, errorMessage: null }),
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
    document.body.className = '';
  });

  it('posts the query on Enter and shows the result in a dropdown panel', async () => {
    render(
      <I18nProvider>
        <UtilityBar />
      </I18nProvider>,
    );

    const input = screen.getByPlaceholderText(/Tell me what you need today/);
    fireEvent.change(input, { target: { value: 'I need rental help' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    // Panel shows the degraded (canned) guidance.
    expect(await screen.findByText(/temporarily unavailable/i)).toBeInTheDocument();

    // Posted to /api/decide with the plain query (no chips).
    const [url, options] = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('/api/decide');
    expect(JSON.parse(options.body as string)).toMatchObject({
      userQuery: 'I need rental help',
      urgent: false,
      preferredCategories: [],
    });

    // Dismissable via the close button.
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByText(/temporarily unavailable/i)).not.toBeInTheDocument();
  });
});
