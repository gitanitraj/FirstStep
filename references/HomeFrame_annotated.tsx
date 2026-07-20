/* =============================================================================
 * ANNOTATED REFERENCE — the civic-portal homepage FRAME (Slice A).
 * Groups the small presentational frame components into one learning file:
 *   pages/HomePage.tsx, components/{UtilityBar, SiteHero, PrimaryNav}.tsx
 * (Section shells DelawareLawsFeature/ResourceDiscovery/CommunityInformation are
 * trivial placeholder scaffolds — filled in slices C–E.) App.tsx routing is
 * annotated at the bottom. See references/decisions.md Decision 021.
 * =============================================================================
 *
 * BIG PICTURE
 *   The homepage is a "trusted civic-information portal" of five vertical
 *   sections. Slice A builds the FRAME (Utility Bar + Hero + primary nav + three
 *   section shells) and introduces client-side routing. Everything data-driven
 *   comes later; here the components are pure structure.
 * ============================================================================= */

import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom';

/* ---------------------------------------------------------------------------
 * pages/HomePage.tsx — composes the five sections in order.
 * A plain composition component: no state, no data. Utility Bar and Hero sit
 * above <main>; the three shells stack inside it.
 * ------------------------------------------------------------------------- */
export function HomePage() {
  return (
    <>
      {/* <UtilityBar/> <SiteHero/> then: */}
      <main className="home-body">
        {/* <DelawareLawsFeature/> <ResourceDiscovery/> <CommunityInformation/> */}
      </main>
    </>
  );
}

/* ---------------------------------------------------------------------------
 * components/UtilityBar.tsx — narrow sticky top strip, 3 zones via a
 * grid-template-columns: 1fr auto 1fr (left slot | centered search | right slot).
 *   - left:  reserved for future social icons (empty now)
 *   - center: the ALWAYS-AVAILABLE AI search. Rendered but `disabled` in Slice A —
 *             wiring to /api/decide (canned responses acceptable) is Slice B.
 *   - right: reserved for ARIA/accessibility controls (a disabled ♿ placeholder)
 * Kept a narrow strip so it's always reachable "without dominating the page".
 * ------------------------------------------------------------------------- */

/* ---------------------------------------------------------------------------
 * components/SiteHero.tsx — trust without oversized imagery.
 * The brand is a <Link to="/"> so the logo "globally returns home from anywhere"
 * (it's a router Link, so it works on every page, not just the homepage). The
 * app name + exact tagline sit beside the logo; <PrimaryNav/> renders on the
 * app-name row (flex row, brand left / nav right).
 * ------------------------------------------------------------------------- */
export function SiteHeroSketch() {
  return (
    <header className="site-hero">
      <Link to="/" className="hero-brand" aria-label="First Step home">
        {/* logo + appname + tagline */}
      </Link>
      {/* <PrimaryNav/> */}
    </header>
  );
}

/* ---------------------------------------------------------------------------
 * components/PrimaryNav.tsx — the 4 highest-level nav items.
 * Uses <NavLink> (not <Link>) so the active route gets an `active` class for
 * styling. Destinations are data-config'd in a NAV_ITEMS array. In Slice A they
 * point at routes that render StubPage; later slices swap the targets to real
 * pages. `value`/labels: Housing Assistance, Community Info, Important Notices,
 * Life Assistance (catchall).
 * ------------------------------------------------------------------------- */
export function PrimaryNavSketch() {
  const NAV_ITEMS = [
    { label: 'Housing Assistance', icon: '🏠', to: '/category/housing-assistance' },
    { label: 'Community Info', icon: '🏘️', to: '/community-info' },
    { label: 'Important Notices', icon: '🔔', to: '/important-notices' },
    { label: 'Life Assistance', icon: '🧭', to: '/life-assistance' },
  ];
  return (
    <nav className="primary-nav" aria-label="Primary">
      {NAV_ITEMS.map((item) => (
        <NavLink key={item.to} to={item.to} className={({ isActive }) => `primary-nav-item${isActive ? ' active' : ''}`}>
          <span aria-hidden="true">{item.icon}</span>
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}

/* ---------------------------------------------------------------------------
 * App.tsx — routing. BrowserRouter's basename MUST be "/app-next" because the
 * SPA is served under that prefix (SpaWebConfig). "/" → HomePage; the four nav
 * destinations → StubPage; "*" catches unknown paths. StubPage reuses the frame
 * (UtilityBar + SiteHero) so navigation feels real while the destination is a
 * "Coming soon" placeholder — this is what the SpaWebConfig deep-link fallback
 * serves on a hard refresh.
 * ------------------------------------------------------------------------- */
export function AppSketch() {
  return (
    <BrowserRouter basename="/app-next">
      <Routes>
        <Route path="/" element={<HomePage />} />
        {/* /category/:key, /community-info, /important-notices, /life-assistance → StubPage */}
        {/* "*" → StubPage "Page not found" */}
      </Routes>
    </BrowserRouter>
  );
}
