import type { ReactNode } from 'react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { ThemeProvider } from '@gimac/design-system'
import { CLE_THEME, LangueComposants, lirePreference } from '@gimac/socle'
import { CompteProvider, type ServiceMonCompte } from '@gimac/compte'
import { ServicesProvider, type Services } from '@gimac/lot1'
import { ServicesConsoleProvider } from './services/ServicesConsole'
import type { ServicesConsole } from './services/ports'

// Fournisseurs de la console : requêtes (sans persistance), thème, langue des composants, services
// injectés de la console, de « Mon compte » et de la messagerie instantanée (services du lot 1, D-64).
export function Fournisseurs({ services, compte, messagerie, clientRequetes, children }: {
  services: ServicesConsole
  compte: ServiceMonCompte
  messagerie: Services
  clientRequetes: QueryClient
  children: ReactNode
}) {
  const theme = lirePreference(CLE_THEME)
  return (
    <QueryClientProvider client={clientRequetes}>
      <ThemeProvider theme={theme === 'dark' ? 'dark' : theme === 'light' ? 'light' : undefined}>
        <LangueComposants>
          <ServicesConsoleProvider services={services}>
            <ServicesProvider services={messagerie}>
              <CompteProvider service={compte}>{children}</CompteProvider>
            </ServicesProvider>
          </ServicesConsoleProvider>
        </LangueComposants>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
