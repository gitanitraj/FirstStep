import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import CategoryPreviewList from './CategoryPreviewList';
import type { CategorySummary } from '../types/api';

const categories: CategorySummary[] = [
  {
    key: 'housing',
    label: 'Housing',
    icon: '🏠',
    resourceCount: 44,
    latestItems: [
      { type: 'resource', score: 0, content: { id: 'HA-001', communityId: 'w', title: 'Red Cross Shelter', summary: '', verified: true } },
      { type: 'flyer', score: 0, content: { id: 'FL-007', communityId: 'w', title: 'Furniture Giveaway', summary: '', verified: false } },
    ],
    latestPolicyUpdate: { id: 'N1', communityId: 'w', title: 'New tenant protection law', summary: '', verified: true },
  },
  {
    key: 'food',
    label: 'Food',
    icon: '🍎',
    resourceCount: 12,
    latestItems: [],
    latestPolicyUpdate: null,
  },
];

describe('CategoryPreviewList', () => {
  it('renders a card per category with count, policy update, and latest items', () => {
    render(<CategoryPreviewList categories={categories} />);

    expect(screen.getByText('Housing')).toBeInTheDocument();
    expect(screen.getByText('44 resources')).toBeInTheDocument();
    expect(screen.getByText(/New tenant protection law/)).toBeInTheDocument();
    expect(screen.getByText('Red Cross Shelter')).toBeInTheDocument();
    expect(screen.getByText('Furniture Giveaway')).toBeInTheDocument();
    expect(screen.getByText('Food')).toBeInTheDocument();
  });

  it('renders the Browse control as inert (disabled) until Step 6 routes exist', () => {
    render(<CategoryPreviewList categories={categories} />);

    const browse = screen.getByRole('button', { name: 'Browse Housing' });
    expect(browse).toBeDisabled();
  });
});
