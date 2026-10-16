import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { I18nProvider } from '../../i18n/I18nProvider';
import SiteHeader from './SiteHeader';

function renderAt(path: string) {
  return render(
    <I18nProvider>
      <MemoryRouter initialEntries={[path]}>
        <SiteHeader />
      </MemoryRouter>
    </I18nProvider>,
  );
}

describe('SiteHeader', () => {
  afterEach(() => {
    localStorage.clear();
    document.body.className = '';
  });

  it('should render the brand and the accessibility controls', () => {
    renderAt('/');

    expect(screen.getByRole('link', { name: /First Step home/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Switch language' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Toggle high contrast' })).toBeInTheDocument();
  });

  it('should not offer an AI entry point', () => {
    // The Ollama agent behind it is no longer wired in. An entry point that
    // cannot answer is worse than none — Version 3 decides whether it returns.
    renderAt('/');

    expect(screen.queryByText(/Get answers with AI/)).not.toBeInTheDocument();
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
  });

  it('should not label the accessibility controls "ARIA"', () => {
    // ARIA is Accessible Rich Internet Applications, not a name for these two
    // buttons. An earlier draft rendered it as a visible chip and wrapped them
    // in a role="group" — decorative ARIA, which is worse than none.
    renderAt('/');

    expect(screen.queryByText('ARIA')).not.toBeInTheDocument();
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it('should render exactly the four global nav items', () => {
    renderAt('/');

    const nav = screen.getByRole('navigation', { name: 'Primary' });

    expect(within(nav).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'About',
      'Housing',
      'Community',
      'Updates',
    ]);
  });
});
