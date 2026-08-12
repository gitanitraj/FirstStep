import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import styles from './SiteFooter.module.css';

/**
 * The site footer.
 *
 * ⚠️ CONTENT IS MOCK. The original First Step had no footer, so there was
 * nothing to carry over. The STRUCTURE here is the deliverable — brand line,
 * quick links, contact, a verification note and attribution — so real copy drops
 * into the same slots without a rewrite.
 *
 * Mock values are written to be obviously placeholder (`hello@example.org`)
 * rather than plausible-looking. A fake-but-believable phone number on a civic
 * resource site is the kind of detail a resident would actually try to call.
 *
 * The verification line is not filler: `04-editorial-principles.md` requires
 * that First Step "encourage residents to connect with the originating
 * organization rather than replacing it", and the footer is where that belongs
 * on every page.
 */
const QUICK_LINKS: { labelKey: string; to: string }[] = [
  { labelKey: 'nav.about', to: '/about' },
  { labelKey: 'nav.community', to: '/community' },
  { labelKey: 'nav.updates', to: '/updates' },
];

export default function SiteFooter() {
  const { t } = useI18n();

  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.brandBlock}>
          <span className={styles.brand}>First Step</span>
          <span className={styles.tagline}>{t('tagline')}</span>
        </div>

        <nav className={styles.links} aria-label={t('footer.linksLabel')}>
          {QUICK_LINKS.map((link) => (
            <Link key={link.to} className={styles.link} to={link.to}>
              {t(link.labelKey)}
            </Link>
          ))}
        </nav>

        <div className={styles.contact}>
          <span className={styles.mock}>{t('footer.mockNotice')}</span>
          <span>hello@example.org</span>
        </div>
      </div>

      <p className={styles.verify}>{t('footer.verify')}</p>
      <p className={styles.attribution}>{t('footer.attribution')}</p>
    </footer>
  );
}
