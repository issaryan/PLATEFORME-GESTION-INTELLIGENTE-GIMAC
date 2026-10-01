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
export const useOperationsParcours = (id: string, filtre?: { etape?: string; fonction?: Fonction }) =>
  useLecture(cles.parcoursDossier(id, `operations:${filtre?.etape ?? ''}:${filtre?.fonction ?? ''}`), (s) => s.parcours.operations(id, filtre), { enabled: Boolean(id) })
export const useTableauActeur = (fonctions: Fonction[], actif = true) =>
  useLecture(cles.parcoursActeur(fonctions), (s) => s.parcours.tableauActeur(fonctions), { enabled: actif && fonctions.length > 0 })
export const usePilotage = (actif = true) => useLecture(cles.parcoursPilotage(), (s) => s.parcours.pilotage(), { enabled: actif })
export const useProcesVerbal = (id: string) => useLecture(cles.parcoursDossier(id, 'pv'), (s) => s.decision.procesVerbal(id), { enabled: Boolean(id) })
export const useApprobation = (id: string) => useLecture(cles.parcoursDossier(id, 'approbation'), (s) => s.decision.approbation(id), { enabled: Boolean(id) })
export const useFicheSuivi = (id: string) => useLecture(cles.parcoursDossier(id, 'fiche-suivi'), (s) => s.decision.ficheSuivi(id), { enabled: Boolean(id) })

// Étape 3 : cadrage
export const useOuvrirLiaison = () => useEffet((s, o: OuvertureLiaison) => s.parcours.ouvrirLiaison(o), P)
export const useDesignerConducteur = () =>
  useEffet((s, v: { liaisonId: string; conducteurId: string; motif: string }) => s.parcours.designerConducteur(v.liaisonId, v.conducteurId, v.motif), P)

// Étapes 4 à 10 : conduite et exécution
export const useAffecterRessources = () =>
  useEffet((s, v: { liaisonId: string; affectations: { fonction: Fonction; personneId: string }[] }) => s.parcours.affecterRessources(v.liaisonId, v.affectations), P)
export const useDeposerLivrable = () =>
  useEffet((s, v: { operationId: string; fichier: File; commentaire: string }) => s.parcours.deposerLivrable(v.operationId, v.fichier, v.commentaire), P)
export const useAttribuer = () =>
  useEffet((s, v: { operationId: string; qualite: 'R' | 'C' | 'V' | 'I' | 'E'; personneId: string; motif: string }) => s.parcours.attribuer(v.operationId, v.qualite, v.personneId, v.motif), P)
export const useDeposerPreuveParcours = () =>
  useEffet((s, v: { operationId: string; fichier: File; valeurReference?: string; codeControle?: string; ceremonie?: Ceremonie }) =>
    s.parcours.deposerPreuve(v.operationId, v.fichier, { valeurReference: v.valeurReference, codeControle: v.codeControle, ceremonie: v.ceremonie }), P)
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
  useEffet((s, v: { operationId: string; fichier: File; commentaire: string }) => s.portailParcours.deposerLivrable(v.operationId, v.fichier, v.commentaire), P)
export const useSignerProcesVerbalPortail = () => useEffet((s, v: { liaisonId: string; assertion: Assertion }) => s.portailParcours.signerProcesVerbal(v.liaisonId, v.assertion), P)
export const useDeposerAttestation = () =>
  useEffet((s, v: { liaisonId: string; type: TypeAttestation; fichier: File }) => s.portailParcours.deposerAttestation(v.liaisonId, v.type, v.fichier), P)
export const useRenseignerFicheSuivi = () =>
  useEffet((s, v: { liaisonId: string; lignes: { serviceCode: string; ouvert: boolean; commentaire: string | null }[] }) => s.portailParcours.renseignerFicheSuivi(v.liaisonId, v.lignes), P)
