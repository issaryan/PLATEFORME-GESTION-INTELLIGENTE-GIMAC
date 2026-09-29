import type { QuitusCircuitProps, QuitusState } from '@gimac/design-system'
import type { Circuit, NatureQuitus, QuitusView } from '../modele/certification'

// Adaptateurs purs : réponses de MS-CERTIFICATION vers les propriétés des composants du catalogue.
// Aucune règle : l'ordre, la fonction du signataire et l'impartialité sont appliqués par le service,
// qui le traduit dans actionsPermises.

const CLE: Record<NatureQuitus, 'tech' | 'sec' | 'conf' | 'dg'> = { TECHNIQUE: 'tech', SECURITE: 'sec', CONFORMITE: 'conf', DIRECTION_GENERALE: 'dg' }

export function etatQuitusVersCircuit(q: QuitusView): QuitusState['state'] {
  if (q.etat === 'SIGNE_FAVORABLE') return 'signed'
  if (q.etat === 'SIGNE_DEFAVORABLE') return 'refused'
  if (q.etat === 'ATTENDU' && q.actionsPermises.includes('SIGNER')) return 'pending'
  return 'todo'
}

/** Circuit du service vers QuitusCircuit en mode « quatre » (sécurité et conformité au même rang). */
export function versQuitusCircuit(c: Circuit, libelleRole: (role: string) => string): NonNullable<QuitusCircuitProps['quitus']> {
  const r: NonNullable<QuitusCircuitProps['quitus']> = {}
  for (const q of c.quitus) {
    r[CLE[q.nature]] = { state: etatQuitusVersCircuit(q), signer: libelleRole(q.signataireRole), at: q.dateSignature, reason: q.motif }
  }
  return r
}

/** Quitus que l'utilisateur courant peut signer, tels que le service les annonce. */
export const quitusSignables = (c: Circuit): QuitusView[] => c.quitus.filter((q) => q.actionsPermises.includes('SIGNER'))

/** Désignations proposées en cas de décision défavorable : opérations et services du socle. */
export function designationsPossibles(c: Circuit): { value: string; label: string }[] {
  return [
    ...c.socle.operations.map((o) => ({ value: `OP:${o.code}`, label: `${o.code} ${o.libelle}` })),
    ...c.socle.services.map((s) => ({ value: `SV:${s}`, label: s })),
  ]
}

export function separerDesignations(valeurs: string[]): { operationsDesignees: string[]; servicesDesignes: string[] } {
  return {
    operationsDesignees: valeurs.filter((v) => v.startsWith('OP:')).map((v) => v.slice(3)),
    servicesDesignes: valeurs.filter((v) => v.startsWith('SV:')).map((v) => v.slice(3)),
  }
}

/** Compare l'empreinte reçue (Repr-Digest) à l'empreinte annoncée ; null si le service n'en a pas fourni. */
export function verifierEmpreinte(recue: string | null, annoncee: string): boolean | null {
  return recue === null ? null : recue.toLowerCase() === annoncee.toLowerCase()
}
