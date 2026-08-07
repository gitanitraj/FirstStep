import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ContentCard from './ContentCard';
import type { ContentItem, ContentType } from '../../types/api';

function item(overrides: Partial<ContentItem> = {}): ContentItem {
  return {
    contentType: 'RESOURCE',
    id: 'R1',
    title: 'Ministry of Caring Shelter',
    summary: 'Overnight beds for adults.',
    organization: 'Ministry of Caring',
    location: 'Wilmington',
    cost: 'free',
    urgency: null,
    date: null,
    url: null,
    ...overrides,
  };
}

function renderCard(i: ContentItem) {
  return render(<ul><ContentCard item={i} /></ul>);
}

describe('ContentCard', () => {
  it('labels the content type from contentType', () => {
    renderCard(item({ contentType: 'RESOURCE' }));
    expect(screen.getByText('Resource')).toBeInTheDocument();
  });

  it('labels every content type, so one card design serves all of them', () => {
    const types: ContentType[] = ['RESOURCE', 'NEWS', 'LAW', 'FLYER', 'EXPERT'];
    const labels = ['Resource', 'News', 'Law', 'Flyer', 'Expert'];
    types.forEach((type, i) => {
      const { unmount } = renderCard(item({ contentType: type }));
      expect(screen.getByText(labels[i])).toBeInTheDocument();
      unmount();
    });
  });

  it('links the title out only when the provider gave a url', () => {
    renderCard(item({ url: 'https://example.org' }));
    const link = screen.getByRole('link', { name: 'Ministry of Caring Shelter' });
    expect(link).toHaveAttribute('href', 'https://example.org');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders the title as plain text when there is no url', () => {
    renderCard(item({ url: null }));
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByText('Ministry of Caring Shelter')).toBeInTheDocument();
  });

  it('shows provider and city together', () => {
    renderCard(item());
    expect(screen.getByText('Ministry of Caring')).toBeInTheDocument();
    expect(screen.getByText('Wilmington')).toBeInTheDocument();
  });

  it('omits every optional field that is absent rather than branching on type', () => {
    // A flyer: no organization-less layout switch, no cost, just a date.
    renderCard(item({
      contentType: 'FLYER', title: 'Housing fair', summary: null, organization: null,
      location: null, cost: null, urgency: null, date: '2026-08-01',
    }));

    expect(screen.getByText('Flyer')).toBeInTheDocument();
    expect(screen.getByText('Housing fair')).toBeInTheDocument();
    expect(screen.getByText('2026-08-01')).toBeInTheDocument();
    expect(screen.queryByText('free')).not.toBeInTheDocument();
    expect(screen.queryByText('Wilmington')).not.toBeInTheDocument();
  });

  it('shows cost and urgency when a resource has them', () => {
    renderCard(item({ cost: 'free', urgency: 'emergency' }));
    expect(screen.getByText('free')).toBeInTheDocument();
    expect(screen.getByText('emergency')).toBeInTheDocument();
  });

  it('does not repeat the organization when it is also the title', () => {
    // Common in the directory data — "American Red Cross" / "American Red Cross"
    // wasted a line on every card until this was caught in live verification.
    renderCard(item({ title: 'American Red Cross', organization: 'American Red Cross' }));

    expect(screen.getAllByText(/American Red Cross/)).toHaveLength(1);
    expect(screen.getByText('Wilmington')).toBeInTheDocument();
  });

  it('treats "standard" urgency as no urgency', () => {
    // "standard" is the ABSENCE of urgency; a chip saying so means nothing.
    renderCard(item({ urgency: 'standard' }));

    expect(screen.queryByText(/standard/i)).not.toBeInTheDocument();
  });
});
