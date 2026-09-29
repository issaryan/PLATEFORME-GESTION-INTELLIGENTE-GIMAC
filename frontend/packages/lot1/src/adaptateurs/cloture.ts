import type { FourElementsChecklistProps } from '@gimac/design-system'
import type { ElementCloture, ElementKey } from '../modele/homologation'

// Adaptateur DES-10 : état des quatre éléments renvoyé par le service (GET /operations/{id}/elements-de-cloture)
// vers les propriétés de FourElementsChecklist. Aucune évaluation : l'état vient du service.
type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string
const CLE: Record<ElementKey, 'owner' | 'proof' | 'timestamp' | 'validation'> = { responsable: 'owner', preuve: 'proof', horodatage: 'timestamp', validation: 'validation' }

export function versQuatreElements(elements: ElementCloture[], t: Traduire): FourElementsChecklistProps {
  const p: Required<Pick<FourElementsChecklistProps, 'elements' | 'missing' | 'actions'>> = { elements: {}, missing: {}, actions: {} }
  for (const e of elements) {
    const k = CLE[e.key]
    if (e.etat === 'REUNI') {
      p.elements[k] = e.detail ?? true
    } else if (e.etat === 'EN_ATTENTE') {
      // État « en attente » du catalogue : ni réuni ni manquant (horodatage réessayé par le service).
      p.elements[k] = 'pending'
      p.missing[k] = t(`cloture.element.${e.key}.EN_ATTENTE`)
    } else {
      p.elements[k] = false
      p.missing[k] = e.detail ?? t(`cloture.element.${e.key}.MANQUANT`)
      if (e.actionManquante) p.actions[k] = t(e.actionManquante.labelKey)
    }
  }
  return p
}

/** Étape de l'assistant d'après l'adresse ; une valeur inconnue ramène à la première étape. */
export const ETAPES_CLOTURE = ['preuve', 'elements', 'signature'] as const
export type EtapeCloture = (typeof ETAPES_CLOTURE)[number]
export const etapeDepuis = (v: string | null): EtapeCloture => ((ETAPES_CLOTURE as readonly string[]).includes(v ?? '') ? (v as EtapeCloture) : 'preuve')

/** Taille de fichier lisible (Ko, Mo) pour l'affichage du FileDropzone. */
export function tailleLisible(octets: number, langue: string): string {
  const f = (n: number) => n.toLocaleString(langue, { maximumFractionDigits: 1 })
  return octets < 1024 * 1024 ? `${f(octets / 1024)} Ko` : `${f(octets / (1024 * 1024))} Mo`
}
