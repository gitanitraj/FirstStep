import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import CategoryUpdates from './CategoryUpdates';
import type { ContentType, UpdateItem } from '../types/api';

function update(
  id: string,
  contentType: ContentType,
  overrides: Partial<UpdateItem> = {},
): UpdateItem {
  return {
    type: 'news',
    contentType,
    id,
    title: `Item ${id}`,
    summary: `Summary ${id}`,
    date: '2026-07-23',
    source: 'Delaware Legislature',
    url: null,
    urgency: null,
    categoryTags: ['Housing'],
    ...overrides,
  };
}

describe('CategoryUpdates', () => {
  it('badges a law and curated news differently', () => {
    // The whole reason contentType exists: `type` reports "news" for both, so a
    // signed bill would otherwise be indistinguishable from an announcement.
    render(
      <CategoryUpdates
        updates={[update('L1', 'LAW'), update('N1', 'NEWS')]}
        lastUpdated="2026-07-23"
      />,
    );

    expect(screen.getByText('Law')).toBeInTheDocument();
    expect(screen.getByText('News')).toBeInTheDocument();
  });

  it('labels every content type from contentType, not from type', () => {
    render(
      <CategoryUpdates
        updates={[
          update('F1', 'FLYER', { type: 'flyer' }),
          update('E1', 'EXPERT', { type: 'expert' }),
          update('R1', 'RESOURCE', { type: 'news' }),
        ]}
        lastUpdated={null}
      />,
    );

    expect(screen.getByText('Flyer')).toBeInTheDocument();
    expect(screen.getByText('Expert')).toBeInTheDocument();
    expect(screen.getByText('Resource')).toBeInTheDocument();
  });

  it('links the title out only when the source gave a url', () => {
    render(
      <CategoryUpdates
        updates={[
          update('L1', 'LAW', { title: 'Relating to Rent', url: 'https://example.gov/L1' }),
          update('F1', 'FLYER', { title: 'Housing fair', url: null }),
        ]}
        lastUpdated={null}
      />,
    );

    const link = screen.getByRole('link', { name: 'Relating to Rent' });
    expect(link).toHaveAttribute('href', 'https://example.gov/L1');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.queryByRole('link', { name: 'Housing fair' })).not.toBeInTheDocument();
    expect(screen.getByText('Housing fair')).toBeInTheDocument();
  });

  it('shows the latest editorial date when there is one', () => {
    render(<CategoryUpdates updates={[update('N1', 'NEWS')]} lastUpdated="2026-07-25" />);

    expect(screen.getByText(/2026-07-25/)).toBeInTheDocument();
  });

  it('shows an empty message rather than a blank section', () => {
    render(<CategoryUpdates updates={[]} lastUpdated={null} />);

    expect(screen.getByText('No recent updates in this category.')).toBeInTheDocument();
  });
});
