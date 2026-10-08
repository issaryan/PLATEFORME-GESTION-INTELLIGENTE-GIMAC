import { http, P } from '@gimac/socle'
import type { CoverageValue, Page, TypeProjet } from '../../modele/commun'
import type { EtapeCode, LiaisonResume, Personne, StatutParcours } from '../../modele/parcours'
import type { FiltreLiaisons, ServiceDecision, ServiceParcours, ServicePortailParcours } from '../ports'
import { empreinteSha256 } from './participant'

// Implémentation HTTP du parcours. Étape 3 : routes réelles de MS-HOMOLOGATION (LiaisonsCadrageResource,
// /api/v1/liaisons, réécrites par la passerelle sous /bff/des/homologation). Étapes 4 à 10 : routes
// prévues par la correspondance (docs/lot1/correspondance.md), à livrer côté services.
const H = P.desHomologation
const L = `${H}/liaisons`
const C = `${P.desCertification}/liaisons`
const PH = `${P.portailHomologation}/liaisons`
const PC = `${P.portailCertification}/liaisons`

/** Vue d'une liaison telle que la sert MS-HOMOLOGATION (LiaisonVue). */
type EtatLiaison = 'OUVERTE' | 'EN_HOMOLOGATION' | 'GELEE' | 'APTE_CERTIFICATION' | 'MISE_EN_SERVICE_AUTORISEE' | 'CERTIFIEE' | 'CLOSE'
export type LiaisonVue = {
  id: string
  typeProjet: TypeProjet
  institutionNom: string
  etat: EtatLiaison
  chefDeProjetNom: string | null
  dateOuverture: string
  phaseCourante: string
  couvertureCertification: CoverageValue
  couvertureMiseEnService: CoverageValue
  operationsEnRetard: number
  /** Champs du modèle hybride (D-57), servis dès que le service les porte ; sinon correspondance d'affichage. */
  /** Référence lisible LIA-AAAA-NNN du parcours ; absente pour une liaison pas encore instanciée. */
  reference?: string | null
  statut?: StatutParcours
  etapeCourante?: EtapeCode
  bloquee?: boolean
  prochaineEcheance?: string | null
  dateCibleProduction?: string | null
}

/**
 * Correspondance d'affichage de l'automate technique (EtatLiaison) vers le statut et l'étape du CR, tant que
 * le service ne sert pas ces deux champs. Aucune règle n'est calculée ici : c'est une traduction d'état.
 */
const AFFICHAGE: Record<EtatLiaison, { statut: StatutParcours; etape: EtapeCode }> = {
  OUVERTE: { statut: 'CADREE', etape: 'CADRAGE' },
  EN_HOMOLOGATION: { statut: 'EN_INTEGRATION', etape: 'INTEGRATION' },
  GELEE: { statut: 'SUSPENDUE', etape: 'INTEGRATION' },
  APTE_CERTIFICATION: { statut: 'EN_TEST', etape: 'CERTIFICATION' },
  CERTIFIEE: { statut: 'CERTIFIEE', etape: 'APPROBATION_SECURITE' },
  MISE_EN_SERVICE_AUTORISEE: { statut: 'APPROUVEE_SECURITE', etape: 'MISE_EN_PRODUCTION' },
  CLOSE: { statut: 'EN_PRODUCTION', etape: 'SUIVI' },
}

export function versResume(v: LiaisonVue): LiaisonResume {
  const a = AFFICHAGE[v.etat] ?? AFFICHAGE.OUVERTE
  return {
    id: v.id,
    // Référence du parcours servie par MS-HOMOLOGATION (LIA-AAAA-NNN) ; à défaut seulement, un extrait de l'identifiant.
    reference: v.reference || (v.id.length > 12 ? v.id.slice(0, 8).toUpperCase() : v.id),
    institutionNom: v.institutionNom,
    typeProjet: v.typeProjet,
    statut: v.statut ?? a.statut,
    etapeCourante: v.etapeCourante ?? a.etape,
    conducteurNom: v.chefDeProjetNom,
    couvertureCertification: v.couvertureCertification,
    couvertureMiseEnService: v.couvertureMiseEnService,
    bloquee: v.bloquee ?? v.etat === 'GELEE',
    tachesEnRetard: v.operationsEnRetard,
    prochaineEcheance: v.prochaineEcheance ?? null,
    dateCibleProduction: v.dateCibleProduction ?? null,
  }
}

/** Filtre de l'interface (pages à partir de 1) vers les paramètres du service (pages à partir de 0). */
const requete = (f: FiltreLiaisons) => ({
  page: Math.max(f.page - 1, 0),
  taille: f.taille,
  recherche: f.recherche || undefined,
  typeProjet: f.typeProjet || undefined,
  mesProjets: f.mesProjets || undefined,
  statut: f.statut || undefined,
  etape: f.etape || undefined,
  bloquees: f.bloquees || undefined,
  sansConducteur: f.sansConducteur || undefined,
})

/** Dépôt dans MS-DOCUMENT (WORM) puis rattachement par référence et empreinte, revérifiée par le service. */
async function deposer(fichier: File, finalite: string, reference: string) {
  const corps = new FormData()
  corps.set('fichier', fichier)
  corps.set('finalite', finalite)
  corps.set('reference', reference)
  const document = await http.post<{ id: string; sha256: string }>(`${P.desDocument}/documents`, { body: corps })
  const empreinte = await empreinteSha256(fichier)
  return { documentId: document.id, empreinte, nom: fichier.name }
}

export const parcoursHttp: ServiceParcours = {
  listerLiaisons: async (f) => {
    const p = await http.get<Page<LiaisonVue>>(L, { query: requete(f) })
    return { contenu: p.contenu.map(versResume), total: p.total }
  },
  conducteurs: async () => (await http.get<{ id: string; nom: string }[]>(`${L}/conducteurs`)).map((c): Personne => ({ ...c, fonction: 'CONDUCTEUR' })),
  ouvrirLiaison: async (o) => versResume(await http.post<LiaisonVue>(L, { body: o })),
  designerConducteur: (id, chefDeProjetId, motif) => http.put(`${L}/${id}/chef-de-projet`, { body: { chefDeProjetId, motif } }),
  dossier: (id) => http.get(`${L}/${id}/dossier`),
  operations: (id, f) => http.get(`${L}/${id}/parcours/operations`, { query: { etape: f?.etape, fonction: f?.fonction } }),
  affecterRessources: (id, affectations) => http.put(`${L}/${id}/ressources`, { body: { affectations } }),
  // Dépôt par code de livrable (D-93 §8.4) : `livrable` absent, l'unique livrable de l'opération.
  deposerLivrable: async (opId, fichier, commentaire, livrable) => {
    const d = await deposer(fichier, 'LIVRABLE', opId)
    await http.post(`${H}/parcours/operations/${opId}/livrable`, { body: { ...d, commentaire, ...(livrable ? { livrable } : {}) } })
  },
  attribuer: (opId, qualite, personneId, motif) => http.put(`${H}/parcours/operations/${opId}/intervenants`, { body: { qualite, personneId, motif } }),
  deposerPreuve: async (opId, fichier, complement) => {
    const d = await deposer(fichier, 'PREUVE', opId)
    await http.post(`${H}/parcours/operations/${opId}/preuves`, { body: { ...d, ...complement } })
  },
  validerOperation: (opId, assertion) => http.post(`${H}/parcours/operations/${opId}/validation`, { body: {}, reauthAssertion: assertion }),
  // D-93 : renvoi motivé par le validateur, sans réauthentification.
  renvoyer: (opId, motif) => http.post(`${H}/parcours/operations/${opId}/renvoi`, { body: { motif } }),
  // D-91 : signature d'une opération (sans corps, clé de sécurité de finalité SIGNATURE_OPERATION).
  signerOperation: (id, code, signature, assertion) =>
    http.post(`${L}/${id}/operations/${encodeURIComponent(code)}/signatures/${encodeURIComponent(signature)}`, { reauthAssertion: assertion }),
  // D-93 : affectation d'une fonction « à affecter » (personne interne ou institution externe), motif facultatif.
  affecterFonction: (id, fonction, a) => http.put(`${L}/${id}/fonctions/${encodeURIComponent(fonction)}`, { body: a }),
  // D-94 §6.5 : fonctions de l'appelant, servies par MS-HOMOLOGATION.
  mesFonctions: () => http.get(`${H}/moi/fonctions`),
  declarerBlocage: (id, b) => http.post(`${L}/${id}/blocages`, { body: b }),
  leverBlocage: (bid, commentaire) => http.post(`${H}/blocages/${bid}/levee`, { body: { commentaire } }),
  constaterCouverture: (id) => http.post(`${L}/${id}/couverture/constat`, { body: {} }),
  ouvrirServiceSupplementaire: async (id, services) => versResume(await http.post<LiaisonVue>(`${L}/${id}/services-supplementaires`, { body: { services } })),
  tableauActeur: (fonctions) => http.get(`${H}/tableaux-de-bord/acteur`, { query: { fonctions: fonctions.join(',') } }),
  pilotage: () => http.get(`${H}/tableaux-de-bord/pilotage`),
  personnes: (fonction) => http.get(`${H}/intervenants`, { query: { fonction } }),
}

export const decisionHttp: ServiceDecision = {
  procesVerbal: (id) => http.get(`${C}/${id}/proces-verbal`),
  etablirProcesVerbal: (id) => http.post(`${C}/${id}/proces-verbal`, { body: {} }),
  signerProcesVerbalGimac: (id, assertion) => http.post(`${C}/${id}/proces-verbal/signature-gimac`, { body: {}, reauthAssertion: assertion }),
  approbation: (id) => http.get(`${C}/${id}/approbation-securite`),
  constituerDossierChangement: async (id, fichier) => {
    const d = await deposer(fichier, 'DOSSIER_CHANGEMENT', id)
    await http.post(`${C}/${id}/approbation-securite/dossier`, { body: d })
  },
  referencerFicheChangement: (id, reference) => http.put(`${C}/${id}/approbation-securite/fiche`, { body: { reference } }),
  relireFicheChangement: (id) => http.post(`${C}/${id}/approbation-securite/fiche/lecture`, { body: {} }),
  verifierAttestation: (id, type, conforme, motif) => http.post(`${C}/${id}/approbation-securite/attestations/${type}/verification`, { body: { conforme, motif } }),
  archiverAttestations: (id) => http.post(`${C}/${id}/approbation-securite/archivage`, { body: {} }),
  signerVisa: (id, nature, favorable, motif, assertion) => http.post(`${C}/${id}/approbation-securite/visas/${nature}`, { body: { favorable, motif }, reauthAssertion: assertion }),
  ficheSuivi: (id) => http.get(`${L}/${id}/fiche-suivi`),
  remettreFicheSuivi: (id) => http.post(`${L}/${id}/fiche-suivi/remise`, { body: {} }),
  constaterMiseEnService: (id) => http.post(`${L}/${id}/fiche-suivi/constat`, { body: {} }),
}

export const portailParcoursHttp: ServicePortailParcours = {
  mesLiaisons: async () => (await http.get<LiaisonVue[]>(PH)).map(versResume),
  dossier: (id) => http.get(`${PH}/${id}/dossier`),
  mesTaches: () => http.get(`${P.portailHomologation}/taches`),
  communiquerCoordonnees: (id, c) => http.put(`${PH}/${id}/coordonnees-techniques`, { body: c }),
  deposerLivrable: async (opId, fichier, commentaire) => {
    const corps = new FormData()
    corps.set('fichier', fichier)
    corps.set('commentaire', commentaire)
    await http.post(`${P.portailHomologation}/operations/${opId}/livrable`, { body: corps })
  },
  procesVerbal: (id) => http.get(`${PC}/${id}/proces-verbal`),
  signerProcesVerbal: (id, assertion) => http.post(`${PC}/${id}/proces-verbal/signature`, { body: {}, reauthAssertion: assertion }),
  approbation: (id) => http.get(`${PC}/${id}/approbation-securite`),
  deposerAttestation: async (id, type, fichier) => {
    const corps = new FormData()
    corps.set('fichier', fichier)
    await http.post(`${PC}/${id}/attestations/${type}`, { body: corps })
  },
  ficheSuivi: (id) => http.get(`${PH}/${id}/fiche-suivi`),
  renseignerFicheSuivi: (id, lignes) => http.put(`${PH}/${id}/fiche-suivi`, { body: { lignes } }),
}
