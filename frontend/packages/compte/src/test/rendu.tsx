import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import i18next from 'i18next'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n, type Langue } from '@gimac/socle'
import { catalogue } from '../i18n'
import { CompteProvider } from '../contexte'
import { monCompteSimule } from '../services/simule'
import type { ServiceMonCompte } from '../services/ports'

// Monte un écran de compte comme dans une couche : routeur, requêtes sans réessai, thème, langue,
// service simulé sans latence par défaut. Aucun appel réseau.
export function rendre(ui: ReactElement, o: { service?: ServiceMonCompte; langue?: Langue } = {}) {
  if (!i18next.isInitialized) initI18n([catalogue], o.langue ?? 'fr')
  else void i18next.changeLanguage(o.langue ?? 'fr')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } } })
  const service = o.service ?? monCompteSimule({ latenceMs: 0 })
  return {
    service,
    ...render(
      <MemoryRouter>
        <QueryClientProvider client={client}>
          <ThemeProvider theme="light">
            <CompteProvider service={service}>{ui}</CompteProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </MemoryRouter>,
    ),
  }
}

/** Service simulé dont certaines opérations sont remplacées. */
export const avec = (surcharge: Partial<ServiceMonCompte>, base: ServiceMonCompte = monCompteSimule({ latenceMs: 0 })): ServiceMonCompte => ({ ...base, ...surcharge })
