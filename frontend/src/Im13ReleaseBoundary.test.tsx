import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ArticlePage from './pages/ArticlePage/ArticlePage';
import OrganizationPage from './pages/OrganizationPage/OrganizationPage';
import TopicPage from './pages/TopicPage/TopicPage';

const article = { id: 'OR-003', title: 'Approved synthetic reading', summary: null, body: 'Public body',
  whyItMatters: null, byline: 'Editor', disclosure: null, publishDate: null, updatedDate: null,
  categoryTags: ['Housing'], subcategory: null };
function open(path: string) {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/originals/:id" element={<ArticlePage />} />
    <Route path="/organization/:slug" element={<OrganizationPage />} />
    <Route path="/category/:key/:topic" element={<TopicPage />} />
  </Routes></MemoryRouter>);
}
const ok = (data: unknown) => vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
  json: async () => ({ success: true, data, errorCode: null, errorMessage: null }),
}));
afterEach(() => vi.unstubAllGlobals());
describe('IM13 release scope and keyboard destination boundary', () => {
  it('T10 forwards the explicit Article owning Community to the approved detail gate', async () => {
    ok(article); open('/originals/OR-003?communityId=bear-de');
    await screen.findByRole('heading', { name: article.title });
    expect(fetch).toHaveBeenCalledWith('/api/originals/OR-003?communityId=bear-de');
  });
  it('T14 places keyboard focus at the loaded Article heading', async () => {
    ok(article); open('/originals/OR-003?communityId=wilmington-de');
    const heading = await screen.findByRole('heading', { name: article.title });
    await waitFor(() => expect(heading).toHaveFocus());
  });
  it('T14 places keyboard focus at the loaded explicit provider heading', async () => {
    ok({ id: 'ORG-001', name: 'Provider', slug: 'provider', website: null, resources: [] });
    open('/organization/provider?communityId=bear-de');
    const heading = await screen.findByRole('heading', { name: 'Provider', level: 1 });
    await waitFor(() => expect(heading).toHaveFocus());
  });
  it('T14 gives a missing Topic a main level-one heading in its announced error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => ({ success: false,
      data: null, errorCode: 'NOT_FOUND', errorMessage: 'Topic not found' }) }));
    open('/category/housing/not-a-topic?communityId=bear-de');
    const alert = await screen.findByRole('alert');
    expect(alert.querySelector('h1')).toHaveTextContent("We couldn't find that topic.");
  });
});
