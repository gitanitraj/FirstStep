import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import App from './App';
import HomePage from './pages/HomePage';

describe('Homepage frame', () => {
  it('renders the civic-portal frame: tagline, primary nav, search, section shells', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.getByText('First Step')).toBeInTheDocument();
    expect(
      screen.getByText(/Your trusted guide to community resources, program updates and local information\./),
    ).toBeInTheDocument();

    // Logo links home.
    expect(screen.getByRole('link', { name: /First Step home/i })).toHaveAttribute('href', '/');

    // Primary nav.
    for (const label of ['Housing Assistance', 'Community Info', 'Important Notices', 'Life Assistance']) {
      expect(screen.getByRole('link', { name: new RegExp(label) })).toBeInTheDocument();
    }

    // Utility Bar AI search.
    expect(screen.getByPlaceholderText(/Tell me what you need today/)).toBeInTheDocument();

    // Section shells.
    expect(screen.getByRole('heading', { name: 'New Delaware Laws' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Community Information' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Organizations' })).toBeInTheDocument();
  });
});

describe('App routing', () => {
  it('routes a primary-nav destination to its stub page (under the /app-next basename)', () => {
    window.history.pushState({}, '', '/app-next/important-notices');
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Important Notices' })).toBeInTheDocument();
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });
});
