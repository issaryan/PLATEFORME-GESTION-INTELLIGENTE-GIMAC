import type { CampagneReexamen, VueReexamen } from '../../modele/reexamens'
import type { ServiceReexamens } from '../portsReexamens'

const OUVERTURE = '2026-10-01T02:00:00Z'

/** Jeu de démonstration du réexamen : permissions SBX visibles comme les autres. */
export function reexamensSimules(): ServiceReexamens {
  let campagne: CampagneReexamen = { id: '2026-t4', period: '2026-T4', state: 'OUVERTE', openedAt: OUVERTURE, approverId: null, closedAt: null, digest: null, enAttente: 2 }
  let acces: VueReexamen['acces'] = [
    { compteId: 'u-sbx-1', identifiant: 'awa.testeuse', nomAffiche: 'Awa Testeuse', institutionId: '99001', profils: ['ROLE_SBX_TESTEUR'], permissions: ['SBX_CER_EXECUTION', 'SBX_CER_CONSULTATION', 'NOT_INBOX_READ'], decision: 'EN_ATTENTE', comment: null, decidedBy: null, decidedAt: null },
    { compteId: 'u-sbx-2', identifiant: 'paul.partenaire', nomAffiche: 'Paul Partenaire', institutionId: '99001', profils: ['ROLE_SBX_DEVELOPPEUR'], permissions: ['SBX_RAC_ADMINISTRATION', 'SBX_SIM_SIMULATION'], decision: 'EN_ATTENTE', comment: null, decidedBy: null, decidedAt: null },
  ]
  const vue = (): VueReexamen => ({ campagne: { ...campagne, enAttente: acces.filter((a) => a.decision === 'EN_ATTENTE').length }, acces: acces.map((a) => ({ ...a })) })
  return {
    lister: async () => [vue().campagne],
    ouvrir: async () => vue().campagne,
    lire: async () => vue(),
    decider: async (_id, compteId, d) => {
      acces = acces.map((a) => a.compteId === compteId ? { ...a, decision: d.decision, profils: d.decision === 'REVOQUE' ? [] : (d.profils ?? a.profils), comment: d.comment ?? null, decidedBy: 'u-super-admin', decidedAt: new Date().toISOString() } : a)
      campagne = vue().campagne
      return vue()
    },
    cloturer: async () => {
      campagne = { ...vue().campagne, state: 'CLOSE', approverId: 'u-super-admin', closedAt: new Date().toISOString(), digest: '8c4d53f0f6ef4e6abf9c8d8d2f47c9bb68a2a596f187f6db72f4b7f73d77e3a1' }
      return campagne
    },
  }
}
