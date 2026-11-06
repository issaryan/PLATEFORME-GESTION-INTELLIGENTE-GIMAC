import { ApiError, type ArriveeNotification, type OptionsTeleversement, type Televersement } from '@gimac/socle'
import { PROFILS } from '../../permissions'
import type { PersonneActivite } from '../../modele/pilotage'
import {
  INITIATEURS_DEFAUT, delaiMaxEffectif, LIMITES_FORMULAIRE, LIMITES_PROCESSUS, LIMITES_PUBLIC, MODES_REMPLACEMENT, REGLES_INITIATEUR, cleDesignation, designationsAttendues, intervenantNormalise, type AnomalieProcessus, type Attendu,
  type ChampFormulaire, type ControleProcessus, type DemandeProcessus, type EtapeInstance, type EtapeProcessus, type Formulaire, type HistoriqueProcessus, type Initiateurs,
  type InstanceProcessus, type Intervenant, type CandidatsProcessus, type Lancement, type Processus, type ResumeInstance, type ResumeProcessus, type SaisieProcessus,
  type ValeurChampFormulaire, type ValeurFormulaire,
} from '../../modele/processus'
import { ajouterJours, aujourdhui, jourDe } from '../../modele/jours'
import type { ServiceProcessus } from '../ports'
import type { MagasinPouvoirs } from './pouvoirs'
import { DUREE_ETAPE_DOCUMENT_MS, type PontTachesProcessus } from './pilotage'
import { PRESENTATIONS_PROFILS } from './presentations'
import { pdfDemo } from './messagerie'
import { definitionChangementDemo } from './changement-modele'
import { referenceSimulee } from './reference-processus'

// MS-PILOTAGE simulé pour les processus d'unité (D-87, docs/lot1/processus-unite.md §5) : mêmes formes et mêmes codes de
// refus que le service ; ses règles ne sont qu'une imitation (contrôle de publication, versions, résolution des
// intervenants, ordre des signatures, validation et renvoi, tâches d'origine PROCESSUS de « Mon activité »). Jeu de
// démonstration : processus publié « Ouverture d'un flux réseau » du pôle réseau, un brouillon et deux instances.

/** D-99 : refus de la séparation des fonctions, même texte que MS-PILOTAGE. */
export const MESSAGE_SEPARATION = 'L’initiateur d’une demande ne peut pas exécuter, valider ou signer les étapes de sa propre demande (séparation des fonctions).'

type Attendre = <T>(v: T | (() => T)) => Promise<T>
type Refus = (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError

/** `contenu` (QUO-04) : contenu en base64, gardé avec l'état de la démonstration pour l'aperçu et le téléchargement. */
type Document = { nomFichier: string; typeMime: string; taille: number; sha256: string; reference?: string | null; deposant?: string | null; contenu?: string }

/** Contenu de démonstration d'un document sans fichier (jeu préparé) : PDF d'une page, ou texte. */
const contenuDemo = (d: Document): Blob => (d.typeMime === 'application/pdf'
  ? pdfDemo([d.nomFichier, 'Document de démonstration', '', `Empreinte SHA-256 : ${d.sha256.slice(0, 16)}…`])
  : new Blob([`${d.nomFichier}\nDocument de démonstration.\n`], { type: d.typeMime || 'text/plain' }))
const enBase64 = (b: Blob): Promise<string> => new Promise((ok, ko) => {
  const lecteur = new FileReader()
  lecteur.onload = () => ok(String(lecteur.result).replace(/^data:[^,]*,/, ''))
  lecteur.onerror = () => ko(lecteur.error)
  lecteur.readAsDataURL(b)
})
const depuisBase64 = (b: string, type: string) => new Blob([Uint8Array.from(atob(b), (c) => c.charCodeAt(0))], { type })
/** Taille au-delà de laquelle le contenu d'un dépôt simulé n'est pas gardé dans la session (stockage de l'onglet limité). */
const CONTENU_MAX = 1_000_000
type EtapeSim = EtapeInstance & { def: EtapeProcessus }
type InstanceSim = Omit<InstanceProcessus, 'etapes' | 'droits'> & { etapes: EtapeSim[] }

/** Empreinte hexadécimale de démonstration (64 caractères), stable pour un même texte. */
export function empreinteDemo(texte: string): string {
  let h = 2166136261
  let s = ''
  for (let tour = 0; tour < 8; tour++) {
    for (let i = 0; i < texte.length; i++) h = Math.imul(h ^ texte.charCodeAt(i), 16777619) >>> 0
    h = Math.imul(h ^ tour, 16777619) >>> 0
    s += h.toString(16).padStart(8, '0')
  }
  return s
}

const pos = (x: number, y: number) => ({ x, y })

/** Processus publié de la démonstration (§5.3) : quatre étapes, un validateur, une signature du chef d'unité. */
export function definitionDemo(): EtapeProcessus[] {
  return [
    {
      code: 'DEMANDE', libelle: 'Demande d’ouverture du flux', action: 'Décrire le flux à ouvrir : source, destination, port, protocole et date souhaitée.',
      intervenant: { mode: 'LANCEUR' }, validateur: null, predecesseurs: [], delaiJours: 1, livrables: [], signatures: [], position: pos(0, 80),
      champs: [
        { code: 'SOURCE', libelle: 'Adresse source', type: 'TEXTE', choix: [], obligatoire: true },
        { code: 'DESTINATION', libelle: 'Adresse de destination', type: 'TEXTE', choix: [], obligatoire: true },
        { code: 'PORT', libelle: 'Port', type: 'NOMBRE', choix: [], obligatoire: true },
        { code: 'PROTOCOLE', libelle: 'Protocole', type: 'CHOIX', choix: ['TCP', 'UDP'], obligatoire: true },
        { code: 'DATE_SOUHAITEE', libelle: 'Date souhaitée', type: 'DATE', choix: [], obligatoire: false },
      ],
    },
    {
      code: 'ANALYSE', libelle: 'Analyse de sécurité du flux', action: 'Vérifier la conformité du flux à la politique de filtrage et rédiger la note d’analyse.',
      intervenant: { mode: 'PROFIL', profil: 'ROLE_POLE_RESEAU' }, validateur: { mode: 'CHEF_UNITE' }, predecesseurs: ['DEMANDE'], delaiJours: 2, delaiMaxJours: 4, position: pos(300, 80),
      livrables: [{ code: 'NOTE_ANALYSE', libelle: 'Note d’analyse', typesPieces: ['application/pdf'], obligatoire: true, modele: null }], signatures: [], champs: [],
    },
    {
      code: 'MISE_EN_OEUVRE', libelle: 'Mise en œuvre sur les équipements', action: 'Appliquer la règle sur les pare-feu et joindre la configuration appliquée.',
      intervenant: { mode: 'AU_LANCEMENT', remplacement: 'ROLE_POLE_RESEAU' }, validateur: null, predecesseurs: ['ANALYSE'], delaiJours: 3, position: pos(600, 80),
      livrables: [{ code: 'CONFIGURATION', libelle: 'Configuration appliquée', typesPieces: ['text/plain', 'application/pdf'], obligatoire: true, modele: null }], signatures: [],
      champs: [{ code: 'EQUIPEMENTS', libelle: 'Équipements modifiés', type: 'TEXTE', choix: [], obligatoire: false }],
    },
    {
      code: 'RECETTE', libelle: 'Recette du flux', action: 'Tester le flux de bout en bout, rédiger le procès-verbal de recette et le faire signer par le chef d’unité.',
      intervenant: { mode: 'PROFIL', profil: 'ROLE_POLE_RESEAU' }, validateur: null, predecesseurs: ['MISE_EN_OEUVRE'], delaiJours: 2, position: pos(900, 80),
      livrables: [{ code: 'PV_RECETTE', libelle: 'Procès-verbal de recette', typesPieces: ['application/pdf'], obligatoire: true, modele: null }],
      signatures: [{ code: 'SIGNATURE_RECETTE', libelle: 'Signature du procès-verbal', signataire: { mode: 'CHEF_UNITE' }, porte: ['PV_RECETTE', 'NOTE_ANALYSE'], ordre: 1 }],
      champs: [{ code: 'RESULTAT', libelle: 'Résultat des tests', type: 'CHOIX', choix: ['Conforme', 'Non conforme'], obligatoire: true }],
    },
  ]
}

/** Demandes ouvertes à tout le GIMAC (D-98) : « Demande de congé » (RH) et « Demande de changement d'adresse IP » (pôle réseau). */
export function demandesDemo(): { code: string; unite: string; libelle: string; description: string; categorie: string; etapes: EtapeProcessus[]; formulaire: Formulaire }[] {
  const champ = (code: string, libelle: string, type: ChampFormulaire['type'], obligatoire: boolean, aide: string, o: Partial<ChampFormulaire> = {}): ChampFormulaire =>
    ({ code, libelle, type, obligatoire, aide, choix: [], typesPieces: [], ...o })
  const etape = (code: string, libelle: string, action: string, intervenant: Intervenant, predecesseurs: string[], x: number, o: Partial<EtapeProcessus> = {}): EtapeProcessus =>
    ({ code, libelle, action, intervenant, validateur: null, predecesseurs, delaiJours: 2, livrables: [], signatures: [], champs: [], position: pos(x, 80), ...o })
  const gestionnaireRh: Intervenant = { mode: 'PROFIL', profil: 'ROLE_COLLABORATEUR', unite: 'RH' }
  const reseau: Intervenant = { mode: 'PROFIL', profil: 'ROLE_POLE_RESEAU' }
  return [
    {
      code: 'DEMANDE_CONGE', unite: 'RH', libelle: 'Demande de congé', categorie: 'Ressources humaines',
      description: 'Demande de congé adressée au service des ressources humaines : vérification des droits, accord du supérieur, enregistrement et clôture.',
      formulaire: { champs: [
        champ('PERIODE', 'Période de congé', 'PERIODE', true, 'Premier et dernier jour d’absence.'),
        champ('MOTIF', 'Motif', 'TEXTE_LONG', true, 'Congé annuel, événement familial, récupération…'),
        champ('JUSTIFICATIF', 'Pièce justificative', 'PIECE_JOINTE', false, 'Facultative : PDF ou image.', { typesPieces: ['application/pdf', 'image/*'] }),
      ] },
      etapes: [
        etape('ANALYSE', 'Vérification des droits à congé', 'Vérifier le solde de congés du demandeur et la conformité de la période demandée.', gestionnaireRh, [], 0,
          { champs: [{ code: 'SOLDE_JOURS', libelle: 'Solde de jours disponible', type: 'NOMBRE', choix: [], obligatoire: true }] }),
        etape('VALIDATION', 'Accord du supérieur hiérarchique', 'Donner l’accord du service sur la période demandée.', { mode: 'N_PLUS_1_LANCEUR', remplacement: 'ROLE_RESPONSABLE_SERVICE' }, ['ANALYSE'], 300,
          { champs: [{ code: 'DECISION', libelle: 'Décision', type: 'CHOIX', choix: ['Accordé', 'Accordé avec dates modifiées'], obligatoire: true }] }),
        etape('EXECUTION', 'Enregistrement du congé', 'Enregistrer le congé dans le dossier du demandeur et établir la décision.', gestionnaireRh, ['VALIDATION'], 600,
          { livrables: [{ code: 'DECISION_CONGE', libelle: 'Décision de congé', typesPieces: ['application/pdf'], obligatoire: false, modele: null }] }),
        etape('CLOTURE', 'Clôture du dossier', 'Informer le demandeur et archiver la demande.', gestionnaireRh, ['EXECUTION'], 900, { delaiJours: 1 }),
      ],
    },
    {
      code: 'CHANGEMENT_ADRESSE_IP', unite: 'POLE_RESEAU', libelle: 'Demande de changement d’adresse IP', categorie: 'Réseau',
      description: 'Demande adressée au pôle réseau pour changer l’adresse IP d’un poste ou d’un équipement : analyse, validation, mise en œuvre et clôture.',
      formulaire: { champs: [
        champ('EQUIPEMENT', 'Équipement', 'TEXTE', true, 'Nom ou numéro d’inventaire du poste ou de l’équipement.'),
        champ('ADRESSE_ACTUELLE', 'Adresse IP actuelle', 'TEXTE', true, 'Par exemple 10.20.4.15.'),
        champ('ADRESSE_DEMANDEE', 'Adresse IP demandée', 'TEXTE', true, 'Adresse souhaitée, ou plage du réseau visé.'),
        champ('JUSTIFICATION', 'Justification', 'TEXTE_LONG', true, 'Pourquoi le changement est nécessaire.'),
      ] },
      etapes: [
        etape('ANALYSE', 'Analyse technique', 'Vérifier le plan d’adressage, la disponibilité de l’adresse demandée et l’impact sur les règles de filtrage.', reseau, [], 0,
          { champs: [{ code: 'ADRESSE_DISPONIBLE', libelle: 'Adresse disponible', type: 'CHOIX', choix: ['Oui', 'Non, autre adresse proposée'], obligatoire: true }] }),
        etape('VALIDATION', 'Validation du chef de pôle', 'Autoriser le changement d’adresse au vu de l’analyse.', { mode: 'CHEF_UNITE', remplacement: 'ROLE_RESPONSABLE_SERVICE' }, ['ANALYSE'], 300,
          { delaiJours: 1, champs: [{ code: 'DECISION', libelle: 'Décision', type: 'CHOIX', choix: ['Accordée', 'Accordée avec réserves'], obligatoire: true }] }),
        etape('EXECUTION', 'Mise en œuvre du changement', 'Appliquer la nouvelle adresse, mettre à jour le DHCP, le DNS et les règles de filtrage.', reseau, ['VALIDATION'], 600,
          { champs: [{ code: 'ADRESSE_ATTRIBUEE', libelle: 'Adresse IP attribuée', type: 'TEXTE', choix: [], obligatoire: true }] }),
        etape('CLOTURE', 'Vérification et clôture', 'Vérifier la connectivité avec le demandeur et clôturer la demande.', reseau, ['EXECUTION'], 900, { delaiJours: 1 }),
      ],
    },
  ]
}

/**
 * D-100 : processus au public restreint par leur créateur, comme le jeu de démonstration de MS-PILOTAGE, pour montrer
 * qu'un administrateur ne voit plus tout dans « Lancer une demande » : « Création d'une machine virtuelle » (pôle système),
 * réservée à deux personnes désignées (l'ingénieur réseau et l'analyste Normes et Agréments), et « Mise à jour de la
 * plateforme » (conformité), réservée aux porteurs des profils Conformité et Sécurité des SI.
 */
export function publicsRestreintsDemo(): { code: string; unite: string; libelle: string; description: string; categorie: string; auteur: string; etapes: EtapeProcessus[]; formulaire: Formulaire; initiateurs: Initiateurs }[] {
  const etapes = (profil: string, unite: string, premiere: string, seconde: string): EtapeProcessus[] => {
    const porteur: Intervenant = { mode: 'PROFIL', profil, unite }
    return [
      { code: 'REALISATION', libelle: premiere, action: 'Réaliser la demande et consigner le résultat.', intervenant: porteur, validateur: null, predecesseurs: [], delaiJours: 3, livrables: [], signatures: [], champs: [], position: pos(0, 80) },
      { code: 'CLOTURE', libelle: seconde, action: 'Informer le demandeur et clôturer la demande.', intervenant: porteur, validateur: { mode: 'CHEF_UNITE', remplacement: 'ROLE_RESPONSABLE_SERVICE' }, predecesseurs: ['REALISATION'], delaiJours: 1, livrables: [], signatures: [], champs: [], position: pos(300, 80) },
    ]
  }
  const vide = { unites: [], profils: [], permissions: [], personnes: [] }
  return [
    {
      code: 'CREATION_MACHINE_VIRTUELLE', unite: 'POLE_SYSTEME', libelle: 'Création d’une machine virtuelle', categorie: 'Système', auteur: 'u-resp-info',
      description: 'Demande de création d’une machine virtuelle sur l’infrastructure de virtualisation : dimensionnement, création et remise.',
      etapes: etapes('ROLE_POLE_SYSTEME', 'POLE_SYSTEME', 'Dimensionnement et création', 'Remise de la machine'),
      formulaire: { champs: [
        { code: 'SYSTEME', libelle: 'Système d’exploitation', type: 'LISTE', choix: ['Linux', 'Windows Server'], obligatoire: true, aide: null, typesPieces: [] },
        { code: 'PROCESSEURS', libelle: 'Nombre de processeurs', type: 'NOMBRE', choix: [], obligatoire: true, aide: null, typesPieces: [] },
        { code: 'MEMOIRE', libelle: 'Mémoire (Go)', type: 'NOMBRE', choix: [], obligatoire: true, aide: null, typesPieces: [] },
      ] },
      initiateurs: { ...vide, regles: ['PERSONNES'], personnes: ['u-res', 'u-sna'] },
    },
    {
      code: 'MISE_A_JOUR_PLATEFORME', unite: 'CONFORMITE', libelle: 'Mise à jour de la plateforme', categorie: 'Conformité', auteur: 'u-conf',
      description: 'Demande de mise à jour de la plateforme soumise à la revue de conformité : analyse d’impact, revue et clôture.',
      etapes: etapes('ROLE_CONFORMITE', 'CONFORMITE', 'Analyse d’impact et revue', 'Clôture de la revue'),
      formulaire: { champs: [] },
      initiateurs: { ...vide, regles: ['PROFILS'], profils: ['ROLE_CONFORMITE', 'ROLE_RSSI'] },
    },
  ]
}

/** Notification d'un événement de processus (QUO-05), adressée à une personne, comme MS-NOTIFICATION la déduit des événements pilotage.process-*. */
export type NotificationProcessusDemo = ArriveeNotification & { destinataire: string }
type EtatSimule = { definitions: Processus[]; instances: InstanceSim[]; documents: Map<string, Document>; sequence: number; notifications?: NotificationProcessusDemo[] }
/** État partagé par les services simulés d'un même magasin des pouvoirs (organisation commune, plusieurs personnes). */
const ETATS = new WeakMap<MagasinPouvoirs, EtatSimule>()
const ABONNES = new WeakMap<MagasinPouvoirs, ((n: NotificationProcessusDemo) => void)[]>()

/**
 * Notifications de processus de la démonstration (QUO-05) : celles adressées à une personne (gardées avec l'état, donc
 * reçues après un changement de profil) et l'abonnement aux suivantes.
 */
export function notificationsProcessusDemo(magasin: MagasinPouvoirs) {
  return {
    pour: (personneId: string): ArriveeNotification[] => (ETATS.get(magasin)?.notifications ?? []).filter((n) => n.destinataire === personneId).map(({ destinataire: _d, ...n }) => n),
    surNotification: (f: (n: NotificationProcessusDemo) => void) => { ABONNES.set(magasin, [...(ABONNES.get(magasin) ?? []), f]) },
  }
}

/**
 * Démonstration d'un parcours à plusieurs personnes (D-98) : dans une session de démonstration ouverte (gm.sessionDemo),
 * l'état des processus est gardé dans l'onglet (sessionStorage) pour survivre au changement de profil de démonstration
 * (rechargement) ; les tâches des étapes en cours sont recréées dans « Mon activité ». Hors session (tests unitaires) : rien.
 */
const CLE_ETAT = 'gm.demoProcessus.v1'
function stockage(): Storage | null {
  try {
    return typeof sessionStorage !== 'undefined' && sessionStorage.getItem('gm.sessionDemo') ? sessionStorage : null
  } catch {
    return null
  }
}
function relireEtat(): EtatSimule | null {
  try {
    const brut = stockage()?.getItem(CLE_ETAT)
    if (!brut) return null
    const e = JSON.parse(brut) as { definitions: Processus[]; instances: InstanceSim[]; documents: [string, Document][]; sequence: number; notifications?: NotificationProcessusDemo[] }
    return { definitions: e.definitions, instances: e.instances, documents: new Map(e.documents), sequence: e.sequence, notifications: e.notifications ?? [] }
  } catch {
    return null
  }
}
function garderEtat(e: EtatSimule) {
  try {
    stockage()?.setItem(CLE_ETAT, JSON.stringify({ definitions: e.definitions, instances: e.instances, documents: [...e.documents.entries()], sequence: e.sequence, notifications: e.notifications ?? [] }))
  } catch {
    // Stockage indisponible ou plein : la démonstration continue en mémoire.
  }
}

export function processusSimule(attendre: Attendre, refus: Refus, moiId: string, magasin: MagasinPouvoirs, pont?: PontTachesProcessus): ServiceProcessus {
  const existant = ETATS.get(magasin)
  const repris = existant ? null : relireEtat()
  const etat: EtatSimule = existant ?? repris ?? { definitions: [], instances: [], documents: new Map(), sequence: 0 }
  ETATS.set(magasin, etat)
  const id = (prefixe: string) => `${prefixe}-${(++etat.sequence).toString().padStart(4, '0')}`
  const maintenant = () => new Date().toISOString()
  const personne = (pid: string): PersonneActivite => magasin.personne(pid)
  const moi = () => personne(moiId)
  const membre = (pid: string) => magasin.organisation.find((m) => m.id === pid)
  const profils = (pid: string) => magasin.profilsDe(pid)
  // D-92 : tous domaines par la permission REF_METIER_ADMIN (composition des profils simulés), jamais par un nom de profil.
  const refMetier = () => profils(moiId).some((p) => (PROFILS[p] ?? []).includes('REF_METIER_ADMIN'))
  const definirToutes = () => magasin.actionsDe(moiId).PROCESSUS_DEFINIR === 'TOUTES'
  const lancerToutes = () => magasin.actionsDe(moiId).PROCESSUS_LANCER === 'TOUTES'
  const admin = () => refMetier() || lancerToutes()
  /** Responsable d'une unité, à défaut celui de la plus proche unité parente qui en a un. */
  const chefDe = (code: string): string | null => {
    for (let u = magasin.unites.find((x) => x.code === code); u; u = magasin.unites.find((x) => x.code === u!.parent)) if (u.responsableId) return u.responsableId
    return null
  }
  const membreDe = (pid: string, unite: string) => {
    const m = membre(pid)
    return !!m?.unite && magasin.sousUnites(unite).includes(m.unite)
  }
  const peutDefinir = (unite: string) => refMetier() || magasin.couvreUnite(moiId, 'PROCESSUS_DEFINIR', unite)
  const peutLancer = (unite: string) => membreDe(moiId, unite) || magasin.couvreUnite(moiId, 'PROCESSUS_LANCER', unite) || admin()
  const permissionsDe = (pid: string) => new Set(profils(pid).flatMap((p) => PROFILS[p] ?? []))
  /**
   * D-98, D-100 (comme MS-PILOTAGE) : voir et lancer une version, c'est faire partie de son public, défini par son créateur
   * (l'une des règles d'initiateurs), et rien d'autre : REF_METIER_ADMIN et PROCESSUS_LANCER (TOUTES ou couvrant l'unité)
   * n'y ajoutent rien. MEMBRES_UNITE couvre les membres de l'unité et ses responsables (unité dirigée ou parente dirigée).
   */
  const initiateurs = (p: Processus): Initiateurs => p.initiateurs ?? INITIATEURS_DEFAUT
  // L'organisation simulée ne recense pas tous les comptes de démonstration (administration) : un compte absent vaut actif.
  const actif = (pid: string) => membre(pid)?.actif !== false
  const dirige = (pid: string, unite: string) => magasin.unitesDirigees(pid).some((u) => magasin.sousUnites(u).includes(unite))
  const peutLancerProcessus = (p: Processus) => {
    const i = initiateurs(p)
    return i.regles.some((r) => {
      switch (r) {
        case 'MEMBRES_UNITE': return membreDe(moiId, p.unite) || dirige(moiId, p.unite)
        case 'TOUT_LE_GIMAC': return actif(moiId)
        case 'UNITES': return i.unites.some((u) => membreDe(moiId, u))
        case 'PROFILS': return actif(moiId) && i.profils.some((x) => profils(moiId).includes(x))
        case 'PERSONNES': return (i.personnes ?? []).includes(moiId) && actif(moiId)
        default: return i.permissions.some((x) => permissionsDe(moiId).has(x))
      }
    })
  }
  /** Refus d'un lancement (mêmes textes que MS-PILOTAGE) : le public défini par le créateur, en message et en détail. */
  const nonInitiateur = (p: Processus) => {
    const i = initiateurs(p)
    const personnes = i.personnes ?? []
    const qui = i.regles.map((r) => {
      switch (r) {
        case 'MEMBRES_UNITE': return `les membres et les responsables de l'unité ${p.unite}`
        case 'TOUT_LE_GIMAC': return 'tout compte interne actif du GIMAC'
        case 'UNITES': return `les membres des unités ${i.unites.join(', ')}`
        case 'PROFILS': return `les porteurs des profils ${i.profils.join(', ')}`
        case 'PERMISSIONS': return `les porteurs des permissions ${i.permissions.join(', ')}`
        default: return personnes.length === 1 ? 'la personne désignée par son créateur' : `les ${personnes.length} personnes désignées par son créateur`
      }
    })
    return refus(403, 'PIL_PROCESSUS_NON_INITIATEUR', `Ce processus ne peut être lancé que par ${qui.join(', ou ')}`,
      { regles: i.regles, unites: i.unites, profils: i.profils, permissions: i.permissions, personnes })
  }
  const peutLire = (unite: string) => peutDefinir(unite) || peutLancer(unite) || magasin.unitesDirigees(moiId).some((u) => magasin.sousUnites(u).includes(unite))
  const peutPiloter = (unite: string) => admin() || magasin.unitesDirigees(moiId).includes(unite) || magasin.couvreUnite(moiId, 'TACHE_CONFIER', unite)
  const documents = etat.documents
  // Désignation hors de l'unité : porteurs actifs d'un profil dans l'unité (sous-unités comprises), puis dans tout le
  // GIMAC (unités actives ou sans rattachement), comme MS-PILOTAGE.
  const porteursDans = (unite: string, profil: string) => magasin.organisation.filter((m) => m.actif && !!m.unite && magasin.sousUnites(unite).includes(m.unite) && profils(m.id).includes(profil))
  const porteursPartout = (profil: string) => magasin.organisation.filter((m) => m.actif && profils(m.id).includes(profil) && (!m.unite || magasin.unites.find((u) => u.code === m.unite)?.active !== false))
  const portePartout = (pid: string, profil: string | null | undefined) => !!profil && porteursPartout(profil).some((m) => m.id === pid)
  const repartition = (l: { unite: string | null }[]) => Object.entries(l.reduce<Record<string, number>>((a, m) => ({ ...a, [m.unite ?? 'sans unité']: (a[m.unite ?? 'sans unité'] ?? 0) + 1 }), {}))
    .sort(([a], [b]) => a.localeCompare(b)).map(([u, n]) => `${u} : ${n}`).join(', ')
  const candidatsDe = (unite: string, profil: string | null): CandidatsProcessus => {
    const vers = (m: { id: string; nomAffiche: string; fonction: string; unite: string | null }) => ({ id: m.id, nomAffiche: m.nomAffiche, fonction: m.fonction || null, unite: m.unite })
    const tri = (a: { nomAffiche: string }, b: { nomAffiche: string }) => a.nomAffiche.localeCompare(b.nomAffiche, 'fr')
    const dans = profil ? porteursDans(unite, profil) : magasin.organisation.filter((m) => m.actif && !!m.unite && magasin.sousUnites(unite).includes(m.unite))
    const ailleurs = profil ? porteursPartout(profil).filter((m) => !dans.includes(m)) : []
    return { unite, profil, dansUnite: dans.map(vers).sort(tri), ailleurs: ailleurs.map(vers).sort(tri) }
  }

  // ---------------------------------------------------------------- définitions
  const definitions = etat.definitions
  // Acteur des gestes du jeu de démonstration (préparé au démarrage), sinon l'utilisateur.
  let acteurForce: PersonneActivite | null = null
  const historiser = (h: HistoriqueProcessus[], action: string, detail: string | null = null, etape: string | null = null, acteur: PersonneActivite | null = acteurForce ?? moi()) =>
    h.push({ quand: maintenant(), acteur, action, etape, detail })
  let numeroCode = 0
  const codeGenere = (unite: string) => {
    const suffixe = `-PRC-${++numeroCode}`
    return unite.slice(0, 40 - suffixe.length) + suffixe
  }
  const nouvelle = (s: SaisieProcessus, auteur: string, o: Partial<Processus> = {}): Processus => {
    const p: Processus = {
      id: id('PRC'), code: s.code ?? codeGenere(s.unite), version: 1, etat: 'BROUILLON', unite: s.unite, libelle: s.libelle, description: s.description, categorie: s.categorie, delaiCibleJours: s.delaiCibleJours,
      delaiMaxJours: s.delaiMaxJours ?? null, etapes: s.etapes.map(normaliserEtape), creeLe: maintenant(), creePar: personne(auteur), modifieLe: null, modifiePar: null, publieeLe: null, publieePar: null, motifPublication: null,
      avertissementsLeves: false, retireeLe: null, retireePar: null, motifRetrait: null, droits: { modifier: false, publier: false, lancer: false, retirer: false }, versions: [],
      avertissements: [], personnes: {}, historique: [{ quand: maintenant(), acteur: personne(auteur), action: 'CREE', etape: null, detail: null }],
      initiateurs: normaliserInitiateurs(s.initiateurs), formulaire: normaliserFormulaire(s.formulaire), referenceModele: s.referenceModele ?? null, ...o,
    }
    definitions.push(p)
    return p
  }
  const normaliserInitiateurs = (i: Partial<Initiateurs> | undefined): Initiateurs => ({
    regles: i?.regles?.length ? [...new Set(i.regles)] : [...INITIATEURS_DEFAUT.regles], unites: [...new Set(i?.unites ?? [])], profils: [...new Set(i?.profils ?? [])], permissions: [...new Set(i?.permissions ?? [])],
    // D-100 : personnes désignées nommément, sans doublon (le détail des noms est rendu par la vue).
    personnes: [...new Set((i?.personnes ?? []).map((x) => x.trim()))],
    // D-99 : séparation des fonctions, l'initiateur n'intervient pas sur sa demande sauf réglage explicite.
    initiateurIntervenant: !!i?.initiateurIntervenant,
  })
  const normaliserFormulaire = (f: Formulaire | undefined): Formulaire => ({
    champs: (f?.champs ?? []).map((c) => ({ ...c, code: c.code.trim(), libelle: c.libelle.trim(), obligatoire: !!c.obligatoire, aide: c.aide?.trim() || null, choix: c.type === 'LISTE' ? c.choix ?? [] : [], typesPieces: c.type === 'PIECE_JOINTE' ? c.typesPieces ?? [] : [] })),
  })
  /**
   * D-136, comme CodesEtapes de MS-PILOTAGE : la clé d'une étape nouvelle n'est que provisoire et devient
   * {CODE}-E{N} (prédécesseurs suivis) ; un code déjà connu de la famille est gardé ; une clé en double est refusée.
   */
  const reecrireEtapes = (code: string, etapes: EtapeProcessus[], connus: Set<string>): EtapeProcessus[] => {
    const motif = new RegExp(`^${code.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}-E(\\d+)$`)
    let n = Math.max(0, ...[...connus, ...etapes.map((e) => e.code)].map((c) => Number(motif.exec(c)?.[1] ?? 0)))
    const nouveaux = new Map<string, string>()
    etapes.forEach((e, i) => {
      if (connus.has(e.code)) return
      if (nouveaux.has(e.code)) throw refus(400, 'PIL_REQUETE_INVALIDE', `Clé d’étape en double : ${e.code}`, { champ: `etapes[${i}].code` })
      const suffixe = `-E${++n}`
      nouveaux.set(e.code, code.slice(0, 40 - suffixe.length) + suffixe)
    })
    return etapes.map((e) => ({ ...e, code: nouveaux.get(e.code) ?? e.code, predecesseurs: e.predecesseurs.map((x) => nouveaux.get(x) ?? x) }))
  }
  const normaliserEtape = (e: EtapeProcessus): EtapeProcessus => ({
    ...e, code: e.code.trim(), libelle: e.libelle.trim(), action: e.action ?? '', intervenant: intervenantNormalise(e.intervenant), validateur: e.validateur ? intervenantNormalise(e.validateur) : null,
    livrables: e.livrables.map((l) => ({ ...l, obligatoire: l.obligatoire ?? true, modele: l.modele || null })), signatures: e.signatures.map((s) => ({ ...s, signataire: intervenantNormalise(s.signataire), ordre: s.ordre || 1 })),
    champs: e.champs.map((c) => ({ ...c, obligatoire: c.obligatoire ?? false, choix: c.type === 'CHOIX' ? c.choix : [] })),
  })
  const vue = (p: Processus): Processus => {
    const versions = definitions.filter((x) => x.code === p.code).sort((a, b) => b.version - a.version).map((x) => ({ id: x.id, version: x.version, etat: x.etat }))
    const personnes = Object.fromEntries(p.etapes.flatMap((e) => [e.intervenant, e.validateur, ...e.signatures.map((s) => s.signataire)]).filter((i): i is Intervenant => !!i?.personneId).map((i) => [i.personneId!, personne(i.personneId!).nomAffiche]))
    const definir = peutDefinir(p.unite)
    // D-106 (§12) : au seul titre du public (sans lire l'unité), la version publiée seule, sans historique de la définition.
    const lecteurUnite = peutLire(p.unite)
    return structuredClone({
      ...p, versions: lecteurUnite ? versions : versions.filter((v) => v.id === p.id), personnes, historique: lecteurUnite ? p.historique : [], avertissements: p.etat === 'PUBLIEE' ? controler(p).filter((a) => a.gravite === 'AVERTISSEMENT') : [],
      initiateurs: avecNoms(initiateurs(p)), formulaire: p.formulaire ?? { champs: [] },
      droits: {
        modifier: definir && p.etat === 'BROUILLON', publier: definir && p.etat === 'BROUILLON', lancer: p.etat === 'PUBLIEE' && peutLancerProcessus(p), retirer: definir && p.etat === 'PUBLIEE',
        // D-103 : le public se règle sur un brouillon comme sur une version publiée, sans nouvelle version.
        modifierPublic: definir && p.etat !== 'RETIREE',
        nouvelleVersion: definir && p.etat !== 'BROUILLON', supprimer: definir && p.etat === 'BROUILLON',
      },
    })
  }
  /** D-100 : personnes désignées du public, avec leur nom affiché (comme la vue de MS-PILOTAGE). */
  const avecNoms = (i: Initiateurs): Initiateurs => {
    const personnes = i.personnes ?? []
    return { ...i, personnes, personnesDetail: personnes.map((x) => ({ id: x, nomAffiche: membre(x)?.nomAffiche ?? null })) }
  }
  const trouver = (pid: string) => {
    const p = definitions.find((x) => x.id === pid)
    // D-98 : une version publiée se lit aussi de qui peut la lancer (formulaire de lancement).
    if (p && p.etat === 'PUBLIEE' && peutLancerProcessus(p)) return p
    // D-106 (§12) : « introuvable » pour une définition inexistante seulement ; refus explicite sinon (comme MS-PILOTAGE).
    if (!p) throw refus(404, 'PIL_PROCESSUS_INTROUVABLE', 'Processus introuvable.')
    if (!peutLire(p.unite) || (p.etat === 'BROUILLON' && !peutDefinir(p.unite))) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Ce processus n’est pas lisible depuis votre unité.')
    return p
  }
  const resume = (p: Processus): ResumeProcessus => {
    const v = vue(p)
    return {
      id: v.id, code: v.code, version: v.version, etat: v.etat, unite: v.unite, libelle: v.libelle, description: v.description, categorie: v.categorie, delaiCibleJours: v.delaiCibleJours, delaiMaxJours: v.delaiMaxJours ?? null,
      nombreEtapes: v.etapes.length, publieeLe: v.publieeLe, modifieLe: v.modifieLe, modifiePar: v.modifiePar, droits: v.droits, initiateurs: v.initiateurs, nombreChampsFormulaire: v.formulaire?.champs.length ?? 0,
    }
  }
  const exigerDefinir = (unite: string) => {
    if (!peutDefinir(unite)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Définir les processus de cette unité exige le pouvoir PROCESSUS_DEFINIR.')
  }
  const verifierSaisie = (s: SaisieProcessus) => {
    const champ = (c: string, m: string) => refus(400, 'PIL_REQUETE_INVALIDE', m, { champ: c })
    if (!s.libelle?.trim() || s.libelle.length > LIMITES_PROCESSUS.libelle) throw champ('libelle', 'Libellé obligatoire, 200 caractères au plus.')
    if ((s.description ?? '').length > LIMITES_PROCESSUS.description) throw champ('description', 'Description de 4000 caractères au plus.')
    if (s.delaiCibleJours !== null && (s.delaiCibleJours < 0 || s.delaiCibleJours > LIMITES_PROCESSUS.delai)) throw champ('delaiCibleJours', 'Délai de 0 à 365 jours.')
    // D-106 : délai maximal facultatif, de 0 à 365 jours, au moins le délai cible (comme MS-PILOTAGE).
    const max = (c: string, m: number | null | undefined, cible: number | null) => {
      if (m === null || m === undefined) return
      if (m < 0 || m > LIMITES_PROCESSUS.delai) throw champ(c, 'Délai de 0 à 365 jours.')
      if (cible !== null && m < cible) throw champ(c, `Le délai maximal (${m} j) ne peut être inférieur au délai cible (${cible} j)`)
    }
    max('delaiMaxJours', s.delaiMaxJours, s.delaiCibleJours)
    if (s.etapes.length > LIMITES_PROCESSUS.etapes) throw champ('etapes', 'Cent étapes au plus.')
    s.etapes.forEach((e, i) => {
      if (!LIMITES_PROCESSUS.codeEtape.test(e.code ?? '')) throw champ(`etapes[${i}].code`, 'Code d’étape invalide.')
      if (e.delaiJours < 0 || e.delaiJours > LIMITES_PROCESSUS.delai) throw champ(`etapes[${i}].delaiJours`, 'Délai de 0 à 365 jours.')
      max(`etapes[${i}].delaiMaxJours`, e.delaiMaxJours, e.delaiJours)
      if (!e.intervenant?.mode) throw champ(`etapes[${i}].intervenant.mode`, 'Mode d’intervenant obligatoire.')
    })
    if (!magasin.unites.some((u) => u.code === s.unite)) throw refus(422, 'PIL_PROCESSUS_UNITE_INCONNUE', 'Unité inconnue.')
    if ((s.formulaire?.champs.length ?? 0) > LIMITES_FORMULAIRE.champs) throw champ('formulaire.champs', 'Trente champs au plus.')
    s.formulaire?.champs.forEach((c, i) => {
      if (!LIMITES_PROCESSUS.codeEtape.test(c.code ?? '')) throw champ(`formulaire.champs[${i}].code`, 'Code de champ invalide.')
      if (!c.libelle?.trim()) throw champ(`formulaire.champs[${i}].libelle`, 'Libellé obligatoire.')
    })
    if (s.initiateurs) verifierInitiateurs(s.initiateurs)
  }
  /** Public saisi (D-98, D-100), comme SaisieProcessus.initiateurs de MS-PILOTAGE : règles connues, formats, bornes. */
  const verifierInitiateurs = (i: Partial<Initiateurs>) => {
    const champ = (c: string, m: string) => refus(400, 'PIL_REQUETE_INVALIDE', m, { champ: c })
    const codes = (c: string, l: string[] | undefined, motif: RegExp, m: string) => {
      if ((l ?? []).length > LIMITES_FORMULAIRE.parametres) throw champ(c, `${LIMITES_FORMULAIRE.parametres} éléments au plus`)
      ;(l ?? []).forEach((x, n) => { if (!motif.test(x?.trim() ?? '')) throw champ(`${c}[${n}]`, m) })
    }
    if ((i.regles ?? []).length > LIMITES_FORMULAIRE.parametres) throw champ('initiateurs.regles', `${LIMITES_FORMULAIRE.parametres} règles au plus`)
    ;(i.regles ?? []).forEach((r, n) => { if (!REGLES_INITIATEUR.includes(r)) throw champ(`initiateurs.regles[${n}]`, 'Valeur inconnue') })
    codes('initiateurs.unites', i.unites, /^[A-Z][A-Z0-9_]{1,31}$/, 'Code d’unité attendu (par exemple POLE_RESEAU)')
    codes('initiateurs.profils', i.profils, /^ROLE_[A-Z][A-Z0-9_]{1,60}$/, 'Profil ROLE_… attendu.')
    codes('initiateurs.permissions', i.permissions, /^[A-Z][A-Z0-9_]{1,79}$/, 'Permission ^[A-Z][A-Z0-9_]{1,79}$ attendue')
    // D-100 : identifiants de comptes (texte de 1 à 80 caractères), 50 au plus, comme MS-PILOTAGE.
    const personnes = i.personnes ?? []
    if (personnes.length > LIMITES_PUBLIC.personnes) throw champ('initiateurs.personnes', `${LIMITES_PUBLIC.personnes} personnes au plus`)
    personnes.forEach((x, n) => { if (!x?.trim() || x.trim().length > 80) throw champ(`initiateurs.personnes[${n}]`, 'Texte de 1 à 80 caractères attendu') })
  }
  /** D-103 : motif d'un changement de public, 3 à 500 caractères (obligatoire sur une version publiée), comme MS-PILOTAGE. */
  const motifPublic = (m: string | undefined, exige: boolean) => {
    const v = m?.trim() ?? ''
    if (!v && !exige) return null
    if (v.length < 3 || v.length > LIMITES_PUBLIC.motifMax) throw refus(400, 'PIL_REQUETE_INVALIDE', `Motif de 3 à ${LIMITES_PUBLIC.motifMax} caractères attendu`, { champ: 'motif' })
    return v
  }
  /** D-103 : résumé d'un public pour l'historique (règles, paramètres, personnes nommées), comme MS-PILOTAGE. */
  const resumePublic = (i: Initiateurs) => i.regles.map((r) => {
    const l = r === 'UNITES' ? i.unites : r === 'PROFILS' ? i.profils : r === 'PERMISSIONS' ? i.permissions : r === 'PERSONNES' ? (i.personnes ?? []).map((x) => membre(x)?.nomAffiche ?? x) : []
    return l.length ? `${r} (${l.join(', ')})` : r
  }).join(', ')
  /** Profil connu : au catalogue ou porté par une personne de l'organisation simulée (comme l'annuaire de MS-PILOTAGE). */
  const profilConnu = (x: string) => x in PROFILS || magasin.organisation.some((m) => profils(m.id).includes(x))
  /**
   * Anomalies du public (D-98, D-100), communes au contrôle de publication et au changement de public d'une version publiée
   * (D-103).
   */
  const anomaliesPublic = (i: Initiateurs): AnomalieProcessus[] => {
    const a: AnomalieProcessus[] = []
    const ajouter = (code: string, element: string, message: string) => a.push({ code, gravite: 'BLOQUANTE', etape: null, element, message })
    if (i.regles.includes('UNITES') && !i.unites.length) ajouter('INITIATEURS_INCOMPLETS', 'initiateurs.unites', 'Règle UNITES sans unité désignée.')
    if (i.regles.includes('PROFILS') && !i.profils.length) ajouter('INITIATEURS_INCOMPLETS', 'initiateurs.profils', 'Règle PROFILS sans profil désigné.')
    if (i.regles.includes('PERMISSIONS') && !i.permissions.length) ajouter('INITIATEURS_INCOMPLETS', 'initiateurs.permissions', 'Règle PERMISSIONS sans permission désignée.')
    if (i.regles.includes('PERSONNES') && !(i.personnes ?? []).length) ajouter('INITIATEURS_INCOMPLETS', 'initiateurs.personnes', 'Règle PERSONNES sans personne désignée')
    if (i.regles.includes('UNITES')) i.unites.filter((u) => !magasin.unites.some((x) => x.code === u)).forEach((u) => ajouter('UNITE_INCONNUE', 'initiateurs.unites', `Unité inconnue : ${u}.`))
    if (i.regles.includes('PROFILS')) i.profils.filter((x) => !profilConnu(x)).forEach((x) => ajouter('PROFIL_INCONNU', 'initiateurs.profils', `Profil inconnu : ${x}`))
    // D-100 : une personne désignée inconnue, externe ou inactive bloque la publication.
    if (i.regles.includes('PERSONNES')) {
      ;(i.personnes ?? []).filter((x) => !membre(x) || membre(x)?.actif === false)
        .forEach((x) => ajouter('PERSONNE_INCONNUE', 'initiateurs.personnes', `Personne désignée inconnue, externe ou inactive : ${membre(x)?.nomAffiche ?? x}`))
    }
    return a
  }

  /** Contrôle de publication (§5.1) : graphe, intervenants, livrables, signatures, champs, couverture des profils. */
  const controler = (p: Processus): AnomalieProcessus[] => {
    const a: AnomalieProcessus[] = []
    const ajouter = (code: string, gravite: AnomalieProcessus['gravite'], etape: string | null, element: string | null, message: string) => a.push({ code, gravite, etape, element, message })
    const codes = p.etapes.map((e) => e.code)
    if (!p.etapes.length) ajouter('AUCUNE_ETAPE', 'BLOQUANTE', null, null, 'Le processus n’a aucune étape.')
    codes.filter((c, i) => codes.indexOf(c) !== i).forEach((c) => ajouter('CODE_DUPLIQUE', 'BLOQUANTE', c, null, `Code d’étape en double : ${c}.`))
    p.etapes.forEach((e) => e.predecesseurs.filter((x) => !codes.includes(x)).forEach((x) => ajouter('PREDECESSEUR_INCONNU', 'BLOQUANTE', e.code, x, `Prédécesseur inconnu : ${x}.`)))
    if (p.etapes.length && !p.etapes.some((e) => !e.predecesseurs.length)) ajouter('AUCUNE_ETAPE_INITIALE', 'BLOQUANTE', null, null, 'Aucune étape sans prédécesseur.')
    // Kahn : les étapes jamais libérées forment un cycle.
    const degres = new Map(p.etapes.map((e) => [e.code, e.predecesseurs.filter((x) => codes.includes(x)).length]))
    const file = p.etapes.filter((e) => degres.get(e.code) === 0).map((e) => e.code)
    const vus = new Set<string>()
    while (file.length) {
      const c = file.shift()!
      vus.add(c)
      p.etapes.filter((e) => e.predecesseurs.includes(c)).forEach((e) => {
        degres.set(e.code, (degres.get(e.code) ?? 1) - 1)
        if (degres.get(e.code) === 0) file.push(e.code)
      })
    }
    const cycle = codes.filter((c) => !vus.has(c))
    if (cycle.length && p.etapes.some((e) => !e.predecesseurs.length)) ajouter('CYCLE', 'BLOQUANTE', cycle[0], cycle.join(','), `Cycle entre les étapes ${cycle.join(', ')}.`)
    const livrablesVus: string[] = []
    const intervenant = (e: EtapeProcessus, i: Intervenant | null, element: string) => {
      if (!i) return
      if (i.mode === 'PROFIL' && !i.profil) ajouter('INTERVENANT_INCOMPLET', 'BLOQUANTE', e.code, element, 'Profil de l’intervenant absent.')
      if (i.mode === 'PERSONNE' && !i.personneId) ajouter('INTERVENANT_INCOMPLET', 'BLOQUANTE', e.code, element, 'Personne de l’intervenant absente.')
      if (i.mode === 'PERSONNE' && i.personneId && !membre(i.personneId)) ajouter('PERSONNE_INCONNUE', 'BLOQUANTE', e.code, element, 'Personne inconnue.')
      if (i.mode === 'PERSONNE' && i.personneId && membre(i.personneId) && !membre(i.personneId)!.actif) ajouter('PERSONNE_INACTIVE', 'BLOQUANTE', e.code, element, 'Personne désactivée.')
      if (i.mode === 'AU_LANCEMENT' && !i.remplacement) ajouter('DESIGNATION_OUVERTE', 'LEVABLE', e.code, element, 'Désignation ouverte à toute personne interne active du GIMAC.')
      if (MODES_REMPLACEMENT.includes(i.mode) && !i.remplacement) ajouter('REMPLACEMENT_ABSENT', 'BLOQUANTE', e.code, `${element}.remplacement`, 'Profil de remplacement obligatoire.')
      const unite = i.unite || p.unite
      // Couverture : dans l'unité, sinon ailleurs dans le GIMAC (information), sinon nulle part (levable).
      const couverture = (profil: string, el: string) => {
        if (porteursDans(unite, profil).length) return
        const ailleurs = porteursPartout(profil)
        if (!ailleurs.length) ajouter('COUVERTURE_PROFIL', 'LEVABLE', e.code, el, `Aucune personne active ne porte le profil ${profil} dans l’unité ${unite} ni ailleurs dans le GIMAC.`)
        else ajouter('COUVERTURE_HORS_UNITE', 'AVERTISSEMENT', e.code, el, `Aucune personne active ne porte le profil ${profil} dans l’unité ${unite} ; ${ailleurs.length} porteur${ailleurs.length > 1 ? 's' : ''} ailleurs dans le GIMAC (${repartition(ailleurs)}) : l’étape ${ailleurs.length > 1 ? 'leur sera offerte' : 'lui sera confiée'}.`)
      }
      if (i.mode === 'PROFIL' && i.profil) couverture(i.profil, element)
      if (i.remplacement) couverture(i.remplacement, `${element}.remplacement`)
      if (i.mode === 'CHEF_UNITE' && !magasin.unites.find((u) => u.code === unite)?.responsableId) ajouter('CHEF_ABSENT', 'LEVABLE', e.code, element, 'L’unité n’a pas de responsable.')
    }
    p.etapes.forEach((e) => {
      if (!e.intervenant?.mode) ajouter('INTERVENANT_ABSENT', 'BLOQUANTE', e.code, 'intervenant', 'Intervenant absent.')
      intervenant(e, e.intervenant, 'intervenant')
      intervenant(e, e.validateur, 'validateur')
      if (e.validateur?.mode === 'PERSONNE' && e.intervenant.mode === 'PERSONNE' && e.validateur.personneId && e.validateur.personneId === e.intervenant.personneId) {
        ajouter('VALIDATEUR_IDENTIQUE', 'BLOQUANTE', e.code, 'validateur', 'Le validateur est l’intervenant.')
      }
      e.livrables.forEach((l) => {
        if (livrablesVus.includes(l.code)) ajouter('LIVRABLE_CODE_DUPLIQUE', 'BLOQUANTE', e.code, l.code, `Livrable en double : ${l.code}.`)
      })
      const sigs = e.signatures.map((s) => s.code)
      sigs.filter((c, i) => sigs.indexOf(c) !== i).forEach((c) => ajouter('SIGNATURE_CODE_DUPLIQUE', 'BLOQUANTE', e.code, `signature.${c}`, `Signature en double : ${c}.`))
      const champs = e.champs.map((c) => c.code)
      champs.filter((c, i) => champs.indexOf(c) !== i).forEach((c) => ajouter('CHAMP_CODE_DUPLIQUE', 'BLOQUANTE', e.code, c, `Champ en double : ${c}.`))
      e.champs.filter((c) => c.type === 'CHOIX' && !c.choix.length).forEach((c) => ajouter('CHAMP_CHOIX_ABSENT', 'BLOQUANTE', e.code, c.code, 'Liste de choix vide.'))
      // Livrables portables : ceux de l'étape et des étapes antérieures (ancêtres).
      const ancetres = new Set<string>()
      const remonter = (c: string) => p.etapes.find((x) => x.code === c)?.predecesseurs.forEach((x) => { if (!ancetres.has(x)) { ancetres.add(x); remonter(x) } })
      remonter(e.code)
      const portables = [...e.livrables.map((l) => l.code), ...p.etapes.filter((x) => ancetres.has(x.code)).flatMap((x) => x.livrables.map((l) => l.code))]
      e.signatures.forEach((s) => {
        intervenant(e, s.signataire, `signature.${s.code}`)
        if (!s.porte.length) ajouter('SIGNATURE_SANS_LIVRABLE', 'BLOQUANTE', e.code, `signature.${s.code}`, 'La signature ne porte sur aucun livrable.')
        s.porte.filter((x) => !portables.includes(x)).forEach((x) => ajouter('SIGNATURE_PORTEE_INVALIDE', 'BLOQUANTE', e.code, `signature.${s.code}`, `Livrable hors de portée : ${x}.`))
      })
      livrablesVus.push(...e.livrables.map((l) => l.code))
    })
    // D-98 : initiateurs et formulaire de lancement.
    a.push(...anomaliesPublic(initiateurs(p)))
    const champsFormulaire = (p.formulaire?.champs ?? []).map((c) => c.code)
    champsFormulaire.filter((c, n) => champsFormulaire.indexOf(c) !== n).forEach((c) => ajouter('CHAMP_CODE_DUPLIQUE', 'BLOQUANTE', null, `formulaire.${c}`, `Code de champ du formulaire en double : ${c}.`))
    ;(p.formulaire?.champs ?? []).filter((c) => c.type === 'LISTE' && !c.choix.length).forEach((c) => ajouter('CHAMP_CHOIX_ABSENT', 'BLOQUANTE', null, `formulaire.${c.code}`, `Liste sans valeur : ${c.code}.`))
    return a
  }

  // ---------------------------------------------------------------- instances
  const instances = etat.instances
  const lien = (inst: InstanceSim, etape?: string) => `/des/processus/instances/${inst.id}${etape ? `?etape=${encodeURIComponent(etape)}` : ''}`
  /**
   * D-99, séparation des fonctions (comme MS-PILOTAGE) : sauf autorisation de la définition (initiateurs.initiateurIntervenant)
   * ou intervenant LANCEUR, l'initiateur n'est ni résolu, ni désigné, ni proposé, ni réaffecté, et n'exécute, ne prend, ne
   * valide ni ne signe les étapes de sa demande.
   */
  const exclu = (inst: InstanceSim): string | null => (definitions.find((p) => p.id === inst.processus.id)?.initiateurs?.initiateurIntervenant ? null : inst.lanceur.id)
  const separe = (inst: InstanceSim, pid: string, role: Intervenant | null | undefined) => pid === exclu(inst) && role?.mode !== 'LANCEUR'
  const separation = (status: number, details: Record<string, unknown>) => refus(status, 'PIL_PROCESSUS_SEPARATION_FONCTIONS', MESSAGE_SEPARATION, details)
  /** Destinataires d'une attente (comme MS-PILOTAGE) : la personne, sinon les porteurs du profil (sans l'initiateur écarté), à défaut le chef. */
  const destinatairesDe = (inst: InstanceSim, a: Attendu | null): string[] => {
    if (!a) return []
    if (a.personne) return [a.personne.id]
    const x = exclu(inst)
    const l = (a.profil ? (a.unite ? porteursDans(a.unite, a.profil) : porteursPartout(a.profil))
      : a.unite ? magasin.organisation.filter((m) => m.actif && !!m.unite && magasin.sousUnites(a.unite!).includes(m.unite)) : []).map((m) => m.id).filter((m) => m !== x)
    const chef = a.unite ? chefDe(a.unite) : null
    return l.length ? l : chef && chef !== x ? [chef] : []
  }
  /**
   * QUO-05 : notification PROCESSUS de MS-NOTIFICATION simulée (mêmes titres que ReglesNotifications), jamais à l'acteur ;
   * gardée avec l'état et remise aux abonnés (cloche de la personne connectée).
   */
  const notifier = (inst: InstanceSim, qui: string[], acteur: PersonneActivite | null, titre: string, suite: string | null, etape: string | null, cle: string) => {
    const le = maintenant()
    const sousTitre = [inst.intitule, suite].filter(Boolean).join(', ') || null
    ;[...new Set(qui)].filter((d) => d !== acteur?.id).forEach((destinataire) => {
      const n: NotificationProcessusDemo = {
        destinataire, type: 'PROCESSUS', titre, sousTitre, lien: lien(inst, etape ?? undefined), acteur: acteur ? { id: acteur.id, nomAffiche: acteur.nomAffiche } : null,
        reference: { service: 'pilotage', type: etape ? 'ETAPE' : 'INSTANCE', id: `${inst.id}${etape ? `/${etape}` : ''}/${cle}` }, le,
      }
      etat.notifications = [...(etat.notifications ?? []), n]
      ;(ABONNES.get(magasin) ?? []).forEach((f) => f(n))
    })
  }
  /** Échéance d'une notification, au format local (JJ/MM/AAAA). */
  const jourLocal = (d: string | null) => (d && /^\d{4}-\d{2}-\d{2}/.test(d) ? `${d.slice(8, 10)}/${d.slice(5, 7)}/${d.slice(0, 4)}` : d)
  const attenteSoumise = (inst: InstanceSim, e: EtapeSim, acteur: PersonneActivite, verbe: 'soumise' | 'signée') => {
    if (e.etat === 'A_SIGNER') {
      const min = Math.min(...e.signatures.filter((s) => s.etat === 'ATTENDUE').map((s) => s.ordre))
      notifier(inst, e.signatures.filter((s) => s.etat === 'ATTENDUE' && s.ordre === min).flatMap((s) => destinatairesDe(inst, s.attendu)), acteur, `Signature attendue : ${e.libelle}`, `${verbe} par ${acteur.nomAffiche}`, e.code, `ATTENTE-${e.renvois}-${verbe}`)
    } else if (e.etat === 'A_VALIDER') {
      notifier(inst, destinatairesDe(inst, e.validateur), acteur, `Validation attendue : ${e.libelle}`, `${verbe} par ${acteur.nomAffiche}`, e.code, `VALIDATION-${e.renvois}`)
    }
  }
  /**
   * Résolution par profil (désignation hors de l'unité) : porteurs actifs dans l'unité, à défaut dans tout le GIMAC ; un
   * seul : à lui ; plusieurs : à prendre (sans unité s'ils sont hors de l'unité) ; aucun : chef de l'unité, à défaut lanceur.
   */
  const parProfil = (inst: InstanceSim, profil: string | null, unite: string, sauf: string | null = null): Attendu => {
    if (!profil) return { personne: null, profil: null, unite }
    const dans = porteursDans(unite, profil).filter((m) => m.id !== sauf)
    if (dans.length === 1) return { personne: personne(dans[0].id), profil: null, unite }
    if (dans.length > 1) return { personne: null, profil, unite }
    const ailleurs = porteursPartout(profil).filter((m) => m.id !== sauf)
    if (ailleurs.length === 1) return { personne: personne(ailleurs[0].id), profil: null, unite }
    if (ailleurs.length > 1) return { personne: null, profil, unite: null }
    // Aucun autre porteur : chef de l'unité ; chef initiateur (D-99) : son responsable ; sinon le lanceur s'il est admis.
    const chef = chefDe(unite)
    if (chef && chef !== sauf && membre(chef)?.actif !== false) return { personne: personne(chef), profil: null, unite }
    const superieur = chef && chef === sauf ? membre(chef)?.responsableId : null
    if (superieur && superieur !== sauf && membre(superieur)?.actif !== false) return { personne: personne(superieur), profil: null, unite }
    return sauf ? { personne: null, profil, unite } : { personne: personne(inst.lanceur.id), profil: null, unite }
  }
  /** Personne résolue d'un intervenant, sinon par son profil (ou son profil de remplacement). */
  const resoudre = (inst: InstanceSim, i: Intervenant, cle: string): Attendu => {
    const unite = i.unite || inst.unite
    const sauf = i.mode === 'LANCEUR' ? null : exclu(inst)
    const actif = (pid: string | null | undefined) => (pid && pid !== sauf && membre(pid)?.actif !== false ? personne(pid) : null)
    const parRemplacement = (p: PersonneActivite | null): Attendu => (p ? { personne: p, profil: null, unite } : parProfil(inst, i.remplacement ?? null, unite, sauf))
    switch (i.mode) {
      // D-138 : adressée au rôle dès qu'un porteur est dans l'unité, même seul (jamais figée sur la personne).
      case 'PROFIL': return i.profil && porteursDans(unite, i.profil).some((m) => m.id !== sauf) ? { personne: null, profil: i.profil, unite } : parProfil(inst, i.profil ?? null, unite, sauf)
      case 'PERSONNE': return parRemplacement(actif(i.personneId))
      case 'AU_LANCEMENT': return parRemplacement(actif(inst.designations[cle]?.id))
      case 'CHEF_UNITE': return parRemplacement(actif(chefDe(unite)))
      case 'LANCEUR': return parRemplacement(actif(inst.lanceur.id))
      default: return parRemplacement(actif(membre(inst.lanceur.id)?.responsableId))
    }
  }
  // D-138 : étape à prendre par un rôle, tout porteur agit sans la prendre (comme MS-PILOTAGE).
  const porteurDuRole = (e: Pick<EtapeSim, 'executant' | 'aPrendre'>) => !e.executant && !!e.aPrendre?.profil && peutAgir({ personne: null, ...e.aPrendre })
  const peutAgir = (a: Attendu | null) => !!a && (a.personne ? a.personne.id === moiId : !!a.profil && profils(moiId).includes(a.profil) && (a.unite ? membreDe(moiId, a.unite) : portePartout(moiId, a.profil)))
  /** Le lanceur choisit l'exécutant d'une étape à prendre, ou de celle qui lui est revenue faute de porteur. */
  const lanceurDesigne = (inst: InstanceSim, e: EtapeSim) => inst.lanceur.id === moiId && (!e.executant || e.executant.id === moiId)
  const peutReaffecter = (inst: InstanceSim, e: EtapeSim) => peutPiloter(inst.unite) || lanceurDesigne(inst, e)
  /** Profil exigé de l'exécutant : celui de l'étape à prendre, de l'intervenant PROFIL, à défaut son profil de remplacement. */
  const profilExige = (e: EtapeSim) => e.aPrendre?.profil ?? (e.def.intervenant.mode === 'PROFIL' ? e.def.intervenant.profil : e.def.intervenant.remplacement) ?? null
  const activer = (inst: InstanceSim, e: EtapeSim, acteur: PersonneActivite) => {
    const r = resoudre(inst, e.def.intervenant, cleDesignation.executant(e.code))
    Object.assign(e, {
      // D-106 : la tâche porte l'échéance maximale (retard au-delà) ; l'étape garde aussi l'échéance cible.
      etat: 'ACTIVE', activeeLe: maintenant(), echeanceCible: ajouterJours(aujourdhui(), e.def.delaiJours),
      echeance: ajouterJours(aujourdhui(), delaiMaxEffectif(e.def.delaiJours, e.def.delaiMaxJours)), executant: r.personne, aPrendre: r.personne ? null : { unite: r.unite, profil: r.profil },
      validateur: e.def.validateur ? resoudre(inst, e.def.validateur, cleDesignation.validateur(e.code)) : null,
      signatures: e.def.signatures.map((s) => ({ code: s.code, libelle: s.libelle, ordre: s.ordre, porte: s.porte, attendu: resoudre(inst, s.signataire, cleDesignation.signature(e.code, s.code)), etat: 'ATTENDUE', signePar: null, signeLe: null, empreinte: null, documents: [] })),
    })
    e.tacheId = pont?.creer({
      titre: `${inst.intitule} : ${e.libelle}`, description: e.action || null, responsable: r.personne?.id ?? null, uniteCible: r.personne ? null : r.unite, profilRequis: r.personne ? null : r.profil,
      createur: inst.lanceur.id, echeance: e.echeance, reference: `${inst.id}/${e.code}`, lien: lien(inst, e.code),
    }) ?? null
    historiser(inst.historique, 'ETAPE_ACTIVEE', r.personne ? r.personne.nomAffiche : `À prendre : ${r.profil ? (PRESENTATIONS_PROFILS[r.profil]?.libelle ?? r.profil) : r.unite}`, e.code, acteur)
    // QUO-05 : l'exécutant (ou les porteurs de l'étape à prendre), puis l'initiateur s'il n'en est pas (DEMANDE_AVANCEE).
    const destinataires = destinatairesDe(inst, { personne: r.personne, profil: r.profil, unite: r.unite })
    notifier(inst, destinataires, acteur, `${r.personne ? 'Étape à réaliser' : 'Étape à prendre'} : ${e.libelle}`, e.echeance ? `échéance ${jourLocal(e.echeance)}` : null, e.code, 'ACTIVEE')
    if (!destinataires.includes(inst.lanceur.id)) {
      notifier(inst, [inst.lanceur.id], acteur, `Votre demande avance : ${e.libelle}`, r.personne ? `étape en cours${e.echeance ? `, échéance ${jourLocal(e.echeance)}` : ''}` : 'en attente de prise en charge', e.code, 'AVANCEE')
    }
  }
  const terminerEtape = (inst: InstanceSim, e: EtapeSim) => {
    Object.assign(e, { etat: 'TERMINEE', termineeLe: maintenant() })
    if (e.tacheId) pont?.terminer(e.tacheId)
    historiser(inst.historique, 'ETAPE_TERMINEE', null, e.code)
    inst.etapes.filter((x) => x.etat === 'EN_ATTENTE' && x.predecesseurs.every((c) => inst.etapes.find((y) => y.code === c)?.etat === 'TERMINEE')).forEach((x) => activer(inst, x, acteurForce ?? moi()))
    inst.avancement = { total: inst.etapes.length, terminees: inst.etapes.filter((x) => x.etat === 'TERMINEE').length }
    if (inst.etapes.every((x) => x.etat === 'TERMINEE')) {
      Object.assign(inst, { etat: 'CLOSE', closeLe: maintenant() })
      historiser(inst.historique, 'INSTANCE_CLOSE')
      notifier(inst, [inst.lanceur.id], acteurForce ?? moi(), `Processus terminé : ${inst.intitule}`, null, null, 'CLOSE')
    }
  }
  const apresSoumission = (inst: InstanceSim, e: EtapeSim) => {
    if (e.signatures.some((s) => s.etat === 'ATTENDUE')) e.etat = 'A_SIGNER'
    else if (e.validateur) e.etat = 'A_VALIDER'
    else terminerEtape(inst, e)
    attenteSoumise(inst, e, acteurForce ?? moi(), 'soumise')
  }
  const vueInstance = (inst: InstanceSim): InstanceProcessus => {
    const piloter = peutPiloter(inst.unite)
    const encours = inst.etat === 'EN_COURS'
    return structuredClone({
      ...inst,
      delais: delaisInstance(inst),
      etapes: inst.etapes.map(({ def, ...brute }) => {
        const e = { ...brute, ...delaisEtape(brute) }
        const minOrdre = Math.min(...e.signatures.filter((s) => s.etat === 'ATTENDUE').map((s) => s.ordre))
        const separeExecution = separe(inst, moiId, def.intervenant)
        return {
          ...e,
          moi: {
            executer: encours && e.etat === 'ACTIVE' && (e.executant?.id === moiId || porteurDuRole(e)) && !separeExecution,
            prendre: encours && e.etat === 'ACTIVE' && !e.executant && !separeExecution && peutAgir(e.aPrendre ? { personne: null, ...e.aPrendre } : null),
            signer: encours && e.etat === 'A_SIGNER' ? e.signatures.filter((s) => s.etat === 'ATTENDUE' && s.ordre === minOrdre && peutAgir(s.attendu)
              && !separe(inst, moiId, def.signatures.find((x) => x.code === s.code)?.signataire)).map((s) => s.code) : [],
            valider: encours && e.etat === 'A_VALIDER' && peutAgir(e.validateur) && e.executant?.id !== moiId && !separe(inst, moiId, def.validateur),
            reaffecter: encours && (piloter || lanceurDesigne(inst, inst.etapes.find((x) => x.code === e.code)!)) && ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat),
          },
        }
      }),
      droits: { suspendre: piloter && encours, reprendre: piloter && inst.etat === 'SUSPENDUE', annuler: piloter && ['EN_COURS', 'SUSPENDUE'].includes(inst.etat), reaffecter: piloter && encours },
    })
  }
  // QUO-03 : le porteur qui peut prendre une étape, la valider ou la signer (profil) lit aussi l'instance, comme MS-PILOTAGE.
  const lisibleInstance = (inst: InstanceSim) => peutLire(inst.unite) || peutPiloter(inst.unite) || inst.lanceur.id === moiId
    || inst.etapes.some((e) => e.executant?.id === moiId || e.validateur?.personne?.id === moiId || e.signatures.some((s) => s.attendu.personne?.id === moiId || s.signePar?.id === moiId)
      || (e.etat === 'ACTIVE' && !e.executant && peutAgir(e.aPrendre ? { personne: null, ...e.aPrendre } : null))
      || (e.etat === 'A_VALIDER' && peutAgir(e.validateur)) || (e.etat === 'A_SIGNER' && e.signatures.some((s) => s.etat === 'ATTENDUE' && peutAgir(s.attendu))))
  // QUO-12 : refus explicite (403) comme le service ; « introuvable » (404) seulement pour une instance inexistante.
  const trouverInstance = (iid: string) => {
    const inst = instances.find((x) => x.id === iid)
    if (!inst) throw refus(404, 'PIL_PROCESSUS_INSTANCE_INTROUVABLE', 'Instance introuvable.')
    if (!lisibleInstance(inst)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Cette instance n’est lisible que de l’unité et de ses intervenants.')
    return inst
  }
  const enCours = (inst: InstanceSim) => {
    if (inst.etat !== 'EN_COURS') throw refus(409, 'PIL_PROCESSUS_INSTANCE_ETAT', 'L’instance n’est pas en cours.')
  }
  const etapeDe = (inst: InstanceSim, code: string) => {
    const e = inst.etapes.find((x) => x.code === code)
    if (!e) throw refus(404, 'PIL_PROCESSUS_ETAPE_INCONNUE', 'Étape inconnue.')
    return e
  }
  const exigerEtat = (e: EtapeSim, attendu: EtapeSim['etat']) => {
    if (e.etat !== attendu) throw refus(409, 'PIL_PROCESSUS_ETAPE_ETAT', 'L’étape n’est pas dans l’état attendu.', { etat: e.etat, attendu })
  }
  const exigerExecutant = (inst: InstanceSim, e: EtapeSim) => {
    if (e.executant?.id !== moiId && !porteurDuRole(e)) throw refus(403, 'PIL_PROCESSUS_NON_INTERVENANT', 'Seul l’exécutant de l’étape agit sur elle.')
    if (separe(inst, moiId, e.def.intervenant)) throw separation(403, { lanceurId: inst.lanceur.id })
  }
  const motifObligatoire = (m: string | undefined) => {
    if (!m?.trim() || m.trim().length > LIMITES_PROCESSUS.motif) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Motif obligatoire.', { champ: 'motif' })
    return m.trim()
  }
  /**
   * Formulaire de lancement (D-98 §2) : toutes les erreurs ensemble, 422 PIL_PROCESSUS_FORMULAIRE_INVALIDE, details.fieldErrors
   * et details.champs (« formulaire.CODE » vers le message), comme le client HTTP les rend depuis le service.
   */
  const valeursFormulaire = (p: Processus, saisies: Lancement['formulaire'], lanceur: string): ValeurChampFormulaire[] => {
    const s = saisies ?? {}
    const erreurs: { field: string; code: string; message: string }[] = []
    const erreur = (code: string, raison: string, message: string) => erreurs.push({ field: `formulaire.${code}`, code: raison, message })
    const champs = p.formulaire?.champs ?? []
    Object.keys(s).filter((c) => !champs.some((x) => x.code === c)).forEach((c) => erreur(c, 'INCONNU', 'Ce champ n’appartient pas au formulaire de lancement.'))
    const date = (v: unknown) => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v)
    const r = champs.map((c): ValeurChampFormulaire => {
      const v = s[c.code]
      const vide = v === null || v === undefined || (typeof v === 'string' && !v.trim())
      if (vide || (c.type === 'CASE' && v === false && c.obligatoire)) {
        if (c.obligatoire) erreur(c.code, 'OBLIGATOIRE', 'Champ obligatoire.')
        return { ...c, valeur: c.type === 'CASE' ? false : null }
      }
      let valeur: ValeurFormulaire = null
      switch (c.type) {
        case 'TEXTE': case 'TEXTE_LONG':
          if (String(v).trim().length > (c.type === 'TEXTE' ? LIMITES_FORMULAIRE.texte : LIMITES_FORMULAIRE.texteLong)) erreur(c.code, 'TROP_LONG', 'Texte trop long.')
          valeur = String(v).trim()
          break
        case 'NOMBRE':
          if (Number.isNaN(Number(String(v).replace(',', '.')))) erreur(c.code, 'FORMAT', 'Nombre attendu.')
          valeur = Number(String(v).replace(',', '.'))
          break
        case 'DATE':
          if (!date(v)) erreur(c.code, 'FORMAT', 'Date AAAA-MM-JJ attendue.')
          valeur = String(v)
          break
        case 'PERIODE': {
          const x = v as { debut?: string; fin?: string }
          if (!date(x?.debut) || !date(x?.fin)) erreur(c.code, 'PERIODE', 'Début et fin de la période attendus.')
          else if (x.fin! < x.debut!) erreur(c.code, 'PERIODE', 'La fin de la période précède son début.')
          valeur = { debut: x?.debut ?? '', fin: x?.fin ?? '' }
          break
        }
        case 'LISTE':
          if (!c.choix.includes(String(v))) erreur(c.code, 'HORS_LISTE', 'Valeur hors de la liste admise.')
          valeur = String(v)
          break
        case 'CASE':
          valeur = v === true || v === 'true'
          break
        default: {
          const id = typeof v === 'object' && v && 'documentId' in v ? String(v.documentId) : String(v)
          const d = documents.get(id)
          if (!d || (d.deposant && d.deposant !== lanceur) || (d.reference && d.reference !== p.id)) erreur(c.code, 'PIECE_INVALIDE', 'Pièce inconnue, ou non déposée par vous pour ce processus.')
          else if (c.typesPieces.length && !c.typesPieces.some((t) => t === d.typeMime || (t.endsWith('/*') && d.typeMime.startsWith(t.slice(0, -1))))) erreur(c.code, 'TYPE_PIECE', 'Type de pièce non admis.')
          else valeur = { documentId: id, nomFichier: d.nomFichier, typeMime: d.typeMime, taille: d.taille, sha256: d.sha256 }
        }
      }
      return { ...c, valeur }
    })
    if (erreurs.length) {
      throw refus(422, 'PIL_PROCESSUS_FORMULAIRE_INVALIDE', `Formulaire de lancement à corriger : ${erreurs.length} champ(s).`,
        { fieldErrors: erreurs.map((x) => ({ objectName: 'lancement', ...x })), champs: Object.fromEntries(erreurs.map((x) => [x.field, x.message])) })
    }
    return r
  }
  const lancer = (p: Processus, l: Lancement, lanceur: string): InstanceSim => {
    const manquantes = designationsAttendues(p.etapes).map((d) => d.cle).filter((c) => !l.designations?.[c])
    if (manquantes.length) throw refus(422, 'PIL_PROCESSUS_DESIGNATION_MANQUANTE', 'Désignations manquantes.', { cles: manquantes })
    if (Object.values(l.designations ?? {}).some((pid) => !membre(pid) || membre(pid)!.actif === false)) throw refus(422, 'PIL_PROCESSUS_DESIGNATION_INVALIDE', 'Personne désignée invalide.')
    if (!l.intitule?.trim()) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Intitulé obligatoire.', { champ: 'intitule' })
    const formulaire = valeursFormulaire(p, l.formulaire, lanceur)
    const reference = referenceSimulee(p.referenceModele, p.code, new Date(), instances.length + 1)
    const inst: InstanceSim = {
      id: id('INS'), reference, processus: { id: p.id, code: p.code, version: p.version, libelle: p.libelle }, unite: p.unite, intitule: l.intitule.trim(), etat: 'EN_COURS', lanceur: personne(lanceur),
      lanceeLe: maintenant(), closeLe: null, motif: null, designations: Object.fromEntries(Object.entries(l.designations ?? {}).map(([k, v]) => [k, personne(v)])),
      avancement: { total: p.etapes.length, terminees: 0 }, historique: [], formulaire,
      etapes: p.etapes.map((def) => ({
        def, code: def.code, libelle: def.libelle, action: def.action, predecesseurs: def.predecesseurs, position: def.position, delaiJours: def.delaiJours, etat: 'EN_ATTENTE', activeeLe: null,
        delaiMaxJours: delaiMaxEffectif(def.delaiJours, def.delaiMaxJours), echeance: null, echeanceCible: null, soumiseLe: null, termineeLe: null, enRetard: false, aLApproche: false, renvois: 0, tacheId: null, executant: null, aPrendre: null, validateur: null,
        livrables: def.livrables.map((x) => ({ ...x, depot: null })),
        champs: def.champs.map((c) => ({ ...c, valeur: def.predecesseurs.length ? null : (l.champs?.[c.code] ?? null) })),
        signatures: [], moi: { executer: false, prendre: false, signer: [], valider: false, reaffecter: false },
      })),
    }
    instances.unshift(inst)
    historiser(inst.historique, 'INSTANCE_LANCEE', null, null, personne(lanceur))
    inst.etapes.filter((e) => !e.predecesseurs.length).forEach((e) => activer(inst, e, personne(lanceur)))
    return inst
  }
  const deposer = (inst: InstanceSim, e: EtapeSim, livrable: string, documentId: string, par: string) => {
    const l = e.livrables.find((x) => x.code === livrable)
    if (!l) throw refus(404, 'PIL_PROCESSUS_LIVRABLE_INCONNU', 'Livrable inconnu.')
    const d = documents.get(documentId)
    if (!d) throw refus(422, 'PIL_PROCESSUS_DOCUMENT_INVALIDE', 'Document inconnu de MS-DOCUMENT.')
    const admis = (t: string) => t === d.typeMime || (t.endsWith('/*') && d.typeMime.startsWith(t.slice(0, -1)))
    if (l.typesPieces.length && !l.typesPieces.some(admis)) throw refus(422, 'PIL_PROCESSUS_TYPE_PIECE', 'Type de pièce non admis pour ce livrable.')
    l.depot = { documentId, ...d, deposePar: personne(par), deposeLe: maintenant() }
    historiser(inst.historique, 'LIVRABLE_DEPOSE', `${l.code} : ${d.nomFichier}`, e.code, personne(par))
  }
  const documentDemo = (nom: string, typeMime: string) => {
    const ticket = id('DOC')
    documents.set(ticket, { nomFichier: nom, typeMime, taille: 48_000 + nom.length * 1000, sha256: empreinteDemo(nom) })
    return ticket
  }
  /** Empreinte signée (§5.3) : lignes CODE:documentId:sha256 des livrables portés déposés, dans l'ordre de porte. */
  const empreinteSignature = (inst: InstanceSim, porte: string[]) => {
    const docs = porte.map((c) => inst.etapes.flatMap((e) => e.livrables).find((l) => l.code === c && l.depot)).filter((l) => !!l).map((l) => ({ livrable: l!.code, documentId: l!.depot!.documentId, sha256: l!.depot!.sha256 }))
    return { docs, empreinte: empreinteDemo(docs.map((x) => `${x.livrable}:${x.documentId}:${x.sha256}\n`).join('')) }
  }

  // ---------------------------------------------------------------- jeu de démonstration (une fois par état partagé)
  const demonstration = () => {
    const flux = nouvelle({ code: 'OUVERTURE_FLUX_RESEAU', unite: 'POLE_RESEAU', libelle: 'Ouverture d’un flux réseau', description: 'Demande, analyse de sécurité, mise en œuvre et recette d’un flux entre la plateforme et un participant.', categorie: 'Réseau', delaiCibleJours: 10, delaiMaxJours: 15, etapes: definitionDemo() }, 'u-resp-info')
    Object.assign(flux, { etat: 'PUBLIEE', publieeLe: `${ajouterJours(aujourdhui(), -20)}T10:00:00+01:00`, publieePar: personne('u-resp-info'), motifPublication: null })
    flux.historique.push({ quand: flux.publieeLe!, acteur: personne('u-resp-info'), action: 'PUBLIE', etape: null, detail: null })
    const changement = nouvelle(definitionChangementDemo(), 'systeme')
    Object.assign(changement, { etat: 'PUBLIEE', publieeLe: maintenant(), publieePar: personne('u-resp-info') })
    nouvelle({
      code: 'ACCES_VPN_PARTICIPANT', unite: 'POLE_RESEAU', libelle: 'Accès VPN d’un participant', description: 'Ouverture d’un accès VPN de recette pour une institution membre.', categorie: 'Réseau', delaiCibleJours: 5,
      etapes: [
        { code: 'DEMANDE', libelle: 'Demande d’accès', action: 'Décrire l’accès demandé.', intervenant: { mode: 'LANCEUR' }, validateur: null, predecesseurs: [], delaiJours: 1, livrables: [], signatures: [], champs: [{ code: 'INSTITUTION', libelle: 'Institution', type: 'TEXTE', choix: [], obligatoire: true }], position: pos(0, 80) },
        { code: 'CONFIGURATION', libelle: 'Configuration du VPN', action: 'Créer le tunnel et transmettre les paramètres.', intervenant: { mode: 'PERSONNE', personneId: 'u-res' }, validateur: { mode: 'CHEF_UNITE' }, predecesseurs: ['DEMANDE'], delaiJours: 2, livrables: [{ code: 'FICHE_VPN', libelle: 'Fiche de paramètres', typesPieces: ['application/pdf'], obligatoire: true, modele: null }], signatures: [], champs: [], position: pos(300, 80) },
      ],
    }, 'U-001')
    // Instance arrivée à la signature du procès-verbal de recette par le chef d'unité.
    // D-99 : l'initiatrice (technicienne réseau) n'intervient pas sur sa demande ; l'ingénieur réseau mène l'analyse et la mise en œuvre.
    const instA = lancer(flux, { intitule: 'Flux SFTP vers la Banque de Libreville', champs: { SOURCE: '10.20.4.15', DESTINATION: '172.16.8.40', PORT: 22, PROTOCOLE: 'TCP', DATE_SOUHAITEE: ajouterJours(aujourdhui(), 3) }, designations: { MISE_EN_OEUVRE: 'u-res' } }, 'u-collab')
    const derouler = (inst: InstanceSim, code: string, executant: string, livrables: [string, string, string][], valeurs: Record<string, string | number> = {}) => {
      acteurForce = personne(executant)
      const e = etapeDe(inst, code)
      if (!e.executant) {
        e.executant = personne(executant)
        e.aPrendre = null
        if (e.tacheId) pont?.affecter(e.tacheId, executant)
      }
      livrables.forEach(([l, nom, type]) => deposer(inst, e, l, documentDemo(nom, type), executant))
      e.champs.forEach((c) => { if (valeurs[c.code] !== undefined) c.valeur = valeurs[c.code] })
      e.soumiseLe = maintenant()
      return e
    }
    terminerEtape(instA, derouler(instA, 'DEMANDE', 'u-collab', []))
    const analyse = derouler(instA, 'ANALYSE', 'u-res', [['NOTE_ANALYSE', 'note-analyse-flux-sftp.pdf', 'application/pdf']])
    analyse.etat = 'A_VALIDER'
    acteurForce = personne('u-resp-info')
    historiser(instA.historique, 'ETAPE_VALIDEE', null, 'ANALYSE')
    terminerEtape(instA, analyse)
    terminerEtape(instA, derouler(instA, 'MISE_EN_OEUVRE', 'u-res', [['CONFIGURATION', 'regles-pare-feu-sftp.txt', 'text/plain']], { EQUIPEMENTS: 'FW-CORE-01, FW-CORE-02' }))
    apresSoumission(instA, derouler(instA, 'RECETTE', 'u-res', [['PV_RECETTE', 'pv-recette-flux-sftp.pdf', 'application/pdf']], { RESULTAT: 'Conforme' }))
    // Instance à l'analyse : tâche à prendre par les porteurs du profil « Pôle réseau ».
    const instB = lancer(flux, { intitule: 'Ouverture du port 443 pour la supervision', champs: { SOURCE: '10.20.6.2', DESTINATION: '10.30.1.9', PORT: 443, PROTOCOLE: 'TCP' }, designations: { MISE_EN_OEUVRE: 'u-res' } }, 'u-collab')
    terminerEtape(instB, derouler(instB, 'DEMANDE', 'u-collab', []))
    // D-98 : demandes ouvertes à tout le GIMAC, publiées par le service destinataire.
    const publiees = demandesDemo().map((d) => {
      const auteur = d.unite === 'RH' ? 'u-rh-chef' : 'u-resp-info'
      const p = nouvelle({ code: d.code, unite: d.unite, libelle: d.libelle, description: d.description, categorie: d.categorie, delaiCibleJours: 5, etapes: d.etapes,
        initiateurs: { regles: ['TOUT_LE_GIMAC'], unites: [], profils: [], permissions: [] }, formulaire: d.formulaire }, auteur)
      Object.assign(p, { etat: 'PUBLIEE', publieeLe: `${ajouterJours(aujourdhui(), -10)}T09:00:00+01:00`, publieePar: personne(auteur), motifPublication: null })
      p.historique.push({ quand: p.publieeLe!, acteur: personne(auteur), action: 'PUBLIE', etape: null, detail: null })
      return p
    })
    // D-100 : publics restreints par leur créateur (personnes désignées, profils).
    publicsRestreintsDemo().forEach((d) => {
      const p = nouvelle({ code: d.code, unite: d.unite, libelle: d.libelle, description: d.description, categorie: d.categorie, delaiCibleJours: 5, etapes: d.etapes, initiateurs: d.initiateurs, formulaire: d.formulaire }, d.auteur)
      Object.assign(p, { etat: 'PUBLIEE', publieeLe: `${ajouterJours(aujourdhui(), -5)}T09:00:00+01:00`, publieePar: personne(d.auteur), motifPublication: null })
      p.historique.push({ quand: p.publieeLe!, acteur: personne(d.auteur), action: 'PUBLIE', etape: null, detail: null })
    })
    // L'ingénieur d'homologation a demandé un congé : vérifié par les RH, en attente de l'accord de son supérieur.
    acteurForce = personne('U-101')
    const conge = lancer(publiees[0], { intitule: 'Congé annuel de fin d’année', champs: {}, designations: {},
      formulaire: { PERIODE: { debut: ajouterJours(aujourdhui(), 30), fin: ajouterJours(aujourdhui(), 44) }, MOTIF: 'Congé annuel.' } }, 'U-101')
    terminerEtape(conge, derouler(conge, 'ANALYSE', 'u-rh', [], { SOLDE_JOURS: 24 }))
    acteurForce = null
  }
  if (!existant && !repris) demonstration()

  // État repris d'un autre profil de démonstration (D-98) : les tâches des étapes activées renaissent dans « Mon activité »,
  // en cours comme terminées ou annulées (QUO-19 : l'historique des tâches de processus survit au rechargement) ; une
  // tâche déjà gardée par « Mon activité » (démonstration continue) est reprise telle quelle.
  if (repris && pont) {
    for (const inst of instances) {
      for (const e of inst.etapes.filter((x) => x.etat !== 'EN_ATTENTE' && (x.activeeLe || x.etat === 'ACTIVE' || x.etat === 'A_SIGNER' || x.etat === 'A_VALIDER'))) {
        const reference = `${inst.id}/${e.code}`
        const gardee = pont.trouver?.(reference) ?? null
        if (gardee) {
          e.tacheId = gardee
          continue
        }
        const enCoursEtape = ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)
        if (!enCoursEtape && !e.executant) continue
        e.tacheId = pont.creer({
          titre: `${inst.intitule} : ${e.libelle}`, description: e.action || null, responsable: e.executant?.id ?? null, uniteCible: e.executant ? null : e.aPrendre?.unite ?? null,
          profilRequis: e.executant ? null : e.aPrendre?.profil ?? null, createur: inst.lanceur.id, echeance: e.echeance, reference, lien: lien(inst, e.code),
        })
        if (e.etat === 'TERMINEE') pont.terminer(e.tacheId)
        else if (e.etat === 'ANNULEE') pont.annuler(e.tacheId)
      }
    }
  }
  // La prise d'une tâche d'étape (Mon activité) désigne l'exécutant de l'étape (§5.3) ; jamais par l'initiateur (D-99).
  if (pont) {
    pont.verifierPrise = (tid, pid, attribution) => {
      const inst = instances.find((i) => i.etapes.some((e) => e.tacheId === tid))
      const e = inst?.etapes.find((x) => x.tacheId === tid)
      if (inst && e && separe(inst, pid, e.def.intervenant)) throw separation(attribution ? 422 : 403, { personneId: pid })
    }
    pont.surPrise = (tid, p) => {
      const inst = instances.find((i) => i.etapes.some((e) => e.tacheId === tid))
      const e = inst?.etapes.find((x) => x.tacheId === tid)
      if (!inst || !e) return
      Object.assign(e, { executant: p, aPrendre: null })
      historiser(inst.historique, 'ETAPE_PRISE', p.nomAffiche, e.code, p)
      garderEtat(etat)
    }
  }

  const resumeInstance = (inst: InstanceSim): ResumeInstance => {
    // D-98 : actions de l'appelant par étape (comme MS-PILOTAGE), « attend ma décision » hors réaffectation.
    const moi = new Map(vueInstance(inst).etapes.map((e) => [e.code, e.moi]))
    return {
      id: inst.id, processus: inst.processus, unite: inst.unite, intitule: inst.intitule, etat: inst.etat, lanceur: inst.lanceur, lanceeLe: inst.lanceeLe, closeLe: inst.closeLe, avancement: inst.avancement,
      etapesEnCours: inst.etapes.filter((e) => ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)).map((e) => {
        const m = moi.get(e.code)!
        return { code: e.code, libelle: e.libelle, etat: e.etat, echeance: e.echeance, moi: m, attendMoi: m.executer || m.prendre || m.signer.length > 0 || m.valider }
      }),
    }
  }
  /** D-106 : retard (échéance maximale dépassée) et échéance proche (cible atteinte, maximum non dépassé), comme MS-PILOTAGE. */
  const delaisEtape = (e: EtapeInstance) => {
    const jour = aujourdhui()
    const enCours = ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)
    const cible = e.echeanceCible ?? e.echeance
    const enRetard = !!e.echeance && enCours && e.echeance < jour
    return { echeanceCible: cible ?? null, enRetard, aLApproche: !!cible && enCours && cible <= jour && !enRetard }
  }
  const delaisInstance = (inst: InstanceSim) => {
    const p = definitions.find((x) => x.id === inst.processus.id)
    if (!p || (p.delaiCibleJours === null && (p.delaiMaxJours ?? null) === null)) return null
    const maxJours = p.delaiCibleJours === null ? p.delaiMaxJours! : delaiMaxEffectif(p.delaiCibleJours, p.delaiMaxJours)
    const cibleJours = p.delaiCibleJours ?? maxJours
    const lancee = jourDe(inst.lanceeLe)
    const echeanceCible = ajouterJours(lancee, cibleJours)
    const echeanceMax = ajouterJours(lancee, maxJours)
    const ouverte = !['CLOSE', 'ANNULEE'].includes(inst.etat)
    const jour = aujourdhui()
    const enRetard = ouverte && echeanceMax < jour
    return { cibleJours, maxJours, echeanceCible, echeanceMax, enRetard, aLApproche: ouverte && !enRetard && echeanceCible <= jour }
  }
  const demande = (inst: InstanceSim): DemandeProcessus => ({
    id: inst.id, processus: inst.processus, unite: inst.unite, intitule: inst.intitule, etat: inst.etat, lanceeLe: inst.lanceeLe, closeLe: inst.closeLe, motif: inst.motif, avancement: inst.avancement,
    etapes: inst.etapes.map((e) => ({
      code: e.code, libelle: e.libelle, etat: e.etat, executant: e.aPrendre && !e.executant ? null : e.executant, aPrendre: e.executant ? null : e.aPrendre, activeeLe: e.activeeLe, echeance: e.echeance,
      termineeLe: e.termineeLe, ...delaisEtape(e),
    })),
    derniereEvolution: inst.historique.at(-1) ?? null,
  })
  const implique = (inst: InstanceSim) => inst.etapes.some((e) => e.executant?.id === moiId || e.validateur?.personne?.id === moiId
    || e.signatures.some((s) => s.attendu.personne?.id === moiId || s.signePar?.id === moiId) || (e.etat === 'ACTIVE' && !e.executant && peutAgir(e.aPrendre ? { personne: null, ...e.aPrendre } : null))
    || (e.etat === 'A_SIGNER' && e.signatures.some((s) => s.etat === 'ATTENDUE' && peutAgir(s.attendu))))

  async function deposerFragmente(fichier: File, reference: string, o: OptionsTeleversement): Promise<Televersement> {
    const pause = (ms: number) => new Promise<void>((ok, ko) => {
      const t = window.setTimeout(ok, ms)
      o.signal?.addEventListener('abort', () => {
        window.clearTimeout(t)
        ko(new DOMException('Envoi annulé', 'AbortError'))
      })
    })
    const total = fichier.size
    for (let i = 0; i <= 10; i++) {
      o.surProgression?.({ phase: 'envoi', envoye: Math.round((total * i) / 10), total })
      if (i < 10) await pause(DUREE_ETAPE_DOCUMENT_MS / 4)
    }
    o.surProgression?.({ phase: 'verification', envoye: total, total })
    await pause(DUREE_ETAPE_DOCUMENT_MS / 4)
    const ticket = id('DOC')
    const sha256 = empreinteDemo(`${fichier.name}:${total}`)
    const contenu = total <= CONTENU_MAX ? await enBase64(fichier).catch(() => undefined) : undefined
    documents.set(ticket, { nomFichier: fichier.name, typeMime: fichier.type || 'application/octet-stream', taille: total, sha256, reference, deposant: moiId, contenu })
    garderEtat(etat)
    o.surProgression?.({ phase: 'pret', envoye: total, total })
    return { documentId: ticket, sha256, dedoublonne: false, depotId: id('DEPOT') }
  }
  const service: ServiceProcessus = {
    // D-92 : portée de l'appelant (GET /processus/portee).
    portee: () => attendre(() => {
      const tous = refMetier() || definirToutes() || lancerToutes()
      const dirigees = magasin.unitesDirigees(moiId)
      const lisibles = magasin.unites.map((u) => u.code).filter((u) => peutLire(u))
      return {
        portee: tous ? 'TOUS_DOMAINES' as const : 'MES_UNITES' as const,
        definirToutesUnites: refMetier() || definirToutes(),
        lancerToutesUnites: refMetier() || lancerToutes(),
        auTitreDe: [...(refMetier() ? ['REF_METIER_ADMIN'] : []), ...(definirToutes() ? ['PROCESSUS_DEFINIR:TOUTES'] : []), ...(lancerToutes() ? ['PROCESSUS_LANCER:TOUTES'] : [])],
        unites: tous ? [] : lisibles,
        unitesDefinissables: tous ? [] : magasin.unites.filter((u) => u.active && magasin.couvreUnite(moiId, 'PROCESSUS_DEFINIR', u.code)).map((u) => u.code).filter((u) => dirigees.includes(u) || magasin.actionsDe(moiId).PROCESSUS_DEFINIR === 'TOUTES'),
      }
    }),
    lister: (q) => attendre(() => definitions
      .filter((p) => peutLire(p.unite) && (p.etat !== 'BROUILLON' || peutDefinir(p.unite)))
      .filter((p) => (!q.unite || p.unite === q.unite) && (!q.etat || p.etat === q.etat) && (!q.q?.trim() || `${p.code} ${p.libelle} ${p.categorie}`.toLowerCase().includes(q.q.trim().toLowerCase())))
      .sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr') || b.version - a.version)
      .map(resume)),
    // D-98 : versions publiées que je peux lancer, par unité propriétaire.
    lancables: (q) => attendre(() => {
      const r = q.q?.trim().toLowerCase()
      const l = definitions.filter((p) => p.etat === 'PUBLIEE' && peutLancerProcessus(p) && (!r || `${p.code} ${p.libelle} ${p.categorie}`.toLowerCase().includes(r)))
        .sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr'))
      const unites = [...new Set(l.map((p) => p.unite))].sort()
      return { groupes: unites.map((u) => ({ unite: u, processus: l.filter((p) => p.unite === u).map(resume) })) }
    }),
    // D-98 : « Mes demandes ».
    demandes: (q) => attendre(() => {
      const r = q.q?.trim().toLowerCase()
      const l = instances.filter((i) => i.lanceur.id === moiId && (!q.etat || i.etat === q.etat) && (!r || `${i.intitule} ${i.processus.libelle} ${i.processus.code}`.toLowerCase().includes(r)))
        .sort((a, b) => b.lanceeLe.localeCompare(a.lanceeLe))
      const page = q.page ?? 0
      const taille = Math.min(q.taille ?? 20, 100)
      return { contenu: l.slice(page * taille, (page + 1) * taille).map(demande), total: l.length, page, taille }
    }),
    lire: (pid) => attendre(() => vue(trouver(pid))),
    creer: (s) => attendre(() => {
      verifierSaisie(s)
      exigerDefinir(s.unite)
      // Code généré comme MS-PILOTAGE ({UNITE}-PRC-{N}, CodeProcessus) : celui de la saisie est ignoré.
      const code = codeGenere(s.unite)
      return vue(nouvelle({ ...s, code, etapes: reecrireEtapes(code, s.etapes.map(normaliserEtape), new Set()) }, moiId))
    }),
    modifier: (pid, s) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      if (p.etat !== 'BROUILLON') throw refus(409, 'PIL_PROCESSUS_ETAT', 'Seul un brouillon se modifie.')
      verifierSaisie({ ...s, code: p.code, unite: p.unite })
      Object.assign(p, { libelle: s.libelle.trim(), description: s.description ?? '', categorie: s.categorie ?? '', delaiCibleJours: s.delaiCibleJours, delaiMaxJours: s.delaiMaxJours ?? null, etapes: reecrireEtapes(p.code, s.etapes.map(normaliserEtape), new Set(definitions.filter((d) => d.code === p.code).flatMap((d) => d.etapes.map((e) => e.code)))), modifieLe: maintenant(), modifiePar: moi() })
      // D-98 : absents d'une modification, initiateurs et formulaire de la définition sont gardés (comme MS-PILOTAGE).
      if (s.initiateurs) p.initiateurs = normaliserInitiateurs(s.initiateurs)
      if (s.formulaire) p.formulaire = normaliserFormulaire(s.formulaire)
      historiser(p.historique, 'MODIFIE')
      return vue(p)
    }),
    supprimer: (pid) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      if (p.etat !== 'BROUILLON') throw refus(409, 'PIL_PROCESSUS_ETAT', 'Seul un brouillon se supprime.')
      definitions.splice(definitions.indexOf(p), 1)
    }),
    controler: (pid) => attendre((): ControleProcessus => {
      const p = trouver(pid)
      if (!peutLire(p.unite)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Le contrôle d’un processus est réservé aux lecteurs de son unité.')
      const a = controler(p)
      return { publiable: !a.some((x) => x.gravite !== 'AVERTISSEMENT'), anomalies: a }
    }),
    publier: (pid, o) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      if (p.etat !== 'BROUILLON') throw refus(409, 'PIL_PROCESSUS_ETAT', 'Seul un brouillon se publie.')
      const a = controler(p)
      if (a.some((x) => x.gravite === 'BLOQUANTE')) throw refus(422, 'PIL_PROCESSUS_ANOMALIES', 'Le processus présente des anomalies bloquantes.', { anomalies: a })
      const levables = a.filter((x) => x.gravite === 'LEVABLE')
      if (levables.length && !o.leverAvertissements) throw refus(422, 'PIL_PROCESSUS_AVERTISSEMENTS', 'Des avertissements doivent être levés.', { anomalies: levables })
      const motif = o.leverAvertissements ? motifObligatoire(o.motif) : o.motif?.trim() || null
      const ancienne = definitions.find((x) => x.code === p.code && x.etat === 'PUBLIEE')
      if (ancienne) Object.assign(ancienne, { etat: 'RETIREE', retireeLe: maintenant(), retireePar: moi(), motifRetrait: `Remplacée par la version ${p.version}` })
      Object.assign(p, { etat: 'PUBLIEE', publieeLe: maintenant(), publieePar: moi(), motifPublication: motif, avertissementsLeves: !!levables.length })
      historiser(p.historique, 'PUBLIE', motif)
      return vue(p)
    }),
    nouvelleVersion: (pid) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      if (p.etat === 'BROUILLON') throw refus(409, 'PIL_PROCESSUS_ETAT', 'Le processus est déjà un brouillon.')
      if (definitions.some((x) => x.code === p.code && x.etat === 'BROUILLON')) throw refus(409, 'PIL_PROCESSUS_BROUILLON_EXISTANT', 'Un brouillon existe déjà pour ce processus.')
      const version = Math.max(...definitions.filter((x) => x.code === p.code).map((x) => x.version)) + 1
      return vue(nouvelle({ ...p, etapes: structuredClone(p.etapes) }, moiId, { version }))
    }),
    retirer: (pid, m) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      const motif = motifObligatoire(m)
      if (p.etat !== 'PUBLIEE') throw refus(409, 'PIL_PROCESSUS_ETAT', 'Seule une version publiée se retire.')
      Object.assign(p, { etat: 'RETIREE', retireeLe: maintenant(), retireePar: moi(), motifRetrait: motif })
      historiser(p.historique, 'RETIRE', motif)
      return vue(p)
    }),

    // D-103 : public d'un brouillon ou d'une version publiée, sans nouvelle version (mêmes refus que MS-PILOTAGE).
    modifierPublic: (pid, s) => attendre(() => {
      const p = trouver(pid)
      exigerDefinir(p.unite)
      if (p.etat === 'RETIREE') throw refus(409, 'PIL_PROCESSUS_NON_MODIFIABLE', 'Une version retirée ne se modifie plus, son public compris', { etat: p.etat })
      if (!s?.initiateurs) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Public absent', { champ: 'initiateurs' })
      verifierInitiateurs(s.initiateurs)
      const publiee = p.etat === 'PUBLIEE'
      const motif = motifPublic(s.motif, publiee)
      const avant = normaliserInitiateurs(initiateurs(p))
      const apres = normaliserInitiateurs({ ...s.initiateurs, initiateurIntervenant: !!avant.initiateurIntervenant })
      if (publiee) {
        const a = anomaliesPublic(apres)
        if (a.length) throw refus(422, 'PIL_PROCESSUS_ANOMALIES', 'Le public comporte des anomalies bloquantes', { anomalies: a })
      }
      if (JSON.stringify(apres) === JSON.stringify(avant)) return vue(p)
      Object.assign(p, { initiateurs: apres, modifieLe: maintenant(), modifiePar: moi() })
      historiser(p.historique, 'PUBLIC_MODIFIE', `Public de la version ${p.version} modifié${motif ? ` : ${motif}` : ''} ; avant : ${resumePublic(avant)} ; après : ${resumePublic(apres)}`)
      return vue(p)
    }),

    instances: (q) => attendre(() => {
      const l = instances
        .filter((i) => (q.vue === 'miennes' ? implique(i) : q.vue === 'lancees' ? i.lanceur.id === moiId : lisibleInstance(i) && (peutLire(i.unite) || peutPiloter(i.unite))))
        .filter((i) => !q.etat || i.etat === q.etat)
        .map(resumeInstance)
      const page = q.page ?? 0
      const taille = Math.min(q.taille ?? 20, 100)
      return { contenu: l.slice(page * taille, (page + 1) * taille), total: l.length, page, taille }
    }),
    lancer: (pid, l) => attendre(() => {
      // Comme MS-PILOTAGE : la définition est cherchée sans condition de lecture, puis les initiateurs décident (403).
      const p = definitions.find((x) => x.id === pid)
      if (!p) throw refus(404, 'PIL_PROCESSUS_INTROUVABLE', 'Processus introuvable.')
      if (!peutLancerProcessus(p)) {
        throw nonInitiateur(p)
      }
      if (p.etat !== 'PUBLIEE') throw refus(409, 'PIL_PROCESSUS_NON_PUBLIE', 'Seule une version publiée se lance.')
      // Personne désignée : active, et le lanceur lui-même, porteuse du profil de remplacement (dans l'unité ou ailleurs
      // dans le GIMAC), ou dans la portée TACHE_CONFIER du lanceur.
      for (const d of designationsAttendues(p.etapes)) {
        const pid = l.designations[d.cle]
        if (!pid) continue
        if (pid === moiId && !p.initiateurs?.initiateurIntervenant) throw separation(422, { cle: d.cle, personneId: pid })
        const admis = membre(pid)?.actif !== false && !!membre(pid) && (pid === moiId || !d.intervenant.remplacement || portePartout(pid, d.intervenant.remplacement) || magasin.couvre(moiId, 'TACHE_CONFIER', pid))
        if (!admis) throw refus(422, 'PIL_PROCESSUS_DESIGNATION_INVALIDE', 'Personne désignée inactive, ou ne portant pas le profil requis, ni dans votre portée.', { cle: d.cle, personneId: pid })
      }
      return vueInstance(lancer(p, l, moiId))
    }),
    instance: (iid) => attendre(() => vueInstance(trouverInstance(iid))),
    telechargerPdf: (iid) => attendre(() => {
      const inst = trouverInstance(iid)
      const signatures = inst.etapes.flatMap((e) => e.signatures.filter((s) => s.etat === 'SIGNEE').map((s) => `${s.code}:${s.signePar?.nomAffiche ?? ''}:${s.empreinte ?? ''}`))
      return `%PDF-1.4\n% ${inst.reference ?? inst.id}\n${inst.intitule}\n${signatures.join('\n')}`
    }).then((contenu) => new Blob([contenu], { type: 'application/pdf' })),
    // QUO-04 : pièce du formulaire de lancement ou livrable déposé, lisible de qui lit l'instance (comme MS-PILOTAGE).
    documentInstance: (iid, documentId) => attendre(() => {
      const inst = trouverInstance(iid)
      const connu = inst.formulaire?.some((c) => !!c.valeur && typeof c.valeur === 'object' && 'documentId' in c.valeur && c.valeur.documentId === documentId)
        || inst.etapes.some((e) => e.livrables.some((l) => l.depot?.documentId === documentId))
      const d = documents.get(documentId)
      if (!connu || !d) throw refus(404, 'PIL_PROCESSUS_DOCUMENT_INCONNU', 'Document inconnu de cette instance.')
      return documentId
    }).then((doc) => {
      // Le contenu est rendu hors de la copie de la réponse (un Blob ne se clone pas), comme le document d'un rapport.
      const d = documents.get(doc)!
      return { blob: d.contenu ? depuisBase64(d.contenu, d.typeMime) : contenuDemo(d), empreinte: d.sha256 }
    }),
    // D-98 : pièce jointe du formulaire de lancement, même dépôt simulé, référence = la définition lancée.
    televerserPieceLancement: (fichier: File, processusId: string, o: OptionsTeleversement = {}) => deposerFragmente(fichier, processusId, o),
    // Dépôt fragmenté simulé (D-74), finalité PROCESSUS_UNITE : progression en dix étapes, vérification, ticket.
    televerserLivrable: (fichier: File, instanceId: string, o: OptionsTeleversement = {}) => deposerFragmente(fichier, instanceId, o),
    deposerLivrable: (iid, code, livrable, documentId) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      exigerEtat(e, 'ACTIVE')
      exigerExecutant(inst, e)
      deposer(inst, e, livrable, documentId, moiId)
      return vueInstance(inst)
    }),
    saisirChamps: (iid, code, valeurs) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      exigerEtat(e, 'ACTIVE')
      exigerExecutant(inst, e)
      Object.entries(valeurs).forEach(([c, v]) => {
        const champ = e.champs.find((x) => x.code === c)
        const invalide = !champ || (v !== null && ((champ.type === 'NOMBRE' && Number.isNaN(Number(v))) || (champ.type === 'DATE' && !/^\d{4}-\d{2}-\d{2}$/.test(String(v)))
          || (champ.type === 'CHOIX' && !champ.choix.includes(String(v))) || String(v).length > LIMITES_PROCESSUS.texte))
        if (invalide) throw refus(422, 'PIL_PROCESSUS_CHAMP_INVALIDE', `Valeur invalide pour ${c}.`, { champ: c })
        champ.valeur = v === null || v === '' ? null : champ.type === 'NOMBRE' ? Number(v) : v
      })
      historiser(inst.historique, 'CHAMPS_SAISIS', null, e.code)
      return vueInstance(inst)
    }),
    signer: (iid, code, signature, assertion) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      exigerEtat(e, 'A_SIGNER')
      const s = e.signatures.find((x) => x.code === signature)
      if (!s) throw refus(404, 'PIL_PROCESSUS_SIGNATURE_INCONNUE', 'Signature inconnue.')
      if (s.etat === 'SIGNEE') throw refus(409, 'PIL_PROCESSUS_DEJA_SIGNEE', 'Signature déjà apposée.')
      if (!peutAgir(s.attendu)) throw refus(403, 'PIL_PROCESSUS_NON_SIGNATAIRE', 'Vous n’êtes pas le signataire attendu.')
      if (separe(inst, moiId, e.def.signatures.find((x) => x.code === signature)?.signataire)) throw separation(403, { lanceurId: inst.lanceur.id })
      if (e.signatures.some((x) => x.etat === 'ATTENDUE' && x.ordre < s.ordre)) throw refus(409, 'PIL_PROCESSUS_SIGNATURE_ORDRE', 'Une signature précédente est attendue.')
      if (!assertion) throw refus(428, 'PIL_PROCESSUS_REAUTH_REQUISE', 'Réauthentification forte exigée.')
      const { docs, empreinte } = empreinteSignature(inst, s.porte)
      Object.assign(s, { etat: 'SIGNEE', signePar: moi(), signeLe: maintenant(), empreinte, documents: docs })
      historiser(inst.historique, 'ETAPE_SIGNEE', s.libelle, e.code)
      if (!e.signatures.some((x) => x.etat === 'ATTENDUE')) {
        if (e.validateur) e.etat = 'A_VALIDER'
        else terminerEtape(inst, e)
      }
      attenteSoumise(inst, e, moi(), 'signée')
      return vueInstance(inst)
    }),
    terminer: (iid, code) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      exigerEtat(e, 'ACTIVE')
      exigerExecutant(inst, e)
      const livrables = e.livrables.filter((l) => l.obligatoire && !l.depot).map((l) => l.code)
      if (livrables.length) throw refus(422, 'PIL_PROCESSUS_LIVRABLES_MANQUANTS', 'Des livrables obligatoires manquent.', { livrables })
      const champs = e.champs.filter((c) => c.obligatoire && (c.valeur === null || c.valeur === '')).map((c) => c.code)
      if (champs.length) throw refus(422, 'PIL_PROCESSUS_CHAMPS_MANQUANTS', 'Des champs obligatoires manquent.', { champs })
      // D-138 : étape du rôle achevée sans prise, son auteur en devient l'exécutant (comme la tâche sur MS-PILOTAGE).
      if (!e.executant) {
        e.executant = personne(moiId)
        e.aPrendre = null
        if (e.tacheId) pont?.affecter(e.tacheId, moiId)
      }
      e.soumiseLe = maintenant()
      historiser(inst.historique, 'ETAPE_SOUMISE', null, e.code)
      apresSoumission(inst, e)
      return vueInstance(inst)
    }),
    valider: (iid, code, decision, motif) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      exigerEtat(e, 'A_VALIDER')
      if (!peutAgir(e.validateur) || e.executant?.id === moiId) throw refus(403, 'PIL_PROCESSUS_NON_VALIDATEUR', 'Vous n’êtes pas le validateur de cette étape.')
      if (separe(inst, moiId, e.def.validateur)) throw separation(403, { lanceurId: inst.lanceur.id })
      if (decision === 'RENVOYER') {
        const m = motifObligatoire(motif)
        Object.assign(e, { etat: 'ACTIVE', renvois: e.renvois + 1, signatures: e.signatures.map((s) => ({ ...s, etat: 'ATTENDUE', signePar: null, signeLe: null, empreinte: null, documents: [] })) })
        historiser(inst.historique, 'ETAPE_RENVOYEE', m, e.code)
        notifier(inst, e.executant ? [e.executant.id] : [], moi(), `Étape renvoyée : ${e.libelle}`, null, e.code, `RENVOI-${e.renvois}`)
      } else {
        historiser(inst.historique, 'ETAPE_VALIDEE', motif?.trim() || null, e.code)
        terminerEtape(inst, e)
      }
      return vueInstance(inst)
    }),
    reaffecter: (iid, code, personneId, m) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      if (!peutReaffecter(inst, e)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Réaffecter exige de diriger l’unité, le pouvoir TACHE_CONFIER, ou d’être le lanceur d’une étape à prendre.')
      const motif = motifObligatoire(m)
      if (personneId === exclu(inst)) throw separation(422, { personneId })
      if (!['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)) throw refus(409, 'PIL_PROCESSUS_ETAPE_ETAT', 'L’étape n’est pas active.', { etat: e.etat, attendu: 'ACTIVE' })
      const unite = e.def.intervenant.unite || inst.unite
      const admis = !!membre(personneId) && membre(personneId)!.actif !== false
        && (membreDe(personneId, unite) || portePartout(personneId, profilExige(e)) || magasin.couvre(moiId, 'TACHE_CONFIER', personneId))
      if (!admis) throw refus(422, 'PIL_PROCESSUS_PERSONNE_INVALIDE', 'La personne doit être active, et membre de l’unité, porteuse du profil requis ou dans votre portée.', { personneId })
      Object.assign(e, { executant: personne(personneId), aPrendre: null })
      if (e.tacheId) pont?.affecter(e.tacheId, personneId)
      historiser(inst.historique, 'ETAPE_REAFFECTEE', `${personne(personneId).nomAffiche} : ${motif}`, e.code)
      notifier(inst, [personneId], moi(), `Étape réaffectée : ${e.libelle}`, `réaffectée par ${moi().nomAffiche}`, e.code, `REAFFECTEE-${personneId}`)
      return vueInstance(inst)
    }),
    candidats: (q) => attendre(() => {
      if (!magasin.unites.some((u) => u.code === q.unite)) throw refus(422, 'PIL_PROCESSUS_UNITE_INCONNUE', 'Unité inconnue.', { unite: q.unite })
      return candidatsDe(q.unite, q.profil || null)
    }),
    candidatsEtape: (iid, code) => attendre(() => {
      const inst = trouverInstance(iid)
      enCours(inst)
      const e = etapeDe(inst, code)
      if (!peutReaffecter(inst, e)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Réservé au chef de l’unité et au lanceur d’une étape à prendre.')
      // D-99 : l'initiateur n'est jamais proposé ; « exclus » le nomme pour que l'interface l'écarte aussi de l'annuaire.
      const c = candidatsDe(e.def.intervenant.unite || inst.unite, profilExige(e))
      const x = exclu(inst)
      return { ...c, dansUnite: c.dansUnite.filter((m) => m.id !== x), ailleurs: c.ailleurs.filter((m) => m.id !== x), exclus: x ? [x] : [] }
    }),
    suspendre: (iid, m) => attendre(() => {
      const inst = trouverInstance(iid)
      if (!peutPiloter(inst.unite)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Suspendre exige de diriger l’unité.')
      const motif = motifObligatoire(m)
      enCours(inst)
      Object.assign(inst, { etat: 'SUSPENDUE', motif })
      historiser(inst.historique, 'INSTANCE_SUSPENDUE', motif)
      return vueInstance(inst)
    }),
    reprendre: (iid, m) => attendre(() => {
      const inst = trouverInstance(iid)
      if (!peutPiloter(inst.unite)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Reprendre exige de diriger l’unité.')
      if (inst.etat !== 'SUSPENDUE') throw refus(409, 'PIL_PROCESSUS_INSTANCE_ETAT', 'L’instance n’est pas suspendue.')
      Object.assign(inst, { etat: 'EN_COURS', motif: m?.trim() || null })
      historiser(inst.historique, 'INSTANCE_REPRISE', m?.trim() || null)
      return vueInstance(inst)
    }),
    annuler: (iid, m) => attendre(() => {
      const inst = trouverInstance(iid)
      if (!peutPiloter(inst.unite)) throw refus(403, 'PIL_PROCESSUS_INTERDIT', 'Annuler exige de diriger l’unité.')
      const motif = motifObligatoire(m)
      if (!['EN_COURS', 'SUSPENDUE'].includes(inst.etat)) throw refus(409, 'PIL_PROCESSUS_INSTANCE_ETAT', 'L’instance est close.')
      inst.etapes.filter((e) => e.etat !== 'TERMINEE').forEach((e) => {
        if (e.etat !== 'EN_ATTENTE' && e.tacheId) pont?.annuler(e.tacheId)
        e.etat = 'ANNULEE'
      })
      Object.assign(inst, { etat: 'ANNULEE', closeLe: maintenant(), motif })
      historiser(inst.historique, 'INSTANCE_ANNULEE', motif)
      notifier(inst, [inst.lanceur.id], moi(), `Processus annulé : ${inst.intitule}`, `annulé par ${moi().nomAffiche}`, null, 'ANNULEE')
      return vueInstance(inst)
    }),
  }
  // Chaque appel réussi garde l'état dans la session de démonstration (parcours à plusieurs personnes, D-98).
  return Object.fromEntries(Object.entries(service).map(([k, f]) => [k, async (...a: unknown[]) => {
    const r = await (f as (...x: unknown[]) => Promise<unknown>)(...a)
    garderEtat(etat)
    return r
  }])) as unknown as ServiceProcessus
}
