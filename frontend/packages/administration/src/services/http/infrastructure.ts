import { http } from '@gimac/socle'
import type { Charge, Geste, Orchestrateur } from '../../modele/infrastructure'
import type { ServiceInfrastructure } from '../ports'
import { PLATEFORME, ouvrirFlux } from './plateforme'

// Implémentation HTTP de l'exploitation de l'infrastructure (D-78) : InfrastructureResource de MS-GATEWAY,
// contrôleur propre de la passerelle (aucun relais), servi depuis toute couche ; session à deux facteurs,
// PLATFORM_OPS_READ ou PLATFORM_OPS_EXECUTE, et assertion FIDO2 pour chaque geste.
export const INFRASTRUCTURE = `${PLATEFORME}/infrastructure`

const ROUTE_GESTE: Record<Geste, { methode: 'post' | 'put'; chemin: string }> = {
  REDEMARRER: { methode: 'post', chemin: 'redemarrage' },
  ARRETER: { methode: 'post', chemin: 'arret' },
  DEMARRER: { methode: 'post', chemin: 'demarrage' },
  ECHELLE: { methode: 'put', chemin: 'repliques' },
  RETABLIR: { methode: 'post', chemin: 'retablissement' },
}
const ORCHESTRATEURS: Orchestrateur[] = ['DOCKER', 'KUBERNETES', 'AUCUN']

export const infrastructureHttp: ServiceInfrastructure = {
  charges: async () => {
    const { donnees, entetes } = await http.getAvecEntetes<Charge[]>(`${INFRASTRUCTURE}/charges`)
    // En-tête absent ou inconnu : déduit des charges, « non configurée » si aucune.
    const entete = (entetes.get('X-Orchestrateur') ?? '').toUpperCase() as Orchestrateur
    const orchestrateur = ORCHESTRATEURS.includes(entete) ? entete : (donnees[0]?.orchestrateur ?? 'AUCUN')
    return { orchestrateur, charges: orchestrateur === 'AUCUN' ? [] : donnees }
  },
  journaux: (nom, lignes, depuis) => http.get(`${INFRASTRUCTURE}/charges/${encodeURIComponent(nom)}/journaux`, { query: { lignes, depuis } }),
  operations: (page, taille) => http.get(`${INFRASTRUCTURE}/operations`, { query: { page, taille } }),
  pods: (nom) => http.get(`${INFRASTRUCTURE}/charges/${encodeURIComponent(nom)}/pods`),
  evenements: (nom) => http.get(`${INFRASTRUCTURE}/charges/${encodeURIComponent(nom)}/evenements`),
  noeuds: () => http.get(`${INFRASTRUCTURE}/noeuds`),
  taches: () => http.get(`${INFRASTRUCTURE}/taches`),
  executer: (nom, geste, d, assertion) => {
    const r = ROUTE_GESTE[geste]
    const body = geste === 'ECHELLE' ? { motif: d.motif, repliques: d.repliques } : { motif: d.motif }
    return http[r.methode](`${INFRASTRUCTURE}/charges/${encodeURIComponent(nom)}/${r.chemin}`, { body, reauthAssertion: assertion })
  },
  flux: (e) => ouvrirFlux({ onImpulsion: () => undefined, onEtat: e.onEtat, onOperation: e.onOperation }),
}
