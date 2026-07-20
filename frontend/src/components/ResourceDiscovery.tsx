import { useI18n } from '../i18n/I18nProvider';

/**
 * Resource Discovery — the homepage's primary navigation area. Two columns with
 * a subtle vertical divider: Organizations (left) and major resource Categories
 * (right). Slice A: two-column shell only; the curated org shortlist and the
 * category list (each a navigation entry) are wired to data in Slice D.
 */
export default function ResourceDiscovery() {
  const { t } = useI18n();
  return (
    <section className="discovery" aria-labelledby="discovery-title">
      <h2 id="discovery-title" className="visually-hidden">
        {t('section.categories')}
      </h2>
      <div className="discovery-columns">
        <div className="discovery-col discovery-orgs">
          <h3 className="discovery-col-title">{t('section.organizations')}</h3>
          <p className="section-placeholder">{t('common.comingSoon')}</p>
        </div>
        <div className="discovery-divider" aria-hidden="true" />
        <div className="discovery-col discovery-categories">
          <h3 className="discovery-col-title">{t('section.categories')}</h3>
          <p className="section-placeholder">{t('common.comingSoon')}</p>
        </div>
      </div>
    </section>
  );
}
