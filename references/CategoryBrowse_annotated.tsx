/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/CategoryBrowse.tsx
 * Slice F5b. See references/decisions.md Decisions 029, 036, 037.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   "Discover" — what is available in this category, presented as topics with
 *   counts. Each topic links to /category/{key}/{slug}, the Slice F6 page.
 *
 * WHAT IT DELIBERATELY DOES NOT DO
 *   It does not decide whether a category is grouped, does not sort, does not
 *   count, and does not filter. All of that was settled server-side by
 *   NavigationService (a read model) and CategoryPageService (an aggregate). This
 *   component reads a shape and renders it — the frontend-displays half of the
 *   BFF principle.
 * ============================================================================= */

import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';
import type { TopicGroup, TopicNavigation } from '../types/api';

interface Props {
  categoryKey: string;
  groups: TopicGroup[];
  topics: TopicNavigation[];
}

export default function CategoryBrowse({ categoryKey, groups, topics }: Props) {
  const { t } = useI18n();
  const grouped = groups.length > 0;

  // One row shape for both branches, so a grouped and a flat category render
  // identical topic rows. Reuses .discovery-item / .discovery-list from the
  // homepage rather than inventing a parallel set of classes — a topic row and a
  // category row are the same affordance, and sharing the CSS keeps them from
  // drifting apart visually.
  const topicLink = (topic: TopicNavigation) => (
    <li key={topic.slug}>
      <Link className="discovery-item" to={`/category/${categoryKey}/${topic.slug}`}>
        <span className="discovery-item-name">{topic.name}</span>
        <span className="discovery-item-count">{topic.count}</span>
      </Link>
    </li>
  );

  return (
    <section className="category-section category-topics" aria-labelledby="category-topics-title">
      <h2 id="category-topics-title" className="category-section-title">
        {t('category.browse')}
      </h2>

      {grouped
        ? groups.map((group) => (
            <div className="category-group" key={group.label}>
              <h3 className="category-group-title">{group.label}</h3>
              <ul className="discovery-list">{group.topics.map(topicLink)}</ul>
            </div>
          ))
        : topics.length > 0 && <ul className="discovery-list">{topics.map(topicLink)}</ul>}

      {/* Utilities declares no subcategories at all: its content is entirely
          topicless and reachable through the updates feed above. */}
      {!grouped && topics.length === 0 && (
        <p className="section-placeholder">{t('category.noTopics')}</p>
      )}
    </section>
  );
}

// =============================================================================
// GROUPED vs FLAT IS READ, NOT DECIDED
// =============================================================================
// `groups` and `topics` are MUTUALLY EXCLUSIVE in the payload:
//
//   grouped category   -> groups populated, topics EMPTY   (housing, community-support)
//   ungrouped category -> topics populated, groups EMPTY   (the other eight)
//
// That is Decision 029's rule — "a category absent from navigation.json renders a
// flat topic list" — enforced structurally by CategoryNavigation since F3.
// Branching on `groups.length > 0` READS that invariant. It does not re-derive
// it, and it must never be replaced by a client-side rule like "group when there
// are more than N topics", which would put an editorial decision in a display
// component and give "how is this category laid out?" two answers.
//
// The three-way render (grouped / flat / neither) is exhaustive by construction:
// if groups is empty AND topics is empty, the category genuinely declares no
// subcategories, and saying so is more useful than rendering an empty <ul>.
// =============================================================================

// =============================================================================
// WHY ZERO-COUNT TOPICS ARE STILL RENDERED
// =============================================================================
// A topic with count 0 gets a row like any other. Hiding it would be an easy
// "improvement" and a real regression: it would conceal exactly what
// validate_navigation.py exists to surface — a canonical topic that nothing can
// reach. A visible "Disability Advocacy 0" is a prompt to an editor; a hidden one
// is a silent gap.
//
// This is the same principle as the 404-vs-empty-page rule on the endpoint:
// never let "nothing here" and "no such thing" look alike.
// Pinned by shouldRenderATopicWithNoContentRatherThanHidingIt.
// =============================================================================

// =============================================================================
// WHY THE CLASS IS .category-topics AND NOT .category-browse
// =============================================================================
// It was .category-browse for about an hour, and the page rendered WRONG: this
// section came out ~240px wide while the two either side of it filled 1152px.
//
// CAUSE: index.css already had a `.category-browse` rule — the "Browse" BUTTON
// from CategoryPreviewList, the Step-5c component RETIRED in Slice A. Its CSS
// outlived it, and `align-self: flex-start; padding: 8px 14px; border-radius: 6px`
// silently applied to a full-width section. Nothing errored; the page simply
// looked broken.
//
// THE LESSON, and it is the second time this project has hit it (Step 5c needed
// "distinct .category-preview* CSS to avoid a .category-group-header collision"):
// DEAD CSS IS NOT INERT. A retired component's styles keep matching, so a new
// class name is not safe just because no component uses it — grep the stylesheet,
// not just the components.
//
// FIXED BY RENAMING THIS SECTION, not by deleting the stale rule: the dead
// .category-preview* / .category-browse / .category-checkbox block is unrelated
// to F5b, and the dead-CSS sweep is a tracked END-OF-REDESIGN TODO. Removing it
// here would be exactly the "improving adjacent code" this project asks against.
//
// Caught by measuring rendered widths in the browser, not by any test — a unit
// test asserts the DOM, and the DOM was correct. Worth remembering when judging
// what live verification is for.
// =============================================================================

// =============================================================================
// WHY TOPIC LINKS ARE SCOPED TO THE CATEGORY
// =============================================================================
// The href is /category/{categoryKey}/{slug}, never /topic/{slug}. Topics are
// NOT globally unique — "Eviction Prevention" is declared by both Housing and
// Legal, and F3 counts it independently under each. A category-scoped URL is what
// makes those two distinct pages rather than one page with an ambiguous parent.
//
// The route is declared in App.tsx as a StubPage until F6 builds it, following
// the precedent Slice D set: link to a declared stub, never to a URL that falls
// through to the not-found route.
// =============================================================================
