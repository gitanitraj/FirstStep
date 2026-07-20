import { useI18n } from '../i18n/I18nProvider';
import { useHighContrast } from '../hooks/useHighContrast';

/**
 * The Utility Bar: a narrow strip across the very top of every page.
 * - Left: reserved space for future social-media icons (empty placeholder).
 * - Center: the always-available AI search box. UI only in Slice A; wiring to
 *   /api/decide (canned responses acceptable) comes in Slice B.
 * - Right: accessibility (ARIA) controls — language toggle (ES/EN) and high
 *   contrast — ported from the original site; plus room for future a11y features.
 */
export default function UtilityBar() {
  const { lang, setLang, t } = useI18n();
  const { highContrast, toggle } = useHighContrast();

  const otherLang = lang === 'en' ? 'es' : 'en';

  return (
    <div className="utility-bar">
      <div className="utility-slot utility-left" aria-hidden="true">
        {/* Social media icons — reserved for a later slice. */}
      </div>

      <div className="utility-center">
        <label className="visually-hidden" htmlFor="ai-search">
          {t('search.placeholder')}
        </label>
        <input
          id="ai-search"
          className="ai-search-input"
          type="search"
          placeholder={t('search.placeholder')}
          // Wiring deferred to Slice B (canned responses acceptable).
          disabled
        />
      </div>

      <div className="utility-slot utility-right">
        <button
          type="button"
          className="utility-button"
          onClick={() => setLang(otherLang)}
          aria-label={t('a11y.language')}
          title={t('a11y.language')}
        >
          {otherLang.toUpperCase()}
        </button>
        <button
          type="button"
          className="utility-button"
          onClick={toggle}
          aria-pressed={highContrast}
          aria-label={t('a11y.contrast')}
          title={t('a11y.contrast')}
        >
          ◐
        </button>
      </div>
    </div>
  );
}
