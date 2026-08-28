import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { I18nProvider } from './i18n/I18nProvider';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage';
import TopicPage from './pages/TopicPage/TopicPage';
import UpdatesPage from './pages/UpdatesPage/UpdatesPage';
import ArticlePage from './pages/ArticlePage/ArticlePage';
import CommunityNoticesPage from './pages/CommunityNoticesPage/CommunityNoticesPage';
import StubPage from './pages/StubPage';

/**
 * App routing. Served under /app-next, so BrowserRouter uses that basename.
 *
 * Slice H rebuilt the homepage as the front door, which means most of these
 * routes are the destinations it now points at. They are stubs on purpose: the
 * slice's scope was the composition, and each destination gets its own slice —
 * the homepage is what tells us which ones are needed and in what order.
 *
 *   /discover         Discover → Explore Resources (all ten categories)
 *   /find-help        Connect → Find Help          (Slice G, organization directory)
 *   /updates          Latest Updates — GOVERNMENT producers (LIVE)
 *   /community-notices[/:view]  Community Notices — NON-GOVERNMENT producers.
 *                     Five routes, one page: overview · events · meetings ·
 *                     announcements · flyers (LIVE)
 *
 * Those two share ONE component; the sector is a prop, because the pages differ
 * only in who published the content (Decision 045).
 *   /community        Community Information         (local news, events, meetings, flyers)
 *   /discover/:facet  a discovery pathway — Seniors is the only one today
 *   /about            About First Step
 */
export default function App() {
  return (
    <I18nProvider>
      <BrowserRouter basename="/app-next">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/category/:key" element={<CategoryPage />} />
          <Route path="/category/:key/:topic" element={<TopicPage />} />
          <Route path="/discover" element={<StubPage name="Discover" />} />
          <Route path="/discover/:facet" element={<StubPage name="Discovery pathway" />} />
          <Route path="/find-help" element={<StubPage name="Find Help" />} />
          <Route path="/updates" element={<UpdatesPage sector="government" />} />
          {/* ONE page, five routes. The `:view` param is the source of truth for
              the active view, so every URL works when typed, shared or reached
              with the back button — no visit to the landing route required. */}
          <Route path="/originals/:id" element={<ArticlePage />} />
          <Route path="/community-notices" element={<CommunityNoticesPage />} />
          <Route path="/community-notices/:view" element={<CommunityNoticesPage />} />
          <Route path="/community" element={<StubPage name="Community" />} />
          <Route path="/about" element={<StubPage name="About First Step" />} />
          <Route path="/organization/:slug" element={<StubPage name="Organization" />} />
          <Route path="*" element={<StubPage name="Page not found" />} />
        </Routes>
      </BrowserRouter>
    </I18nProvider>
  );
}
