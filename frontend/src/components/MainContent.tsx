import HeroGuidance from './HeroGuidance';

/**
 * The main content column of the homepage (the roadmap's "MainContent").
 * Step 5a fills the top with the merged Hero + AI guidance widget. The
 * Important Updates feed (5b) and CategoryPreviewList (5c) drop in below.
 */
export default function MainContent() {
  return (
    <>
      <HeroGuidance />
      <p className="section-placeholder">Important Updates & category previews — coming next</p>
    </>
  );
}
