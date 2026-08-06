import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import CategoryBrowse from './CategoryBrowse';
import type { TopicNavigation } from '../types/api';

function topic(name: string, slug: string, count: number): TopicNavigation {
  return { name, slug, count, countsByType: {} };
}

function renderBrowse(props: Parameters<typeof CategoryBrowse>[0]) {
  return render(
    <MemoryRouter>
      <CategoryBrowse {...props} />
    </MemoryRouter>,
  );
}

describe('CategoryBrowse', () => {
  it('renders group labels for a grouped category', () => {
    renderBrowse({
      categoryKey: 'housing',
      groups: [
        { label: 'Need Help Right Away', topics: [topic('Emergency Shelter', 'emergency-shelter', 10)] },
        { label: 'Find a Place to Live', topics: [topic('Sober Living', 'sober-living', 4)] },
      ],
      topics: [],
    });

    expect(screen.getByText('Need Help Right Away')).toBeInTheDocument();
    expect(screen.getByText('Find a Place to Live')).toBeInTheDocument();
  });

  it('renders a flat list for an ungrouped category', () => {
    renderBrowse({
      categoryKey: 'food',
      groups: [],
      topics: [topic('Food Pantry', 'food-pantry', 9), topic('Prepared Meals', 'prepared-meals', 1)],
    });

    expect(screen.getByRole('link', { name: /Food Pantry/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Prepared Meals/ })).toBeInTheDocument();
  });

  it('links topics at the topic route scoped to their category', () => {
    renderBrowse({
      categoryKey: 'housing',
      groups: [{ label: 'Need Help Right Away', topics: [topic('Emergency Shelter', 'emergency-shelter', 10)] }],
      topics: [],
    });

    expect(screen.getByRole('link', { name: /Emergency Shelter/ })).toHaveAttribute(
      'href',
      '/category/housing/emergency-shelter',
    );
  });

  it('renders a topic with no content rather than hiding it', () => {
    // Suppressing empty topics would conceal exactly what validate_navigation.py
    // exists to surface — a canonical topic nothing can reach.
    renderBrowse({
      categoryKey: 'legal',
      groups: [],
      topics: [topic('Disability Advocacy', 'disability-advocacy', 0)],
    });

    expect(screen.getByRole('link', { name: /Disability Advocacy/ })).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('explains itself when a category declares no topics at all', () => {
    // utilities: 0 subcategories, all of its content reachable only via updates.
    renderBrowse({ categoryKey: 'utilities', groups: [], topics: [] });

    expect(screen.getByText(/No topics yet/)).toBeInTheDocument();
  });
});
