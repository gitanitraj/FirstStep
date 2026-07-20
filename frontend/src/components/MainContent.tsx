import HeroGuidance from './HeroGuidance';
import ImportantUpdates from './ImportantUpdates';

/**
 * The main content column of the homepage (the roadmap's "MainContent").
 * Step 5a: the merged Hero + AI guidance widget. Step 5b: the Important Updates
 * feed. The CategoryPreviewList (5c) drops in below.
 */
export default function MainContent() {
  return (
    <>
      <HeroGuidance />
      <ImportantUpdates />
      <p className="section-placeholder">Category previews — coming next</p>
    </>
  );
}
