import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import ResourceDiscovery from './ResourceDiscovery';
import type { CategorySummary, OrgSummary } from '../types/api';

const organizations: OrgSummary[] = [
  { name: 'American Red Cross', slug: 'american-red-cross', resourceCount: 6 },
  { name: 'Ministry of Caring', slug: 'ministry-of-caring', resourceCount: 4 },
];

const categories: CategorySummary[] = [
  { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
  { key: 'food', label: 'Food', icon: '🍎', resourceCount: 12, latestItems: [], latestPolicyUpdate: null },
];

function renderDiscovery() {
  return render(
    <MemoryRouter>
      <ResourceDiscovery organizations={organizations} categories={categories} error={null} />
    </MemoryRouter>,
  );
}

describe('ResourceDiscovery', () => {
  it('renders organization links pointing at their landing pages', () => {
    renderDiscovery();

    const red = screen.getByRole('link', { name: /American Red Cross/ });
    expect(red).toHaveAttribute('href', '/organization/american-red-cross');
    expect(screen.getByRole('link', { name: /Ministry of Caring/ })).toBeInTheDocument();
  });

  it('renders category links pointing at their category pages', () => {
    renderDiscovery();

    expect(screen.getByRole('link', { name: /Housing/ })).toHaveAttribute('href', '/category/housing');
    expect(screen.getByRole('link', { name: /Food/ })).toHaveAttribute('href', '/category/food');
  });

  it('shows an error message when the payload failed to load', () => {
    render(
      <MemoryRouter>
        <ResourceDiscovery organizations={null} categories={null} error="boom" />
      </MemoryRouter>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('boom');
  });
});
