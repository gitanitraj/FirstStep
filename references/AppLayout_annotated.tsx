/* =============================================================================
 * ANNOTATED REFERENCE — frontend/src/components/AppLayout.tsx
 * Homepage-redesign Step 4 (AppLayout + Sidebar). See references/decisions.md
 * Decision 017. Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS COMPONENT IS
 *   The homepage shell: a sticky site header above a two-column grid whose left
 *   column is the <Sidebar> and whose right column is the main content area.
 *   This is the structural frame every later homepage step builds inside.
 *
 * WHY IT IS THIS THIN
 *   AppLayout holds NO state and fetches NO data — it is pure composition. The
 *   data-owning logic lives in the children (Sidebar fetches its own
 *   categories). Keeping the layout a dumb frame means Step 5 can drop the real
 *   MainContent into the <main> without touching this file.
 *
 *   NO ROUTER (Step-4 scope decision). AppLayout is rendered directly by
 *   App.tsx — there is no <BrowserRouter> and no <Routes>. Real client routes
 *   (result pages) are Step 6; the SpaWebConfig catch-all widening flagged in
 *   Decision 016 travels with those routes, not before them. react-router-dom
 *   stays installed-but-unused until then.
 * ============================================================================= */

// The only import: the sibling Sidebar. AppLayout's whole job is to place it.
import Sidebar from './Sidebar';

/**
 * Homepage shell (Step 4): sticky header + two-column (sidebar + main) grid.
 * The main area is a placeholder until Step 5 fills it with the Hero+AI merge,
 * Important Updates, and category previews.
 */
export default function AppLayout() {
  return (
    // Fragment (<>…</>): the header and the layout grid are siblings, not nested
    // — the header spans full width and is sticky; the grid is width-capped and
    // centered. Wrapping them in a <div> would add a pointless node.
    <>
      {/* Sticky header. The .site-header/.header-content/.logo classes mirror
          the existing hand-written demo header in static/styles.css, recolored
          from the shared palette vars in index.css. z-index:100 + top:0 keep it
          above scrolling content; the sidebar's own sticky top:80px sits below
          it so the two never overlap. */}
      <header className="site-header">
        <div className="header-content">
          <h1 className="logo">First Step</h1>
        </div>
      </header>

      {/* .home-layout is the CSS grid: `240px 1fr` on desktop, collapsing to a
          single column under 768px (see the @media block in index.css). */}
      <div className="home-layout">
        <Sidebar />

        {/* <main> landmark. Placeholder only this step — Step 5 replaces the
            inner <p> with the Hero+AI widget, Important Updates, and the
            CategoryPreviewList. .home-main is a vertical flex column with gap. */}
        <main className="home-main">
          <p>Main content — Step 5</p>
        </main>
      </div>
    </>
  );
}
