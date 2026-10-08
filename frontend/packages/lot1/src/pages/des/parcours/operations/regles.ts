import type { Status } from '@gimac/design-system'
import { ETAPES, type EtatOperationParcours, type Fonction, type OperationParcours, type SegmentParcours } from '../../../../modele/parcours'

// Onglet « Opérations » : présentation des opérations telles que le service les renvoie. Aucune règle
// métier n'est évaluée ici (états, verrous, actions permises viennent du service) ; seuls le tri, les
// filtres de lecture, les tons des badges et la mise en forme du code de contrôle sont locaux.

export const TON_ETAT: Record<EtatOperationParcours, Status> = {
  VERROUILLEE: 'neutral', A_FAIRE: 'info', EN_COURS: 'info', EN_VALIDATION: 'warn', EN_SIGNATURE: 'warn', FAITE: 'ok', BLOQUEE: 'crit', SANS_OBJET: 'neutral',
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

/** Étape d'une opération : celle de la version du référentiel (D-89, D-93), l'étape canonique à défaut. */
export const etapeDe = (o: Pick<OperationParcours, 'etape' | 'etapeCode'>) => o.etapeCode || o.etape

export function filtrer(ops: OperationParcours[], f: Filtres, fonctions: Fonction[], libelle: (o: OperationParcours) => string): OperationParcours[] {
  const q = f.recherche.trim().toLowerCase()
  return ops.filter(
    (o) =>
      (!f.etape || etapeDe(o) === f.etape) &&
      (!f.miennes || concerne(o, fonctions)) &&
      (f.vue !== 'a-faire' || OUVERTES.includes(o.etat)) &&
      (f.vue !== 'a-valider' || o.etat === 'EN_VALIDATION' || o.etat === 'EN_SIGNATURE') &&
      (f.vue !== 'retard' || o.sla === 'DEPASSE') &&
      (!q || o.rang.toLowerCase() === q || libelle(o).toLowerCase().includes(q)),
  )
}

export type Groupe = { etape: string; segments: { segment: SegmentParcours; operations: OperationParcours[] }[] }

/**
 * Regroupement par étape puis par segment (tronc amont, branche, tronc aval), ordre du service conservé ; étapes dans
 * l'ordre de la version (`ordre`, D-93), sinon l'ordre canonique, puis celles qui n'y figurent pas.
 */
export function grouper(ops: OperationParcours[], ordre: string[] = ETAPES): Groupe[] {
  const codes = [...ordre, ...new Set(ops.map(etapeDe).filter((e) => !ordre.includes(e)))]
  return codes.flatMap((etape) => {
    const siennes = ops.filter((o) => etapeDe(o) === etape)
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
 * Écran où se réalise le geste d'une opération, quand ce n'est pas l'onglet des opérations : lu sur l'attribut `vue`
 * de l'opération (D-93 §8.5), jamais sur son rang. AFFECTATION : geste du dossier ; DECISION et ATTESTATIONS : onglet
 * de la décision ; MISE_EN_SERVICE : onglet de la mise en service. L'onglet des opérations les montre en lecture.
 */
export type GesteAilleurs = 'affectation' | 'decision' | 'mise-en-service'
export function ongletDuGeste(o: Pick<OperationParcours, 'vue'>): GesteAilleurs | null {
  if (o.vue === 'AFFECTATION') return 'affectation'
  if (o.vue === 'DECISION' || o.vue === 'ATTESTATIONS') return 'decision'
  if (o.vue === 'MISE_EN_SERVICE') return 'mise-en-service'
  return null
}
/**
 * Carte de l'onglet « Décision » où se réalise une opération (lien profond ?operation=, D-98) : lue sur le déclencheur, le
 * jalon, le geste et la vue de l'opération, jamais sur son rang. Procès-verbal : constat de couverture, établissement et
 * signature (jalons APTE_CERTIFICATION et CERTIFIEE) ; approbation de la sécurité : dossier et fiche de changement,
 * attestations, approbation (jalon APPROUVEE_SECURITE, vue ATTESTATIONS) ; null si l'opération ne se rattache à aucune carte.
 */
export type CarteDecision = 'pv' | 'approbation'
export function carteDecision(o: Pick<OperationParcours, 'vue' | 'geste' | 'jalon' | 'declencheur'> | undefined): CarteDecision | null {
  if (!o) return null
  const d = o.declencheur ?? ''
  if (/\.(change-record-[a-z]+|attestations-archived|security-approved)\.v\d+$/.test(d) || o.jalon === 'APPROUVEE_SECURITE' || o.vue === 'ATTESTATIONS') return 'approbation'
  if (/\.pv-[a-z]+\.v\d+$/.test(d) || o.jalon === 'APTE_CERTIFICATION' || o.jalon === 'CERTIFIEE' || o.geste === 'CONSTATER_COUVERTURE') return 'pv'
  return o.vue === 'DECISION' ? 'pv' : null
}

export function lienDuGeste(g: GesteAilleurs, o: { liaisonId: string; id: string }): string {
  const base = `/des/parcours/${o.liaisonId}`
  return g === 'affectation' ? `${base}?onglet=etapes&action=affecter` : `${base}?onglet=${g}&operation=${encodeURIComponent(o.id)}`
}

/**
 * Opération dont le responsable tient une fonction externe (catalogue des fonctions, D-91 : `externes`) : elle se réalise
 * depuis le portail ou par l'institution liée.
 */
export const externe = (o: OperationParcours, externes: readonly string[]) => !!o.raci.R && externes.includes(o.raci.R.fonction)

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
    aValider: ops.filter((o) => o.etat === 'EN_VALIDATION' || o.etat === 'EN_SIGNATURE').length,
    retard: ops.filter((o) => o.sla === 'DEPASSE').length,
  }
}
