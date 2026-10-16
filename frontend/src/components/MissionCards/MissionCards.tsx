import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import styles from './MissionCards.module.css';

/**
 * The three introductory pathways — Discover, Connect, Stay Informed.
 *
 * These explain First Step to a first-time visitor. They are **UX pathways, not
 * domain concepts** (Decision 041): each answers one resident question and leads
 * into a capability that already exists or has a named owning slice. Nothing
 * here queries anything, which is why this component takes no props and the
 * homepage payload has no field for it.
 *
 * They are deliberately EQUAL IN IMPORTANCE — same size, same treatment, no
 * primary. A visitor who does not yet know what they need should not be nudged
 * toward one of the three.
 *
 * **Title, one sentence, one button.** An earlier draft also carried an emoji
 * and an italic question ("What is available?"). Both were removed: the emoji
 * added decoration without meaning, and the question restated what the sentence
 * beneath it already said.
 */
const PATHWAYS: { key: string; to: string }[] = [
  { key: 'discover', to: '/discover' },
  { key: 'connect', to: '/find-help' },
  { key: 'informed', to: '/updates' },
];

export default function MissionCards() {
  const { t } = useI18n();

  return (
    <section className={styles.section} aria-labelledby="mission-title">
      <h2 id="mission-title" className="visually-hidden">
        {t('mission.sectionTitle')}
      </h2>
      <ul className={styles.grid}>
        {PATHWAYS.map((pathway) => (
          <li key={pathway.key} className={styles.card}>
            <h3 className={styles.title}>{t(`mission.${pathway.key}.title`)}</h3>
            <p className={styles.body}>{t(`mission.${pathway.key}.body`)}</p>
            <Link className={styles.action} to={pathway.to}>
              {t(`mission.${pathway.key}.action`)}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
