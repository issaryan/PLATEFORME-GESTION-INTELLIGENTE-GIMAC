import type { DiffLine, TreeNode } from '@gimac/design-system'
import type { SegmentParcours } from '../modele/parcours'
import type { ReferenceOperation, ReferentielVersion } from '../modele/homologation'

// Adaptateurs DES-12. Regroupements et mises en forme d'affichage uniquement, aucune règle métier :
// l'état des versions, le contrôle d'acyclicité et l'impact des migrations viennent du service.
type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string
const TON = { BROUILLON: 'info', PUBLIEE: 'ok', RETIREE: 'neutral' } as const

/** Arbre : type de projet, puis versions (les plus récentes d'abord). */
export function versArbre(versions: ReferentielVersion[], t: Traduire): TreeNode[] {
  const types = [...new Set(versions.map((v) => v.typeProjet))]
  return types.map((type) => ({
    key: `type:${type}`,
    label: t(`typeProjet.${type}`, { defaultValue: String(type) }),
    icon: 'kanban' as const,
    count: versions.filter((v) => v.typeProjet === type).length,
    children: versions
      .filter((v) => v.typeProjet === type)
      .sort((a, b) => (b.numero ?? Number.MAX_SAFE_INTEGER) - (a.numero ?? Number.MAX_SAFE_INTEGER))
      .map((v) => ({
        key: v.id,
        label: v.numero === null ? t('referentiel.brouillon') : t('referentiel.versionN', { n: v.numero }),
        badge: { status: TON[v.etat], label: t(`referentiel.etat.${v.etat}`), icon: null },
      })),
  }))
}

/** Opérations regroupées par segment, dans l'ordre du parcours (tronc amont, branche, tronc aval). */
export const SEGMENTS: SegmentParcours[] = ['AMONT', 'BRANCHE', 'AVAL']
export function parSegment(ops: ReferenceOperation[]): { segment: SegmentParcours; operations: ReferenceOperation[] }[] {
  return SEGMENTS.map((segment) => ({ segment, operations: ops.filter((o) => o.segment === segment) })).filter((g) => g.operations.length > 0)
}

const valeur = (o: ReferenceOperation, c: keyof ReferenceOperation) => {
  const v = o[c]
  if (Array.isArray(v)) return v.map((x) => (typeof x === 'object' ? JSON.stringify(x) : String(x))).join(', ')
  return v === null || v === undefined ? '' : typeof v === 'object' ? JSON.stringify(v) : String(v)
}
// Égalité sur tous les champs significatifs ; affichage court pour que les deux colonnes tiennent.
const CHAMPS: (keyof ReferenceOperation)[] = [
  'libelle', 'description', 'etape', 'segment', 'responsable', 'validateur', 'consultes', 'informes', 'externes', 'predecesseurs', 'delaiJours', 'grain', 'plateforme',
  'cle', 'secret', 'valeurReference', 'livrables', 'geste', 'jalon', 'declencheur', 'particularite', 'partie', 'serviceCode', 'idScenario', 'natureCouverture',
]
const signature = (o: ReferenceOperation) => CHAMPS.map((c) => valeur(o, c)).join('|')
const ligne = (o: ReferenceOperation) => [o.code.padEnd(7), o.libelle, `← ${valeur(o, 'predecesseurs') || '∅'}`].join('  ')

/** Lignes de DiffView : version active (gauche) contre version courante (droite), par code d'opération. */
export function versLignesDiff(avant: ReferenceOperation[], apres: ReferenceOperation[]): DiffLine[] {
  const codes = [...new Set([...avant.map((o) => o.code), ...apres.map((o) => o.code)])]
  let ln = 0
  let rn = 0
  return codes.map((code): DiffLine => {
    const a = avant.find((o) => o.code === code)
    const b = apres.find((o) => o.code === code)
    if (a && !b) return { ln: ++ln, type: 'del', left: ligne(a) }
    if (!a && b) return { rn: ++rn, type: 'add', right: ligne(b) }
    const x = a as ReferenceOperation
    const y = b as ReferenceOperation
    return signature(x) === signature(y) ? { ln: ++ln, rn: ++rn, text: ligne(x) } : { ln: ++ln, rn: ++rn, type: 'mod', left: ligne(x), right: ligne(y) }
  })
}

/** Suite ordonnée d'un cycle, fermée sur son premier code : A → B → C → A. */
export function suiteCycle(cycle: string[]): string {
  return cycle.length ? [...cycle, cycle[0]].join(' → ') : ''
}
