import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import i18next from 'i18next'
import { initI18n, type Langue } from '@gimac/socle'
import { catalogues } from '../i18n'
import { Fournisseurs, creerClientRequetes } from '../Fournisseurs'
import { servicesSimules } from '../services/simule'
import type { Services } from '../services/ports'

// Monte une page du lot 1 comme dans l'application : routeur, services (simulés sans latence par défaut),
// requêtes sans réessai, thème et langue choisis. Aucun appel réseau.
export function rendrePage(page: ReactElement, o: { chemin: string; motif: string; langue?: Langue; theme?: 'light' | 'dark'; services?: Services }) {
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
            <Route path={o.motif} element={page} />
          </Routes>
        )}
      </Fournisseurs>
    </MemoryRouter>,
  )
}
