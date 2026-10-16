import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ContentCollection from './ContentCollection';
import type { CategoryCollection, ContentItem } from '../../types/api';

/**
 * Assertions target roles and text, never class names — a test that asserts a
 * class name fails on a restyle and passes on a broken layout.
 */

function item(id: string, title: string): ContentItem {
  return {
    contentType: 'RESOURCE',
    id,
    title,
    summary: `${title} summary`,
    organization: 'Some Org',
    location: 'Wilmington',
    cost: null,
    urgency: null,
    date: null,
    url: null,
    imageUrl: null,
  };
}

function collection(count: number, overrides: Partial<CategoryCollection> = {}): CategoryCollection {
  const items = Array.from({ length: count }, (_, i) => item(`R${i + 1}`, `Item ${i + 1}`));
  return {
    key: 'get-help',
    labelKey: 'collection.getHelp',
    layer: 'ACTION',
    count: items.length,
    items,
    ...overrides,
  };
}

describe('ContentCollection', () => {
  it('shouldRenderTheCollectionLabelAsItsHeading', () => {
    render(<ContentCollection collection={collection(2)} />);

    expect(screen.getByRole('heading', { name: 'Get help' })).toBeInTheDocument();
  });

  it('shouldNameTheSectionByItsHeadingForAssistiveTech', () => {
    render(<ContentCollection collection={collection(2)} />);

    expect(screen.getByRole('region', { name: 'Get help' })).toBeInTheDocument();
  });

  it('shouldShowTheFullCountEvenWhileOnlyAPreviewIsListed', () => {
    render(<ContentCollection collection={collection(9)} />);

    expect(screen.getByText('9')).toBeInTheDocument();
  });

  it('shouldListEveryItemWithoutAnExpandControlWhenTheCollectionIsSmall', () => {
    render(<ContentCollection collection={collection(3)} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('shouldPreviewAndOfferTheRemainderWhenTheCollectionIsLong', () => {
    render(<ContentCollection collection={collection(9)} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(4);
    expect(screen.getByRole('button', { name: '5 more' })).toBeInTheDocument();
  });

  it('shouldRevealTheRemainderInPlaceWhenExpanded', () => {
    render(<ContentCollection collection={collection(9)} />);

    fireEvent.click(screen.getByRole('button', { name: '5 more' }));

    expect(screen.getAllByRole('listitem')).toHaveLength(9);
    expect(screen.getByRole('button', { name: 'Show less' })).toBeInTheDocument();
  });

  it('shouldTellAssistiveTechWhetherTheListIsExpanded', () => {
    render(<ContentCollection collection={collection(9)} />);
    const button = screen.getByRole('button');

    expect(button).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(button);

    expect(button).toHaveAttribute('aria-expanded', 'true');
  });

  it('shouldCollapseBackToThePreviewWhenToggledOff', () => {
    render(<ContentCollection collection={collection(9)} />);

    fireEvent.click(screen.getByRole('button'));
    fireEvent.click(screen.getByRole('button'));

    expect(screen.getAllByRole('listitem')).toHaveLength(4);
  });

  it('shouldDefaultToHeadingLevelThree', () => {
    render(<ContentCollection collection={collection(2)} />);

    expect(screen.getByRole('heading', { name: 'Get help', level: 3 })).toBeInTheDocument();
  });

  it('shouldSitAtTheHeadingLevelTheCallerPlacesIt', () => {
    // A heading level is a property of POSITION. Get Help is a peer of What to
    // Know, so it renders at h2; hardcoding h3 made it an orphan with no parent.
    render(<ContentCollection collection={collection(2)} headingLevel={2} />);

    expect(screen.getByRole('heading', { name: 'Get help', level: 2 })).toBeInTheDocument();
  });

  it('shouldSubordinateSupportingCollectionsWhenPlacedBeneathAGroupHeading', () => {
    render(
      <ContentCollection
        collection={collection(2, { key: 'faqs', labelKey: 'collection.faqs', layer: 'SUPPORTING' })}
        headingLevel={4}
      />,
    );

    expect(screen.getByRole('heading', { name: 'FAQs', level: 4 })).toBeInTheDocument();
  });

  it('shouldRenderAnyCollectionFromItsDataRatherThanBranchingOnTheKey', () => {
    // The generic-component rule: a seventh collection must cost nothing. If this
    // ever needs a code change to pass, a per-collection component has crept in.
    render(
      <ContentCollection
        collection={collection(2, { key: 'law', labelKey: 'collection.law', layer: 'INFORMATION' })}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Law' })).toBeInTheDocument();
  });
});
