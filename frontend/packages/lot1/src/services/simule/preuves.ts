import type { TypeProjet } from '../../modele/commun'
import type { Blocage, DepotLivrable, EtatLivrable, EvenementPreuve, LivrableAttendu, OperationParcours } from '../../modele/parcours'
import { preuvesDe } from '../../modele/preuvesCR'

// Preuves de la démonstration (D-109), comme EtatsPreuves de MS-HOMOLOGATION : livrables attendus de chaque opération
// lus dans les données D-109 (preuvesCR.ts), état de chacun, pièce par service, chronologie réelle. Aucune règle propre :
// une opération reprise n'est jamais montrée en attente, une opération close avant l'exigence la montre « non exigée ».

/** Dépôts de la démonstration, par identifiant d'opération. */
export type DepotsDemo = Map<string, DepotLivrable[]>

const fermee = (o: OperationParcours) => o.etat === 'FAITE' || o.etat === 'SANS_OBJET' || o.etat === 'EN_SIGNATURE'

/** État d'un livrable pour un dépôt donné (ou son absence). */
export function etatLivrable(o: OperationParcours, depot: DepotLivrable | null | undefined): EtatLivrable {
  if (!depot) {
    if (o.reprise) return 'REPRISE_SANS_PIECE'
    return fermee(o) ? 'NON_EXIGEE' : 'ATTENDUE'
  }
  if (depot.sansObjet) return 'SANS_OBJET'
  if (fermee(o)) return 'VALIDEE'
  const renvoye = (o.renvois ?? []).some((r) => r.le.slice(0, 10) >= depot.deposeLe.slice(0, 10))
  return (o.etat === 'A_FAIRE' || o.etat === 'EN_COURS') && renvoye ? 'REFUSEE' : 'DEPOSEE'
}

/** Dépôt le plus récent d'un livrable (et d'un service). */
const dernier = (l: DepotLivrable[], code: string, service?: string | null) =>
  [...l].reverse().find((d) => d.livrable === code && (service === undefined || (d.service ?? null) === (service ?? null)))

/** Livrables attendus de l'opération (D-109), sinon le livrable unique de la démonstration (description de l'opération). */
export function livrablesAttendusDemo(o: OperationParcours, type: TypeProjet, services: string[], depots: DepotLivrable[], peutDeposer: boolean): LivrableAttendu[] {
  const def = preuvesDe(type, o.rang)
  const preuve: DepotLivrable | null = o.preuve && !o.reprise ? { documentRef: o.preuve.documentRef, nom: o.preuve.nom, empreinte: o.preuve.empreinte, deposeLe: o.preuve.deposeLe, deposePar: o.preuve.deposePar } : null
  const modeles = def?.livrables ?? [{ code: 'L1', libelle: o.description || o.libelle || o.rang, nature: o.geste === 'DEPOSER_LIVRABLE' ? 'LIVRABLE' as const : 'PREUVE' as const, deposant: o.raci.R?.fonction ?? 'DDRM', typesPieces: [], obligatoire: true, grain: 'OPERATION' as const, consigne: '' }]
  return modeles.map((m, i) => {
    const depot = dernier(depots, m.code) ?? (i === 0 ? preuve : null)
    const lignes = m.grain === 'SERVICE'
      ? services.map((s) => {
          const d = dernier(depots, m.code, s) ?? dernier(depots, m.code, null) ?? (i === 0 ? preuve : null)
          return { serviceCode: s, etat: etatLivrable(o, d), depot: d ?? null }
        })
      : null
    const etats = lignes?.map((x) => x.etat) ?? []
    const etat: EtatLivrable = lignes && lignes.length
      ? (['REFUSEE', 'ATTENDUE', 'REPRISE_SANS_PIECE', 'NON_EXIGEE', 'DEPOSEE'] as EtatLivrable[]).find((x) => etats.includes(x)) ?? (etats.every((x) => x === 'SANS_OBJET') ? 'SANS_OBJET' : 'VALIDEE')
      : etatLivrable(o, depot)
    return {
      code: m.code, libelle: m.libelle, nature: m.nature, deposant: m.deposant, libelleDeposant: null, typesPieces: m.typesPieces, obligatoire: m.obligatoire,
      depot: depot ?? null, peutDeposer, grain: m.grain, consigne: m.consigne || null, etat, services: lignes,
    }
  })
}

/** Chronologie réelle de la liaison, du plus récent au plus ancien (EtatsPreuves.chronologie). */
export function chronologieDemo(ops: OperationParcours[], blocages: Blocage[], depots: DepotsDemo): EvenementPreuve[] {
  const l: EvenementPreuve[] = []
  const de = (o: OperationParcours) => ({ operationId: o.id, rang: o.rang, operation: o.libelle ?? o.rang, etapeCode: o.etapeCode ?? o.etape })
  for (const o of ops) {
    const d = depots.get(o.id) ?? []
    d.forEach((x) => l.push({ date: x.deposeLe, type: x.sansObjet ? 'SANS_OBJET' : 'DEPOT', ...de(o), livrable: x.livrable ?? null, service: x.service ?? null, acteur: x.deposePar, detail: x.sansObjet ? x.motif ?? null : x.nom ?? null, documentRef: x.documentRef ?? null, empreinte: x.empreinte ?? null }))
    if (o.preuve && !o.reprise && !d.length) l.push({ date: o.preuve.deposeLe, type: 'DEPOT', ...de(o), acteur: o.preuve.deposePar, detail: o.preuve.nom, documentRef: o.preuve.documentRef, empreinte: o.preuve.empreinte })
    ;(o.renvois ?? []).forEach((r) => l.push({ date: r.le.slice(0, 10), type: 'RENVOI', ...de(o), acteur: r.parNom, detail: r.motif }))
    ;(o.signatures ?? []).filter((s) => s.etat === 'SIGNEE').forEach((s) => l.push({ date: (s.signeLe ?? '').slice(0, 10) || null, instant: s.signeLe ?? null, type: 'SIGNATURE', ...de(o), acteur: s.signeParNom ?? null, detail: `${s.code} · ${s.signataire}`, empreinte: s.empreinte ?? null }))
    if (o.etat === 'FAITE' && o.faiteLe) l.push({ date: o.faiteLe, type: o.reprise ? 'REPRISE' : 'FAITE', ...de(o), acteur: o.reprise ? null : o.raci.R?.nom ?? null, detail: o.reprise ? 'reprise, pièce non numérisée' : null })
  }
  for (const b of blocages) {
    l.push({ date: b.depuis, type: 'BLOCAGE', etapeCode: b.etape, detail: b.motif })
    if (b.leveLe) l.push({ date: b.leveLe, type: 'LEVEE', etapeCode: b.etape, detail: b.motif })
  }
  return l.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? '') || (b.instant ?? '').localeCompare(a.instant ?? ''))
}
