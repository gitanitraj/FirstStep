/* IM-05 annotated mirror.
 * What: controlled help-only Category/Topic browsing and its resident contract.
 * Why: consume IM-01/03/04 eligibility without duplicating classification or identity.
 * Interaction: shared helpFor, lifecycle-filtered services, HTTP DTOs and React navigation.
 * Alternatives: rejected all-content Topic counts, capped inventory and inferred Flyer activation.
 */
import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import TopicPage from './TopicPage';
import CategoryPage from '../CategoryPage/CategoryPage';
import type { TopicPage as Payload } from '../../types/api';
const payload: Payload = {
  metadata: { categoryKey: 'housing', categoryLabel: 'Housing', categoryIcon: '🏠', name: 'Housing', slug: 'help', totalCount: 55, countsByType: { RESOURCE: 55 } },
  topics: [{ name: 'Emergency Shelter', slug: 'emergency-shelter', count: 55, countsByType: { RESOURCE: 55 } }],
  items: Array.from({ length: 55 }, (_, n) => ({ contentType: 'RESOURCE', id: `R${n}`, title: `Programa José ${n}`, summary: 'Información útil', organization: 'Provider', location: null, cost: null, urgency: null, date: null, url: `https://example.org/${n}`, imageUrl: null })),
};
function History() { const navigate = useNavigate(); return <button onClick={() => navigate(-1)}>History back</button>; }
function renderAt(path: string) {
  return render(<MemoryRouter initialEntries={[path]}><History /><Routes>
    <Route path="/category/:key" element={<CategoryPage />} />
    <Route path="/category/:key/:topic" element={<TopicPage />} />
  </Routes></MemoryRouter>);
}
function stub(data: unknown = payload) { vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => ({ success: true, data }) })); }
afterEach(() => vi.unstubAllGlobals());
describe('IM-05 help browse journey', () => {
  it('direct all-help preserves Community, focuses context and reaches every result', async () => {
    stub(); renderAt('/category/housing/help?communityId=second-community');
    expect(await screen.findByRole('heading', { name: 'All Housing Help', level: 1 })).toHaveFocus();
    expect(fetch).toHaveBeenCalledWith('/api/category/housing/help?communityId=second-community');
    expect(screen.getAllByRole('link', { name: /^Programa José/ })).toHaveLength(4);
    expect(screen.queryByText('Programa José 54')).not.toBeInTheDocument();
    const more = screen.getByRole('button', { name: /51 more/ });
    expect(more).toHaveAttribute('aria-expanded', 'false'); fireEvent.click(more);
    expect(screen.getAllByRole('link', { name: /^Programa José/ })).toHaveLength(55);
    expect(screen.getByRole('link', { name: 'Programa José 54' })).toHaveAttribute('href', 'https://example.org/54');
    expect(screen.getByText('55 help listings')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Narrow by topic'));
    expect(screen.getByRole('link', { name: 'Emergency Shelter (55)' })).toHaveAttribute('href', '/category/housing/emergency-shelter?communityId=second-community');
  });
  it('all help to Topic to all help supports history without losing Community', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((path: string) => Promise.resolve({ json: async () => ({ success: true,
      data: path.includes('emergency-shelter') ? { ...payload, metadata: { ...payload.metadata, name: 'Emergency Shelter', slug: 'emergency-shelter' } } : payload }) })));
    renderAt('/category/housing/help?communityId=second-community');
    fireEvent.click(await screen.findByText('Narrow by topic'));
    fireEvent.click(screen.getByRole('link', { name: 'Emergency Shelter (55)' }));
    expect(await screen.findByRole('heading', { name: 'Emergency Shelter', level: 1 })).toHaveFocus();
    fireEvent.click(within(screen.getByRole('navigation', { name: 'Breadcrumb' })).getByRole('link', { name: 'All Housing Help' }));
    expect(await screen.findByRole('heading', { name: 'All Housing Help' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'History back' }));
    expect(await screen.findByRole('heading', { name: 'Emergency Shelter', level: 1 })).toBeInTheDocument();
    expect(fetch).toHaveBeenLastCalledWith('/api/category/housing/emergency-shelter?communityId=second-community');
  });
  it('Category exposes scoped All Help and Topic links with help counts', async () => {
    stub({ metadata: { key: 'food', label: 'Food', countsByType: {} }, updates: [], groups: [], topics: [{ name: 'Food Pantry', slug: 'food-pantry', count: 2, countsByType: {} }], collections: [], organizations: [] });
    renderAt('/category/food?communityId=second-community');
    expect(await screen.findByRole('link', { name: 'All Food Help' })).toHaveAttribute('href', '/category/food/help?communityId=second-community');
    expect(screen.getByRole('link', { name: /Food Pantry 2 help listings/ })).toHaveAttribute('href', '/category/food/food-pantry?communityId=second-community');
  });
  it('announces loading distinctly from empty and failure', () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {}))); renderAt('/category/food/help');
    expect(screen.getByRole('status')).toHaveTextContent('Loading'); expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
  it('valid empty Topic has help-specific status and return paths', async () => {
    stub({ ...payload, metadata: { ...payload.metadata, name: 'Eviction Prevention', totalCount: 0 }, items: [] });
    renderAt('/category/housing/eviction-prevention');
    expect(await screen.findByText(/No current help/)).toHaveAttribute('role', 'status');
    expect(screen.getByRole('link', { name: 'All Housing Help' })).toBeInTheDocument(); expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
  it('network failure is distinct from invalid route and hides raw server text', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('private network details'))); renderAt('/category/food/help');
    expect(await screen.findByRole('alert')).toHaveTextContent('could not load help'); expect(screen.queryByText(/private network/)).not.toBeInTheDocument();
  });
  it('Flyer without source navigates to original media, never a fabricated detail', async () => {
    stub({ ...payload, metadata: { ...payload.metadata, totalCount: 1 }, items: [{ ...payload.items[0], contentType: 'FLYER', title: 'Acceso público', url: null, imageUrl: '/images/seasonal/Help%20image.png' }] });
    renderAt('/category/housing/help');
    expect(await screen.findByRole('link', { name: 'Acceso público' })).toHaveAttribute('href', '/images/seasonal/Help%20image.png');
  });
});
