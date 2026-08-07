import { useI18n } from '../i18n/I18nProvider';
import { CONTENT_TYPE_LABEL } from '../i18n/contentTypeLabel';
import type { UpdateItem } from '../types/api';

/**
 * "Stay Informed" — what has changed in this category: news, signed legislation,
 * flyers and expert answers, newest first.
 *
 * This section is why the category page is an aggregate rather than just
 * navigation. Every item here carries a category and no subcategory, so no topic
 * tile can reach it — roughly half of a category's content (Decision 036).
 *
 * BADGES ARE DERIVED FROM `contentType`, NEVER `type`. The backend sends what a
 * thing IS and this component decides how to name it, which is the end state
 * Decision 036 commits Slice H to. `type` reports "news" for both curated news
 * and a signed bill; reading it here would make a law indistinguishable from an
 * announcement — the exact conflation `contentType` was added to remove.
 */
interface Props {
  updates: UpdateItem[];
  lastUpdated: string | null;
}

export default function CategoryUpdates({ updates, lastUpdated }: Props) {
  const { t } = useI18n();

  return (
    <section className="category-section category-updates" aria-labelledby="category-updates-title">
      <div className="category-section-head">
        <h2 id="category-updates-title" className="category-section-title">
          {t('category.updates')}
        </h2>
        {lastUpdated && (
          <span className="category-updated">
            {t('category.latest')} {lastUpdated}
          </span>
        )}
      </div>

      {updates.length === 0 ? (
        <p className="section-placeholder">{t('category.noUpdates')}</p>
      ) : (
        <ul className="category-update-list">
          {updates.map((u) => (
            <li className="category-update" key={`${u.contentType}-${u.id}`}>
              <div className="category-update-head">
                <span className={`category-badge badge-${u.contentType.toLowerCase()}`}>
                  {t(CONTENT_TYPE_LABEL[u.contentType])}
                </span>
                {/* Link out only when the source gave us a URL (news and laws do;
                    flyers and expert answers do not). Editorial standard:
                    encourage residents to reach the originating organization
                    rather than replacing it. */}
                {u.url ? (
                  <a
                    className="category-update-title"
                    href={u.url}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {u.title}
                  </a>
                ) : (
                  <span className="category-update-title">{u.title}</span>
                )}
              </div>
              {u.summary && <p className="category-update-summary">{u.summary}</p>}
              <p className="category-update-meta">
                {u.source && <span>{u.source}</span>}
                {u.source && u.date && <span> · </span>}
                {u.date && <span>{u.date}</span>}
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
