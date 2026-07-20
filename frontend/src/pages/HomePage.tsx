import UtilityBar from '../components/UtilityBar';
import SiteHero from '../components/SiteHero';
import DelawareLawsFeature from '../components/DelawareLawsFeature';
import ResourceDiscovery from '../components/ResourceDiscovery';
import CommunityInformation from '../components/CommunityInformation';

/**
 * The civic-portal homepage (Slice A frame). Five vertical sections: Utility Bar,
 * Hero, New Delaware Laws feature, Resource Discovery, and Community Information.
 * Sections C–E fill the feature/discovery/community shells with real content.
 */
export default function HomePage() {
  return (
    <>
      <UtilityBar />
      <SiteHero />
      <main className="home-body">
        <DelawareLawsFeature />
        <ResourceDiscovery />
        <CommunityInformation />
      </main>
    </>
  );
}
