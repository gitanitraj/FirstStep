import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import HeroGuidance from './HeroGuidance';
import type { DecisionResponse } from '../types/api';

function mockDecide(data: DecisionResponse) {
  const fetchMock = vi.fn().mockResolvedValue({
    json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

const goodResponse: DecisionResponse = {
  answerTitle: 'Here is how to find rental help',
  steps: [{ order: 1, title: 'Call the housing line', action: 'Dial 211', why: 'They triage requests' }],
  citations: [{ sourceType: 'resource', id: 'HA-001', label: 'Red Cross', contentSource: null }],
  notes: '',
};

const providerUnavailableResponse: DecisionResponse = {
  answerTitle: 'Unable to generate guidance',
  steps: [],
  citations: [],
  notes: 'AI call failed: No AI provider is configured (no ChatClient.Builder bean available).',
};

const noMatchResponse: DecisionResponse = {
  answerTitle: 'No close matches',
  steps: [],
  citations: [],
  notes: 'I could not find resources matching that request near Wilmington.',
};

describe('HeroGuidance', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the hero heading, question input, and Get Help button', () => {
    mockDecide(goodResponse);
    render(<HeroGuidance />);

    expect(screen.getByRole('heading', { name: /what do you need help with/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/rental help/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Get Help' })).toBeInTheDocument();
  });

  it('posts the query and renders the returned guidance', async () => {
    mockDecide(goodResponse);
    render(<HeroGuidance />);

    fireEvent.change(screen.getByPlaceholderText(/rental help/i), {
      target: { value: 'I need rental help' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Get Help' }));

    expect(await screen.findByText('Here is how to find rental help')).toBeInTheDocument();
    expect(screen.getByText(/Call the housing line/)).toBeInTheDocument();
  });

  it('shows a friendly notice (not the raw error) when the AI provider is unavailable', async () => {
    mockDecide(providerUnavailableResponse);
    render(<HeroGuidance />);

    fireEvent.change(screen.getByPlaceholderText(/rental help/i), {
      target: { value: 'anything' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Get Help' }));

    expect(await screen.findByText(/temporarily unavailable/i)).toBeInTheDocument();
    // The raw backend/internal error must not leak to the resident.
    expect(screen.queryByText(/ChatClient\.Builder/)).not.toBeInTheDocument();
    expect(screen.queryByText('Unable to generate guidance')).not.toBeInTheDocument();
  });

  it('shows the AI\'s own notes for a legitimate "no matches" answer', async () => {
    mockDecide(noMatchResponse);
    render(<HeroGuidance />);

    fireEvent.change(screen.getByPlaceholderText(/rental help/i), {
      target: { value: 'anything' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Get Help' }));

    expect(
      await screen.findByText(/could not find resources matching that request/i),
    ).toBeInTheDocument();
  });

  it('includes urgent:true in the POST body when the Urgent chip is active', async () => {
    const fetchMock = mockDecide(goodResponse);
    render(<HeroGuidance />);

    fireEvent.click(screen.getByRole('button', { name: /Urgent/ }));
    fireEvent.change(screen.getByPlaceholderText(/rental help/i), {
      target: { value: 'I need rental help' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Get Help' }));

    await screen.findByText('Here is how to find rental help');

    const [, options] = fetchMock.mock.calls[0];
    const body = JSON.parse(options.body as string);
    expect(body).toMatchObject({ userQuery: 'I need rental help', urgent: true });
  });
});
