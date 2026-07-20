import { useI18n } from '../i18n/I18nProvider';

/**
 * Community Information — timely community info outside traditional resource
 * directories. Its primary component is the flyer carousel. Slice A: section
 * shell only; the carousel arrives in Slice E.
 */
export default function CommunityInformation() {
  const { t } = useI18n();
  return (
    <section className="community-info" aria-labelledby="community-title">
      <h2 id="community-title" className="community-title">
        {t('section.community')}
      </h2>
      <p className="section-placeholder">{t('common.comingSoon')}</p>
    </section>
  );
}
