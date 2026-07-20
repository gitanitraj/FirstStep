import { NavLink } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';

/**
 * The platform's highest-level navigation, shown on the app-name row of the hero.
 * Slice A: items route to stub pages; real destinations arrive in later slices
 * (Housing → category page (F); Community Info → carousel page (E/H); Important
 * Notices → the Important Notices page (H); Life Assistance is a catchall for now).
 */
const NAV_ITEMS: { labelKey: string; icon: string; to: string }[] = [
  { labelKey: 'nav.housing', icon: '🏠', to: '/category/housing-assistance' },
  { labelKey: 'nav.community', icon: '🏘️', to: '/community-info' },
  { labelKey: 'nav.important', icon: '🔔', to: '/important-notices' },
  { labelKey: 'nav.life', icon: '🧭', to: '/life-assistance' },
];

export default function PrimaryNav() {
  const { t } = useI18n();
  return (
    <nav className="primary-nav" aria-label="Primary">
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) => `primary-nav-item${isActive ? ' active' : ''}`}
        >
          <span className="primary-nav-icon" aria-hidden="true">
            {item.icon}
          </span>
          <span>{t(item.labelKey)}</span>
        </NavLink>
      ))}
    </nav>
  );
}
