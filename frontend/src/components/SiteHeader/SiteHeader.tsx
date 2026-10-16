import { Link, NavLink } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import { useHighContrast } from '../../hooks/useHighContrast';
import logoFeet from '../../assets/logo-feet.png';
import styles from './SiteHeader.module.css';

/**
 * The global header — three zones in one row, then the nav on its own row.
 *
 * <pre>
 *   ┌──────────────┬─────────────────────────────┬─────────┐
 *   │ First Step   │                             │  ES  ⊕  │
 *   └──────────────┴─────────────────────────────┴─────────┘
 *     About  |  Housing  |  Community  |  Updates
 * </pre>
 *
 * THIS SHAPE COMES FROM THE ORIGINAL FIRST STEP, not from a new design. v1's
 * header was branding | AI banner | utilities with a separate nav bar beneath,
 * and that is where the accessibility controls have always lived — the right of
 * the header row.
 *
 * Replaces `UtilityBar` + `SiteHero`, which between them held the same zones
 * across two stacked strips.
 *
 * The centre held an AI banner until the Ollama agent behind it was retired.
 * Rather than leave a prompt pointing at a capability the application no longer
 * has, both the banner and the search were removed — an entry point that cannot
 * answer is worse than none. Recorded on the Version 3 backlog.
 */
const NAV_ITEMS: { labelKey: string; to: string }[] = [
  { labelKey: 'nav.about', to: '/about' },
  { labelKey: 'nav.housing', to: '/category/housing' },
  { labelKey: 'nav.community', to: '/community' },
  { labelKey: 'nav.updates', to: '/updates' },
];

export default function SiteHeader() {
  const { lang, setLang, t } = useI18n();
  const { highContrast, toggle } = useHighContrast();
  const otherLang = lang === 'en' ? 'es' : 'en';

  return (
    <>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to="/" className={styles.brand} aria-label={t('brand.home')}>
            <img
              className={styles.logo}
              src={logoFeet}
              width={44}
              height={44}
              alt=""
            />
            <span className={styles.brandText}>
              <span className={styles.appName}>First Step</span>
              <span className={styles.tagline}>{t('tagline')}</span>
            </span>
          </Link>

          {/* No visible label and no role='group'. An earlier draft put the word
            'ARIA' here, which was a misreading — ARIA is Accessible Rich Internet
            Applications, not a name for these controls. The two buttons already
            carry their own accessible names, so a wrapper role would be ARIA for
            its own sake, and no ARIA beats decorative ARIA. */}
          <div className={styles.utilities}>
            <button
              type="button"
              className={styles.utilityButton}
              onClick={() => setLang(otherLang)}
              aria-label={t('a11y.language')}
              title={t('a11y.language')}
            >
              {otherLang.toUpperCase()}
            </button>
            <button
              type="button"
              className={styles.utilityButton}
              onClick={toggle}
              aria-pressed={highContrast}
              aria-label={t('a11y.contrast')}
              title={t('a11y.contrast')}
            >
              ⊕
            </button>
          </div>
        </div>
      </header>

      <nav className={styles.nav} aria-label="Primary">
        <div className={styles.navInner}>
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
              }
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </div>
      </nav>
    </>
  );
}
