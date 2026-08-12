/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/NewLaws/
 *   NewLaws.tsx + NewLaws.module.css
 * Slice C originally, as DelawareLawsFeature (Decision 024). Reframed in
 * Slice H as ImportantChanges (042); RENAMED AGAIN in the H polish pass (043).
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   "New Laws in Delaware" — the retained V2 RSS/news scroll, and the
 *   homepage's answer to PASSIVE DISCOVERY.
 *
 * THE ONE IDEA
 *   It is a TEASER into the Updates ecosystem, not a news ticker. Everything
 *   below follows from that.
 * ============================================================================= */

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/I18nProvider';
import type { LawItem } from '../../types/api';
import styles from './NewLaws.module.css';

const ROTATE_MS = 5000;

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

const CHANGE_TYPES = ['legislation', 'policy', 'benefits', 'deadlines', 'announcements'];

export default function NewLaws({ laws }: { laws: LawItem[] | null }) {
  const { t } = useI18n();
  const [index, setIndex] = useState(0);
  const count = laws?.length ?? 0;

  useEffect(() => {
    if (count < 2 || prefersReducedMotion()) {
      return;
    }
    const id = setInterval(() => setIndex((i) => (i + 1) % count), ROTATE_MS);
    return () => clearInterval(id);
  }, [count]);

  const safeIndex = count > 0 ? index % count : 0;
  const bill = laws && count > 0 ? laws[safeIndex] : null;

  return (
    <section className={styles.section} aria-labelledby="laws-title">
      <div className={styles.head}>
        <h2 id="laws-title" className={styles.title}>{t('section.laws')}</h2>
        <Link className={styles.more} to="/updates">{t('mission.informed.action')}</Link>
      </div>

      {!bill && <p className={styles.placeholder}>{t('common.comingSoon')}</p>}

      {bill && (
        <div className={styles.rotator}>
          {/* key on the index so the fade-in replays on each change. */}
          <p className={styles.bill} key={safeIndex}>
            {bill.url
              ? <a href={bill.url} target="_blank" rel="noopener noreferrer">{bill.title}</a>
              : bill.title}
            {bill.date && <span className={styles.billDate}> · {bill.date}</span>}
          </p>
          {count > 1 && <div className={styles.dots}>…one button per item…</div>}
        </div>
      )}

      <ul className={styles.types}>
        {CHANGE_TYPES.map((type) => (
          <li key={type} className={styles.type}>{t(`changes.${type}`)}</li>
        ))}
      </ul>
    </section>
  );
}

// =============================================================================
// SECTION 1 — PASSIVE DISCOVERY, AND WHY THE SECTION EXISTS
// =============================================================================
// Not every visitor arrives with a defined need. The AI search serves the one
// who does; this section serves the one who does not.
//
// Its job is to expose residents to changes that may affect their housing,
// health, employment, benefits or daily lives — so someone who came looking for
// nothing in particular still leaves knowing something. The goal is helping
// people find information THEY DID NOT KNOW TO LOOK FOR, which is a thing a
// search box structurally cannot do.
//
// SECTION 2 — WHY IT SHOWS ONE ITEM AND A LIST OF CATEGORIES
// =============================================================================
// Three deliberate limits, all serving "teaser, not ticker":
//
//   1. ONE change at a time (the rotator, unchanged from Slice C). Reproducing
//      the Updates page on the homepage would defeat both.
//   2. A LINK OUT, given equal prominence to the heading.
//   3. A STATIC LIST of what KINDS of change live behind that link —
//      legislation, policy, benefit and program changes, deadlines, government
//      announcements.
//
// The third is the part worth defending. It is NOT derived from the feed, and
// that is the point: it describes the DESTINATION'S REMIT, which does not change
// when the feed does. A visitor learns what Updates is for even when the scroll
// is empty — and the scroll HAS been empty, twice, because the upstream Delaware
// RSS feed served malformed XML during F5b and F6 (tech-debt item 5).
//
// A test covers exactly that: with `laws={null}`, the change types still render.
// The section degrades to "here is what you would find" instead of a blank box.
//
// SECTION 3 — TWO RENAMES, AND WHY THE SECOND UNDID THE FIRST
// =============================================================================
//   Slice C  "New Delaware Laws"              named the DATA SOURCE
//   Slice H  "Important Changes in Delaware"  named the RESIDENT'S QUESTION
//   043      "New Laws in Delaware"           names the DATA SOURCE again
//
// The middle name was defensible and still wrong in practice. It promised
// "changes" — policy shifts, benefit changes, deadlines — while the section only
// ever renders signed legislation from one RSS feed. A heading that describes a
// broader remit than its content delivers is a small lie told on every page load.
//
// The chips beneath it are where that broader remit belongs: they describe what
// the Updates DESTINATION carries, and they are explicitly a teaser for it. The
// heading now describes what is actually in the box, and the chips describe what
// is behind the link. Both are honest.
//
// SECTION 3b — THE BOX NO LONGER RESIZES AS IT ROTATES
// =============================================================================
// `.bill` carries `min-height: calc(1.45em * 3)` — three lines, enough for the
// longest title in the feed (a House Joint Resolution running to a full
// paragraph) at the narrowest desktop width.
//
// Without it the box grew and shrank every five seconds and shoved the two
// columns below it up and down the page. Short titles now leave the space empty,
// which is the intended trade: a stable page beats a tightly-packed one, and
// motion under a link is worse than whitespace beside it.
//
// UNCHANGED: the rotator itself, the 5s cadence, the fade, the dots, and
// `prefersReducedMotion()`. The reduced-motion check is an accessibility
// guarantee, not a nicety — a 5-second auto-advancing animation is exactly what
// that media query exists to suppress, and the interval is never started rather
// than started-and-hidden.
//
// The data source is unchanged too: LegislationService.getRecentSignedBills(),
// server-side, seven most recent. Display-only, as before.
//
// SECTION 4 — MIGRATION NOTE
// =============================================================================
// Moved from `components/DelawareLawsFeature.tsx` + `.laws-*` rules in index.css
// (Slice H), then the folder renamed again to match the heading (043).
// to a co-located CSS Module. The rotator styles were carried over faithfully —
// the scroll itself is unchanged, only its framing is — including the
// `@keyframes` and the reduced-motion override, which moved with it rather than
// being left behind in a stylesheet whose component no longer exists. Leaving
// them would have created exactly the orphaned-CSS condition that caused two
// collisions before Decision 039's sweep.
// =============================================================================
