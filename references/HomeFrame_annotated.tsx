/* =============================================================================
 * ANNOTATED REFERENCE — the FRONT DOOR frame (Slice H).
 * Groups the frame into one learning file:
 *   components/SiteHeader/SiteHeader.tsx · pages/HomePage.tsx · App.tsx routing
 * Supersedes the Slice A frame (UtilityBar + SiteHero + PrimaryNav), all deleted.
 * See references/decisions.md Decision 042 and docs/architecture/05-front-door.md.
 * =============================================================================
 *
 * BIG PICTURE
 *   The homepage is a COMPOSITION, and the complexity lives behind it. Every
 *   section offers a way in and stops. Searching, filtering, browsing and full
 *   CivicContent presentation belong to the destination pages.
 *
 *   The measure of HomePage.tsx is how little it does.
 * ============================================================================= */

// =============================================================================
// PART 1 — SiteHeader: three zones, then the nav row
// =============================================================================
//
//   ┌──────────────┬────────────────────────┬──────────────┐
//   │ First Step   │ 🤖 Get answers with AI │     ES  ⊕    │
//   │ + tagline    │                        │              │
//   └──────────────┴────────────────────────┴──────────────┘
//     About  |  Housing  |  Community  |  Updates
//
// NAV STYLING IS v1's, RESTORED (044): labels are near-white rather than the
// cream --bg-lighter, which read as washed-out orange against the green, and
// hover fills the WHOLE TAB with rgba(255,255,255,.12) rather than only drawing
// an underline — the target a resident aims at is the thing that lights up.
//
// THIS SHAPE IS NOT NEW. It is the ORIGINAL First Step header, recovered:
// backing/src/main/resources/static/index.html has `.header-content` with
// branding on the left, an `.ai-banner-header` in the middle reading "Have
// questions? Get answers with AI.", and `.header-utilities` on the right holding
// exactly two buttons — `ES` and `⊕`. Beneath it sits a separate `.main-nav`.
//
// That recovery settled where the accessibility controls belong. It did NOT
// settle what "ARIA" meant in the spec — see the next section for that.

const NAV_ITEMS: { labelKey: string; to: string }[] = [
  { labelKey: 'nav.about', to: '/about' },
  { labelKey: 'nav.housing', to: '/category/housing' },
  { labelKey: 'nav.community', to: '/community' },
  { labelKey: 'nav.updates', to: '/updates' },
];

export default function SiteHeader() {
  const { lang, setLang, t } = useI18n();
  const { highContrast, toggle } = useHighContrast();
  const { pathname } = useLocation();
  const otherLang = lang === 'en' ? 'es' : 'en';
  const onHome = pathname === '/';

  return (
    <>
      <header className={styles.header}>
        <Link to="/" className={styles.brand} aria-label={t('brand.home')}>
          <img className={styles.logo} src={logoFeet} width={44} height={44} alt="" />
          <span className={styles.brandText}>
            <span className={styles.appName}>First Step</span>
            <span className={styles.tagline}>{t('tagline')}</span>
          </span>
        </Link>

        {/* No visible label, no role="group" — see the ARIA section below. */}
        <div className={styles.utilities}>
          <button … onClick={() => setLang(otherLang)}>{otherLang.toUpperCase()}</button>
          <button … onClick={toggle} aria-pressed={highContrast}>⊕</button>
        </div>
      </header>

      <nav className={styles.nav} aria-label="Primary">
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.to} to={item.to} className={…}>{t(item.labelKey)}</NavLink>
        ))}
      </nav>
    </>
  );
}

// -----------------------------------------------------------------------------
// "ARIA" — A MISREADING, CORRECTED (Decision 043)
// -----------------------------------------------------------------------------
// The spec listed the nav as `First Step | About | Housing | Community | Updates
// | ARIA`. Slice H read the last item as a LABEL for the utilities cluster and
// rendered the word "ARIA" beside the two buttons, wrapped in a role="group".
//
// **ARIA is Accessible Rich Internet Applications** — the W3C spec. v1 used some
// and it was ultimately removed; none is being reintroduced yet. So the chip and
// the wrapper are both gone. The buttons already carry their own accessible
// names, which made the wrapper ARIA for its own sake, and **no ARIA beats
// decorative ARIA**.
//
// A test pins it as a PROHIBITION rather than a feature: no "ARIA" text anywhere,
// and no role="group". Kept, deliberately, are the attributes doing real work —
// aria-labelledby on sections, aria-live on the AI answer, aria-pressed on the
// contrast toggle, aria-hidden on decorative emoji and arrows.
//
// -----------------------------------------------------------------------------
// THE AI ENTRY POINT WAS REMOVED, NOT RELOCATED (Decision 044)
// -----------------------------------------------------------------------------
// The header centre held a "Have questions? Get answers with AI." banner, and
// the page below it held a search section. Both are gone: the Ollama agent that
// powered them is no longer wired in, and **an entry point that cannot answer is
// worse than none** — a prominent "ask us anything" box is a promise, and a
// resident in difficulty is the wrong person to disappoint.
//
// `components/AiSearch/`, `AiResultCard.tsx` and `POST /api/decide` are retained
// UNRENDERED for whenever this is decided. On the Version 3 backlog.
//
// Consequence, stated plainly: the front door no longer serves "intentional
// discovery" directly. Discover -> Explore Resources carries that job until the
// search returns.
//
// The header centre is now empty; the brand sits left and the accessibility
// controls are pushed right with `margin-left: auto`.
//
// -----------------------------------------------------------------------------
// WHY ONE COMPONENT INSTEAD OF THREE
// -----------------------------------------------------------------------------
// Slice A had UtilityBar (sticky strip: search + a11y) ABOVE SiteHero (brand +
// PrimaryNav) — two stacked full-width bars holding what v1 fit in one row plus
// a nav. Collapsing them recovers vertical space at the top of every page, which
// is the space a front door most needs.
//
// The cost is that SiteHeader renders on EVERY route, so this slice's frame
// change touches the category, topic and stub pages. That is called out in the
// verification steps rather than discovered later.

// =============================================================================
// PART 2 — HomePage: the composition
// =============================================================================

export default function HomePage() {
  const [home, setHome] = useState<HomePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<HomePayload>('/api/home').then(setHome).catch((err: Error) => setError(err.message));
  }, []);

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        <MissionCards />                                          {/* static, no data */}
        <NewLaws laws={home?.delawareLaws ?? null} />              {/* passive discovery */}

        <div className={styles.split}>
          <CommunityResources pathways={home?.communityResources ?? null} />
          <FirstStepOriginals originals={home?.originals ?? null} />
        </div>

        <CommunityInformation flyers={home?.communityFlyers ?? null} />
        {error && <p className={styles.error} role="alert">{error}</p>}
      </main>
      <SiteFooter />
    </>
  );
}

// -----------------------------------------------------------------------------
// TWO VISITORS, ONE PAGE
// -----------------------------------------------------------------------------
// The section order is not decorative. It serves two different people:
//
//   INTENTIONAL DISCOVERY — "I need housing help." Served by the Discover
//   mission card until the AI search returns; it used to start at a search
//   section directly under the header.
//
//   PASSIVE DISCOVERY — arrives with no defined need. Everything below the fold
//   is for them: what has CHANGED that may affect their housing, health,
//   employment or benefits, then what EXISTS, then what the community is saying.
//   The goal is helping residents find information they did not know to look for.
//
// -----------------------------------------------------------------------------
// ONE REQUEST, SIX SECTIONS, EACH HANDLING ITS OWN NULL
// -----------------------------------------------------------------------------
// HomePage owns the single GET /api/home call (the BFF load) and distributes.
// The frame renders IMMEDIATELY — header, AI search, mission cards and the
// footer need no data at all — so the page is never blank while the payload
// loads. Each data section takes `T[] | null` and shows its own placeholder,
// rather than the page gating everything behind one spinner.
//
// THE UPDATES FEED IS NO LONGER HERE (043). It became TWO destination pages
// split by who produced the content — Latest Updates (government: agencies,
// officials, programs) and Community Notices (non-government: churches,
// nonprofits, community groups). A single merged feed on the front door could
// not honour that distinction, and the front door's job is the way in.
// `HomePayload.updates` went with it; /api/updates still serves those pages.
//
// THE PAGE IS FULL WIDTH (043) — no centred max-width column. Sections indent by
// the shared `--page-gutter` token so they align down the page while the content
// uses the whole viewport. The AI form keeps a 760px measure, because a search
// field spanning 2000px is unusable.

// =============================================================================
// PART 3 — App routing: the destinations the front door points at
// =============================================================================
//
//   /                    HomePage
//   /category/:key       CategoryPage        (Slice F — REAL)
//   /category/:key/:topic TopicPage          (Slice F — REAL)
//   /discover            StubPage            Discover → Explore Resources
//   /discover/:facet     StubPage            a discovery pathway (Seniors)
//   /find-help           StubPage            Connect → Find Help (Slice G)
//   /updates             StubPage            Stay Informed → View Updates
//   /community           StubPage            Community Information
//   /about               StubPage
//   /organization/:slug  StubPage            (Slice G)
//   *                    StubPage            not found
//
// MOST OF THESE ARE STUBS ON PURPOSE. Slice H's scope was the composition, and
// the front door is what tells us which destinations are needed and in what
// order. Two of the three mission cards therefore land on stubs the day this
// ships — a consequence of the intended sequencing (compose first, fill in
// behind), recorded up front rather than discovered during verification.
//
// SIX OF THE SEVEN Community Resources pathways are NOT stubs: housing,
// employment, health, legal, furniture-household and food all resolve to real
// category pages built in Slice F. Only Seniors is a stub, because the `seniors`
// derivation is Front Door gap 3 and is not implemented.
//
// `/discover/:facet` is its own route rather than reusing `/category/:key`, and
// that separation IS the architecture: a discovery pathway is a filtered view
// ACROSS the taxonomy, never a node within it. Routing Seniors to
// /category/seniors would assert a taxonomy entry that must never exist. A test
// pins both hrefs.
// =============================================================================
