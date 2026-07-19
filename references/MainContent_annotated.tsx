/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/MainContent.tsx
 * Homepage-redesign Step 5a. See references/decisions.md Decision 018.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The composing container for the homepage's main column (the roadmap's
 *   "MainContent"). It owns no state and fetches no data — it just stacks the
 *   sections of the main column in order. AppLayout renders it inside <main
 *   className="home-main">, replacing Step 4's placeholder <p>.
 *
 * WHY A SEPARATE CONTAINER (vs. rendering HeroGuidance straight in AppLayout)
 *   Step 5 is being built in three slices (5a/5b/5c). This container gives 5b's
 *   Important Updates and 5c's CategoryPreviewList an obvious, single home to drop
 *   into — each future slice adds one child here and nothing in AppLayout changes.
 *   In 5a it holds just <HeroGuidance /> plus a placeholder line for what's next.
 * ============================================================================= */

import HeroGuidance from './HeroGuidance';

export default function MainContent() {
  return (
    // Fragment: the children are stacked by the parent <main className="home-main">
    // flex column (gap between sections comes from that CSS), so no wrapper needed.
    <>
      <HeroGuidance />
      {/* Removed in 5b/5c as the real sections land. */}
      <p className="section-placeholder">Important Updates & category previews — coming next</p>
    </>
  );
}
