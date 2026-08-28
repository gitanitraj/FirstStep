import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ArticlePage from './ArticlePage';
import type { ArticleDetail } from '../../types/api';

const article = (over: Partial<ArticleDetail> = {}): ArticleDetail => ({
  id: 'OR-003',
  title: 'How to read a Delaware eviction notice',
  summary: 'What the dates on an eviction notice mean.',
  whyItMatters: 'Tenants routinely miss the response window.',
  body: 'First paragraph.\n\nSecond paragraph.',
  byline: 'Admin',
  disclosure: null,
  publishDate: '2026-08-10',
  updatedDate: '2026-08-10',
  categoryTags: ['Housing', 'Legal'],
  subcategory: 'Eviction Prevention',
  ...over,
});

function mockArticle(data: ArticleDetail) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }),
    }),
  );
}

function mockNotFound() {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      json: () =>
        Promise.resolve({
          success: false,
          data: null,
          errorCode: 'NOT_FOUND',
          errorMessage: 'Article not found: OR-001',
        }),
    }),
  );
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/originals/:id" element={<ArticlePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ArticlePage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should request the article named in the URL', async () => {
    mockArticle(article());

    renderAt('/originals/OR-003');

    await screen.findByRole('heading', { level: 1 });
    expect(fetch).toHaveBeenCalledWith('/api/originals/OR-003');
  });

  it('should render the full body, one paragraph per blank-line break', async () => {
    // The reason this page exists: First Step hosts the text, so the whole
    // article must be readable here.
    mockArticle(article({ body: 'First paragraph.\n\nSecond paragraph.\n\nThird.' }));

    renderAt('/originals/OR-003');

    expect(await screen.findByText('First paragraph.')).toBeInTheDocument();
    expect(screen.getByText('Second paragraph.')).toBeInTheDocument();
    expect(screen.getByText('Third.')).toBeInTheDocument();
  });

  it('should show the byline as the public credit', async () => {
    mockArticle(article({ byline: 'Admin' }));

    renderAt('/originals/OR-003');

    expect(await screen.findByText(/Published by/)).toHaveTextContent('Published by Admin');
  });

  it('should render the AI disclosure from its controlled key', async () => {
    // The client renders the key it is given. It infers nothing about how the
    // article was written, and there is no generatedBy in the contract to infer
    // it from.
    mockArticle(article({ disclosure: 'ai-assisted' }));

    renderAt('/originals/OR-003');

    expect(
      await screen.findByText(/written with AI assistance and reviewed by First Step/),
    ).toBeInTheDocument();
  });

  it('should NOT show a disclosure when the article carries none', async () => {
    mockArticle(article({ disclosure: null }));

    renderAt('/originals/OR-003');

    await screen.findByRole('heading', { level: 1 });
    expect(screen.queryByText(/AI assistance/)).not.toBeInTheDocument();
  });

  it('should keep the byline and the disclosure as separate statements', async () => {
    // Credit and disclosure are different claims. Folding one into the other
    // would make them impossible to edit independently.
    mockArticle(article({ byline: 'Admin', disclosure: 'ai-assisted' }));

    renderAt('/originals/OR-003');

    const byline = await screen.findByText(/Published by/);
    expect(byline).toHaveTextContent('Published by Admin');
    expect(byline).not.toHaveTextContent(/AI assistance/);
  });

  it('should render Why it matters as its own labelled region', async () => {
    mockArticle(article({ whyItMatters: 'Tenants routinely miss the response window.' }));

    renderAt('/originals/OR-003');

    const why = await screen.findByRole('complementary', { name: 'Why it matters' });
    expect(within(why).getByText('Tenants routinely miss the response window.')).toBeInTheDocument();
  });

  it('should mark up dates as machine-readable time elements', async () => {
    mockArticle(article({ publishDate: '2026-08-10', updatedDate: '2026-08-12' }));

    const { container } = renderAt('/originals/OR-003');

    await screen.findByRole('heading', { level: 1 });
    const times = container.querySelectorAll('time');
    expect(times).toHaveLength(2);
    expect(times[0]).toHaveAttribute('datetime', '2026-08-10');
  });

  it('should not show an update line when the article has never been revised', async () => {
    mockArticle(article({ publishDate: '2026-08-10', updatedDate: '2026-08-10' }));

    renderAt('/originals/OR-003');

    await screen.findByRole('heading', { level: 1 });
    expect(screen.queryByText(/Updated/)).not.toBeInTheDocument();
  });

  it('should render an article that carries no publish date', async () => {
    // publishDate is descriptive, never a gate. An approved article without one
    // is served and simply shows no date.
    mockArticle(article({ publishDate: null, updatedDate: null }));

    const { container } = renderAt('/originals/OR-003');

    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(container.querySelectorAll('time')).toHaveLength(0);
  });

  it('should show an ordinary not-found state for a 404', async () => {
    // The page must not try to work out whether the article is unapproved or
    // nonexistent - the backend deliberately makes those indistinguishable.
    mockNotFound();

    renderAt('/originals/OR-001');

    expect(await screen.findByRole('alert')).toHaveTextContent('Article not found');
  });

  it('should not echo the server error message into the not-found state', async () => {
    // "Article not found: OR-001" is the server's wording, English-only and
    // carrying the id back at the reader. The page shows its own translated copy.
    mockNotFound();

    renderAt('/originals/OR-001');

    const alert = await screen.findByRole('alert');
    expect(alert).not.toHaveTextContent('OR-001');
  });

  it('should offer a way back from the not-found state', async () => {
    mockNotFound();

    renderAt('/originals/OR-001');

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByRole('link')).toHaveAttribute('href', '/');
  });

  it('should announce the loading state', async () => {
    mockArticle(article());

    renderAt('/originals/OR-003');

    expect(screen.getByRole('status')).toHaveTextContent('Loading');
    await screen.findByRole('heading', { level: 1 });
  });
});
