import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import ContentCard from './ContentCard';
import type { ContentItem } from '../../types/api';

const item = (overrides: Partial<ContentItem> = {}): ContentItem => ({
  contentType: 'RESOURCE', id: 'synthetic', title: 'Public offering', summary: null,
  organization: null, location: null, cost: null, urgency: null, date: null,
  url: null, imageUrl: null, ...overrides,
});
const show = (content: ContentItem, href?: string) => render(
  <MemoryRouter><ul><ContentCard item={content} href={href} /></ul></MemoryRouter>,
);

describe('IM12-T12 resident navigation boundary', () => {
  it.each(['https://example.org/service', 'http://example.org/service', 'tel:+13025550123'])(
    'retains an explicit supported public destination %s', (url) => {
      show(item({ url }));
      expect(screen.getByRole('link', { name: 'Public offering' })).toHaveAttribute('href', url);
    },
  );
  it.each(['javascript:alert(1)', 'data:text/html,private', 'file:///private/catalog', '//unapproved.example']) (
    'does not navigate an unsupported source destination %s', (url) => {
      show(item({ url }));
      expect(screen.queryByRole('link')).not.toBeInTheDocument();
      expect(screen.getByText('Public offering')).toBeInTheDocument();
    },
  );
  it.each(['EA-001', 'EA-005', 'FAQ-002'])(
    'does not invent an Originals route for source-less EXPERT %s', (id) => {
      show(item({ id, contentType: 'EXPERT' }));
      expect(screen.queryByRole('link')).not.toBeInTheDocument();
    },
  );
  it('retains an explicitly approved internal Article destination', () => {
    show(item({ id: 'OR-003', contentType: 'NEWS' }), '/originals/OR-003');
    expect(screen.getByRole('link')).toHaveAttribute('href', '/originals/OR-003');
  });
  it('retains independently navigable encoded Flyer media without inventing a provider', () => {
    const media = '/images/seasonal/United%20Neighbors%202026.jpg';
    show(item({ id: 'FL-008', contentType: 'FLYER', imageUrl: media }));
    expect(screen.getByRole('link')).toHaveAttribute('href', media);
    expect(screen.getByRole('img', { name: 'Public offering' })).toHaveAttribute('src', media);
  });
});
