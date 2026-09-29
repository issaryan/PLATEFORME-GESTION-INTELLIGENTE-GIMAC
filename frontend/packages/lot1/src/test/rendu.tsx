import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import i18next from 'i18next'
import { SessionProvider, initI18n, type Langue, type Session } from '@gimac/socle'
import { catalogues } from '../i18n'
import { Fournisseurs, creerClientRequetes } from '../Fournisseurs'
import { servicesSimules } from '../services/simule'
import type { Services } from '../services/ports'

// Monte une page du lot 1 comme dans l'application : routeur, services (simulés sans latence par défaut),
// requêtes sans réessai, thème et langue choisis. Aucun appel réseau.
/** Session de test : matrice des rôles donnée ; sans permissions, aucune session (tout est affiché). */
export const sessionTest = (permissions: string[], roles: string[] = []): Session => ({
  sujet: 'u-test', nom: 'Agent de test', identifiant: 'test', roles, permissions, institutionId: '', acr: '2', doubleFacteur: true,
})

/** Routes supplémentaires (destination d'une navigation) : leur élément est rendu sous la même session. */
type Autre = { motif: string; element: ReactElement }

export function rendrePage(page: ReactElement, o: { chemin: string; motif: string; langue?: Langue; theme?: 'light' | 'dark'; services?: Services; session?: Session; autres?: Autre[] }) {
  if (!i18next.isInitialized) initI18n(catalogues, o.langue ?? 'fr')
  else void i18next.changeLanguage(o.langue ?? 'fr')
  document.documentElement.setAttribute('data-theme', o.theme ?? 'light')
  const client = creerClientRequetes()
  client.setDefaultOptions({ queries: { retry: false, staleTime: 0 } })
  return render(
    <MemoryRouter initialEntries={[o.chemin]}>
      <Fournisseurs couche="des" services={o.services ?? servicesSimules({ latenceMs: 0 })} canal={false} clientRequetes={client}>
        {() => (
          <Routes>
            {[{ motif: o.motif, element: page }, ...(o.autres ?? [])].map((r) => (
              <Route key={r.motif} path={r.motif} element={o.session ? <SessionProvider session={o.session}>{r.element}</SessionProvider> : r.element} />
            ))}
          </Routes>
        )}
      </Fournisseurs>
    </MemoryRouter>,
  )
}
