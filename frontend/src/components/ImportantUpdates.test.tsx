import { Profiler } from 'react';
import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ImportantUpdates from './ImportantUpdates';
import type { UpdateItem } from '../types/api';

const POLL_MS = 5 * 60 * 1000;

function envelope(data: UpdateItem[]) {
  return { json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }) };
}

const feedA: UpdateItem[] = [
  { contentType: 'NEWS', id: 'N1', title: 'News One', summary: 'First', date: '2026-05-01', source: 'Legislature', url: null, urgency: 'high', categoryTags: null },
];
const feedB: UpdateItem[] = [
  { contentType: 'FLYER', id: 'F1', title: 'News Two', summary: 'Second', date: '2026-06-01', source: 'Community Center', url: null, urgency: null, categoryTags: null },
];

describe('ImportantUpdates', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('renders the aggregated feed from /api/updates', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(envelope(feedA)));
    render(<ImportantUpdates />);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0); // flush the initial fetch
    });

    expect(screen.getByRole('heading', { name: 'Important Updates' })).toBeInTheDocument();
    expect(screen.getByText('News One')).toBeInTheDocument();
    expect(screen.getByText(/2026-05-01/)).toBeInTheDocument();
  });

  it('updates on a changed poll but skips re-render when data is unchanged (diffing)', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(envelope(feedA)) // initial
      .mockResolvedValueOnce(envelope(feedA)) // poll 1 — identical
      .mockResolvedValueOnce(envelope(feedB)); // poll 2 — changed
    vi.stubGlobal('fetch', fetchMock);

    const onRender = vi.fn();
    render(
      <Profiler id="important-updates" onRender={onRender}>
        <ImportantUpdates />
      </Profiler>,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0); // initial load applied
    });
    expect(screen.getByText('News One')).toBeInTheDocument();
    const commitsAfterInitial = onRender.mock.calls.length;

    // Poll 1 returns identical data → change-diffing must suppress the state
    // update, so no additional commit happens.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(POLL_MS);
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(onRender.mock.calls.length).toBe(commitsAfterInitial);
    expect(screen.getByText('News One')).toBeInTheDocument();

    // Poll 2 returns changed data → state updates and the UI reflects it.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(POLL_MS);
    });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(onRender.mock.calls.length).toBeGreaterThan(commitsAfterInitial);
    expect(screen.getByText('News Two')).toBeInTheDocument();
    expect(screen.queryByText('News One')).not.toBeInTheDocument();
  });
});
