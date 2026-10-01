import type { Status } from '@gimac/design-system'
import { ETAPES, type EtapeCode, type EtatOperationParcours, type Fonction, type OperationParcours, type SegmentParcours } from '../../../../modele/parcours'

// Onglet « Opérations » : présentation des opérations telles que le service les renvoie. Aucune règle
// métier n'est évaluée ici (états, verrous, actions permises viennent du service) ; seuls le tri, les
// filtres de lecture, les tons des badges et la mise en forme du code de contrôle sont locaux.

export const TON_ETAT: Record<EtatOperationParcours, Status> = {
  VERROUILLEE: 'neutral', A_FAIRE: 'info', EN_COURS: 'info', EN_VALIDATION: 'warn', FAITE: 'ok', BLOQUEE: 'crit', SANS_OBJET: 'neutral',
}

export type Vue = '' | 'a-faire' | 'a-valider' | 'retard'
export const VUES: Exclude<Vue, ''>[] = ['a-faire', 'a-valider', 'retard']
export type Filtres = { vue: Vue; etape: string; miennes: boolean; recherche: string }

const OUVERTES: EtatOperationParcours[] = ['A_FAIRE', 'EN_COURS', 'BLOQUEE']

/** Vrai si l'une des fonctions de l'utilisateur porte l'opération (responsable, validateur ou consulté). */
export function concerne(o: OperationParcours, fonctions: Fonction[]): boolean {
  const f = (x: Fonction | undefined) => x !== undefined && fonctions.includes(x)
  return f(o.raci.R?.fonction) || f(o.raci.V?.fonction) || o.raci.C.some((p) => f(p.fonction))
}

export function filtrer(ops: OperationParcours[], f: Filtres, fonctions: Fonction[], libelle: (o: OperationParcours) => string): OperationParcours[] {
  const q = f.recherche.trim().toLowerCase()
  return ops.filter(
    (o) =>
      (!f.etape || o.etape === f.etape) &&
      (!f.miennes || concerne(o, fonctions)) &&
      (f.vue !== 'a-faire' || OUVERTES.includes(o.etat)) &&
      (f.vue !== 'a-valider' || o.etat === 'EN_VALIDATION') &&
      (f.vue !== 'retard' || o.sla === 'DEPASSE') &&
      (!q || o.rang.toLowerCase() === q || libelle(o).toLowerCase().includes(q)),
  )
}

export type Groupe = { etape: EtapeCode; segments: { segment: SegmentParcours; operations: OperationParcours[] }[] }

/** Regroupement par étape (ordre du parcours) puis par segment (tronc amont, branche, tronc aval), ordre du service conservé. */
export function grouper(ops: OperationParcours[]): Groupe[] {
  return ETAPES.flatMap((etape) => {
    const siennes = ops.filter((o) => o.etape === etape)
    if (!siennes.length) return []
    const segments = (['AMONT', 'BRANCHE', 'AVAL'] as SegmentParcours[])
      .map((segment) => ({ segment, operations: siennes.filter((o) => o.segment === segment) }))
      .filter((s) => s.operations.length > 0)
    return [{ etape, segments }]
  })
}

/** Plateformes distinctes d'un ensemble d'opérations (en-tête d'un segment de branche). */
export const plateformes = (ops: OperationParcours[]) => [...new Set(ops.map((o) => o.plateforme).filter((p): p is NonNullable<typeof p> => p !== null))]

/**
 * Écran où se réalise le geste d'une opération, quand ce n'est pas l'onglet des opérations : l'affectation
 * des ressources (rang 6) est un geste du dossier ; les verrous de la décision (rangs 18 à 26) et l'étape 10
 * (rangs 32 à 35) ont leurs onglets. L'onglet des opérations les montre en lecture, avec le lien.
 */
export type GesteAilleurs = 'affectation' | 'decision' | 'mise-en-service'
export function ongletDuGeste(rang: string): GesteAilleurs | null {
  if (rang === '6') return 'affectation'
  if (/^(1[89]|2[0-6])$/.test(rang)) return 'decision'
  // Fiche de suivi (32, 33) et service supplémentaire (35) ; le suivi de l'exploitation (34) se prouve ici.
  if (/^3[235]$/.test(rang)) return 'mise-en-service'
  return null
}
export function lienDuGeste(g: GesteAilleurs, o: { liaisonId: string; id: string }): string {
  const base = `/des/parcours/${o.liaisonId}`
  return g === 'affectation' ? `${base}?onglet=etapes&action=affecter` : `${base}?onglet=${g}&operation=${encodeURIComponent(o.id)}`
}

/** Opération dont le responsable est externe (participant, personnalisateur) : elle se réalise depuis le portail. */
export const externe = (o: OperationParcours) => o.raci.R?.fonction === 'PARTICIPANT' || o.raci.R?.fonction === 'PERSONNALISATEUR'

/**
 * Code de contrôle (KCV) : 4 à 8 caractères hexadécimaux, majuscules, sans espace. Une saisie plus longue
 * est refusée, jamais tronquée : ce serait peut-être une clé ou une composante, dont une partie serait gardée.
 */
export function lireCodeControle(brut: string): { valeur: string; erreur: 'vide' | 'format' | 'tropLong' | null } {
  const valeur = brut.replace(/\s+/g, '').toUpperCase()
  if (!valeur) return { valeur, erreur: 'vide' }
  if (valeur.length > 8) return { valeur: '', erreur: 'tropLong' }
  return { valeur, erreur: /^[0-9A-F]{4,8}$/.test(valeur) ? null : 'format' }
}

/** Compteurs d'en-tête de l'onglet (sur la liaison entière, indépendants des filtres). */
export function compter(ops: OperationParcours[]) {
  return {
    total: ops.length,
    faites: ops.filter((o) => o.etat === 'FAITE').length,
    aFaire: ops.filter((o) => OUVERTES.includes(o.etat)).length,
    aValider: ops.filter((o) => o.etat === 'EN_VALIDATION').length,
    retard: ops.filter((o) => o.sla === 'DEPASSE').length,
  }
}
