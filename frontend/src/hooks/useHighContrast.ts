import { useCallback, useEffect, useState } from 'react';

const STORAGE_KEY = 'firststep.highContrast';

/**
 * High-contrast accessibility mode. Toggles a `high-contrast` class on <body>
 * (CSS overrides live in index.css) and persists the choice. A display-only
 * preference — no backend involved.
 */
export function useHighContrast(): { highContrast: boolean; toggle: () => void } {
  const [highContrast, setHighContrast] = useState<boolean>(() => {
    return typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY) === 'true';
  });

  useEffect(() => {
    document.body.classList.toggle('high-contrast', highContrast);
    try {
      localStorage.setItem(STORAGE_KEY, String(highContrast));
    } catch {
      /* ignore storage failures */
    }
  }, [highContrast]);

  const toggle = useCallback(() => setHighContrast((v) => !v), []);

  return { highContrast, toggle };
}
