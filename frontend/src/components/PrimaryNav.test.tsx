import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import PrimaryNav from './PrimaryNav';

describe('PrimaryNav', () => {
  it('renders the four primary-nav items pointing at their routes', () => {
    render(
      <MemoryRouter>
        <PrimaryNav />
      </MemoryRouter>,
    );

    const expected: [string, string][] = [
      ['Housing Assistance', '/category/housing-assistance'],
      ['Community Info', '/community-info'],
      ['Important Notices', '/important-notices'],
      ['Life Assistance', '/life-assistance'],
    ];

    for (const [label, href] of expected) {
      expect(screen.getByRole('link', { name: new RegExp(label) })).toHaveAttribute('href', href);
    }
  });
});
