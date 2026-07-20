import { Link } from 'react-router-dom';
import UtilityBar from '../components/UtilityBar';
import SiteHero from '../components/SiteHero';
import { useI18n } from '../i18n/I18nProvider';

/**
 * Placeholder for a primary-nav destination that a later slice will build out.
 * It reuses the frame (Utility Bar + Hero) so navigation feels real and proves
 * the router + SpaWebConfig deep-link serving.
 */
export default function StubPage({ name }: { name: string }) {
  const { t } = useI18n();
  return (
    <>
      <UtilityBar />
      <SiteHero />
      <main className="home-body">
        <section className="stub-page">
          <h2>{name}</h2>
          <p className="section-placeholder">{t('common.comingSoon')}</p>
          <Link to="/" className="stub-back">
            {t('stub.back')}
          </Link>
        </section>
      </main>
    </>
  );
}
