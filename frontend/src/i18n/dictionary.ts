export type Lang = 'en' | 'es';

/**
 * UI-chrome translations (a display concern — content translation would be a
 * backend responsibility, and isn't done here). Keys are dotted for grouping.
 * Mirrors the old app.js `t()` dictionary approach. No Oxford commas in English.
 */
export const dictionary: Record<Lang, Record<string, string>> = {
  en: {
    'brand.home': 'First Step home',
    'tagline': 'Your trusted guide to community resources, program updates and local information.',
    'nav.housing': 'Housing Assistance',
    'nav.community': 'Community Info',
    'nav.important': 'Important Notices',
    'nav.life': 'Life Assistance',
    'search.placeholder': 'Tell me what you need today…',
    'search.thinking': 'Thinking…',
    'search.error': 'Something went wrong. Please try again.',
    'search.unavailable': 'AI guidance is temporarily unavailable — try browsing categories below.',
    'search.sources': 'Sources',
    'search.close': 'Close',
    'search.resultsLabel': 'AI guidance',
    'section.laws': 'New Delaware Laws',
    'section.organizations': 'Organizations',
    'section.categories': 'Categories',
    'section.community': 'Community Information',
    'common.comingSoon': 'Coming soon.',
    'a11y.language': 'Switch language',
    'a11y.contrast': 'Toggle high contrast',
    'stub.back': '← Back to home',
  },
  es: {
    'brand.home': 'Inicio de First Step',
    'tagline': 'Tu guía confiable de recursos comunitarios, actualizaciones de programas e información local.',
    'nav.housing': 'Asistencia de Vivienda',
    'nav.community': 'Información Comunitaria',
    'nav.important': 'Avisos Importantes',
    'nav.life': 'Asistencia para la Vida',
    'search.placeholder': 'Dime qué necesitas hoy…',
    'search.thinking': 'Pensando…',
    'search.error': 'Algo salió mal. Inténtalo de nuevo.',
    'search.unavailable': 'La guía de IA no está disponible por el momento — explora las categorías más abajo.',
    'search.sources': 'Fuentes',
    'search.close': 'Cerrar',
    'search.resultsLabel': 'Guía de IA',
    'section.laws': 'Nuevas Leyes de Delaware',
    'section.organizations': 'Organizaciones',
    'section.categories': 'Categorías',
    'section.community': 'Información de la Comunidad',
    'common.comingSoon': 'Próximamente.',
    'a11y.language': 'Cambiar idioma',
    'a11y.contrast': 'Alternar alto contraste',
    'stub.back': '← Volver al inicio',
  },
};
