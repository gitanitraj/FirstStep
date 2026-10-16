import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import NewLaws from './NewLaws';
import type { LawItem } from '../../types/api';

const laws: LawItem[] = [
  { title: 'Relating to Rent Increases.', url: 'https://legis.delaware.gov/1', date: '2026-07-13' },
  { title: 'Relating to SNAP Eligibility.', url: null, date: '2026-07-10' },
];

function renderWith(value: LawItem[] | null) {
  return render(
    <MemoryRouter>
      <NewLaws laws={value} />
    </MemoryRouter>,
  );
}

describe('NewLaws', () => {
  it('should use the renamed heading', () => {
    renderWith(laws);

    expect(screen.getByRole('heading', { name: 'New Laws in Delaware' })).toBeInTheDocument();
  });

  it('should show exactly one law at a time rather than the whole feed', () => {
    // A teaser, not a news ticker. Every title IS in the DOM — that is how the
    // box sizes itself to the tallest and stops resizing as it rotates — but
    // only one is ever visible, and the hidden ones are out of the a11y tree.
    const { container } = renderWith(laws);

    const active = container.querySelectorAll('[class*="billActive"]');

    expect(active).toHaveLength(1);
    expect(active[0].textContent).toMatch(/Relating to Rent Increases\./);
  });

  it('should size the stack to every title so the box cannot resize', () => {
    // The load-bearing property: all titles share one grid cell. Rendering only
    // the current one would let the box grow and shrink every five seconds.
    const { container } = renderWith(laws);

    expect(container.querySelectorAll('[class*="billStack"] > p')).toHaveLength(laws.length);
  });

  it('should link out to the full Updates page', () => {
    renderWith(laws);

    expect(screen.getByRole('link', { name: 'View Updates' })).toHaveAttribute('href', '/updates');
  });

  it('should describe the kinds of change behind the link even with no feed', () => {
    // The change-type list is static: it describes the destination's remit,
    // which does not disappear when the RSS feed does. That matters, because
    // the upstream feed has been unavailable twice during development.
    renderWith(null);

    expect(screen.getByText('Legislation')).toBeInTheDocument();
    expect(screen.getByText('Benefit and program changes')).toBeInTheDocument();
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });

  it('should open an external law in a new tab safely', () => {
    renderWith(laws);

    const link = screen.getByRole('link', { name: /Relating to Rent Increases/ });

    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
