/* =============================================================================
 * ANNOTATED REFERENCE — accessibility infrastructure (Slice I, pulled forward).
 * Groups: i18n/dictionary.ts, i18n/I18nProvider.tsx, hooks/useHighContrast.ts.
 * See references/decisions.md Decision 022. Keep in sync with the sources.
 * =============================================================================
 *
 * TWO USER-FACING CONTROLS live in the Utility Bar's right slot, ported from the
 * old static demo: a Language toggle (EN/ES) and a High Contrast toggle.
 * ============================================================================= */

/* ---------------------------------------------------------------------------
 * i18n/dictionary.ts — the translation table.
 * A plain `Record<Lang, Record<string, string>>` of UI-CHROME strings only.
 * WHY chrome-only: translating the interface is a DISPLAY concern (frontend);
 * translating CONTENT (resource data) would be backend work and isn't done here.
 * Keys are dotted for grouping (nav.housing, section.laws, a11y.contrast…).
 * No Oxford commas in the English strings (project rule).
 * ------------------------------------------------------------------------- */
export type Lang = 'en' | 'es';

/* ---------------------------------------------------------------------------
 * i18n/I18nProvider.tsx — context + provider + useI18n() hook.
 *
 * KEY DESIGN CHOICE: the context's DEFAULT value is a working English t(), so a
 * component that calls useI18n() renders fine even with NO provider around it.
 * That's why the frame/nav unit tests can render components bare (they get
 * English) — only the App wraps <I18nProvider> to add real switching.
 *
 * The provider: holds `lang` (seeded from localStorage), `setLang` (persists to
 * localStorage), and `t(key)` = dictionary[lang][key] ?? dictionary.en[key] ?? key
 * (falls back to English, then the raw key, so a missing translation degrades
 * gracefully). A useEffect mirrors `lang` onto <html lang> for assistive tech.
 * ------------------------------------------------------------------------- */
export function useI18nSketch() {
  // const { lang, setLang, t } = useI18n();
  // t('tagline'); setLang('es');
}

/* ---------------------------------------------------------------------------
 * hooks/useHighContrast.ts — the high-contrast toggle.
 * Returns { highContrast, toggle }. A useEffect does the two side effects:
 *   document.body.classList.toggle('high-contrast', highContrast)  // drives CSS
 *   localStorage.setItem(...)                                       // persists
 * The actual theme is pure CSS: `body.high-contrast <selector>` overrides in
 * index.css (black + #ff0), REWRITTEN for the new frame classes because the old
 * styles.css rules targeted components that no longer exist.
 *
 * GOTCHA (Decision 022): interactive elements like .primary-nav-item carry
 * `transition: all 0.2s`, so on toggle their colors ANIMATE — a screenshot taken
 * at t≈0 catches them mid-transition (looked like the rule wasn't applying; it
 * was). Non-transitioning elements (cards) flip instantly. Let transitions
 * settle before asserting visuals; consider suppressing transitions during a
 * theme switch as later polish.
 * ------------------------------------------------------------------------- */
export function useHighContrastSketch() {
  // const { highContrast, toggle } = useHighContrast();
  // <button aria-pressed={highContrast} onClick={toggle}>◐</button>
}
