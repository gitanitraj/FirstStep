/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/ContentCollection/ContentCollection.tsx
 * Category destination. See references/decisions.md Decision 051.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   One purposeful collection on a category destination: heading, count, a
 *   preview of its items, and a control that reveals the rest in place.
 *
 * ONE GENERIC COMPONENT RENDERS EVERY COLLECTION
 *   Decision 045's amendment permits grouping by controlled metadata PROVIDED
 *   the grouping uses generic presentation components. A LawSection beside a
 *   FlyerSection would be the violation — not the grouping itself, but
 *   enumerating the metadata in code. Here the collection is DATA, so a seventh
 *   collection costs nothing. A test pins this by rendering a second collection
 *   key and asserting no code change was needed.
 *
 * WHAT THIS REPLACED
 *   An earlier draft proposed a universal "Content Explorer" — one collection
 *   viewed under switchable axes — as the ORGANIZING CONCEPT of the whole page.
 *   That was retired: a destination is a set of purposeful collections, not one
 *   collection under several axes. The switcher mechanism survives, but scoped
 *   to the supporting FAQ/Q&A destinations, which was its intended use all
 *   along.
 *
 * WHY EXPANSION IS LOCAL STATE AND NOT A URL PARAMETER
 *   The standing rule is that the URL is the source of truth WHEN VIEW STATE
 *   NEEDS TO BE SHAREABLE. "I scrolled a list open" is not a view another
 *   person needs to land in. An earlier draft made every "See more"
 *   URL-addressable and even specified the parameter naming scheme — an
 *   implementation detail promoted to architecture. Decision 051 declined it.
 *   If a collection later earns deep-linking, it becomes a search param without
 *   changing this component's contract.
 *
 * WHY THERE IS NO EMPTY BRANCH
 *   The backend refuses to build an empty collection, so this component never
 *   receives one. The rule lives at construction rather than in every consumer.
 *
 * ACCESSIBILITY
 *   The section is named by its own heading via aria-labelledby, so it appears
 *   as a named region. aria-expanded reports state; aria-controls points at the
 *   LIST it actually controls — an early version pointed it at the heading,
 *   which is the kind of error that tests asserting roles catch and tests
 *   asserting class names never do.
 * ============================================================================= */

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
}

export default function ContentCollection({
  collection,
  headingLevel = 3,
  layout = 'full',
}: Props) {
  const { t } = useI18n();
  const [expanded, setExpanded] = useState(false);
  const Heading = `h${headingLevel}` as 'h2' | 'h3' | 'h4';

  // A collection is never sent empty, so there is no empty branch to render —
  // the backend refuses to build one rather than the client hiding it.
  const shown = expanded ? collection.items : collection.items.slice(0, PREVIEW_COUNT);
  const remaining = collection.count - PREVIEW_COUNT;
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

      {remaining > 0 && (
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


// =============================================================================
// THE HEADING-LEVEL PROP — A DEFECT THE UNIT TESTS COULD NOT SEE
// =============================================================================
// This component originally hardcoded <h3>. Every test passed; the page rendered
// correctly; the outline was wrong:
//
//     H1  Housing
//     H2  Where do you need help?
//     H3    NEED HELP RIGHT AWAY ...
//     H3  Get help            <- orphaned: no H2 parent
//     H2  What to know
//     H3    Policy info / Law / Flyers / First Rate News
//     H3    FAQs / Expert Q&A <- read as PEERS of the primary four
//
// A screen-reader user navigating by heading would have found Get Help nested
// inside topic discovery, and the supporting collections standing beside Policy
// Info as equals. Both contradict the architecture the payload already encodes
// in `layer`.
//
// THE FIX, AND THE ONE IT IS NOT. A heading level is a property of POSITION, and
// only the caller knows the position. So the level became a prop. The tempting
// alternative — a PrimaryCollection and a SupportingCollection component — is the
// same violation as a component per metadata value, in a new costume: it
// enumerates positions in code instead of enumerating types.
//
// HOW IT WAS FOUND: by dumping the rendered document outline, not by a unit
// test. Assertions that ask "is this heading present" pass on a broken outline;
// only reading the hierarchy as a whole exposes it. The tests now pin LEVELS as
// well as names, which is what makes the fix a regression guard rather than a
// one-off correction.
// =============================================================================
