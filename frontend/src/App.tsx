import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { I18nProvider } from './i18n/I18nProvider';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage';
import StubPage from './pages/StubPage';

/**
 * App routing (Slice A). Served under /app-next, so BrowserRouter uses that
 * basename. The homepage lives at "/"; primary-nav destinations render stub
 * pages until their real pages are built in later slices. Wrapped in I18nProvider
 * so the language toggle reaches every page.
 */
export default function App() {
  return (
    <I18nProvider>
      <BrowserRouter basename="/app-next">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/category/:key" element={<CategoryPage />} />
          {/* Declared so topic links resolve to "Coming soon" rather than falling
              through to the not-found route. The real page is Slice F6. */}
          <Route path="/category/:key/:topic" element={<StubPage name="Topic" />} />
          <Route path="/organization/:slug" element={<StubPage name="Organization" />} />
          <Route path="/community-info" element={<StubPage name="Community Info" />} />
          <Route path="/important-notices" element={<StubPage name="Important Notices" />} />
          <Route path="/life-assistance" element={<StubPage name="Life Assistance" />} />
          <Route path="*" element={<StubPage name="Page not found" />} />
        </Routes>
      </BrowserRouter>
    </I18nProvider>
  );
}
