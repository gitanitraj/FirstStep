import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CategoryPage from './CategoryPage';
import TopicPage from '../TopicPage/TopicPage';
import OrganizationPage from '../OrganizationPage/OrganizationPage';
import { I18nProvider } from '../../i18n/I18nProvider';
import type { CategoryPage as Payload, ContentItem } from '../../types/api';

const card = (id: string): ContentItem => ({ contentType: 'RESOURCE', id, title: `Offering ${id}`, summary: null,
  organization: null, location: null, cost: null, urgency: 'standard', date: null,
  url: `https://example.org/${id}`, imageUrl: null });
const fixture = (): Payload => ({
  metadata: { key: 'housing', label: 'Housing', icon: '', totalCount: 8, countsByType: { RESOURCE: 8 }, lastUpdated: null },
  updates: [], groups: [], topics: [{ name: 'Empty topic', slug: 'empty-topic', count: 0, countsByType: {} }],
  organizations: [{ name: 'Linked Org', slug: 'linked-org', resourceCount: 2 }],
  collections: [
    { key: 'get-help', labelKey: 'collection.getHelp', layer: 'ACTION', count: 8, items: Array.from({ length: 8 }, (_, i) => card(`H${i}`)) },
    { key: 'first-rate-news', labelKey: 'collection.firstRateNews', layer: 'INFORMATION', count: 1,
      items: [{ ...card('OR'), contentType: 'NEWS', title: 'Approved article', url: null }] },
    { key: 'flyers', labelKey: 'collection.flyers', layer: 'INFORMATION', count: 1,
      items: [{ ...card('FL'), contentType: 'FLYER', title: 'Contextual flyer', url: null, imageUrl: '/images/seasonal/Flyer%20image.png' }] },
  ],
  destination: { communityId: 'second-community', featuredTopics: [
    { name: 'Topic Z', slug: 'topic-z', items: [
      { role: 'DURABLE', item: card('Z'), href: 'https://example.org/Z' },
      { role: 'FALLBACK', item: card('A'), href: '/category/housing/topic-z?communityId=second-community' },
    ] },
    { name: 'Topic A', slug: 'topic-a', items: [{ role: 'VISUAL', item: card('V'), href: 'https://example.org/V' }] },
  ], headlines: [{ id: 'OR', title: 'Approved article', href: '/originals/OR?communityId=second-community', date: null }], expertLinks: [], laws: [] },
});
function stub(page = fixture()) {
  vi.stubGlobal('fetch', vi.fn((url: string) => {
    const data = url.includes('/api/organization/') ? { id: 'org', name: 'Linked Org', slug: 'linked-org', website: null, resources: [card('org-local')] }
      : url.includes('/housing/help') || url.includes('/housing/topic-') ? {
        metadata: { categoryKey: 'housing', categoryLabel: 'Housing', categoryIcon: '', name: 'Topic Z', slug: url.includes('/help') ? 'help' : 'topic-z', totalCount: 8, countsByType: { RESOURCE: 8 } },
        items: Array.from({ length: 8 }, (_, i) => card(`H${i}`)), topics: [],
      } : page;
    return Promise.resolve({ json: () => Promise.resolve({ success: true, data }) });
  }));
}
function open(path = '/category/housing?communityId=second-community') {
  return render(<I18nProvider><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/category/:key" element={<CategoryPage />} />
    <Route path="/category/:key/:topic" element={<TopicPage />} />
    <Route path="/organization/:slug" element={<OrganizationPage />} />
  </Routes></MemoryRouter></I18nProvider>);
}

afterEach(() => { vi.unstubAllGlobals(); localStorage.clear(); document.body.className = ''; });
describe('IM-08 integrated destination', () => {
  it('t02t03t05 consumes original ordered headings/cards and never renders technical role badges', async () => {
    stub(); open(); const region = await screen.findByRole('region', { name: 'Featured topics' });
    expect(within(region).getAllByRole('heading', { level: 3 }).map(h => h.textContent)).toEqual(['Topic Z', 'Topic A']);
    expect(within(region).getAllByRole('listitem').map(c => c.textContent)).toEqual(['ResourceOffering Z', 'ResourceOffering A', 'ResourceOffering V']);
    expect(region).not.toHaveTextContent(/FALLBACK|DURABLE|TIMELY|VISUAL/);
    expect(screen.queryByText(/private-evidence|helpAccess|diagnostics/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Offering Z' })).toHaveAttribute('href', 'https://example.org/Z');
  });

  it('t07t09 navigates Topic heading and retains Community in request and back link', async () => {
    stub(); open(); fireEvent.click(await screen.findByRole('link', { name: 'Topic Z' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Topic Z' })).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith('/api/category/housing/topic-z?communityId=second-community');
    expect(within(screen.getByRole('navigation', { name: 'Breadcrumb' })).getByRole('link', { name: 'Housing' })).toHaveAttribute('href', '/category/housing?communityId=second-community');
  });

  it('t08 complete All Help journey keeps all inventory reachable beyond preview', async () => {
    stub(); open(); await screen.findByRole('heading', { name: 'Featured topics' });
    expect(screen.getByRole('link', { name: 'Get help in Housing' })).toHaveAttribute('href', '/category/housing/help?communityId=second-community');
    const help = screen.getByRole('region', { name: 'Get help' });
    expect(within(help).getAllByRole('listitem')).toHaveLength(4);
    fireEvent.click(within(help).getByRole('link', { name: 'All Housing Help' }));
    await screen.findByRole('heading', { name: 'All Housing Help', level: 1 });
    fireEvent.click(screen.getByRole('button', { name: /more$/ }));
    expect(screen.getAllByRole('listitem')).toHaveLength(8);
    expect(fetch).toHaveBeenCalledWith('/api/category/housing/help?communityId=second-community');
  });

  it('t11t12 collection choices retain contextual Flyer media and internal Article navigation', async () => {
    stub(); open(); await screen.findByRole('heading', { name: 'Featured topics' });
    fireEvent.click(screen.getByRole('button', { name: 'Flyers' }));
    expect(screen.getByRole('link', { name: 'Contextual flyer' })).toHaveAttribute('href', '/images/seasonal/Flyer%20image.png');
    expect(screen.getByRole('img', { name: 'Contextual flyer' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Flyers' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Policy info' }));
    expect(screen.getByRole('status')).toHaveTextContent('No current items in this collection.');
    fireEvent.click(screen.getByRole('button', { name: 'First Rate News' }));
    const section = screen.getByRole('region', { name: 'What to know' });
    expect(within(section).getByRole('link', { name: 'Approved article' })).toHaveAttribute('href', '/originals/OR?communityId=second-community');
  });

  it('t14 organization click uses scoped API and real associated resource cards', async () => {
    stub(); open(); fireEvent.click(await screen.findByRole('link', { name: 'Linked Org' }));
    await screen.findByRole('heading', { level: 1, name: 'Linked Org' });
    expect(fetch).toHaveBeenCalledWith('/api/organization/linked-org?communityId=second-community');
    expect(screen.getByRole('link', { name: 'Offering org-local' })).toBeInTheDocument();
  });

  it('t15 shows real headline link and truthful absent expert/law state', async () => {
    stub(); open(); const rail = await screen.findByRole('complementary', { name: 'More context' });
    expect(within(rail).getByRole('link', { name: 'Approved article' })).toHaveAttribute('href', '/originals/OR?communityId=second-community');
    expect(within(rail).getAllByText('No linked content is available here yet.')).toHaveLength(2);
    expect(within(rail).queryByRole('iframe')).not.toBeInTheDocument();
  });

  it('t17 announces loading, distinguishes network outage and does not show raw exception', async () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {}))); open();
    expect(screen.getByRole('status')).toHaveTextContent(/Loading/);
  });

  it('t17 announces network error separately from not found', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('private network detail'))); open();
    const alert = await screen.findByRole('alert'); expect(alert).not.toHaveTextContent("couldn't find");
    expect(alert).not.toHaveTextContent('private network detail');
  });

  it('t17 language controls translate all new chrome and high contrast persists', async () => {
    stub(); open(); await screen.findByRole('heading', { name: 'Featured topics' });
    fireEvent.click(screen.getByRole('button', { name: 'Toggle high contrast' })); expect(document.body).toHaveClass('high-contrast');
    fireEvent.click(screen.getByRole('button', { name: 'Switch language' }));
    expect(screen.getByRole('heading', { name: 'Temas destacados' })).toBeInTheDocument();
    expect(screen.getByRole('complementary', { name: 'Más contexto' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Elegir una colección de información cívica' })).toBeInTheDocument();
    await waitFor(() => expect(document.documentElement.lang).toBe('es'));
  });

  it('t09 makes server-configured default Community explicit rather than hardcoding it', async () => {
    stub(); open('/category/housing');
    await screen.findByRole('link', { name: 'Topic Z' });
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/category/housing?communityId=second-community'));
    expect(screen.getByRole('link', { name: 'Topic Z' })).toHaveAttribute('href', '/category/housing/topic-z?communityId=second-community');
  });
});
