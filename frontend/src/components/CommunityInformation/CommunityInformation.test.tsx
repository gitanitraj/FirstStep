import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import CommunityInformation from './CommunityInformation';
import type { FlyerCard } from '../../types/api';

const flyer = (n: number): FlyerCard => ({
  imageUrl: `/images/seasonal/Flyer%20${n}.jpg`,
  title: `Flyer ${n}`,
  organization: 'West End Neighborhood House',
  eventDate: '2026-07-20',
});

function renderWith(flyers: FlyerCard[] | null) {
  return render(
    <MemoryRouter>
      <CommunityInformation flyers={flyers} />
    </MemoryRouter>,
  );
}

describe('CommunityInformation', () => {
  it('should cap the flyer preview at three even when more are available', () => {
    renderWith([flyer(1), flyer(2), flyer(3), flyer(4), flyer(5)]);

    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('should render the flyer image with its title as alt text', () => {
    renderWith([flyer(1)]);

    expect(screen.getByAltText('Flyer 1')).toHaveAttribute(
      'src',
      '/images/seasonal/Flyer%201.jpg',
    );
  });

  it('should offer one route into the Community Notices destination', () => {
    // The section is flyers and a way in. The three pathway cards it used to
    // carry (Events / Meetings / Announcements) restated this single link — and
    // those views now live on the destination, where they belong.
    renderWith([flyer(1)]);

    const links = screen.getAllByRole('link');

    expect(links).toHaveLength(1);
    expect(links[0]).toHaveAttribute('href', '/community-notices');
  });

  it('should render the heading and intro even with no flyers', () => {
    renderWith(null);

    expect(screen.getByRole('heading', { name: 'Community Notices' })).toBeInTheDocument();
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
  });
});
