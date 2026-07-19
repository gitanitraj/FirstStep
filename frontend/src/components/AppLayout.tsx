import Sidebar from './Sidebar';
import MainContent from './MainContent';

/**
 * Homepage shell (Step 4): sticky header + two-column (sidebar + main) grid.
 * The main column is filled by MainContent (Step 5: Hero+AI merge, then
 * Important Updates and category previews).
 */
export default function AppLayout() {
  return (
    <>
      <header className="site-header">
        <div className="header-content">
          <h1 className="logo">First Step</h1>
        </div>
      </header>
      <div className="home-layout">
        <Sidebar />
        <main className="home-main">
          <MainContent />
        </main>
      </div>
    </>
  );
}
