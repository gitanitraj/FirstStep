import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';
import type { TopicGroup, TopicNavigation } from '../types/api';

/**
 * "Discover" — what is available in this category, by topic.
 *
 * GROUPED vs FLAT is the payload's invariant, not a choice made here: a category
 * grouped in navigation.json arrives with `groups` populated and `topics` empty;
 * one absent from it arrives the other way round. Branching on `groups.length`
 * reads that invariant rather than re-deciding it (Decision 029/036).
 *
 * Topics with a count of 0 are rendered, not hidden. Suppressing them would
 * conceal exactly what validate_navigation.py exists to surface — a canonical
 * topic nothing can reach.
 */
interface Props {
  categoryKey: string;
  groups: TopicGroup[];
  topics: TopicNavigation[];
}

export default function CategoryBrowse({ categoryKey, groups, topics }: Props) {
  const { t } = useI18n();
  const grouped = groups.length > 0;

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
