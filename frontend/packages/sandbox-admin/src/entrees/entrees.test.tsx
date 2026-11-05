import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { render, renderHook, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import i18next from 'i18next'
import { SessionProvider, initI18n } from '@gimac/socle'
import { catalogues as cataloguesLot1 } from '@gimac/lot1'
import { CHEMINS_SANDBOX, ENTREES_SANDBOX, RoutesSandbox } from '.'
import type { EntreeSandbox } from './types'
import { catalogues } from '../i18n'
import { fr } from '../i18n/fr'
import { en } from '../i18n/en'
import { es } from '../i18n/es'
import { ServicesSandboxProvider, useServicesSandbox } from '../services/contexte'
import { ADMIN, session, simules } from '../test/rendu'

// Registre des entrées de l'espace sandbox (D-121, D-122) : une entrée de plus ne touche à aucune autre.
function monter(chemin: string, o: { permissions?: string[]; garde?: (cle: string, page: ReactNode) => ReactNode; entrees?: readonly EntreeSandbox[] } = {}) {
  if (!i18next.isInitialized) initI18n([...cataloguesLot1, ...catalogues], 'fr')
  else void i18next.changeLanguage('fr')
  const permissions = o.permissions ?? ADMIN
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ServicesSandboxProvider services={simules({ permissions })}>
          <SessionProvider session={session(permissions)}>
            <Routes>
              <Route path="/des/sandbox/*" element={<RoutesSandbox garde={o.garde} entrees={o.entrees} />} />
            </Routes>
          </SessionProvider>
        </ServicesSandboxProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

describe('espace sandbox du DES', () => {
  it('registre : espaces et supervision gardés par leurs permissions, chemins sous /des/sandbox', () => {
    expect(ENTREES_SANDBOX.map((e) => [e.cle, e.permission])).toEqual([['sandboxEspaces', 'SBX_HAB_CONSULTATION'], ['sandboxSupervision', 'SBX_SUP_CONSULTATION'], ['sandboxActivite', 'SBX_SUP_CONSULTATION']])
    expect(CHEMINS_SANDBOX).toEqual({ sandboxEspaces: '/des/sandbox/espaces', sandboxSupervision: '/des/sandbox/supervision', sandboxActivite: '/des/sandbox/activite' })
  })

  it('routes : liste, fiche, et toute autre adresse ramène à la première entrée', async () => {
    const r = monter('/des/sandbox/espaces')
    expect(await screen.findByRole('heading', { level: 1, name: 'Espaces des institutions' })).toBeTruthy()
    r.unmount()
    const f = monter('/des/sandbox/espaces/99001')
    expect(await screen.findByRole('heading', { level: 1, name: 'Espace de l’institution 99001' })).toBeTruthy()
    f.unmount()
    monter('/des/sandbox/inconnue')
    expect(await screen.findByRole('heading', { level: 1, name: 'Espaces des institutions' })).toBeTruthy()
  })

  it('garde : fonctionnalité du catalogue (application) puis permission de l’entrée', async () => {
    const r = monter('/des/sandbox/espaces', { garde: (cle) => <p>masquée {cle}</p> })
    expect(await screen.findByText('masquée sandboxEspaces')).toBeTruthy()
    r.unmount()
    monter('/des/sandbox/espaces', { permissions: ['PIL_ACTIVITY_OWN'] })
    expect(await screen.findByText(i18next.t('catalogue.refuse.titre'))).toBeTruthy()
  })

  it('la supervision s’ajoute sans toucher aux autres', async () => {
    monter('/des/sandbox/supervision')
    expect(await screen.findByRole('heading', { level: 1, name: 'Supervision de la sandbox' })).toBeTruthy()
  })

  it('l’activité des tests s’ajoute de même', async () => {
    monter('/des/sandbox/activite')
    expect(await screen.findByRole('heading', { level: 1, name: 'Activité des tests' })).toBeTruthy()
  })

  it('services : employés hors de leur fournisseur, une erreur explicite', () => {
    expect(() => renderHook(() => useServicesSandbox())).toThrow('useServicesSandbox doit être employé sous ServicesSandboxProvider')
  })

  it('traductions : mêmes clés en français, anglais et espagnol, aucune vide', () => {
    const cles = Object.keys(fr).sort()
    expect(Object.keys(en).sort()).toEqual(cles)
    expect(Object.keys(es).sort()).toEqual(cles)
    for (const l of [fr, en, es]) expect(Object.values(l).every((v) => v.trim().length > 0)).toBe(true)
  })
})
