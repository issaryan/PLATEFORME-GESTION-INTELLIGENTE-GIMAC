import { ApiError, ReauthCancelled } from '@gimac/socle'
import type { ListQuery, Page } from '../../modele/commun'
import type { ClotureOperation, ElementCloture, Liaison, OuvertureLiaison } from '../../modele/homologation'
import type { PortailLiaison } from '../../modele/portail'
import type { Fonction } from '../../modele/parcours'
import type { ServiceCertification, ServiceHomologation, ServicePortail, Services } from '../ports'
import * as D from './donnees'
import { participantSimule, type TransitionObservee } from './participant'
import { PERM, PROFILS } from '../../permissions'
import { messagerieSimulee, type MessagerieSimulee } from './messagerie'
import { parcoursSimule, type EvenementsParcoursDemo } from './parcours'
import { ORGANISATION_DEMO, pilotageSimule, type PontTachesProcessus } from './pilotage'
import { notificationsProcessusDemo, processusSimule } from './processus'
import { magasinPouvoirs, pouvoirsSimules, type MagasinPouvoirs } from './pouvoirs'
import { apercuSimule } from './apercu'
import { referentielsSimules } from './referentiels'

// Implémentation simulée des ports, pour développer les écrans sans la passerelle : un serveur fictif en
// mémoire, avec latence, identifiants de corrélation et refus normalisés comme ceux des services réels.
// Les « règles » ici ne sont que celles d'un serveur de substitution ; l'interface n'en porte aucune.

export type OptionsSimulation = {
  latenceMs?: number
  echecs?: boolean
  /** Utilisateur de la démonstration, membre des conversations simulées (messagerie, D-64) et auteur de « Mon activité » (D-76). */
  utilisateur?: { id: string; nom: string; fonction?: string | null; profils?: string[] }
  /**
   * Magasin de MS-IAM simulé pour les pouvoirs (D-81) : partagé avec l'administration simulée de la même page
   * (matrice, demandes à traiter) ; un magasin propre à défaut.
   */
  pouvoirs?: MagasinPouvoirs
}

/**
 * Démonstration continue (D-98) : avec gm.demoContinue = 1 (stockage local), le parcours et le registre simulés gardent
 * leur état d'un chargement à l'autre, pour enchaîner les acteurs (changement de profil) sur une même liaison. Sans ce
 * réglage, chaque chargement repart du jeu initial.
 */
function persistanceDemo(cle: string): { lire(): string | null; ecrire(valeur: string): void } | undefined {
  try {
    if (typeof localStorage === 'undefined' || localStorage.getItem('gm.demoContinue') !== '1') return undefined
  } catch {
    return undefined
  }
  return { lire: () => localStorage.getItem(`gm.demo.${cle}`), ecrire: (v) => localStorage.setItem(`gm.demo.${cle}`, v) }
}

let compteur = 0
const correlation = () => `sim-${Date.now().toString(36)}-${(++compteur).toString(36)}`
const clone = <T>(v: T): T => structuredClone(v)
const maintenant = () => new Date().toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).replace(',', '')
const refus = (status: number, code: string, message: string, details?: Record<string, unknown>) => new ApiError(status, code, message, correlation(), details)

/** Statuts d'une institution nommés dans les tâches de décision (données du service, en français comme MS-PILOTAGE). */
const LIBELLES_STATUT: Record<string, string> = {
  CANDIDAT: 'Candidat', ADHERENT: 'Adhérent', EN_HOMOLOGATION: 'En homologation', ACTIF: 'Actif', A_REGULARISER: 'À régulariser', SUSPENDU: 'Suspendu', RETIRE: 'Retiré', CLOS_SANS_SUITE: 'Clos sans suite',
}

function pagine<T>(lignes: T[], q: ListQuery, champs: (keyof T)[]): Page<T> {
  const r = (q.recherche ?? '').toLowerCase()
  let l = r ? lignes.filter((x) => champs.some((c) => String(x[c] ?? '').toLowerCase().includes(r))) : lignes
  Object.entries(q.filtres ?? {}).forEach(([k, v]) => {
    if (v !== undefined && v !== '') l = l.filter((x) => String((x as Record<string, unknown>)[k]) === String(v))
  })
  if (q.tri) {
    const [cle, sens] = q.tri.split(',')
    l = [...l].sort((a, b) => String((a as Record<string, unknown>)[cle]).localeCompare(String((b as Record<string, unknown>)[cle]), 'fr', { numeric: true }) * (sens === 'desc' ? -1 : 1))
  }
  const debut = (q.page - 1) * q.taille
  return { contenu: clone(l.slice(debut, debut + q.taille)), total: l.length }
}

/** Services simulés : les ports, plus la réception de messages de démonstration (notifications, D-80). */
/** Offre d'une décision à prononcer (D-98) visible de l'utilisateur de la démonstration : notifiée avec son lien profond. */
export type DecisionDemo = { tacheId: string; titre: string; lien: string; createur: { id: string; nomAffiche: string }; /** Instant de la proposition (DDR-15). */ le?: string }
/**
 * Proposition de l'utilisateur prononcée ou refusée (DDR-08) : MS-NOTIFICATION la lui notifie (participant.transition-approved.v1
 * définitive ou transition-rejected.v1), avec le motif du refus et le lien des propositions de statut.
 */
export type DecisionTrancheeDemo = {
  transitionId: string; institutionId: string; institutionNom: string; vers: string; decision: 'APPROUVEE' | 'REJETEE'; motif: string | null; lien: string; le: string
  acteur: { id: string; nomAffiche: string } | null
}
/** Décisions de la démonstration (MS-NOTIFICATION simulé : TACHE vers l'écran de la décision, DEMANDE pour le proposant). */
export type DecisionsDemo = {
  aPrononcer(): DecisionDemo[]; surOffre(f: (d: DecisionDemo) => void): void
  tranchees(): DecisionTrancheeDemo[]; surTranchee(f: (d: DecisionTrancheeDemo) => void): void
}
export type ServicesSimules = Services & {
  messagerie: MessagerieSimulee; decisions: DecisionsDemo; evenementsParcours: EvenementsParcoursDemo
  /** DDR-20 : tâche ouverte confiée à l'utilisateur par un autre (notification de démonstration vers cette tâche). */
  activiteDemo: { tacheConfiee(): { id: string; titre: string; createur: { id: string; nomAffiche: string } } | null }
  /** QUO-05 : notifications des étapes et de la clôture des processus (MS-NOTIFICATION simulé). */
  processusDemo: ReturnType<typeof notificationsProcessusDemo>
}

export function servicesSimules(o: OptionsSimulation = {}): ServicesSimules {
  const latence = o.latenceMs ?? 350
  // État propre à chaque instance : les liaisons ouvertes ou modifiées ne fuient pas d'une instance à l'autre.
  const liaisons: Liaison[] = clone(D.liaisons)
  const attendre = <T>(v: T | (() => T)): Promise<T> =>
    new Promise((res, rej) =>
      window.setTimeout(() => {
        try {
          res(clone(typeof v === 'function' ? (v as () => T)() : v))
        } catch (e) {
          rej(e)
        }
      }, latence),
    )
  const trouverLiaison = (id: string): Liaison => {
    const l = liaisons.find((x) => x.id === id)
    if (!l) throw refus(404, 'HOM_LIAISON_NOT_FOUND', 'Liaison introuvable.')
    return l
  }
  // État mutable des opérations par liaison, dérivé une fois du jeu de données.
  const operations = new Map(liaisons.map((l) => [l.id, D.operationsDe(l)]))
  const toutesOperations = () => [...operations.values()].flat()
  const trouverOperation = (id: string) => {
    const op = toutesOperations().find((x) => x.id === id)
    if (!op) throw refus(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable.')
    return op
  }
  const depots = new Map<string, ClotureOperation['dernierDepot']>()
  // DDR-01 : l'utilisateur de la démonstration est l'acteur des écritures (proposant, approbateur), comme le sujet du jeton.
  const participant = participantSimule(attendre, refus, persistanceDemo('participant'), o.utilisateur ? { id: o.utilisateur.id, nom: o.utilisateur.nom } : undefined)
  // Référentiels et modélisateur (D-77) : état propre à l'instance.
  const referentiels = referentielsSimules(attendre, refus, {
    utilisateur: o.utilisateur?.id, profils: o.utilisateur?.profils, maintenant, liaisonsActives: (versionId) => liaisons.filter((l) => l.versionId === versionId && l.etat !== 'CLOSE').length,
    liaisonsDuType: (type) => liaisons.filter((l) => l.typeProjet === type).length,
  })
  let numeroLiaison = 40

  /** « Mes projets » : liaisons dont le conducteur de projet est l'utilisateur de la démonstration. */
  const listerLiaisons = (q: ListQuery) => {
    const { mesProjets, ...filtres } = q.filtres ?? {}
    const lignes = mesProjets ? liaisons.filter((l) => l.chefDeProjetId === D.CONDUCTEUR_DEMO) : liaisons
    return pagine(lignes, { ...q, filtres }, ['institutionNom', 'id', 'chefDeProjetNom'])
  }
  const conducteur = (id: string) => {
    const c = D.CONDUCTEURS.find((x) => x.id === id)
    if (!c) throw refus(409, 'HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED', 'Le collaborateur désigné ne détient pas le profil de conducteur de projet.')
    return c
  }
  /** Ouverture au cadrage (OpenLiaisonService) : éligibilité, unicité, habilitations, référentiel publié. */
  const ouvrir = (e: OuvertureLiaison): Liaison => {
    const inst = participant.pont.institution(e.institutionId)
    if (!inst || (inst.statut !== 'ADHERENT' && inst.statut !== 'EN_HOMOLOGATION')) {
      throw new ApiError(409, 'HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', "L'institution n'est ni adhérente ni en homologation.", correlation(), { statut: inst?.statut ?? null })
    }
    if (liaisons.some((l) => l.institutionId === e.institutionId && l.typeProjet === e.typeProjet && l.etat !== 'CLOSE')) {
      throw refus(409, 'HOM_LIAISON_DUPLICATE', 'Une liaison est déjà ouverte pour cette institution et ce type de projet.')
    }
    const chef = conducteur(e.chefDeProjetId)
    const banque = e.banquePartenaireId ? participant.pont.institution(e.banquePartenaireId) : undefined
    if (e.banquePartenaireId && banque?.categorie !== 'BANQUE') throw refus(409, 'HOM_LIAISON_CARRIER_NOT_BANK', 'Le porteur désigné n\'est pas une banque.')
    const version = referentiels.versions.find((r) => r.typeProjet === e.typeProjet && r.etat === 'PUBLIEE')
    if (!version) throw refus(409, 'HOM_REFERENTIAL_NONE_PUBLISHED', 'Aucun référentiel publié pour ce type de projet.')
    const n = String(++numeroLiaison).padStart(3, '0')
    const base: Liaison = {
      id: `LIA-2026-${n}`, typeProjet: e.typeProjet, institutionId: inst.id, institutionNom: inst.nom, etat: 'OUVERTE', chefDeProjetId: chef.id, chefDeProjetNom: chef.nom,
      banquePartenaireId: banque?.id ?? null, banquePartenaireNom: banque?.nom ?? null, dateOuverture: maintenant().slice(0, 10), motifEtat: null,
      versionId: version.id, versionNumero: version.numero ?? 1, phaseCourante: 'CADRAGE', couvertureCertification: D.cov(0, 3), couvertureMiseEnService: D.cov(0, 2),
      operationsEnRetard: 0, etatsParcourus: [], transitionsPermises: ['EN_HOMOLOGATION'], actionsPermises: ['DESIGNER_CHEF_DE_PROJET'], perimetre: D.PERIMETRES[e.typeProjet] ?? ['TRANSVERSE'],
    }
    const l = { ...base, intervenants: D.intervenantsDe(base) }
    liaisons.push(l)
    operations.set(l.id, D.operationsDe(l))
    // homologation.liaison-opened.v1, consommé de façon asynchrone par MS-PARTICIPANT.
    window.setTimeout(() => participant.pont.liaisonOuverte(inst.id, l.id), latence)
    return l
  }

  const homologation: ServiceHomologation = {
    ...referentiels.port,
    simulerMigration: () => attendre([
      { liaisonId: 'LIA-2026-017', institutionNom: 'Établissement de paiement A', conservees: 3, ajoutees: 1, retirees: 1, modifiees: 0, exclue: false, motifExclusion: null },
      { liaisonId: 'LIA-2025-102', institutionNom: 'Banque B', conservees: 4, ajoutees: 0, retirees: 0, modifiees: 0, exclue: true, motifExclusion: 'circuit de quitus ouvert' },
    ]),
    migrerLiaisons: () => attendre(undefined),
    historiqueReferentiel: (id) => attendre([{ event: `Version ${id} créée`, at: '22/09/2026 11:02', actor: 'Ingénieur d\'homologation', correlationId: 'RQ-2026-0922-4' }]),

    listerLiaisons: (q) => attendre(() => listerLiaisons(q)),
    tableauDeBordLiaisons: (q) => attendre(() => pagine(liaisons.map((l, i) => ({ ...l, delaiConsomme: [34, 61, 48, 5][i] ?? 0, delaiStandard: 60 })), q, ['institutionNom'])),
    lireLiaison: (id) => attendre(() => trouverLiaison(id)),
    historiqueLiaison: (id) => attendre([
      { event: 'Liaison ouverte', at: '03/03/2026 09:12', actor: 'Chef du DES', correlationId: 'RQ-2026-0303-2' },
      { event: 'Opérations instanciées depuis le référentiel version 3', at: '03/03/2026 09:12', actor: 'Système' },
      ...(id === 'LIA-2026-021' ? [{ event: 'Liaison gelée : institution suspendue', at: '21/09/2026 17:40', actor: 'Système', correlationId: 'RQ-2026-0921-9' }] : []),
    ]),
    ouvrirLiaison: (e) => attendre(() => ouvrir(e)),
    designerChefDeProjet: (id, chef) => attendre(() => {
      const l = trouverLiaison(id)
      if (!l.actionsPermises.includes('DESIGNER_CHEF_DE_PROJET')) throw refus(409, 'HOM_LIAISON_STATE', 'La liaison ne permet plus de désigner son conducteur de projet.')
      const c = conducteur(chef)
      Object.assign(l, { chefDeProjetId: c.id, chefDeProjetNom: c.nom })
      l.intervenants = D.intervenantsDe(l)
      return l
    }),
    conducteurs: () => attendre(D.CONDUCTEURS),
    degelerLiaison: (id) => attendre(() => {
      const l = trouverLiaison(id)
      if (!l.actionsPermises.includes('DEGELER')) throw refus(409, 'HOM_LIAISON_NOT_FROZEN', 'La liaison n\'est pas gelée.')
      Object.assign(l, { etat: 'EN_HOMOLOGATION', motifEtat: null, actionsPermises: ['DESIGNER_CHEF_DE_PROJET'] })
    }),
    exporterDossierDePreuves: () => attendre(undefined),
    dernierExportPreuves: (id) => attendre(id === 'LIA-2025-102' ? { id: 'EXP-2026-0019', etat: 'SCELLE', dateExport: '24/09/2026 18:02', empreinteManifeste: D.empreinte('manifeste-102') } : null),
    grapheLiaison: (id) => attendre(() => {
      trouverLiaison(id)
      const g = D.grapheDe(id)
      const etats = new Map((operations.get(id) ?? []).map((x) => [x.id, x.etat]))
      return { ...g, noeuds: g.noeuds.map((n) => ({ ...n, etat: etats.get(n.id) ?? n.etat })) }
    }),
    couverturesLiaison: (id) => attendre(() => D.couverturesDe(trouverLiaison(id))),

    listerOperations: (liaisonId, f) => attendre(() => (operations.get(liaisonId) ?? []).filter((x) => !f?.enRetard || x.joursDepassement > 0)),
    lireOperation: (id) => attendre(() => trouverOperation(id)),
    preuvesOperation: (id) => attendre(() => {
      const op = trouverOperation(id)
      return op.etat === 'ELIGIBLE' || op.etat === 'VERROUILLEE' ? [] : [
        { documentRef: `DOC-2026-${id.slice(-3)}1`, version: 2, typePiece: 'Rapport de tests signé', empreinte: D.empreinte(id + 'v2'), dateDepot: '19/09/2026 11:20', scelle: true, valide: true },
        { documentRef: `DOC-2026-${id.slice(-3)}0`, version: 1, typePiece: 'Rapport de tests signé', empreinte: D.empreinte(id + 'v1'), dateDepot: '12/09/2026 15:02', scelle: true, valide: false },
      ]
    }),
    attributionsOperation: () => attendre([{ event: 'Responsable désigné : Joël Ekotto Mballa', at: '03/03/2026 09:15', actor: 'Chef du DES', correlationId: 'RQ-2026-0303-5' }]),
    grilleOperation: () => attendre([
      { code: 'G1', libelle: 'Les journaux de tests couvrent tous les scénarios attendus', reponse: 'CONFORME' as const, nonConformiteSignalee: false },
      { code: 'G2', libelle: 'Les écarts relevés sont justifiés et acceptés', reponse: null, nonConformiteSignalee: false },
    ]),
    renseignerGrille: () => attendre(undefined),
    attribuerOperation: (id, responsableId) => attendre(() => {
      const op = trouverOperation(id)
      const c = D.COLLABORATEURS.find((x) => x.id === responsableId)
      if (c) Object.assign(op, { responsableId: c.id, responsableNom: c.nom })
    }),
    rouvrirOperation: (id) => attendre(() => {
      Object.assign(trouverOperation(id), { etat: 'ROUVERTE', actionsPermises: ['DEPOSER_PREUVE', 'ATTRIBUER'] })
    }),
    elementsDeCloture: (id) => attendre((): ClotureOperation => {
      const op = trouverOperation(id)
      const depot = depots.get(id) ?? (op.etat === 'PREUVE_DEPOSEE' ? { documentRef: 'DOC-2026-7B12', empreinte: D.empreinte(id + 'v2'), version: 2, scelle: true, controle: { resultat: 'ACCEPTEE' as const } } : null)
      const clos = op.etat === 'CLOSE' || op.etat === 'VALIDEE_HORODATAGE_EN_ATTENTE'
      const el = (key: ElementCloture['key'], etat: ElementCloture['etat'], detail?: string, action?: string): ElementCloture => ({ key, etat, detail, actionManquante: etat === 'REUNI' || !action ? undefined : { labelKey: action } })
      return {
        id: op.id, liaisonId: op.liaisonId, code: op.code, libelle: op.libelle, etat: op.etat, preuveAttendue: 'Rapport de tests signé',
        dernierDepot: depot,
        elements: [
          el('responsable', op.responsableNom ? 'REUNI' : 'MANQUANT', op.responsableNom ?? undefined, 'operation.action.attribuer'),
          el('preuve', depot ? 'REUNI' : 'MANQUANT', depot ? `${depot.documentRef}, version ${depot.version}` : undefined, 'cloture.action.deposer'),
          el('horodatage', clos ? (op.etat === 'CLOSE' ? 'REUNI' : 'EN_ATTENTE') : depot ? 'EN_ATTENTE' : 'MANQUANT', undefined, 'cloture.horodatage.attendu'),
          el('validation', clos ? 'REUNI' : 'MANQUANT', undefined, 'cloture.action.valider'),
        ],
        empreinteAValider: depot ? D.empreinte(id + 'cloture') : null,
        restrictionValidation: null,
        transitionsPermises: [],
        actionsPermises: clos ? [] : depot ? ['DEPOSER_PREUVE', 'VALIDER'] : ['DEPOSER_PREUVE'],
      }
    }),
    deposerPreuve: (id, fichier, typePiece) => attendre(() => {
      if (/carte|pan/i.test(fichier.name)) throw refus(422, 'HOM_EVIDENCE_REJECTED', 'la pièce contient un numéro de carte non masqué')
      const op = trouverOperation(id)
      const r = { documentRef: `DOC-2026-${(Date.now() % 10000).toString(16).toUpperCase()}`, empreinte: D.empreinte(fichier.name + fichier.size + typePiece), version: 1, scelle: true, controle: { resultat: 'ACCEPTEE' as const } }
      depots.set(id, r)
      Object.assign(op, { etat: 'PREUVE_DEPOSEE', actionsPermises: ['VALIDER', 'DEPOSER_PREUVE', 'RENSEIGNER_GRILLE'] })
      return r
    }),
    validerOperation: (id, _e, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      Object.assign(trouverOperation(id), { etat: 'VALIDEE_HORODATAGE_EN_ATTENTE', actionsPermises: [] })
      // Horodatage RFC 3161 obtenu de façon asynchrone par le service, comme en recette.
      window.setTimeout(() => Object.assign(trouverOperation(id), { etat: 'CLOSE', actionsPermises: ['ROUVRIR'] }), 4000)
    }),

    campagnesLiaison: (liaisonId) => attendre(D.campagnesDe(liaisonId)),
    scenariosInconnus: (liaisonId) => attendre(D.scenariosInconnusDe(liaisonId)),
    qualifierScenario: () => attendre(undefined),
    etatReprise: () => attendre({ etatDisjoncteur: 'FERME' as const, depuis: null }),

    rechercherCollaborateurs: (r) => attendre(D.COLLABORATEURS.filter((c) => c.nom.toLowerCase().includes(r.toLowerCase())).map((c) => ({ value: c.id, label: c.nom, hint: `role.${c.role}` }))),
  }

  const pdf = (texte: string) => new Blob([texte], { type: 'application/pdf' })
  const certification: ServiceCertification = {
    dossierParLiaison: (liaisonId) => attendre(D.dossier.liaisonId === liaisonId ? D.dossier : null),
    lireDossier: (id) => attendre(() => {
      if (id !== D.dossier.id) throw refus(404, 'CER_DOSSIER_NOT_FOUND', 'Dossier introuvable.')
      return D.dossier
    }),
    historiqueDossier: () => attendre([{ event: 'Dossier ouvert à la déclaration d\'aptitude', at: '24/09/2026 18:05', actor: 'Système', correlationId: 'RQ-2026-0924-7' }]),
    circuit: () => attendre(D.circuit),
    signerQuitus: (_c, nature, decision, assertion) => attendre(() => {
      if (!assertion) throw refus(401, 'IAM_REAUTH_REQUIRED', 'Réauthentification exigée.')
      if (decision.socleEmpreinte !== D.circuit.socle.empreinte) throw refus(409, 'CER_SOCLE_PERIME', 'Le socle d\'évaluation a changé : relisez-le avant de signer.')
      const q = D.circuit.quitus.find((x) => x.nature === nature)
      if (q) Object.assign(q, { etat: decision.decisionFavorable ? 'SIGNE_FAVORABLE' : 'SIGNE_DEFAVORABLE', dateSignature: maintenant(), dateHorodatage: maintenant(), motif: decision.motif, actionsPermises: [] })
      if (D.circuit.quitus.filter((x) => x.nature !== 'DIRECTION_GENERALE').every((x) => x.etat === 'SIGNE_FAVORABLE')) {
        const dg = D.circuit.quitus.find((x) => x.nature === 'DIRECTION_GENERALE')
        if (dg && dg.etat === 'ATTENDU') dg.actionsPermises = ['SIGNER']
      }
    }),
    conditions: () => attendre(D.conditions),
    verifierDossier: () => attendre(() => {
      D.conditions.forEach((c) => (c.dateVerification = c.satisfaite ? maintenant() : c.dateVerification))
    }),
    ficheDeChangement: () => attendre(D.fiche),
    lierFicheDeChangement: (_d, reference) => attendre(() => {
      Object.assign(D.fiche, { reference, dateLecture: maintenant(), aRelire: false })
      D.dossier.referenceFicheChangement = reference
    }),
    procesVerbal: () => attendre(null),
    deposerProcesVerbal: () => attendre(undefined),
    horodatages: () => attendre(D.horodatagesDossier),
    emettreAttestation: () => attendre(() => {
      throw refus(409, 'CER_DOSSIER_INCOMPLETE', 'Le dossier n\'est pas complet : deux conditions restent à satisfaire.')
    }),
    lireAttestation: (numero) => attendre(() => {
      const a = D.attestations.find((x) => x.numero === numero)
      if (!a) throw refus(404, 'CER_ATTESTATION_NOT_FOUND', 'Attestation introuvable.')
      return a
    }),
    telechargerAttestation: (numero) => attendre({ blob: pdf(`Attestation ${numero}`), empreinte: D.attestations.find((x) => x.numero === numero)?.empreinteDocument ?? null }),
    demanderStatut: () => attendre(undefined),
    approuverStatut: () => attendre(undefined),
    registre: (q) => attendre(() => pagine(D.registre, q, ['numero', 'institutionNom'])),
    exporterRegistre: () => attendre({ blob: new Blob(['numero;institution\n'], { type: 'text/csv' }), empreinte: null }),
  }

  const projectionPortail = (l: Liaison): PortailLiaison => ({
    id: l.id, typeProjet: l.typeProjet, etat: l.etat, etatsParcourus: l.etatsParcourus, phaseCourante: l.phaseCourante, versionNumero: l.versionNumero,
    couvertureCertification: l.couvertureCertification, couvertureMiseEnService: l.couvertureMiseEnService, services: D.couverturesDe(l),
    graphe: D.grapheDe(l.id), resultats: D.campagnesDe(l.id), transitionsPermises: [], actionsPermises: [],
  })
  // Le portail est cloisonné à l'institution du jeton : ici, « Établissement de paiement A ».
  const INSTITUTION_PORTAIL = 'INST-0042'
  const portail: ServicePortail = {
    mesLiaisons: () => attendre(() => liaisons.filter((l) => l.institutionId === INSTITUTION_PORTAIL).map(projectionPortail)),
    maLiaison: (id) => attendre(() => {
      const l = liaisons.find((x) => x.id === id && x.institutionId === INSTITUTION_PORTAIL)
      if (!l) throw refus(404, 'HOM_LIAISON_NOT_FOUND', 'Ce parcours est introuvable.')
      return projectionPortail(l)
    }),
    aFaire: (liaisonId) => attendre(() => (operations.get(liaisonId) ?? []).filter((x) => x.partie === 'PARTICIPANT' && x.etat !== 'CLOSE').map((x) => ({
      id: x.id, liaisonId, code: x.code, libelle: x.libelle, etat: x.etat, echeance: x.echeance, joursRestants: x.joursDepassement ? -x.joursDepassement : 12,
      preuveAttendue: 'Rapport de tests signé', accuse: depots.get(x.id) ? { empreinte: depots.get(x.id)!.empreinte, dateDepot: maintenant() } : null,
      transitionsPermises: [], actionsPermises: ['DEPOSER_PREUVE'],
    }))),
    operationAFaire: (liaisonId, id) => portail.aFaire(liaisonId).then((l) => {
      const x = l.find((o) => o.id === id)
      if (!x) throw refus(404, 'HOM_OPERATION_NOT_FOUND', 'Opération introuvable.')
      return x
    }),
    deposerPreuve: (id, fichier, typePiece) => homologation.deposerPreuve(id, fichier, typePiece).then(() => undefined),
    certifications: () => attendre([{ id: 'CERT-0042', liaisonId: 'LIA-2026-017', etatDossier: 'OUVERT', motifCommunicable: null, attestation: { numero: 'ATT-2026-0007', etat: 'EN_VIGUEUR', empreinteDocument: D.attestations[0].empreinteDocument } }]),
    telechargerAttestation: () => certification.telechargerAttestation('ATT-2026-0007'),
  }

  const magasin = o.pouvoirs ?? magasinPouvoirs()
  if (o.utilisateur?.profils) magasin.definirProfils(o.utilisateur.id, o.utilisateur.profils)
  const { parcours, decision, portailParcours, evenements } = parcoursSimule(attendre, INSTITUTION_PORTAIL, (id) => participant.dossier(id).then((d) => d.institution.nom, () => undefined),
    {
      profils: o.utilisateur?.profils, fonctions: () => referentiels.fonctions,
      // D-98 : instanciation depuis la version publiée du type, et événements consommés par MS-PARTICIPANT.
      versionPubliee: referentiels.versionPubliee,
      surLiaisonOuverte: (institutionId, liaisonId) => participant.pont.liaisonOuverte(institutionId, liaisonId),
      surJalon: (institutionId, jalon, liaisonId) => { if (jalon === 'EN_PRODUCTION') participant.pont.enProduction(institutionId, liaisonId) },
      persistance: persistanceDemo('parcours'),
      // DDR-05 : auteur réel des dépôts, supervision admise sans être offerte ; DDR-24 : version publiée ; DDR-15 : dates amont.
      utilisateur: o.utilisateur ? { id: o.utilisateur.id, nom: o.utilisateur.nom } : undefined,
      permissions: (o.utilisateur?.profils ?? []).flatMap((p) => PROFILS[p] ?? []),
      numeroPublie: (type) => referentiels.versions.find((v) => v.typeProjet === type && v.etat === 'PUBLIEE')?.numero ?? null,
      datesAmont: (id) => participant.dossier(id).then((d) => ({ contact: d.institution.datePriseDeContact ?? null, adhesion: d.adhesion.dateAdhesion ?? null }), () => undefined),
      // HOM-04 : après chaque geste, les fonctions à affecter sont offertes en tâches à prendre (MS-PILOTAGE simulé).
      apresEffet: () => offrirFonctions(),
      // CER-05 : libellé servi du type (catalogue des types de projet).
      libelleType: (type) => referentiels.types().find((x) => x.code === type)?.libelle ?? null,
    })

  // Tâches d'origine PROCESSUS (D-87) : le moteur simulé des processus les crée dans « Mon activité ».
  let pont: PontTachesProcessus | undefined
  const pilotage = pilotageSimule(attendre, refus, o.utilisateur ? { id: o.utilisateur.id, nom: o.utilisateur.nom } : undefined, magasin, (p) => { pont = p }, persistanceDemo('pilotage'))

  // HOM-04 (D-93 §8.8) : une fonction interne à affecter devient une tâche à prendre par les porteurs de ses permissions,
  // terminée quand elle est pourvue ; sa prise (pilotage.task-taken.v1) affecte la personne à la fonction sur la liaison.
  const offresFonctions = new Map<string, { liaisonId: string; fonction: Fonction }>()
  function offrirFonctions() {
    if (!pont || !evenements.fonctionsAPrendre) return
    const offres = evenements.fonctionsAPrendre()
    offres.forEach((x) => {
      const tid = pont!.offrir({ titre: x.titre, description: x.description, createur: 'U-001', permissionsRequises: x.permissions, origine: { type: 'OPERATION', reference: x.reference, service: 'homologation', lien: x.lien } })
      offresFonctions.set(tid, { liaisonId: x.liaisonId, fonction: x.fonction })
    })
    // Fonction pourvue entre-temps (affectation par le conducteur) : la tâche à prendre est terminée.
    ;[...offresFonctions.entries()].filter(([, v]) => !offres.some((x) => x.liaisonId === v.liaisonId && x.fonction === v.fonction)).forEach(([tid, v]) => {
      pont!.terminerOrigine(`${v.liaisonId}/AFFECTER/${v.fonction}`)
      offresFonctions.delete(tid)
    })
  }
  offrirFonctions()

  // D-98 : tâche de l'approbateur d'une transition (participant.transition-* consommés par MS-PILOTAGE simulé) : à prendre
  // par les porteurs de la permission d'approbation, close quand la transition est prononcée ou refusée ; notifiée vers
  // l'onglet des transitions de l'institution.
  const moiId = o.utilisateur?.id ?? 'U-001'
  const approbateur = () => magasin.profilsDe(moiId).flatMap((p) => PROFILS[p] ?? []).includes(PERM.transitionApprouver)
  const offertes: DecisionDemo[] = []
  const abonnes: ((d: DecisionDemo) => void)[] = []
  const abonnesTranchees: ((d: DecisionTrancheeDemo) => void)[] = []
  const nomDe = (id: string | null) => (id ? ORGANISATION_DEMO.find((m) => m.id === id)?.nomAffiche ?? id : null)
  const tranchee = (e: Pick<TransitionObservee, 'institutionId' | 'institutionNom' | 'transition'>): DecisionTrancheeDemo => {
    const t = e.transition
    return {
      transitionId: t.id, institutionId: e.institutionId, institutionNom: e.institutionNom, vers: t.vers, decision: t.etat === 'REJETEE' ? 'REJETEE' : 'APPROUVEE', motif: t.motifRejet,
      lien: `/des/institutions/${e.institutionId}?onglet=transitions`, le: t.decideeLe ?? new Date().toISOString(),
      acteur: t.approbateurId ? { id: t.approbateurId, nomAffiche: nomDe(t.approbateurId) ?? t.approbateurId } : null,
    }
  }
  const relaisTransition = (e: TransitionObservee) => {
    if (!pont) return
    const t = e.transition
    const reference = `${e.institutionId}/TRANSITION/${t.id}`
    if (e.type !== 'PROPOSEE') {
      pont.terminerOrigine(reference)
      // DDR-08 : le proposant est avisé de la décision (jamais l'approbateur, qui agit).
      if (t.demandeurId === moiId && t.approbateurId !== moiId) abonnesTranchees.forEach((f) => f(tranchee(e)))
      return
    }
    const lien = `/des/institutions/${e.institutionId}?onglet=transitions`
    // DDR-14, DDR-16 : statuts nommés et onglet « Propositions de statut », comme ApprobationsTransitionsService de MS-PILOTAGE.
    const titre = t.vers === 'ADHERENT' ? `Prononcer l’adhésion : ${e.institutionNom}` : `Statuer sur le passage de ${LIBELLES_STATUT[t.de]} à ${LIBELLES_STATUT[t.vers]} : ${e.institutionNom}`
    const tacheId = pont.offrir({
      titre, createur: t.demandeurId, permissionsRequises: [PERM.transitionApprouver], le: t.demandeeLe,
      description: `Proposition de passage de ${LIBELLES_STATUT[t.de]} à ${LIBELLES_STATUT[t.vers]}. Motif : ${t.motif}. Prononcer ou refuser depuis l’onglet Propositions de statut de la fiche de l’institution ; l’auteur de la proposition ne peut pas la prononcer.`,
      origine: { type: 'DECISION', reference, service: 'participant', lien },
    })
    if (!approbateur() || t.demandeurId === moiId || offertes.some((d) => d.tacheId === tacheId)) return
    const d = { tacheId, titre, lien, le: t.demandeeLe, createur: { id: t.demandeurId, nomAffiche: nomDe(t.demandeurId) ?? t.demandeurId } }
    offertes.push(d)
    abonnes.forEach((f) => f(d))
  }
  participant.pont.propositions().forEach(relaisTransition)
  participant.pont.surTransition = relaisTransition
  // DDR-08 : décisions récentes (sept jours) sur les propositions de l'utilisateur, prises dans une autre session de la démonstration.
  const depuis = Date.now() - 7 * 86_400_000
  const decisions: DecisionsDemo = {
    aPrononcer: () => [...offertes], surOffre: (f) => { abonnes.push(f) },
    tranchees: () => participant.pont.tranchees().filter((e) => e.transition.demandeurId === moiId && e.transition.approbateurId !== moiId && Date.parse(e.transition.decideeLe ?? '') >= depuis).map(tranchee),
    surTranchee: (f) => { abonnesTranchees.push(f) },
  }

  const processus = processusSimule(attendre, refus, o.utilisateur?.id ?? 'U-001', magasin, pont)
  // HOM-04 : la prise d'une tâche de fonction à affecter (Mon activité, Tâches à prendre) affecte la personne sur la liaison.
  if (pont) {
    const p = pont
    const avant = p.surPrise
    p.surPrise = (tid, qui) => {
      avant?.(tid, qui)
      const v = offresFonctions.get(tid)
      if (!v) return
      evenements.prendreFonction?.(v.liaisonId, v.fonction, { id: qui.id, nom: qui.nomAffiche })
      offresFonctions.delete(tid)
      p.terminer(tid)
    }
  }

  return {
    decisions,
    evenementsParcours: evenements,
    activiteDemo: { tacheConfiee: () => pont?.tacheConfiee(moiId) ?? null },
    processusDemo: notificationsProcessusDemo(magasin),
    participant,
    parcours,
    decision,
    portailParcours,
    homologation,
    certification,
    portail,
    messagerie: messagerieSimulee(attendre, refus, o.utilisateur ? { id: o.utilisateur.id, nom: o.utilisateur.nom, fonction: o.utilisateur.fonction ?? null } : undefined,
      // QUO-09 : sans lecture des liaisons, aucune conversation de démonstration ne mène à une liaison.
      { lectureLiaisons: o.utilisateur?.profils ? o.utilisateur.profils.some((p) => (PROFILS[p] ?? []).includes(PERM.liaisonLire)) : undefined }),
    // Mon activité (D-76) : agenda, activités, tâches, rapports et équipe de l'utilisateur de la démonstration.
    pilotage,
    // Processus d'unité (D-87) : définitions, instances et tâches d'étapes ; jeu de démonstration du pôle réseau.
    processus,
    // Pouvoirs hiérarchiques et demandes à l'administration (D-81) : MS-IAM simulé.
    pouvoirs: pouvoirsSimules(attendre, refus, magasin, o.utilisateur?.id ?? 'U-001'),
    // Aperçu bureautique (D-83) : conversion simulée en un petit PDF de démonstration.
    apercu: apercuSimule(attendre, refus),
    // Clé de sécurité simulée : confirme après un délai ; l'option echecs simule une annulation.
    reauth: {
      assertion: () => new Promise((res, rej) => window.setTimeout(() => (o.echecs ? rej(new ReauthCancelled()) : res(btoa('assertion-simulee'))), 900)),
    },
  }
}
