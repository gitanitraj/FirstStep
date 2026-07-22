import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import type { HomePayload } from '../types/api';
import UtilityBar from '../components/UtilityBar';
import SiteHero from '../components/SiteHero';
import DelawareLawsFeature from '../components/DelawareLawsFeature';
import ResourceDiscovery from '../components/ResourceDiscovery';
import CommunityInformation from '../components/CommunityInformation';

/**
 * The civic-portal homepage. Five vertical sections: Utility Bar, Hero, New
 * Delaware Laws feature, Resource Discovery, and Community Information.
 *
 * HomePage owns the single GET /api/home request (the BFF load) and distributes
 * the payload to the sections — Slice D wires Resource Discovery
 * (organizations + categories); the Laws feature (C) and Community carousel (E)
 * consume the same payload later. The frame (Utility Bar + Hero) renders
 * immediately so the page is never blank while the payload loads.
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
      <UtilityBar />
      <SiteHero />
      <main className="home-body">
        <DelawareLawsFeature laws={home?.delawareLaws ?? null} />
        <ResourceDiscovery
          organizations={home?.organizations ?? null}
          categories={home?.categories ?? null}
          error={error}
        />
        <CommunityInformation flyers={home?.communityFlyers ?? null} />
      </main>
    </>
  );
}
