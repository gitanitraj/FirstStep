import { Link } from 'react-router-dom';
import PrimaryNav from './PrimaryNav';
import { useI18n } from '../i18n/I18nProvider';
import logoFeet from '../assets/logo-feet.png';

/**
 * The Hero: establishes trust without oversized imagery. Logo (upper-left) links
 * home from anywhere; the app name + tagline sit beside it, with the primary nav
 * on the app-name row. Occupies the upper third of the page below the Utility Bar.
 */
export default function SiteHero() {
  const { t } = useI18n();
  return (
    <header className="site-hero">
      <Link to="/" className="hero-brand" aria-label={t('brand.home')}>
        <img className="hero-logo" src={logoFeet} width={48} height={48} alt="" />
        <span className="hero-brand-text">
          <span className="hero-appname">First Step</span>
          <span className="hero-tagline">{t('tagline')}</span>
        </span>
      </Link>
      <PrimaryNav />
    </header>
  );
}
