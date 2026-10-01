import { useEffect, useState, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query'
import { LangueProvider, ThemeProvider, type LangueDS } from '@gimac/design-system'
import { useTranslation } from 'react-i18next'
import { CLE_THEME, lirePreference, openLiveChannel, type Couche, type EtatCanal } from '@gimac/socle'
import { ServicesProvider } from './services/ServicesProvider'
import type { Services } from './services/ports'
import { ExecuteurActions } from './ressources/actions'

// Fournisseurs communs d'une couche : requêtes (sans persistance, DS-GIMAC-01 §8.6), thème, services
// injectés, exécuteur d'actions et canal poussé (actif avec les services réels uniquement).
export function creerClientRequetes() {
  return new QueryClient({ defaultOptions: { queries: { retry: 1, staleTime: 15_000, refetchOnWindowFocus: false } } })
}

function CanalPousse({ couche, actif, onEtat }: { couche: Couche; actif: boolean; onEtat: (e: EtatCanal) => void }) {
  const qc = useQueryClient()
  useEffect(() => {
    if (!actif) return
    // La messagerie (D-64) est réservée aux couches internes : le DES et la console.
    return openLiveChannel(couche, couche === 'portail' ? ['homologation', 'certification'] : ['homologation', 'certification', 'collaboration'], qc, onEtat, { baseMs: 1000, maxMs: 30_000 })
  }, [couche, actif, qc, onEtat])
  return null
}

/** Aligne les libellés internes des composants du catalogue sur la langue choisie (FR, EN, ES). */
function LangueCatalogue({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation()
  const l = i18n.language.slice(0, 2)
  const langue: LangueDS = l === 'en' || l === 'es' ? l : 'fr'
  return <LangueProvider langue={langue}>{children}</LangueProvider>
}

export function Fournisseurs({ couche, services, canal, clientRequetes, children }: {
  couche: Couche
  services: Services
  /** Canal poussé : désactivé en mode simulé (aucune passerelle). */
  canal: boolean
  clientRequetes: QueryClient
  children: (etatCanal: EtatCanal | undefined) => ReactNode
}) {
  const [etat, setEtat] = useState<EtatCanal | undefined>()
  const themeInitial = lirePreference(CLE_THEME) === 'dark' ? 'dark' : lirePreference(CLE_THEME) === 'light' ? 'light' : undefined
  return (
    <QueryClientProvider client={clientRequetes}>
      <ThemeProvider theme={themeInitial}>
        <LangueCatalogue>
        <ServicesProvider services={services}>
          <CanalPousse couche={couche} actif={canal} onEtat={setEtat} />
          <ExecuteurActions>{children(etat)}</ExecuteurActions>
        </ServicesProvider>
        </LangueCatalogue>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
