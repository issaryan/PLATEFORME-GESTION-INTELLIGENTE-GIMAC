import { http } from '@gimac/socle'
import { PILOTAGE } from './pilotage'

/** Téléchargement du PDF d'une instance, lisible de qui lit déjà l'instance, dans la langue demandée (D-139). */
export async function telechargerProcessusPdf(instanceId: string, langue?: string): Promise<Blob> {
  const q = langue ? `?langue=${encodeURIComponent(langue)}` : ''
  return (await http.blob(`${PILOTAGE}/processus/instances/${encodeURIComponent(instanceId)}/pdf${q}`, 'application/pdf')).blob
}
