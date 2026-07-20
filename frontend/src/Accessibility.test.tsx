import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { I18nProvider } from './i18n/I18nProvider';
import HomePage from './pages/HomePage';

function renderHome() {
  return render(
    <I18nProvider>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </I18nProvider>,
  );
}

describe('Accessibility controls', () => {
  afterEach(() => {
    localStorage.clear();
    document.body.className = '';
  });

  it('toggles the UI language between English and Spanish', () => {
    renderHome();

    // Starts English.
    expect(screen.getByText(/Your trusted guide to community resources/)).toBeInTheDocument();

    // Switch to Spanish via the language button.
    fireEvent.click(screen.getByRole('button', { name: 'Switch language' }));

    expect(screen.getByText(/Tu guía confiable de recursos comunitarios/)).toBeInTheDocument();
    expect(screen.queryByText(/Your trusted guide to community resources/)).not.toBeInTheDocument();
    // Nav label also translated.
    expect(screen.getByRole('link', { name: /Asistencia de Vivienda/ })).toBeInTheDocument();
  });

  it('toggles the high-contrast body class', () => {
    renderHome();
    const button = screen.getByRole('button', { name: 'Toggle high contrast' });

    expect(document.body.classList.contains('high-contrast')).toBe(false);
    fireEvent.click(button);
    expect(document.body.classList.contains('high-contrast')).toBe(true);
    expect(button).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(button);
    expect(document.body.classList.contains('high-contrast')).toBe(false);
  });
});
