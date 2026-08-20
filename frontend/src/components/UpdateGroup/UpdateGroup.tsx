import { useI18n } from '../../i18n/I18nProvider';
import { CONTENT_TYPE_LABEL } from '../../i18n/contentTypeLabel';
import type { UpdateGroup as UpdateGroupData } from '../../types/api';
import styles from './UpdateGroup.module.css';

interface Props {
  group: UpdateGroupData;
}

/**
 * ONE generic component that renders ANY content-type group.
 *
 * <pre>
 *   Laws  (180)
 *   ├─ Relating to Rent Increases.          Delaware General Assembly · 2026-08-06
 *   └─ …
 * </pre>
 *
 * **This is the constraint that makes grouping legitimate.** Decision 045 permits
 * presentation to group CivicContent by controlled metadata — but only through a
 * generic component, never a bespoke one per metadata value. A `NewsGroup`,
 * `LawGroup` and `FlyerGroup` would be four files that differ by a heading, and
 * would quietly make `contentType` a thing the UI enumerates rather than reads.
 * Adding a sixth ContentType must cost nothing here.
 *
 * The heading label comes from `CONTENT_TYPE_LABEL`, the exhaustive
 * `Record<ContentType, string>` that FAILS THE BUILD if a type has no label — so
 * a new type cannot reach a resident as `undefined`.
 *
 * Items arrive reverse-chronological from the server and are rendered in that
 * order; ordering within a group is the part that carries meaning.
 */
export default function UpdateGroup({ group }: Props) {
  const { t } = useI18n();
  const label = t(`${CONTENT_TYPE_LABEL[group.contentType]}.plural`);

  return (
    <section className={styles.group} aria-labelledby={`group-${group.contentType}`}>
      <div className={styles.head}>
        <h2 id={`group-${group.contentType}`} className={styles.title}>
          {label}
        </h2>
        <span className={styles.count}>{group.count}</span>
      </div>

      <ul className={styles.list}>
        {group.items.map((item) => (
          <li key={item.id} className={styles.item}>
            {item.url ? (
              <a
                className={styles.itemTitle}
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
              >
                {item.title}
              </a>
            ) : (
              <span className={styles.itemTitle}>{item.title}</span>
            )}
            {item.summary && <p className={styles.summary}>{item.summary}</p>}
            <p className={styles.meta}>
              {item.source && <span>{item.source}</span>}
              {item.source && item.date && <span> · </span>}
              {item.date && <span>{item.date}</span>}
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}
