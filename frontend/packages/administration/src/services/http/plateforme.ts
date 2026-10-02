import { http } from '@gimac/socle'
import type { EtatFlux, ImpulsionPlateforme } from '../../modele/plateforme'
import type { EcouteurFlux, ServicePlateforme } from '../ports'

// Implémentation HTTP de la supervision : routes /api/plateforme de MS-GATEWAY (PlateformeResource),
// session à deux facteurs et permission exigées par la passerelle.
export const PLATEFORME = '/api/plateforme'

export type OptionsFlux = { adresse?: string; baseMs?: number; maxMs?: number }

/**
 * Flux temps réel (SSE, événement « plateforme ») : état de connexion signalé, reconnexion à délai
 * exponentiel plafonné après une coupure, reprise immédiate du compteur dès que le flux est rouvert.
 */
export function ouvrirFlux(e: EcouteurFlux, o: OptionsFlux = {}): () => void {
  const adresse = o.adresse ?? `${PLATEFORME}/flux`
  const base = o.baseMs ?? 1000
  const max = o.maxMs ?? 30_000
  let source: EventSource | null = null
  let tentative = 0
  let ferme = false
  let minuterie: number | undefined
  const etat = (x: EtatFlux) => {
    if (!ferme) e.onEtat(x)
  }
  const connecter = () => {
    etat('connexion')
    source = new EventSource(adresse, { withCredentials: true })
    source.onopen = () => {
      tentative = 0
      etat('connecte')
    }
    source.addEventListener('plateforme', (m) => {
      try {
        e.onImpulsion(JSON.parse((m as MessageEvent<string>).data) as ImpulsionPlateforme)
      } catch {
        /* événement illisible : ignoré, le suivant rétablit l'état */
      }
    })
    source.onerror = () => {
      source?.close()
      if (ferme) return
      etat('perdu')
      minuterie = window.setTimeout(connecter, Math.min(max, 2 ** tentative++ * base))
    }
  }
  connecter()
  return () => {
    ferme = true
    window.clearTimeout(minuterie)
    source?.close()
  }
}

export const plateformeHttp: ServicePlateforme = {
  services: () => http.get(`${PLATEFORME}/services`),
  serveurs: () => http.get(`${PLATEFORME}/serveurs`),
  alertes: () => http.get(`${PLATEFORME}/alertes`),
  configuration: () => http.get(`${PLATEFORME}/configuration`),
  journalisation: (service, filtre) => http.get(`${PLATEFORME}/journalisation/${encodeURIComponent(service)}`, { query: { filtre: filtre || undefined } }),
  modifierJournalisation: (service, d) => http.post(`${PLATEFORME}/journalisation/${encodeURIComponent(service)}`, { body: d }),
  flux: (e) => ouvrirFlux(e),
}
