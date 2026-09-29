import { useLecture } from './base'
import { cles } from './cles'

// Hooks de données de DES-10 : seules lectures de l'écran ; les effets passent par les actions du descripteur.
export const useElementsDeCloture = (operationId: string) =>
  useLecture(cles.operation(operationId, 'cloture'), (s) => s.homologation.elementsDeCloture(operationId), {
    // « validée, horodatage en attente » : le service réessaie seul ; l'écran se met à jour en attendant le canal poussé.
    refetchInterval: (q) => (q.state.data?.etat === 'VALIDEE_HORODATAGE_EN_ATTENTE' ? 2000 : false),
  })

export const useOperation = (operationId: string) =>
  useLecture(cles.operation(operationId), (s) => s.homologation.lireOperation(operationId))
