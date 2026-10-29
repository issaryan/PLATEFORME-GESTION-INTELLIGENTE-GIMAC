import type { InstanceProcessus } from '../../../modele/processus'
import type { ServiceProcessus } from '../../../services/ports'

/** Déclenche le téléchargement local du PDF servi par MS-PILOTAGE et libère aussitôt l'URL temporaire. */
export async function telechargerPdfInstance(service: ServiceProcessus, instance: Pick<InstanceProcessus, 'id' | 'reference'>): Promise<void> {
  const blob = await service.telechargerPdf(instance.id)
  const url = URL.createObjectURL(blob)
  try {
    const a = document.createElement('a')
    a.href = url
    a.download = `${instance.reference ?? instance.id}.pdf`
    a.click()
  } finally {
    URL.revokeObjectURL(url)
  }
}
