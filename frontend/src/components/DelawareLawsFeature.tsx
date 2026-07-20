import { useI18n } from '../i18n/I18nProvider';

/**
 * "New Delaware Laws" feature — directly below the Hero. Highlights recently
 * signed legislation by rotating ONE bill title into view at a time (7 most
 * recent). Slice A: section shell only; the rotator + real bill data (from the
 * GovernorSignedLegislation RSS feed) arrive in Slice C.
 */
export default function DelawareLawsFeature() {
  const { t } = useI18n();
  return (
    <section className="laws-feature" aria-labelledby="laws-title">
      <h2 id="laws-title" className="laws-title">
        {t('section.laws')}
      </h2>
      <p className="section-placeholder">{t('common.comingSoon')}</p>
    </section>
  );
}
