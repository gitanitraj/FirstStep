import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DelawareLawsFeature from './DelawareLawsFeature';
import type { LawItem } from '../types/api';

const laws: LawItem[] = [
  { title: 'Bill A', url: 'https://legis.example/a', date: '2026-07-15' },
  { title: 'Bill B', url: 'https://legis.example/b', date: '2026-07-13' },
  { title: 'Bill C', url: null, date: '2026-07-06' },
];

describe('DelawareLawsFeature', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the first bill as a link plus a dot per bill', () => {
    render(<DelawareLawsFeature laws={laws} />);

    expect(screen.getByRole('heading', { name: 'New Delaware Laws' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Bill A' })).toHaveAttribute('href', 'https://legis.example/a');
    expect(screen.getAllByRole('button', { name: /Law \d of 3/ })).toHaveLength(3);
  });

  it('auto-rotates to the next bill after 5 seconds', () => {
    render(<DelawareLawsFeature laws={laws} />);

    expect(screen.getByText('Bill A')).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(screen.getByText('Bill B')).toBeInTheDocument();
    expect(screen.queryByText('Bill A')).not.toBeInTheDocument();
  });

  it('jumps to a bill when its dot is clicked', () => {
    render(<DelawareLawsFeature laws={laws} />);

    fireEvent.click(screen.getByRole('button', { name: 'Law 3 of 3' }));
    // Bill C has no url → plain text, not a link.
    expect(screen.getByText('Bill C')).toBeInTheDocument();
  });

  it('shows a placeholder when there are no bills', () => {
    render(<DelawareLawsFeature laws={[]} />);
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });
});
