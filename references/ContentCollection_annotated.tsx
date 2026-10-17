/*
 * WHAT: Complete annotated mirror of frontend/src/components/ContentCollection/ContentCollection.tsx for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
import { Link } from 'react-router-dom';
import { useState } from 'react';
import ContentCard from '../ContentCard/ContentCard';
import { useI18n } from '../../i18n/I18nProvider';
import type { CategoryCollection } from '../../types/api';
import styles from './ContentCollection.module.css';

/**
 * One purposeful collection on a category destination (Decision 051).
 *
 * ONE GENERIC COMPONENT RENDERS EVERY COLLECTION. A `LawSection` beside a
 * `FlyerSection` would be the Decision 045 violation — not the grouping itself,
 * but enumerating the metadata in code. The collection is data, so a seventh
 * collection costs nothing here.
 *
 * PROGRESSIVE DISCLOSURE, NOT A SECOND REQUEST. The payload already carries every
 * matching item, so "See more" reveals what is in hand rather than fetching. The
 * page stays one request, and expanding never shows a spinner.
 *
 * Expansion is deliberately LOCAL STATE. The standing rule is that the URL is the
 * source of truth *when view state needs to be shareable* — and "I scrolled a list
 * open" is not a view another person needs to land in. Decision 051 declined to
 * make every expansion URL-addressable; if a collection later earns that, it
 * becomes a search param without changing this contract.
 */

/** How many items a collection shows before "See more". */
const PREVIEW_COUNT = 4;

interface Props {
  collection: CategoryCollection;
  /**
   * The heading level this collection sits at in the page's document outline.
   *
   * A HEADING LEVEL IS A PROPERTY OF POSITION, NOT OF THE COMPONENT. Hardcoding
   * `h3` made Get Help — a peer of What to Know — render as an orphaned h3 with
   * no h2 parent, so assistive technology read it as nested inside topic
   * discovery. The caller knows where the collection sits; the component cannot.
   *
   * The alternative was a component per position, which is the same violation as
   * a component per metadata value. One generic component, one prop.
   */
  headingLevel?: 2 | 3 | 4;
  /**
   * Pass 2 owns spatial arrangement. The prop exists so placement stays the
   * caller's decision, and only `full` is built today.
   */
  layout?: 'full' | 'compact';
  moreHref?: string;
  moreLabel?: string;
  previewCount?: number;
}

export default function ContentCollection({
  collection,
  headingLevel = 3,
  layout = 'full',
  moreHref, moreLabel, previewCount = PREVIEW_COUNT,
}: Props) {
  const { t } = useI18n();
  const [expanded, setExpanded] = useState(false);
  const Heading = `h${headingLevel}` as 'h2' | 'h3' | 'h4';

  // A collection is never sent empty, so there is no empty branch to render —
  // the backend refuses to build one rather than the client hiding it.
  const shown = expanded ? collection.items : collection.items.slice(0, previewCount);
  const remaining = collection.count - previewCount;
  const headingId = `collection-${collection.key}`;
  const listId = `${headingId}-list`;

  return (
    <section
      className={layout === 'compact' ? `${styles.collection} ${styles.compact}` : styles.collection}
      aria-labelledby={headingId}
    >
      <div className={styles.head}>
        <Heading id={headingId} className={styles.title}>
          {t(collection.labelKey)}
        </Heading>
        <span className={styles.count}>{collection.count}</span>
      </div>

      <ul id={listId} className={styles.list}>
        {shown.map((item) => (
          <ContentCard key={`${item.contentType}-${item.id}`} item={item} />
        ))}
      </ul>

      {moreHref ? <Link className={styles.more} to={moreHref}>{moreLabel}</Link> : remaining > 0 && (
        <button
          type="button"
          className={styles.more}
          onClick={() => setExpanded(!expanded)}
          aria-expanded={expanded}
          aria-controls={listId}
        >
          {expanded ? t('collection.seeLess') : `${remaining} ${t('collection.seeMore')}`}
        </button>
      )}
    </section>
  );
}
