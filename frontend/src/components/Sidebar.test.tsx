import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Sidebar from './Sidebar';
import type { CategorySummary } from '../types/api';

describe('Sidebar', () => {
  beforeEach(() => {
    const categories: CategorySummary[] = [
      { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
      { key: 'food', label: 'Food', icon: '🍎', resourceCount: 12, latestItems: [], latestPolicyUpdate: null },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ success: true, data: categories, errorCode: null, errorMessage: null }),
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders each category label with its resource count', async () => {
    render(<Sidebar />);

    expect(await screen.findByText('Housing')).toBeInTheDocument();
    expect(screen.getByText('44')).toBeInTheDocument();
    expect(screen.getByText('Food')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('toggles a category checkbox on click (local state)', async () => {
    render(<Sidebar />);

    const housing = (await screen.findByLabelText(/Housing/)) as HTMLInputElement;
    expect(housing.checked).toBe(false);

    fireEvent.click(housing);
    expect(housing.checked).toBe(true);

    fireEvent.click(housing);
    expect(housing.checked).toBe(false);
  });
});
