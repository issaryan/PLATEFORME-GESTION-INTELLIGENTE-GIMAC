import { afterEach, beforeAll, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from '../i18n/i18n'
import { Enveloppe } from './Enveloppe'

// D-62 : l'espace personnel (Mon compte, Se déconnecter) est présent dans les trois couches.
beforeAll(() => {
  initI18n([], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(cleanup)

function Ici() {
  return <span data-testid="ici">{useLocation().pathname}</span>
}
const monter = (espacePersonnel?: string) =>
  render(
    <MemoryRouter initialEntries={['/console/tableau-de-bord']}>
      <ThemeProvider theme="light">
        <Enveloppe couche="console" navigation={[{ entrees: [{ cle: 'tb', libelleKey: 'x.tb', icone: 'gauge', chemin: '/console/tableau-de-bord' }] }]} espacePersonnel={espacePersonnel}>
          <Routes><Route path="*" element={<Ici />} /></Routes>
        </Enveloppe>
      </ThemeProvider>
    </MemoryRouter>,
  )

describe('Enveloppe : espace personnel', () => {
  it('groupe « Mon espace » : Mon compte et Se déconnecter, navigation sans rechargement', () => {
    monter('/console')
    expect(screen.getByText('Mon espace')).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Mon compte' }))
    expect(screen.getByTestId('ici').textContent).toBe('/console/mon-compte')
    expect(screen.getByRole('link', { name: 'Se déconnecter' }).getAttribute('href')).toBe('/console/deconnexion')
    expect(document.querySelector('.gm-footer')).toBeTruthy()
  })
  it('sans espace personnel : groupe absent', () => {
    monter()
    expect(screen.queryByText('Mon espace')).toBeNull()
  })
})
