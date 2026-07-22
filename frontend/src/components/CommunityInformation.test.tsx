import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import CommunityInformation from './CommunityInformation';
import type { FlyerCard } from '../types/api';

const flyers: FlyerCard[] = [
  {
    imageUrl: '/images/seasonal/Health%20Fair.jpg',
    title: 'Free Community Health Fair',
    organization: 'Westside Family Healthcare',
    eventDate: '2026-08-05',
  },
];

describe('CommunityInformation', () => {
  it('renders a flyer card with its image and caption', () => {
    render(<CommunityInformation flyers={flyers} />);

    const img = screen.getByRole('img', { name: 'Free Community Health Fair' });
    expect(img).toHaveAttribute('src', '/images/seasonal/Health%20Fair.jpg');
    expect(screen.getByText('Free Community Health Fair')).toBeInTheDocument();
    expect(screen.getByText(/Westside Family Healthcare/)).toBeInTheDocument();
  });

  it('shows a placeholder when there are no flyers', () => {
    render(<CommunityInformation flyers={[]} />);
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });
});
