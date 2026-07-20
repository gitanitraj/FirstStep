import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { dictionary, type Lang } from './dictionary';

const STORAGE_KEY = 'firststep.lang';

interface I18nValue {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: (key: string) => string;
}

function translate(lang: Lang, key: string): string {
  return dictionary[lang][key] ?? dictionary.en[key] ?? key;
}

// Default value works WITHOUT a provider (English) so components render fine in
// isolation / tests; the provider adds real switching + persistence.
const I18nContext = createContext<I18nValue>({
  lang: 'en',
  setLang: () => {},
  t: (key) => translate('en', key),
});

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(() => {
    const stored = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
    return stored === 'es' ? 'es' : 'en';
  });

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* ignore storage failures */
    }
  }, []);

  // Keep the document language attribute in sync for assistive tech.
  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);

  const value = useMemo<I18nValue>(
    () => ({ lang, setLang, t: (key) => translate(lang, key) }),
    [lang, setLang],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nValue {
  return useContext(I18nContext);
}
