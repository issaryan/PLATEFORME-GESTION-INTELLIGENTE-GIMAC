import { http } from '@gimac/socle'
import { PILOTAGE } from './pilotage'

/** Téléchargement du PDF d'une instance, lisible de qui lit déjà l'instance. */
export async function telechargerProcessusPdf(instanceId: string): Promise<Blob> {
  return (await http.blob(`${PILOTAGE}/processus/instances/${encodeURIComponent(instanceId)}/pdf`, 'application/pdf')).blob
}
