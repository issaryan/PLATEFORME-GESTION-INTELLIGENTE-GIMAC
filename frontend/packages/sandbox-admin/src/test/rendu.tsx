import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import i18next from 'i18next'
import { SessionProvider, initI18n, type Langue, type Session } from '@gimac/socle'
import { catalogues as cataloguesLot1 } from '@gimac/lot1'
import { catalogues } from '../i18n'
import { ServicesSandboxProvider } from '../services/contexte'
import { servicesSandboxSimules, type OptionsEspacesSimules } from '../services/simule'
import type { ServicesSandbox } from '../services/ports'
import type { OptionsSupervisionSimulee } from '../services/supervisionSimulee'
import type { OptionsActiviteSimulee } from '../activite/simule'
import { PERMISSION, PERMISSION_SUPERVISION } from '../modele'

// Monte un écran comme dans l'application du département : routeur (/des/sandbox/*), services simulés sans latence,
// requêtes sans réessai, session portant les permissions demandées. Aucun appel réseau.

export const ADMIN = [PERMISSION.consultation, PERMISSION.administration, PERMISSION_SUPERVISION, 'PIL_ACTIVITY_OWN']
export const LECTEUR = [PERMISSION.consultation, 'PIL_ACTIVITY_OWN']

export const session = (permissions: string[]): Session => ({
  sujet: 'u-test', nom: 'Agent de test', identifiant: 'test', roles: [], permissions, institutionId: '', acr: '2', doubleFacteur: true,
})

export const simules = (o: Partial<OptionsEspacesSimules & OptionsSupervisionSimulee & OptionsActiviteSimulee> = {}) => servicesSandboxSimules({ permissions: ADMIN, maintenant: () => new Date('2026-10-22T09:00:00Z'), ...o })

export function rendre(page: ReactElement, o: { permissions?: string[]; services?: ServicesSandbox; langue?: Langue; chemin?: string; route?: string } = {}) {
  if (!i18next.isInitialized) initI18n([...cataloguesLot1, ...catalogues], o.langue ?? 'fr')
  else void i18next.changeLanguage(o.langue ?? 'fr')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } } })
  const permissions = o.permissions ?? ADMIN
  const services = o.services ?? simules({ permissions })
  const r = render(
    <MemoryRouter initialEntries={[o.chemin ?? '/des/sandbox/espaces']}>
      <QueryClientProvider client={client}>
        <ServicesSandboxProvider services={services}>
          <SessionProvider session={session(permissions)}>
            <Routes>
              <Route path={o.route ?? '/des/sandbox/espaces'} element={page} />
              <Route path="/des/sandbox/espaces/:code" element={<p>fiche ouverte</p>} />
              <Route path="/des/parcours/:id" element={<p>dossier ouvert</p>} />
            </Routes>
          </SessionProvider>
        </ServicesSandboxProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  )
  return { ...r, services }
}
