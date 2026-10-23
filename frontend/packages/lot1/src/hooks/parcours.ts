import type { Ceremonie, Fonction, OuvertureLiaison, TypeAttestation } from '../modele/parcours'
import type { Assertion, FiltreLiaisons } from '../services/ports'
import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks du parcours de la liaison, étapes 3 à 10 (D-56 à D-59) : seuls points d'appel des ports
// ServiceParcours, ServiceDecision et ServicePortailParcours. Toute action invalide le sujet « parcours »
// (dossier, tableaux de bord de tous les acteurs, pilotage) : un geste d'un acteur se reflète chez les autres.

const P = [cles.parcours, cles.homologation]

// Lectures
export const useLiaisonsParcours = (f: FiltreLiaisons, actif = true) => useLecture(cles.parcoursLiaisons(f), (s) => s.parcours.listerLiaisons(f), { enabled: actif })
export const useConducteurs = (actif = true) => useLecture(cles.parcoursPersonnes('CONDUCTEUR'), (s) => s.parcours.conducteurs(), { enabled: actif })
export const usePersonnes = (fonction: Fonction, actif = true) => useLecture(cles.parcoursPersonnes(fonction), (s) => s.parcours.personnes(fonction), { enabled: actif })
export const useDossierLiaison = (id: string) => useLecture(cles.parcoursDossier(id), (s) => s.parcours.dossier(id), { enabled: Boolean(id) })
export const useOperationsParcours = (id: string, filtre?: { etape?: string; fonction?: Fonction }, actif = true) =>
  useLecture(cles.parcoursDossier(id, `operations:${filtre?.etape ?? ''}:${filtre?.fonction ?? ''}`), (s) => s.parcours.operations(id, filtre), { enabled: Boolean(id) && actif })
export const useTableauActeur = (fonctions: Fonction[], actif = true) =>
  useLecture(cles.parcoursActeur(fonctions), (s) => s.parcours.tableauActeur(fonctions), { enabled: actif })
/** D-98 : mes actions de certification, rafraîchies à chaque geste (sujet « parcours ») et par le canal poussé. */
export const useMesOperations = (actif = true) => useLecture([...cles.parcours, 'mes-operations'], (s) => s.parcours.mesOperations(), { enabled: actif })
export const usePilotage = (actif = true) => useLecture(cles.parcoursPilotage(), (s) => s.parcours.pilotage(), { enabled: actif })
export const useProcesVerbal = (id: string) => useLecture(cles.parcoursDossier(id, 'pv'), (s) => s.decision.procesVerbal(id), { enabled: Boolean(id) })
export const useApprobation = (id: string) => useLecture(cles.parcoursDossier(id, 'approbation'), (s) => s.decision.approbation(id), { enabled: Boolean(id) })
/** D-94 §6.5 : fonctions de l'utilisateur, servies par MS-HOMOLOGATION (plus aucune table rôle → fonction). */
export const useMesFonctions = (actif = true) => useLecture(['parcours', 'moi-fonctions'], (s) => s.parcours.mesFonctions(), { enabled: actif, staleTime: 300_000 })
export const useFicheSuivi = (id: string) => useLecture(cles.parcoursDossier(id, 'fiche-suivi'), (s) => s.decision.ficheSuivi(id), { enabled: Boolean(id) })
/** D-109 : chronologie réelle de la liaison, relue à chaque geste (sujet « parcours »). */
export const useChronologieParcours = (id: string, actif = true) => useLecture(cles.parcoursDossier(id, 'chronologie'), (s) => s.parcours.chronologie(id), { enabled: Boolean(id) && actif })
/** US-EXI-17 : résultats de la sandbox repris pour la liaison (lecture seule, MS-HOMOLOGATION). */
export const useResultatsSandbox = (id: string, actif = true) => useLecture(cles.parcoursDossier(id, 'sandbox'), (s) => s.parcours.resultatsSandbox(id), { enabled: Boolean(id) && actif })
/** US-EXI-17 : exécutions retenues d'une campagne reprise, pagination serveur. */
export const useExecutionsSandbox = (id: string, campagneId: string | undefined, page: number, taille: number) =>
  useLecture(cles.parcoursDossier(id, `sandbox:${campagneId ?? ''}:${page}:${taille}`), (s) => s.parcours.executionsSandbox(id, campagneId ?? '', page, taille), { enabled: Boolean(id && campagneId) })
/** D-109 : pièce d'une opération (empreinte inscrite, revérifiée par le service puis par le navigateur). */
export const useTelechargerPieceParcours = () => useEffet((s, v: { operationId: string; documentId: string }) => s.parcours.telechargerPiece(v.operationId, v.documentId), [])

// Étape 3 : cadrage
// L'ouverture fait passer l'institution adhérente en homologation (MS-PARTICIPANT) : le registre est relu aussi.
export const useOuvrirLiaison = () => useEffet((s, o: OuvertureLiaison) => s.parcours.ouvrirLiaison(o), [...P, cles.participant])
export const useDesignerConducteur = () =>
  useEffet((s, v: { liaisonId: string; conducteurId: string; motif: string }) => s.parcours.designerConducteur(v.liaisonId, v.conducteurId, v.motif), P)

// Étapes 4 à 10 : conduite et exécution
export const useAffecterRessources = () =>
  useEffet((s, v: { liaisonId: string; affectations: { fonction: Fonction; personneId: string }[] }) => s.parcours.affecterRessources(v.liaisonId, v.affectations), P)
export const useDeposerLivrable = () =>
  useEffet((s, v: { operationId: string; fichier: File; commentaire: string; livrable?: string; service?: string }) => s.parcours.deposerLivrable(v.operationId, v.fichier, v.commentaire, v.livrable, v.service), P)
/** D-109 : livrable sans objet, motivé (un service ou tous pour un livrable au grain du service). */
export const useDeclarerSansObjet = () =>
  useEffet((s, v: { operationId: string; livrable: string; motif: string; service?: string }) => s.parcours.declarerSansObjet(v.operationId, v.livrable, v.motif, v.service), P)
/** D-93 : renvoi motivé, signature (D-91) et affectation d'une fonction à affecter. */
export const useRenvoyerOperation = () => useEffet((s, v: { operationId: string; motif: string }) => s.parcours.renvoyer(v.operationId, v.motif), P)
export const useSignerOperation = () =>
  useEffet((s, v: { liaisonId: string; code: string; signature: string; assertion: Assertion }) => s.parcours.signerOperation(v.liaisonId, v.code, v.signature, v.assertion), P)
export const useAffecterFonction = () =>
  useEffet((s, v: { liaisonId: string; fonction: Fonction; personneId?: string; institutionId?: string; motif?: string }) =>
    s.parcours.affecterFonction(v.liaisonId, v.fonction, { ...(v.personneId ? { personneId: v.personneId } : {}), ...(v.institutionId ? { institutionId: v.institutionId } : {}), ...(v.motif ? { motif: v.motif } : {}) }), P)
export const useAttribuer = () =>
  useEffet((s, v: { operationId: string; qualite: 'R' | 'C' | 'V' | 'I' | 'E'; personneId: string; motif: string }) => s.parcours.attribuer(v.operationId, v.qualite, v.personneId, v.motif), P)
export const useDeposerPreuveParcours = () =>
  useEffet((s, v: { operationId: string; fichier: File; valeurReference?: string; codeControle?: string; ceremonie?: Ceremonie; livrable?: string; service?: string }) =>
    s.parcours.deposerPreuve(v.operationId, v.fichier, { valeurReference: v.valeurReference, codeControle: v.codeControle, ceremonie: v.ceremonie, ...(v.livrable ? { livrable: v.livrable } : {}), ...(v.service ? { service: v.service } : {}) }), P)
export const useValiderOperationParcours = () => useEffet((s, v: { operationId: string; assertion?: Assertion }) => s.parcours.validerOperation(v.operationId, v.assertion), P)
export const useDeclarerBlocage = () =>
  useEffet((s, v: { liaisonId: string; motif: string; prochaineAction: string; responsableId: string; echeanceLevee: string }) =>
    s.parcours.declarerBlocage(v.liaisonId, { motif: v.motif, prochaineAction: v.prochaineAction, responsableId: v.responsableId, echeanceLevee: v.echeanceLevee }), P)
export const useLeverBlocage = () => useEffet((s, v: { blocageId: string; commentaire: string }) => s.parcours.leverBlocage(v.blocageId, v.commentaire), P)
export const useConstaterCouverture = () => useEffet((s, liaisonId: string) => s.parcours.constaterCouverture(liaisonId), P)
export const useOuvrirServiceSupplementaire = () =>
  useEffet((s, v: { liaisonId: string; services: string[] }) => s.parcours.ouvrirServiceSupplementaire(v.liaisonId, v.services), P)

// Verrou 1 : procès-verbal de certification
export const useEtablirProcesVerbal = () => useEffet((s, liaisonId: string) => s.decision.etablirProcesVerbal(liaisonId), P)
export const useSignerProcesVerbalGimac = () => useEffet((s, v: { liaisonId: string; assertion: Assertion }) => s.decision.signerProcesVerbalGimac(v.liaisonId, v.assertion), P)

// Verrou 2 : approbation de la sécurité
export const useConstituerDossierChangement = () => useEffet((s, v: { liaisonId: string; fichier: File }) => s.decision.constituerDossierChangement(v.liaisonId, v.fichier), P)
export const useReferencerFicheChangement = () => useEffet((s, v: { liaisonId: string; reference: string }) => s.decision.referencerFicheChangement(v.liaisonId, v.reference), P)
export const useRelireFicheChangement = () => useEffet((s, liaisonId: string) => s.decision.relireFicheChangement(liaisonId), P)
export const useVerifierAttestation = () =>
  useEffet((s, v: { liaisonId: string; type: TypeAttestation; conforme: boolean; motif: string }) => s.decision.verifierAttestation(v.liaisonId, v.type, v.conforme, v.motif), P)
export const useArchiverAttestations = () => useEffet((s, liaisonId: string) => s.decision.archiverAttestations(liaisonId), P)
export const useSignerVisa = () =>
  useEffet((s, v: { liaisonId: string; nature: 'CONFORMITE' | 'DIRECTION_GENERALE'; favorable: boolean; motif: string; assertion: Assertion }) =>
    s.decision.signerVisa(v.liaisonId, v.nature, v.favorable, v.motif, v.assertion), P)

// Étape 10 : mise en service constatée
export const useRemettreFicheSuivi = () => useEffet((s, liaisonId: string) => s.decision.remettreFicheSuivi(liaisonId), P)
export const useConstaterMiseEnService = () => useEffet((s, liaisonId: string) => s.decision.constaterMiseEnService(liaisonId), P)

// Portail du participant (qualité Externe)
export const useMesLiaisonsPortail = () => useLecture(cles.parcoursPortail('liaisons'), (s) => s.portailParcours.mesLiaisons())
export const useDossierPortail = (id: string) => useLecture(cles.parcoursPortail('dossier', id), (s) => s.portailParcours.dossier(id), { enabled: Boolean(id) })
export const useMesTachesPortail = () => useLecture(cles.parcoursPortail('taches'), (s) => s.portailParcours.mesTaches())
export const useProcesVerbalPortail = (id: string) => useLecture(cles.parcoursPortail('pv', id), (s) => s.portailParcours.procesVerbal(id), { enabled: Boolean(id) })
export const useApprobationPortail = (id: string) => useLecture(cles.parcoursPortail('approbation', id), (s) => s.portailParcours.approbation(id), { enabled: Boolean(id) })
export const useFicheSuiviPortail = (id: string) => useLecture(cles.parcoursPortail('fiche-suivi', id), (s) => s.portailParcours.ficheSuivi(id), { enabled: Boolean(id) })
export const useCommuniquerCoordonnees = () =>
  useEffet((s, v: { liaisonId: string; adresse: string; port: string; pointsTerminaison?: string }) =>
    s.portailParcours.communiquerCoordonnees(v.liaisonId, { adresse: v.adresse, port: v.port, pointsTerminaison: v.pointsTerminaison }), P)
export const useDeposerLivrablePortail = () =>
  useEffet((s, v: { operationId: string; fichier: File; commentaire: string; livrable?: string; service?: string }) => s.portailParcours.deposerLivrable(v.operationId, v.fichier, v.commentaire, v.livrable, v.service), P)
export const useSignerProcesVerbalPortail = () => useEffet((s, v: { liaisonId: string; assertion: Assertion }) => s.portailParcours.signerProcesVerbal(v.liaisonId, v.assertion), P)
export const useDeposerAttestation = () =>
  useEffet((s, v: { liaisonId: string; type: TypeAttestation; fichier: File }) => s.portailParcours.deposerAttestation(v.liaisonId, v.type, v.fichier), P)
export const useRenseignerFicheSuivi = () =>
  useEffet((s, v: { liaisonId: string; lignes: { serviceCode: string; ouvert: boolean; commentaire: string | null }[] }) => s.portailParcours.renseignerFicheSuivi(v.liaisonId, v.lignes), P)
