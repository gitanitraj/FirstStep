import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import CommunityResources from './CommunityResources';
import type { ResourcePathway } from '../../types/api';

const pathways: ResourcePathway[] = [
  { key: 'housing', label: 'Housing', icon: '🏠', kind: 'category' },
  { key: 'seniors', label: 'Seniors', icon: '🧓', kind: 'discovery' },
  { key: 'food', label: 'Food', icon: '🍎', kind: 'category' },
];

function renderWith(value: ResourcePathway[] | null) {
  return render(
    <MemoryRouter>
      <CommunityResources pathways={value} />
    </MemoryRouter>,
  );
}

describe('CommunityResources', () => {
  it('should render one card per authored pathway in file order', () => {
    renderWith(pathways);

    const links = screen.getAllByRole('link');

    // The trailing arrow is the click affordance, decorative and aria-hidden.
    expect(links.map((link) => link.textContent)).toEqual([
      '🏠Housing→',
      '🧓Seniors→',
      '🍎Food→',
    ]);
  });

  it('should route a category pathway to its category page', () => {
    renderWith(pathways);

    expect(screen.getByRole('link', { name: /Housing/ })).toHaveAttribute('href', '/category/housing');
  });

  it('should route a discovery pathway away from the category namespace', () => {
    // The load-bearing assertion of this component. Seniors is a controlled
    // query over CivicContent metadata, NOT a category — routing it to
    // /category/seniors would assert a taxonomy entry that must never exist.
    renderWith(pathways);

    expect(screen.getByRole('link', { name: /Seniors/ })).toHaveAttribute('href', '/discover/seniors');
  });

  it('should show a loading placeholder before the payload arrives', () => {
    renderWith(null);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });

  it('should render nothing but the heading when the authored list is empty', () => {
    // A missing homepage.json degrades to an empty column rather than throwing —
    // this is the UI half of that contract.
    renderWith([]);

    expect(screen.getByRole('heading', { name: 'Community Resources' })).toBeInTheDocument();
    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });
});
