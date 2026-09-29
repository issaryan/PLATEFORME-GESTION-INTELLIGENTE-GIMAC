import type { ReactNode } from 'react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { ThemeProvider } from '@gimac/design-system'
import { CLE_THEME, LangueComposants, lirePreference } from '@gimac/socle'
import { CompteProvider, type ServiceMonCompte } from '@gimac/compte'
import { ServicesConsoleProvider } from './services/ServicesConsole'
import type { ServicesConsole } from './services/ports'

// Fournisseurs de la console : requêtes (sans persistance), thème, langue des composants, services
// injectés de la console et de « Mon compte ».
export function Fournisseurs({ services, compte, clientRequetes, children }: {
  services: ServicesConsole
  compte: ServiceMonCompte
  clientRequetes: QueryClient
  children: ReactNode
}) {
  const theme = lirePreference(CLE_THEME)
  return (
    <QueryClientProvider client={clientRequetes}>
      <ThemeProvider theme={theme === 'dark' ? 'dark' : theme === 'light' ? 'light' : undefined}>
        <LangueComposants>
          <ServicesConsoleProvider services={services}>
            <CompteProvider service={compte}>{children}</CompteProvider>
          </ServicesConsoleProvider>
        </LangueComposants>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
