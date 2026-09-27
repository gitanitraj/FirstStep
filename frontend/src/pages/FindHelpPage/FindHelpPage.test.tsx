import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import FindHelpPage from './FindHelpPage';
import type { FindHelpPage as PageData } from '../../types/api';

const page = (over: Partial<PageData> = {}): PageData => ({
  gateways: [
    { key: 'housing', label: 'Housing', icon: '🏠', destination: '/category/housing', available: true },
    { key: 'legal', label: 'Legal', icon: '⚖️', destination: '/category/legal', available: true },
    { key: 'seniors', label: 'Seniors', icon: '🧓', destination: null, available: false },
  ],
  openToEveryone: [],
  directory: [
    { name: 'Kingswood Community Center', slug: 'kingswood-community-center',
      services: [{ title: 'Community Services', category: 'Housing', city: 'Wilmington', phone: '302-764-9022' }] },
    { name: 'Ministry of Caring Inc', slug: 'ministry-of-caring-inc', services: [] },
  ],
  ...over,
});

function mockPage(data: PageData) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }),
  }));
}
const renderPage = () => render(<MemoryRouter><FindHelpPage /></MemoryRouter>);

describe('FindHelpPage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('should send a category gateway to the category page, not filter in place', async () => {
    // The central claim: selecting a category LEAVES Find Help, so the resident
    // reaches an environment carrying every content form for that need.
    mockPage(page());

    renderPage();

    const housing = await screen.findByRole('link', { name: /Housing/ });
    expect(housing).toHaveAttribute('href', '/category/housing');
  });

  it('should render an unbuilt pathway visibly rather than hiding it', async () => {
    // Data populates the experience; it does not decide the information
    // architecture. Removing the door would shrink the site's navigation to
    // match a thin table.
    mockPage(page());

    renderPage();

    expect(await screen.findByText('Seniors')).toBeInTheDocument();
    expect(screen.getByText('Coming soon')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Seniors/ })).not.toBeInTheDocument();
  });

  it('should state the selection rule in the starting-point heading', async () => {
    // Not "Popular" (no usage data) and not "Recently added" (no date field).
    // The label IS the disclosure.
    mockPage(page({
      openToEveryone: [{
        contentType: 'RESOURCE', id: 'R1', title: 'Food pantry', summary: 'Open weekly',
        organization: 'Kingswood', location: 'Wilmington', cost: 'Free', urgency: null,
        date: null, url: null, imageUrl: null,
      }],
    }));

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Open to everyone' })).toBeInTheDocument();
    expect(screen.getByText(/no age or gender requirement/)).toBeInTheDocument();
  });

  it('should link a directory entry to its organization page', async () => {
    mockPage(page());

    renderPage();

    const link = await screen.findByRole('link', { name: 'Kingswood Community Center' });
    expect(link).toHaveAttribute('href', '/organization/kingswood-community-center');
  });

  it('should attribute a phone number to the service, not the organization', async () => {
    // Decision 049 surviving the page whose job is "who do I call". The number
    // sits on the service line, never beside the organization name.
    mockPage(page());

    renderPage();

    const entry = (await screen.findByRole('link', { name: 'Kingswood Community Center' })).closest('li')!;
    const orgLink = within(entry).getByRole('link');
    expect(orgLink).not.toHaveTextContent('302-764-9022');
    expect(within(entry).getByText(/302-764-9022/)).toBeInTheDocument();
  });

  it('should not repeat the organization name as a service title', async () => {
    // Resources derive their title from the organization, so the backend sends
    // null rather than a name that would render twice under one heading.
    mockPage(page({
      directory: [{
        name: 'American Red Cross', slug: 'american-red-cross',
        services: [{ title: null, category: 'Housing', city: 'Wilmington', phone: '302-656-6420' }],
      }],
    }));

    renderPage();

    const entry = (await screen.findByRole('link', { name: 'American Red Cross' })).closest('li')!;
    expect(within(entry).getAllByText(/American Red Cross/)).toHaveLength(1);
    expect(within(entry).getByText(/302-656-6420/)).toBeInTheDocument();
  });

  it('should list an organization that has no services', async () => {
    // Several are known only through a flyer or expert answer. They are no less
    // contactable for it.
    mockPage(page());

    renderPage();

    expect(await screen.findByRole('link', { name: 'Ministry of Caring Inc' })).toBeInTheDocument();
  });

  it('should filter the directory by name without leaving the page', async () => {
    mockPage(page());

    renderPage();
    await screen.findByRole('link', { name: 'Kingswood Community Center' });
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'ministry' } });

    expect(screen.queryByRole('link', { name: 'Kingswood Community Center' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ministry of Caring Inc' })).toBeInTheDocument();
  });

  it('should match a service name as well as an organization name', async () => {
    mockPage(page());

    renderPage();
    await screen.findByRole('link', { name: 'Kingswood Community Center' });
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'community services' } });

    expect(screen.getByRole('link', { name: 'Kingswood Community Center' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ministry of Caring Inc' })).not.toBeInTheDocument();
  });

  it('should put the directory after the gateways in the document', async () => {
    // Ordering is the design: asking someone in difficulty to scan 63
    // organizations before offering a way in is the directory this page was
    // scoped against.
    mockPage(page());

    renderPage();
    await screen.findByRole('heading', { name: /What kind of help/ });

    const headings = screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent);
    expect(headings[0]).toMatch(/What kind of help/);
    expect(headings[headings.length - 1]).toMatch(/specific organization or service/);
  });
});
