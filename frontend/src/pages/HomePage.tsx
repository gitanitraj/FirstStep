import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import type { HomePayload } from '../types/api';
import SiteHeader from '../components/SiteHeader/SiteHeader';
import MissionCards from '../components/MissionCards/MissionCards';
import NewLaws from '../components/NewLaws/NewLaws';
import CommunityResources from '../components/CommunityResources/CommunityResources';
import FirstStepOriginals from '../components/FirstStepOriginals/FirstStepOriginals';
import CommunityInformation from '../components/CommunityInformation/CommunityInformation';
import SiteFooter from '../components/SiteFooter/SiteFooter';
import styles from './HomePage.module.css';

/**
 * The front door.
 *
 * <pre>
 *   header · mission cards · new laws
 *   community resources | first step originals (sidebar)
 *   community information · footer
 * </pre>
 *
 * **The homepage is a composition, and the complexity lives behind it.** Every
 * section here offers a way in and stops; searching, filtering, browsing and
 * full CivicContent presentation belong to the destination pages. The measure of
 * this file is how little it does.
 *
 * **The AI search was removed**, not relocated: it was powered by an Ollama agent
 * that is no longer wired in, so a prominent "ask us anything" box would have
 * promised something the application cannot currently do. Recorded on the
 * Version 3 backlog. Until it returns, Discover is the front door's entry point
 * for a resident who already knows what they need.
 *
 * Content caps at `--page-max` and centres, with `--page-gutter` inside it, so
 * every section aligns down the page and lines stay readable on a wide monitor.
 *
 * HomePage owns the single GET /api/home request (the BFF load) and distributes
 * the payload. The frame renders immediately so the page is never blank while
 * the payload loads, and each section handles its own null.
 */
export default function HomePage() {
  const [home, setHome] = useState<HomePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<HomePayload>('/api/home')
      .then(setHome)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <>
      <SiteHeader />
      <main className={styles.body}>
        <MissionCards />
        <NewLaws laws={home?.delawareLaws ?? null} />

        <div className={styles.split}>
          <CommunityResources pathways={home?.communityResources ?? null} />
          <FirstStepOriginals originals={home?.originals ?? null} />
        </div>

        <CommunityInformation flyers={home?.communityFlyers ?? null} />

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
