import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import OrganizationPage from './OrganizationPage';
import type { ContentItem, OrganizationPage as OrgPage } from '../../types/api';

const resource = (id: string, title: string): ContentItem => ({
  contentType: 'RESOURCE',
  id, title,
  summary: `Summary ${id}`,
  organization: 'Kingswood Community Center',
  location: 'Wilmington', cost: 'Free', urgency: null,
  date: null, url: 'https://example.org/service', imageUrl: null,
});

const org = (over: Partial<OrgPage> = {}): OrgPage => ({
  id: 'ORG-024',
  name: 'Kingswood Community Center',
  slug: 'kingswood-community-center',
  website: null,
  resources: [resource('R1', 'After-school program')],
  ...over,
});

function mockOk(data: OrgPage) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }),
  }));
}
function mockNotFound() {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    json: () => Promise.resolve({ success: false, data: null, errorCode: 'NOT_FOUND',
      errorMessage: 'Unknown organization: nope' }),
  }));
}
function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes><Route path="/organization/:slug" element={<OrganizationPage />} /></Routes>
    </MemoryRouter>,
  );
}

describe('OrganizationPage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('should request the organization named by the slug in the URL', async () => {
    mockOk(org());

    renderAt('/organization/kingswood-community-center');

    await screen.findByRole('heading', { level: 1 });
    expect(fetch).toHaveBeenCalledWith('/api/organization/kingswood-community-center');
  });

  it('should show the organization name and its resources', async () => {
    mockOk(org());

    renderAt('/organization/kingswood-community-center');

    expect(await screen.findByRole('heading', { level: 1 }))
      .toHaveTextContent('Kingswood Community Center');
    expect(screen.getByText('After-school program')).toBeInTheDocument();
  });

  it('should show no organization website when none is established', async () => {
    // Decision 049: absence beats an arbitrary authoritative link. Most
    // organizations legitimately have none.
    mockOk(org({ website: null }));

    renderAt('/organization/kingswood-community-center');

    await screen.findByRole('heading', { level: 1 });
    expect(screen.queryByText('Visit their website')).not.toBeInTheDocument();
  });

  it('should show the organization website when the data establishes one', async () => {
    mockOk(org({ website: 'https://kingswoodcc.org' }));

    renderAt('/organization/kingswood-community-center');

    const link = await screen.findByRole('link', { name: 'Visit their website' });
    expect(link).toHaveAttribute('href', 'https://kingswoodcc.org');
  });

  it('should render a registered organization that lists nothing', async () => {
    // "Exists but lists nothing" and "does not exist" are different facts and
    // must not share a rendering.
    mockOk(org({ resources: [] }));

    renderAt('/organization/kingswood-community-center');

    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByText(/does not list any services/)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('should show a not-found state for an unknown slug', async () => {
    mockNotFound();

    renderAt('/organization/nope');

    expect(await screen.findByRole('alert')).toHaveTextContent('Organization not found');
  });

  it('should not echo the server error message into the not-found state', async () => {
    mockNotFound();

    renderAt('/organization/nope');

    expect(await screen.findByRole('alert')).not.toHaveTextContent('Unknown organization');
  });

  it('should announce the loading state', async () => {
    mockOk(org());

    renderAt('/organization/kingswood-community-center');

    expect(screen.getByRole('status')).toHaveTextContent('Loading');
    await screen.findByRole('heading', { level: 1 });
  });

  it('should keep service-level links on the resource card', async () => {
    // Service contact belongs to the service. That is the reason the
    // organization record withholds it, so the card must still carry it.
    mockOk(org({ website: null }));

    renderAt('/organization/kingswood-community-center');

    const list = await screen.findByRole('list');
    expect(within(list).getByRole('link')).toHaveAttribute('href', 'https://example.org/service');
  });
});
