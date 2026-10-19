import type { DagViewProps, GanttViewProps, Milestone, OperationState } from '@gimac/design-system'
import type { DossierLiaison, EtapeVersion, OperationParcours } from '../modele/parcours'

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string

// Vues « Graphe » et « Planning » du dossier d'une liaison (demande du 19/10/2026, D-112) : où en est chaque
// opération dans les étapes de la version, et le planning des étapes du projet. Aucune règle métier : états,
// échéances, dates et décomptes viennent de MS-HOMOLOGATION ; ces fonctions ne font que les disposer.

const JOUR = 86_400_000

/** Étapes de la version dans leur ordre ; à défaut (service antérieur à D-89), celles du parcours. */
export function etapesDuDossier(dossier: DossierLiaison, t: Traduire): { code: string; libelle: string; etape?: EtapeVersion }[] {
  if (dossier.etapesVersion?.length) {
    return [...dossier.etapesVersion].sort((a, b) => a.ordre - b.ordre).map((e) => ({ code: e.code, libelle: e.libelle, etape: e }))
  }
  return dossier.etapes.map((e) => ({ code: e.code, libelle: t(`parcours.etape.${e.code}`) }))
}

/** État d'affichage d'une opération : close, verrouillée, hors périmètre, en retard (échéance passée) ou éligible. */
export function etatAffiche(o: OperationParcours, aujourdhui: string): OperationState {
  if (o.etat === 'FAITE') return 'closed'
  if (o.etat === 'SANS_OBJET') return 'out'
  if (o.etat === 'VERROUILLEE') return 'locked'
  if (o.sla === 'DEPASSE' || (o.echeance && o.echeance < aujourdhui)) return 'late'
  return 'eligible'
}

const etapeOp = (o: OperationParcours) => o.etapeCode ?? o.etape

/** Graphe des opérations, une colonne par étape ; arcs : tous les prédécesseurs (à défaut, les non faits). */
export function grapheDuDossier(dossier: DossierLiaison, ops: OperationParcours[], t: Traduire, aujourdhui: string): DagViewProps {
  const etapes = etapesDuDossier(dossier, t)
  const connues = new Set(etapes.map((e) => e.code))
  const sansEtape = ops.some((o) => !connues.has(etapeOp(o)))
  const phases = [
    ...etapes.filter((e) => ops.some((o) => etapeOp(o) === e.code)).map((e) => ({ key: e.code, label: e.libelle })),
    ...(sansEtape ? [{ key: '', label: t('modelisateur.sansEtape') }] : []),
  ]
  const rangs = new Set(ops.map((o) => o.rang))
  return {
    phases,
    nodes: ops.map((o) => ({
      code: o.rang,
      label: o.libelle ?? t(`parcours.operation.${o.rang}`, { defaultValue: o.rang }),
      owner: o.raci.R?.nom ?? t('parcoursVues.aDesigner'),
      state: etatAffiche(o, aujourdhui),
      phase: connues.has(etapeOp(o)) ? etapeOp(o) : '',
      deps: (o.predecesseurs ?? o.predecesseursNonFaits).filter((r) => rangs.has(r)),
      elements: { owner: !!o.raci.R, proof: !!o.preuve || o.reprise === true, timestamp: !!o.faiteLe, validation: o.etat === 'FAITE' },
    })),
  }
}

const jour = (iso: string) => Date.parse(`${iso.slice(0, 10)}T00:00:00Z`)
const ecart = (de: string, a: string) => Math.round((jour(a) - jour(de)) / JOUR)
/** Libellé jj/mm d'un jour ISO. */
const jjmm = (iso: string) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`
/** Numéro de semaine ISO 8601. */
export function semaineIso(iso: string): number {
  const d = new Date(jour(iso))
  // Jeudi de la même semaine : il fixe l'année de la semaine ; semaine 1 = celle du premier jeudi de l'année.
  d.setUTCDate(d.getUTCDate() - ((d.getUTCDay() + 6) % 7) + 3)
  const jeudi = d.getTime()
  const debutAnnee = Date.UTC(d.getUTCFullYear(), 0, 1)
  return 1 + Math.floor((jeudi - debutAnnee) / JOUR / 7)
}

/**
 * Planning des étapes du projet : une barre par étape, du début réel (ou de la plus ancienne échéance de ses
 * opérations) à sa clôture (ou son échéance) ; avancement = actions faites sur actions de l'étape ; étape en retard
 * (échéance passée, non close) sur le chemin critique. L'origine est la date de démarrage de la liaison.
 */
export function planningDuDossier(dossier: DossierLiaison, ops: OperationParcours[], t: Traduire, aujourdhui: string): GanttViewProps & { vide: boolean } {
  const origine = dossier.dateDemarrage.slice(0, 10)
  const jalons: Milestone[] = []
  for (const { code, libelle, etape } of etapesDuDossier(dossier, t)) {
    const siennes = ops.filter((o) => etapeOp(o) === code && o.etat !== 'SANS_OBJET')
    const echeances = siennes.map((o) => o.echeance).filter((d): d is string => !!d).sort()
    const debut = etape?.debut?.slice(0, 10) ?? siennes.map((o) => o.faiteLe).filter((d): d is string => !!d).sort()[0] ?? echeances[0]
    const fin = etape?.closeLe?.slice(0, 10) ?? etape?.echeance?.slice(0, 10) ?? echeances[echeances.length - 1]
    if (!debut || !fin) continue
    const start = Math.max(0, ecart(origine, debut))
    const total = etape?.actionsTotal ?? siennes.length
    const faites = etape?.actionsFaites ?? siennes.filter((o) => o.etat === 'FAITE').length
    const close = !!etape?.closeLe || (total > 0 && faites === total)
    jalons.push({
      key: code,
      label: libelle,
      owner: etape?.responsable?.nom ?? t('parcoursVues.aDesigner'),
      start,
      duration: Math.max(1, ecart(debut, fin) + 1),
      done: total ? Math.round((100 * faites) / total) : 0,
      critical: !close && fin < aujourdhui,
    })
  }
  const finMax = Math.max(ecart(origine, aujourdhui), ...jalons.map((j) => j.start + j.duration), dossier.dateCibleProduction ? ecart(origine, dossier.dateCibleProduction) : 0)
  const days = Math.max(14, finMax + 7)
  const weeks: { day: number; label: string }[] = []
  for (let d = 0; d < days; d += 7) {
    const iso = new Date(jour(origine) + d * JOUR).toISOString().slice(0, 10)
    weeks.push({ day: d, label: `S${semaineIso(iso)}` })
  }
  const today = ecart(origine, aujourdhui)
  return { start: jjmm(origine), days, today: today >= 0 && today <= days ? today : undefined, weeks, milestones: jalons, vide: jalons.length === 0 }
}
