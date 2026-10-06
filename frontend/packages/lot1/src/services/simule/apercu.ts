import type { ApiError } from '@gimac/socle'
import type { ServiceApercu } from '../ports'
import { pdfDemo } from './messagerie'

// Aperçu bureautique simulé (D-83) : mêmes refus que MS-DOCUMENT (docs/lot1/apercu-bureautique.md §2), puis un
// petit PDF de démonstration qui reprend le nom et le type du document (une page par diapositive pour une
// présentation, comme la conversion réelle). Un nom contenant « protege » imite un fichier protégé par mot de
// passe (422), « indisponible » un moteur injoignable (503).

type Attendre = <T>(v: T | (() => T)) => Promise<T>
type Refus = (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError

const FORMATS = ['docx', 'doc', 'odt', 'rtf', 'pptx', 'ppt', 'odp', 'xlsx', 'xls', 'ods']
const TAILLE_MAX = 20 * 1024 * 1024

export function apercuSimule(attendre: Attendre, refus: Refus): ServiceApercu {
  return {
    // Contrôles simulés, puis le PDF hors de la copie de la réponse (un Blob ne se clone pas partout).
    convertir: (source, nom) => attendre(() => {
      const ext = nom.includes('.') ? nom.slice(nom.lastIndexOf('.') + 1).toLowerCase() : ''
      if (source.size > TAILLE_MAX) throw refus(413, 'DOC_APERCU_TROP_VOLUMINEUX', 'Document trop volumineux.', { taille: source.size, tailleMax: TAILLE_MAX })
      if (!FORMATS.includes(ext)) throw refus(415, 'DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE', 'Format non pris en charge.')
      if (/protege/i.test(nom)) throw refus(422, 'DOC_APERCU_ILLISIBLE', 'Document illisible ou protégé par mot de passe.')
      if (/indisponible/i.test(nom)) throw refus(503, 'DOC_APERCU_INDISPONIBLE', 'Conversion momentanément indisponible.')
      return ext
    }).then((ext) => {
      const presentation = ['pptx', 'ppt', 'odp'].includes(ext)
      const tableur = ['xlsx', 'xls', 'ods'].includes(ext)
      return pdfDemo([
        nom,
        presentation ? 'Présentation convertie pour l’aperçu (démonstration)' : tableur ? 'Classeur converti pour l’aperçu (démonstration)' : 'Document converti pour l’aperçu (démonstration)',
        '',
        'Aperçu produit par la plateforme : le document n’est jamais exécuté.',
        'Téléchargez le fichier pour le modifier dans son application.',
      ])
    }),
  }
}
