import type { ApiError, OptionsTeleversement, Televersement } from '@gimac/socle'
import type {
  Activite, DestinatairesRapport, EntreeEvenement, EtatRapportDuJour, EtatTache, Evenement, GroupeTaches, MembreEquipe, PersonneActivite, Rapport, Rattachement, Tache, TypeRapport, Unite,
} from '../../modele/pilotage'
import { LIMITES_GROUPE, TRANSITIONS_TACHE, motifExige } from '../../modele/pilotage'
import { magasinPouvoirs, type MagasinPouvoirs } from './pouvoirs'
import { ajouterJours, aujourdhui, instant, jourDe, jourOuvreSuivant, lundiDe, lundiDeSemaine, semaineIso } from '../../modele/jours'
import type { ServicePilotage } from '../ports'
import { pdfDemo } from './messagerie'
import { PROFILS } from '../../permissions'

// « Mon activité » simulée (D-76, docs/lot1/mon-activite.md) : mêmes formes de réponse et mêmes codes de refus
// que MS-PILOTAGE et l'organisation de MS-IAM ; ses règles ne sont qu'une imitation : équipe (chaîne des
// responsables), lecture réduite de l'agenda d'un membre, transitions des tâches, rapport unique par période,
// destinataire fixé à la soumission, visa final. Jeu de démonstration daté d'aujourd'hui, en mémoire.

type Attendre = <T>(v: T | (() => T)) => Promise<T>
type Refus = (status: number, code: string, message: string) => ApiError

/** Catalogue initial des unités (§1.1, CR p.13-14, CCT p.19-21). */
export const UNITES_DEMO: Unite[] = [
  { code: 'DG', libelle: 'Direction Générale', type: 'DIRECTION', parent: null, active: true },
  { code: 'DES', libelle: 'Département Exploitation et Supervision', type: 'DEPARTEMENT', parent: 'DG', active: true },
  { code: 'SNA', libelle: 'Service Normes et Agréments', type: 'SERVICE', parent: 'DES', active: true },
  { code: 'PROD_MONETIQUE', libelle: 'Service Production monétique', type: 'SERVICE', parent: 'DES', active: true },
  { code: 'PROD_INFO', libelle: 'Service Production informatique', type: 'SERVICE', parent: 'DES', active: true },
  { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true },
  { code: 'POLE_SYSTEME', libelle: 'Pôle système', type: 'POLE', parent: 'PROD_INFO', active: true },
  { code: 'DDRM', libelle: 'Département Développement du Réseau et du Marché', type: 'DEPARTEMENT', parent: 'DG', active: true },
  { code: 'SSI', libelle: 'Sécurité des systèmes d’information', type: 'SERVICE', parent: 'DG', active: true },
  { code: 'CONFORMITE', libelle: 'Conformité', type: 'SERVICE', parent: 'DG', active: true },
  // D-98 : service destinataire de la demande de congé.
  { code: 'RH', libelle: 'Service des ressources humaines', type: 'SERVICE', parent: 'DG', active: true },
]

type Membre = { id: string; nomAffiche: string; fonction: string; unite: string | null; responsableId: string | null }

/** Organisation de démonstration : comptes de l'annuaire simulé, unité la plus fine et responsable (n+1). */
export const ORGANISATION_DEMO: Membre[] = [
  { id: 'u-dg', nomAffiche: 'Jean-Baptiste Ella Mintsa', fonction: 'Directeur général', unite: 'DG', responsableId: null },
  { id: 'U-001', nomAffiche: 'Paul Mbarga Atangana', fonction: 'Chef du département DES', unite: 'DES', responsableId: 'u-dg' },
  { id: 'U-020', nomAffiche: 'Bertrand Nguema Obiang', fonction: 'Chef de la DDRM', unite: 'DDRM', responsableId: 'u-dg' },
  { id: 'sim-charge', nomAffiche: 'Fatimé Abakar Hissein', fonction: 'Chargée de la DDRM', unite: 'DDRM', responsableId: 'U-020' },
  { id: 'U-201', nomAffiche: 'Aïcha Mahamat Nour', fonction: 'Conductrice de projet', unite: 'DES', responsableId: 'U-001' },
  { id: 'U-101', nomAffiche: 'Joël Ekotto Mballa', fonction: 'Ingénieur d’homologation', unite: 'SNA', responsableId: 'U-001' },
  { id: 'u-sna', nomAffiche: 'Rachel Biyong Mengue', fonction: 'Analyste Normes et Agréments', unite: 'SNA', responsableId: 'U-001' },
  { id: 'u-prod', nomAffiche: 'Serge Ondoua Essomba', fonction: 'Chargé de production monétique', unite: 'PROD_MONETIQUE', responsableId: 'U-001' },
  { id: 'u-resp-info', nomAffiche: 'Gaston Ndzana Ebode', fonction: 'Chef du service Production informatique', unite: 'PROD_INFO', responsableId: 'U-001' },
  { id: 'u-res', nomAffiche: 'Idriss Mahamat Saleh', fonction: 'Ingénieur réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info' },
  { id: 'u-sys', nomAffiche: 'Chantal Ngo Bassong', fonction: 'Ingénieure système', unite: 'POLE_SYSTEME', responsableId: 'u-resp-info' },
  { id: 'u-collab', nomAffiche: 'Nadège Mvondo Assiga', fonction: 'Technicienne réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info' },
  { id: 'u-rssi', nomAffiche: 'Hervé Kamga Tchoupo', fonction: 'Responsable de la sécurité des SI', unite: 'SSI', responsableId: 'u-dg' },
  { id: 'u-conf', nomAffiche: 'Mireille Obame Nze', fonction: 'Responsable conformité', unite: 'CONFORMITE', responsableId: 'u-dg' },
  { id: 'u-rh-chef', nomAffiche: 'Solange Ateba Mvondo', fonction: 'Cheffe du service des ressources humaines', unite: 'RH', responsableId: 'u-dg' },
  { id: 'u-rh', nomAffiche: 'Thierry Bekolo Ngoa', fonction: 'Gestionnaire des ressources humaines', unite: 'RH', responsableId: 'u-rh-chef' },
]

/**
 * Profils des comptes de l'organisation absents de l'annuaire de la messagerie (service des ressources humaines, D-98) : la
 * cheffe du service le dirige par son rattachement (responsable de l'unité) et porte le profil de responsable de service,
 * comme dans l'organisation simulée de MS-PILOTAGE (QUO-01 : chacun a son compte de démonstration, profilsDemo.ts).
 */
export const PROFILS_ORGANISATION_DEMO: Record<string, string[]> = { 'u-rh-chef': ['ROLE_RESPONSABLE_SERVICE'], 'u-rh': ['ROLE_COLLABORATEUR'] }

/** Chaîne des unités, de la plus haute à la plus fine. */
const cheminDe = (unites: Unite[], code: string | null): Unite[] => {
  const chemin: Unite[] = []
  let u = unites.find((x) => x.code === code)
  while (u && chemin.length < 10) {
    chemin.unshift(u)
    u = unites.find((x) => x.code === u!.parent)
  }
  return chemin
}

/** Rattachement de démonstration d'un compte (GET organisation/moi), null s'il est hors de l'organisation simulée. */
export function rattachementDemo(id: string, nomAffiche?: string): Rattachement {
  const m = ORGANISATION_DEMO.find((x) => x.id === id)
  const resp = ORGANISATION_DEMO.find((x) => x.id === m?.responsableId)
  const chaine: string[] = []
  for (let r = resp; r && chaine.length < 10; r = ORGANISATION_DEMO.find((x) => x.id === r!.responsableId)) chaine.push(r.id)
  const chemin = cheminDe(UNITES_DEMO, m?.unite ?? null)
  return {
    id, nomAffiche: m?.nomAffiche ?? nomAffiche ?? id, unite: chemin.at(-1) ?? null, chemin,
    responsable: resp ? { id: resp.id, nomAffiche: resp.nomAffiche, fonction: resp.fonction } : null,
    chaine, equipeDirecte: ORGANISATION_DEMO.filter((x) => x.responsableId === id).length,
  }
}

/**
 * Pont des tâches d'étapes de processus (D-87 §5.3) : le moteur simulé des processus crée, réaffecte, termine et annule
 * les tâches d'origine PROCESSUS de « Mon activité » ; la prise ou l'attribution d'une tâche lui est signalée.
 */
export type PontTachesProcessus = {
  creer(t: { titre: string; description: string | null; responsable: string | null; uniteCible: string | null; profilRequis: string | null; createur: string; echeance: string | null; reference: string; lien: string }): string
  affecter(tacheId: string, responsable: string | null, uniteCible?: string | null, profilRequis?: string | null): void
  terminer(tacheId: string): void
  annuler(tacheId: string): void
  surPrise: ((tacheId: string, personne: PersonneActivite) => void) | null
  /** Tâche gardée d'une origine (référence), ou null : reprise d'une démonstration sans doublon (QUO-19). */
  trouver?: (reference: string) => string | null
  /** Contrôle avant la prise ou l'attribution (D-99, séparation des fonctions) : lève le refus du moteur des processus. */
  verifierPrise?: ((tacheId: string, personneId: string, attribution: boolean) => void) | null
  /**
   * Offre d'un autre service (D-98 : transition de statut à prononcer, consommée de MS-PARTICIPANT) : à prendre par les
   * porteurs de l'une des permissions, idempotente par origine.reference ; rend l'identifiant de la tâche.
   */
  offrir(t: { titre: string; description: string | null; createur: string; permissionsRequises: string[]; origine: Tache['origine']; /** Instant de l'événement d'origine (DDR-15). */ le?: string }): string
  /** Tâches ouvertes d'une origine terminées par le service (transition prononcée ou refusée). */
  terminerOrigine(reference: string): void
  /**
   * Démonstration (DDR-20) : tâche ouverte confiée à une personne par un autre, pour la notification de démonstration
   * (pilotage.task-assigned.v1) qui mène à cette tâche même.
   */
  tacheConfiee(responsableId: string): { id: string; titre: string; createur: PersonneActivite } | null
}

/** Durée d'une étape du dépôt simulé d'un document de rapport (dix étapes d'envoi, une de vérification). */
export const DUREE_ETAPE_DOCUMENT_MS = 80

type EtatEvenement = Omit<Evenement, 'moi'>
type EtatTacheSim = Required<Tache>
type EtatRapport = Rapport & { documentBlob?: Blob }

export function pilotageSimule(
  attendreJeu: Attendre, refus: Refus, utilisateur: { id: string; nom: string } = { id: 'U-001', nom: 'Paul Mbarga Atangana' }, magasin: MagasinPouvoirs = magasinPouvoirs(),
  relier?: (pont: PontTachesProcessus) => void,
  /** DDR-09 : démonstration continue, tâches gardées d'un chargement à l'autre (changement d'acteur). */
  persistance?: { lire(): string | null; ecrire(valeur: string): void },
): ServicePilotage {
  // Catalogue des unités avec leur responsable (D-81 §2), partagé avec le magasin des pouvoirs.
  const unites = magasin.unites
  const organisation = ORGANISATION_DEMO.some((m) => m.id === utilisateur.id)
    ? ORGANISATION_DEMO
    : [...ORGANISATION_DEMO, { id: utilisateur.id, nomAffiche: utilisateur.nom, fonction: '', unite: null, responsableId: null }]
  const moi: PersonneActivite = { id: utilisateur.id, nomAffiche: organisation.find((m) => m.id === utilisateur.id)!.nomAffiche }
  const personne = (id: string): PersonneActivite => {
    const m = organisation.find((x) => x.id === id)
    return { id, nomAffiche: m?.nomAffiche ?? id }
  }
  const responsableDe = (id: string) => organisation.find((m) => m.id === id)?.responsableId ?? null
  /** Équipe (§3) : comptes dont l'utilisateur figure dans la chaîne des responsables, avec leur niveau. */
  const equipeDe = (id: string) => {
    const l: { membre: Membre; niveau: number }[] = []
    const parcourir = (chef: string, niveau: number) => organisation.filter((m) => m.responsableId === chef).forEach((m) => {
      if (l.some((x) => x.membre.id === m.id) || niveau > 10) return
      l.push({ membre: m, niveau })
      parcourir(m.id, niveau + 1)
    })
    parcourir(id, 1)
    return l
  }
  const dansEquipe = (id: string) => equipeDe(moi.id).some((x) => x.membre.id === id)
  const lisible = (collaborateur?: string) => {
    if (collaborateur && collaborateur !== moi.id && !dansEquipe(collaborateur)) throw refus(403, 'PIL_HORS_EQUIPE', 'Ce compte n’est pas dans votre équipe.')
    return collaborateur ?? moi.id
  }

  let sequence = 0
  const id = (prefixe: string) => `${prefixe}-${(++sequence).toString().padStart(4, '0')}`
  const maintenant = () => new Date().toISOString()
  const jour = aujourdhui()
  const hier = ajouterJours(jour, -1)
  const avantHier = ajouterJours(jour, -2)
  const demain = ajouterJours(jour, 1)

  // ---------------------------------------------------------------- jeu de démonstration
  const activites: Activite[] = []
  const evenements: EtatEvenement[] = []
  const taches: EtatTacheSim[] = []
  const rapports: EtatRapport[] = []

  const activite = (auteur: string, j: string, titre: string, statut: Activite['statut'], dureeMinutes: number | null, o: Partial<Activite> = {}) => {
    const ordre = activites.filter((a) => a.auteur.id === auteur && a.jour === j).length + 1
    const a: Activite = { id: id('ACT'), jour: j, titre, description: null, statut, dureeMinutes, ordre, tacheId: null, reporteeAu: null, auteur: personne(auteur), modifieeLe: maintenant(), ...o }
    activites.push(a)
    return a
  }
  const tache = (responsable: string | null, createur: string, titre: string, etat: EtatTache, echeance: string | null, o: Partial<Tache> = {}) => {
    const t: EtatTacheSim = {
      id: id('TCH'), titre, description: null, echeance, priorite: 'NORMALE', etat, responsable: responsable ? personne(responsable) : null, createur: personne(createur),
      groupeId: null, uniteCible: null, profilRequis: null, profilsRequis: null, permissionsRequises: null, aPrendre: false,
      origine: { type: 'INTERNE', reference: null, service: null, lien: null }, enRetard: false, creeeLe: `${ajouterJours(jour, -6)}T08:30:00+01:00`,
      termineeLe: etat === 'TERMINEE' ? `${hier}T16:10:00+01:00` : null,
      historique: [{ quand: `${ajouterJours(jour, -6)}T08:30:00+01:00`, acteur: personne(createur), action: 'CREEE', detail: null }],
      ...o,
    }
    taches.push(t)
    return t
  }
  const evenement = (proprietaire: string, titre: string, categorie: Evenement['categorie'], debut: string, fin: string, participants: [string, Evenement['participants'][number]['reponse']][] = [], o: Partial<EtatEvenement> = {}) => {
    const e: EtatEvenement = {
      id: id('EVT'), titre, description: null, lieu: null, debut, fin, journeeEntiere: false, categorie, proprietaire: personne(proprietaire),
      participants: participants.map(([p, reponse]) => ({ ...personne(p), reponse })), ...o,
    }
    evenements.push(e)
    return e
  }

  const TRAVAUX: Record<string, string> = {
    DES: 'Arbitrer le planning des essais de LIA-2026-030', DDRM: 'Instruire le dossier d’adhésion de la Banque de Libreville', SNA: 'Vérifier les preuves de l’opération OP-E05',
    PROD_MONETIQUE: 'Préparer la bascule de la liaison LIA-2026-027', PROD_INFO: 'Planifier la maintenance des serveurs de recette', POLE_RESEAU: 'Configurer le VPN du participant',
    POLE_SYSTEME: 'Mettre à jour les serveurs de recette', SSI: 'Analyser les attestations PCI DSS reçues', CONFORMITE: 'Relire la fiche de conformité de LIA-2026-019', DG: 'Préparer le comité de direction',
  }
  organisation.forEach((m, i) => {
    const resp = m.responsableId
    // Activités : la veille terminée, la journée en cours avec un report de la veille.
    activite(m.id, hier, 'Point de situation avec l’équipe', 'FAITE', 30)
    activite(m.id, hier, TRAVAUX[m.unite ?? 'DG'] ?? 'Traiter les demandes en attente', 'FAITE', 150)
    activite(m.id, hier, 'Classer les comptes rendus de la semaine', 'REPORTEE', null, { reporteeAu: jour })
    activite(m.id, jour, 'Point de situation avec l’équipe', i % 3 === 2 ? 'A_FAIRE' : 'FAITE', i % 3 === 2 ? null : 30)
    activite(m.id, jour, TRAVAUX[m.unite ?? 'DG'] ?? 'Traiter les demandes en attente', 'EN_COURS', 90, { description: 'Suite des travaux de la veille.' })
    activite(m.id, jour, 'Classer les comptes rendus de la semaine', 'A_FAIRE', null)
    if (i % 2 === 0) activite(m.id, jour, 'Répondre aux courriels en attente', 'A_FAIRE', null)
    activite(m.id, demain, 'Préparer la revue hebdomadaire', 'A_FAIRE', null)
    // Tâches : une confiée par le responsable, une issue d'un autre service (en retard), une terminée.
    // DDR-21 : un responsable d'équipe a sa propre fiche (celle de l'unité), distincte de celle qu'il confie à ses membres.
    const chefEquipe = organisation.some((x) => x.responsableId === m.id)
    tache(m.id, resp ?? m.id, chefEquipe ? 'Mettre à jour la fiche de procédure de l’unité' : 'Mettre à jour la fiche de procédure du service', 'OUVERTE', ajouterJours(jour, 3), { description: 'Intégrer les changements de la note de service de septembre.' })
    if (i % 2 === 0) {
      tache(m.id, resp ?? m.id, 'Compléter les preuves de l’opération OP-E05 de LIA-2026-030', 'EN_COURS', hier, {
        priorite: 'HAUTE', origine: { type: 'OPERATION', reference: 'LIA-2026-030:OP-E05', service: 'homologation', lien: '/des/parcours/LIA-2026-030' },
      })
    }
    tache(m.id, m.id, 'Archiver les pièces du dossier LIA-2026-019', 'TERMINEE', ajouterJours(jour, -1), { priorite: 'BASSE' })
    // Rapports : celui d'avant-hier visé, celui d'hier soumis, l'hebdomadaire précédent visé.
    if (resp) {
      const r = personne(resp)
      const base = (type: TypeRapport, periode: string, etat: Rapport['etat'], o: Partial<EtatRapport> = {}): EtatRapport => ({
        id: id('RAP'), type, periode, mode: 'PREREMPLI', contenu: `# Rapport du ${periode}\n\n## Réalisé\n- Point de situation avec l’équipe (30 min)\n- ${TRAVAUX[m.unite ?? 'DG'] ?? 'Travaux du service'} (2 h 30)\n\n## Reporté\n- Classer les comptes rendus de la semaine`,
        document: null, etat, auteur: personne(m.id), destinataire: r, soumisLe: `${periode.includes('W') ? lundiDeSemaine(periode) : periode}T18:00:00+01:00`,
        viseLe: etat === 'VISE' ? `${jour}T08:15:00+01:00` : null, visa: etat === 'VISE' ? { par: r, commentaire: 'Merci pour ce suivi précis.' } : null,
        indicateurs: { activitesFaites: 2, activitesPrevues: 3, minutes: 180, tachesTerminees: 1 }, ...o,
      })
      rapports.push(base('QUOTIDIEN', avantHier, 'VISE'))
      rapports.push(base('QUOTIDIEN', hier, 'SOUMIS'))
      rapports.push(base('HEBDOMADAIRE', semaineIso(ajouterJours(jour, -7)), 'VISE'))
      // Rapport du jour d'un membre sur trois déjà soumis, un autre en brouillon.
      if (m.id !== utilisateur.id && i % 3 === 0) rapports.push(base('QUOTIDIEN', jour, 'SOUMIS', { soumisLe: maintenant() }))
      if (m.id !== utilisateur.id && i % 3 === 1) rapports.push(base('QUOTIDIEN', jour, 'BROUILLON', { soumisLe: null, destinataire: null }))
    }
  })
  // Tâches confiées (D-81 §6) : une tâche à prendre par le pôle réseau, un groupe confié à deux personnes.
  if (organisation.some((m) => m.id === 'U-001')) {
    tache(null, 'U-001', 'Vérifier la supervision des liens du participant', 'A_PRENDRE', ajouterJours(jour, 4), {
      description: 'Contrôler les alarmes de supervision des deux liens avant la recette.', priorite: 'HAUTE', uniteCible: 'POLE_RESEAU',
    })
    tache('u-sna', 'U-001', 'Relire la grille d’essais de LIA-2026-030', 'EN_COURS', ajouterJours(jour, 2), { groupeId: 'GRP-0001' })
    tache('U-101', 'U-001', 'Relire la grille d’essais de LIA-2026-030', 'TERMINEE', ajouterJours(jour, 2), { groupeId: 'GRP-0001' })
    // Tâche offerte par MS-HOMOLOGATION (D-93 §8.8) : fonction à affecter, prise par tout porteur de sa permission.
    tache(null, 'U-001', 'Prendre la fonction Normes et Agréments (LIA-2026-041)', 'A_PRENDRE', ajouterJours(jour, 3), {
      priorite: 'HAUTE', permissionsRequises: ['CER_DOSSIER_VERIFY'], aPrendre: true,
      origine: { type: 'OPERATION', reference: 'LIA-2026-041/AFFECTER/SNA', service: 'homologation', lien: '/des/parcours/LIA-2026-041?onglet=etapes&fonction=SNA' },
    })
  }
  // Rapport déposé en document (D-74) : le pôle réseau, soumis au chef de service.
  const parDocument = rapports.find((r) => r.auteur.id === 'u-res' && r.periode === hier)
  if (parDocument) {
    const blob = pdfDemo(['Rapport d’activité du pôle réseau', `Journée du ${hier}`, '', 'VPN du participant configuré et testé.', 'Supervision des liens : aucun incident.'])
    Object.assign(parDocument, { mode: 'DOCUMENT', contenu: 'Rapport joint, signé par le chef de pôle.', document: { ticket: id('DOC'), nom: `rapport-pole-reseau-${hier}.pdf`, typeMime: 'application/pdf', taille: blob.size }, documentBlob: blob })
  }
  // Agenda : revue hebdomadaire de chaque responsable, absences, formation, invitation en attente.
  organisation.filter((m) => organisation.some((x) => x.responsableId === m.id)).forEach((chef) => {
    const equipe = organisation.filter((x) => x.responsableId === chef.id)
    const lundi = lundiDe(jour)
    evenement(chef.id, `Revue hebdomadaire : ${unites.find((u) => u.code === chef.unite)?.libelle ?? chef.nomAffiche}`, 'REUNION', instant(lundi, '09:00'), instant(lundi, '10:00'), equipe.map((x, i) => [x.id, i === 0 ? 'EN_ATTENTE' : 'ACCEPTE']), { lieu: 'Salle 2', description: 'Avancement, priorités et points de blocage.' })
  })
  organisation.forEach((m, i) => {
    evenement(m.id, i % 2 ? 'Point téléphonique avec le correspondant' : 'Préparation du comité de pilotage', 'ACTIVITE', instant(jour, '11:00'), instant(jour, '12:00'))
    if (i % 4 === 1) evenement(m.id, 'Déplacement à la BEAC', 'DEPLACEMENT', instant(demain, '08:00'), instant(demain, '13:00'), [], { lieu: 'Yaoundé' })
  })
  evenement('u-sys', 'Congé annuel', 'ABSENCE', instant(jour, '00:00'), instant(ajouterJours(jour, 2), '00:00'), [], { journeeEntiere: true })
  evenement('u-rssi', 'Formation à la sécurité des systèmes d’information', 'FORMATION', instant(ajouterJours(jour, 2), '14:00'), instant(ajouterJours(jour, 2), '17:00'),
    organisation.filter((m) => m.id !== 'u-rssi').slice(0, 8).map((m) => [m.id, 'ACCEPTE']), { lieu: 'Salle de formation' })
  const hote = responsableDe(moi.id) ?? organisation.find((m) => m.id !== moi.id)!.id
  evenement(hote, 'Comité de pilotage des liaisons', 'REUNION', instant(demain, '15:00'), instant(demain, '16:30'), [[moi.id, 'EN_ATTENTE']], { lieu: 'Salle du conseil', description: 'Arbitrages du trimestre.' })

  // DDR-09 : démonstration continue, tâches (créées, confiées, prises, offertes par les services) relues du stockage, et
  // compteur des identifiants repris après le plus grand, pour qu'un identifiant ne soit jamais réattribué.
  try {
    const garde = persistance?.lire()
    if (garde) {
      taches.splice(0, taches.length, ...(JSON.parse(garde) as EtatTacheSim[]))
      for (const m of garde.matchAll(/"[A-Z]{3}-(\d+)"/g)) sequence = Math.max(sequence, Number(m[1]))
    }
  } catch {
    // État illisible : jeu initial.
  }
  const sauver = () => {
    try {
      persistance?.ecrire(JSON.stringify(taches))
    } catch {
      // Stockage indisponible : la démonstration continue en mémoire.
    }
  }
  const attendre = (<T,>(v: T | (() => T)) => attendreJeu(() => {
    const r = typeof v === 'function' ? (v as () => T)() : v
    sauver()
    return r
  })) as Attendre

  // ---------------------------------------------------------------- règles imitées
  const ouverte = (t: Tache) => t.etat !== 'TERMINEE' && t.etat !== 'ANNULEE'
  const vueTache = (t: EtatTacheSim): Tache => ({ ...t, enRetard: ouverte(t) && !!t.echeance && t.echeance < aujourdhui() })
  const membreDe = (qui: string) => organisation.find((m) => m.id === qui)?.unite ?? null
  /** Unités dont les tâches à prendre me sont visibles : mon unité et ses parentes, mes unités dirigées. */
  const unitesVisibles = () => {
    const l = new Set(magasin.unitesDirigees(moi.id))
    for (let u = unites.find((x) => x.code === membreDe(moi.id)); u; u = unites.find((x) => x.code === u!.parent)) l.add(u.code)
    return l
  }
  const porteProfil = (t: Tache) => !t.profilRequis || magasin.profilsDe(moi.id).includes(t.profilRequis)
  // Offre par permission ou par profils (D-93 §8.8), étape offerte hors de l'unité (profil requis sans unité) : visible et
  // prenable par leurs seuls porteurs, sans unité exigée.
  const mesPermissions = () => magasin.profilsDe(moi.id).flatMap((p) => PROFILS[p] ?? [])
  const portePermission = (t: Tache) => !t.permissionsRequises?.length || t.permissionsRequises.some((p) => mesPermissions().includes(p))
  const porteProfils = (t: Tache) => !t.profilsRequis?.length || t.profilsRequis.some((p) => magasin.profilsDe(moi.id).includes(p))
  const offre = (t: Tache) => !!t.permissionsRequises?.length || !!t.profilsRequis?.length
  // D-99 : l'étape à prendre d'une demande n'est pas offerte à son initiateur (créateur de la tâche), comme MS-PILOTAGE.
  const aPrendreVisible = (t: Tache) => t.etat === 'A_PRENDRE' && (t.uniteCible ? unitesVisibles().has(t.uniteCible) : offre(t) || !!t.profilRequis) && porteProfil(t) && portePermission(t) && porteProfils(t)
    && !(t.origine.type === 'PROCESSUS' && t.createur.id === moi.id)
  const trouverTache = (tid: string) => {
    const t = taches.find((x) => x.id === tid)
    const r = t?.responsable?.id
    if (!t || (r !== moi.id && t.createur.id !== moi.id && !(r && dansEquipe(r)) && !aPrendreVisible(t))) throw refus(404, 'PIL_TACHE_INTROUVABLE', 'Tâche introuvable.')
    return t
  }
  const confiable = (qui: string) => qui === moi.id || magasin.couvre(moi.id, 'TACHE_CONFIER', qui)
  // Tâches d'étapes de processus (D-87) : créées et suivies par le moteur des processus.
  const pont: PontTachesProcessus = {
    creer: (x) => {
      const t = tache(x.responsable, x.createur, x.titre, x.responsable ? 'OUVERTE' : 'A_PRENDRE', x.echeance, {
        description: x.description, uniteCible: x.uniteCible, profilRequis: x.profilRequis, creeeLe: maintenant(), historique: [],
        origine: { type: 'PROCESSUS', reference: x.reference, service: 'pilotage', lien: x.lien },
      })
      t.historique.push({ quand: maintenant(), acteur: personne(x.createur), action: 'CREEE', detail: null })
      return t.id
    },
    affecter: (tid, responsable, uniteCible = null, profilRequis = null) => {
      const t = taches.find((x) => x.id === tid)
      if (!t) return
      Object.assign(t, responsable ? { responsable: personne(responsable), etat: t.etat === 'A_PRENDRE' ? 'OUVERTE' : t.etat } : { responsable: null, etat: 'A_PRENDRE', uniteCible, profilRequis })
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'REAFFECTEE', detail: responsable ? `À ${personne(responsable).nomAffiche}` : null })
    },
    terminer: (tid) => {
      const t = taches.find((x) => x.id === tid)
      if (t && ouverte(t)) Object.assign(t, { etat: 'TERMINEE', termineeLe: maintenant() })
    },
    annuler: (tid) => {
      const t = taches.find((x) => x.id === tid)
      if (t && ouverte(t)) Object.assign(t, { etat: 'ANNULEE' })
    },
    surPrise: null,
    offrir: (x) => {
      const existante = taches.find((t) => t.origine.reference === x.origine.reference && offre(t))
      if (existante) return existante.id
      // DDR-15 : créée à l'instant de l'événement d'origine (la proposition), non à celui de sa consommation.
      const le = x.le ?? maintenant()
      const t = tache(null, x.createur, x.titre, 'A_PRENDRE', null, {
        description: x.description, priorite: 'HAUTE', permissionsRequises: x.permissionsRequises, aPrendre: true, creeeLe: le, historique: [], origine: x.origine,
      })
      t.historique.push({ quand: le, acteur: personne(x.createur), action: 'CREEE', detail: `Créée par le service ${x.origine.service ?? ''}`.trim() })
      return t.id
    },
    tacheConfiee: (responsableId) => {
      const t = taches.find((x) => x.responsable?.id === responsableId && x.createur.id !== responsableId && ouverte(x) && x.origine.type === 'INTERNE')
      return t ? { id: t.id, titre: t.titre, createur: t.createur } : null
    },
    terminerOrigine: (reference) => {
      taches.filter((t) => t.origine.reference === reference && ouverte(t)).forEach((t) => Object.assign(t, { etat: 'TERMINEE', aPrendre: false, termineeLe: maintenant() }))
    },
    trouver: (reference) => taches.find((t) => t.origine.type === 'PROCESSUS' && t.origine.reference === reference)?.id ?? null,
  }
  // Effets des autres services (processus, décisions) gardés eux aussi.
  for (const cle of ['creer', 'affecter', 'terminer', 'annuler', 'offrir', 'terminerOrigine'] as const) {
    const effet = pont[cle] as (...a: unknown[]) => unknown
    ;(pont as unknown as Record<string, unknown>)[cle] = (...a: unknown[]) => {
      const r = effet(...a)
      sauver()
      return r
    }
  }
  relier?.(pont)
  const deProcessus = (t: Tache) => t.origine.type === 'PROCESSUS'
  const relances = new Map<string, number>()
  const vueGroupe = (gid: string): GroupeTaches => {
    const l = taches.filter((t) => t.groupeId === gid).map(vueTache)
    if (!l.length || (l[0].createur.id !== moi.id && !l.some((t) => t.responsable?.id === moi.id || (t.responsable && dansEquipe(t.responsable.id))))) {
      throw refus(404, 'PIL_GROUPE_INTROUVABLE', 'Groupe de tâches introuvable.')
    }
    return {
      groupeId: gid, titre: l[0].titre, createur: l[0].createur, taches: l.map(({ historique: _h, ...t }) => t),
      avancement: { total: l.length, terminees: l.filter((t) => t.etat === 'TERMINEE').length, enRetard: l.filter((t) => t.enRetard).length },
    }
  }
  const vueEvenement = (e: EtatEvenement, collaborateur?: string): Evenement => {
    if (e.proprietaire.id === moi.id) return { ...e, moi: 'PROPRIETAIRE' }
    if (e.participants.some((p) => p.id === moi.id)) return { ...e, moi: 'PARTICIPANT' }
    // Agenda d'un membre de l'équipe : catégorie, titre et horaires seulement.
    return { ...e, description: null, lieu: null, participants: [], moi: collaborateur ? 'EQUIPE' : 'PARTICIPANT' }
  }
  const concerne = (e: EtatEvenement, qui: string) => e.proprietaire.id === qui || e.participants.some((p) => p.id === qui)
  const chevauche = (e: EtatEvenement, du: string, au: string) => jourDe(e.debut) <= au && jourDe(new Date(new Date(e.fin).getTime() - 1)) >= du
  const valider = (e: EntreeEvenement) => {
    if (!e.titre.trim() || !(new Date(e.fin).getTime() > new Date(e.debut).getTime())) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Titre et horaires obligatoires, la fin après le début.')
    if (e.participants.length > 50) throw refus(422, 'PIL_PARTICIPANT_INVALIDE', 'Cinquante participants au plus.')
  }
  const sienEvenement = (eid: string) => {
    const e = evenements.find((x) => x.id === eid)
    if (!e || !concerne(e, moi.id)) throw refus(404, 'PIL_EVENEMENT_INTROUVABLE', 'Événement introuvable.')
    if (e.proprietaire.id !== moi.id) throw refus(403, 'PIL_ACTION_RESERVEE', 'Seul le propriétaire modifie cet événement.')
    return e
  }
  const sienne = (aid: string) => {
    const a = activites.find((x) => x.id === aid)
    if (!a || a.auteur.id !== moi.id) throw refus(404, 'PIL_ACTIVITE_INTROUVABLE', 'Activité introuvable.')
    return a
  }
  const trier = (l: Activite[]) => [...l].sort((a, b) => a.jour.localeCompare(b.jour) || a.ordre - b.ordre)
  const minutesDuJour = (qui: string, du: string, au: string) => activites.filter((a) => a.auteur.id === qui && a.jour >= du && a.jour <= au)
  const bornes = (type: TypeRapport, periode: string): [string, string] => (type === 'QUOTIDIEN' ? [periode, periode] : [lundiDeSemaine(periode), ajouterJours(lundiDeSemaine(periode), 6)])
  const indicateurs = (qui: string, type: TypeRapport, periode: string): Rapport['indicateurs'] => {
    const [du, au] = bornes(type, periode)
    const l = minutesDuJour(qui, du, au)
    return {
      activitesFaites: l.filter((a) => a.statut === 'FAITE').length, activitesPrevues: l.filter((a) => a.statut !== 'ANNULEE').length,
      minutes: l.reduce((s, a) => s + (a.dureeMinutes ?? 0), 0),
      tachesTerminees: taches.filter((t) => t.responsable?.id === qui && t.termineeLe && jourDe(t.termineeLe) >= du && jourDe(t.termineeLe) <= au).length,
    }
  }
  const duree = (m: number | null) => (!m ? '' : m < 60 ? ` (${m} min)` : ` (${Math.floor(m / 60)} h${m % 60 ? ` ${String(m % 60).padStart(2, '0')}` : ''})`)
  const rubriques = (du: string, au: string) => {
    const l = trier(minutesDuJour(moi.id, du, au))
    const liste = (x: string[]) => (x.length ? x.map((s) => `- ${s}`).join('\n') : '- Néant')
    const terminees = taches.filter((t) => t.responsable?.id === moi.id && t.termineeLe && jourDe(t.termineeLe) >= du && jourDe(t.termineeLe) <= au)
    return [
      `## Réalisé\n${liste(l.filter((a) => a.statut === 'FAITE').map((a) => `${a.titre}${duree(a.dureeMinutes)}`))}`,
      `## En cours\n${liste(l.filter((a) => a.statut === 'EN_COURS').map((a) => a.titre))}`,
      `## Reporté\n${liste(l.filter((a) => a.statut === 'REPORTEE').map((a) => `${a.titre}${a.reporteeAu ? ` (reporté au ${jourLong(a.reporteeAu)})` : ''}`))}`,
      `## Tâches terminées\n${liste(terminees.map((t) => t.titre))}`,
    ]
  }
  // QUO-18 : dates du texte prérempli au format local, comme RedactionRapport de MS-PILOTAGE (« jeudi 8 octobre 2026 »).
  const jourLong = (j: string) => new Date(`${j}T12:00:00`).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
  const preremplissage = (type: TypeRapport, periode: string) => {
    if (type === 'QUOTIDIEN') {
      const suivant = jourOuvreSuivant(periode)
      const prevu = trier(minutesDuJour(moi.id, suivant, suivant)).map((a) => `- ${a.titre}`)
      return [`# Rapport quotidien du ${jourLong(periode)}`, ...rubriques(periode, periode), `## Prévu demain (${jourLong(suivant)})\n${prevu.length ? prevu.join('\n') : '- Néant'}`].join('\n\n')
    }
    const [du] = bornes(type, periode)
    const jours = Array.from({ length: 5 }, (_, i) => ajouterJours(du, i))
    const total = minutesDuJour(moi.id, du, ajouterJours(du, 6)).reduce((s, a) => s + (a.dureeMinutes ?? 0), 0)
    return [`# Rapport hebdomadaire de la semaine ${periode}`, ...jours.map((j) => `### ${jourLong(j).replace(/^./, (c) => c.toUpperCase())}\n\n${rubriques(j, j).join('\n\n')}`), `## Total des heures\n- ${Math.floor(total / 60)} h ${String(total % 60).padStart(2, '0')}`].join('\n\n')
  }
  const futur = (type: TypeRapport, periode: string) => (type === 'QUOTIDIEN' ? periode > aujourdhui() : periode > semaineIso(aujourdhui()))
  const trouverRapport = (rid: string) => {
    const r = rapports.find((x) => x.id === rid)
    const auteur = r?.auteur.id === moi.id
    const lecteur = r && r.etat !== 'BROUILLON' && (r.destinataire?.id === moi.id || dansEquipe(r.auteur.id))
    if (!r || (!auteur && !lecteur)) throw refus(404, 'PIL_RAPPORT_INTROUVABLE', 'Rapport introuvable.')
    return r
  }
  const vueRapport = (r: EtatRapport): Rapport => {
    const { documentBlob: _b, ...v } = r
    return { ...v, indicateurs: r.auteur.id === moi.id ? indicateurs(moi.id, r.type, r.periode) : r.indicateurs }
  }
  const brouillon = (rid: string) => {
    const r = trouverRapport(rid)
    if (r.auteur.id !== moi.id || r.etat !== 'BROUILLON') throw refus(409, 'PIL_RAPPORT_NON_MODIFIABLE', 'Seul un brouillon de l’auteur se modifie.')
    return r
  }
  /**
   * Chaîne hiérarchique de l'auteur dans l'ordre de résolution du destinataire (§2.4) : n+1 actif ; chefs des unités, de
   * la sienne vers le sommet (jamais l'auteur) ; puis les autres responsables de la chaîne. Vide au sommet.
   */
  const chaineDe = (auteur: string): DestinatairesRapport['choix'] => {
    const actif = (id: string | null | undefined): id is string => !!id && id !== auteur && magasin.organisation.find((m) => m.id === id)?.actif !== false && organisation.some((m) => m.id === id)
    const l: DestinatairesRapport['choix'] = []
    const ajouter = (id: string, lien: DestinatairesRapport['choix'][number]['lien'], unite: string | null) => { if (!l.some((x) => x.id === id)) l.push({ id, nomAffiche: personne(id).nomAffiche, lien, unite }) }
    const n1 = responsableDe(auteur)
    if (actif(n1)) ajouter(n1, 'N_PLUS_UN', null)
    let u = unites.find((x) => x.code === organisation.find((m) => m.id === auteur)?.unite)
    for (let i = 0; u && i < 10; i++, u = unites.find((x) => x.code === u!.parent)) {
      if (actif(u.responsableId)) ajouter(u.responsableId, i === 0 ? 'CHEF_UNITE' : 'CHEF_UNITE_PARENTE', u.code)
    }
    for (let r = responsableDe(n1 ?? ''), i = 0; r && i < 10; r = responsableDe(r), i++) if (actif(r)) ajouter(r, 'CHAINE', null)
    return l
  }
  const destinatairesDe = (auteur: string): DestinatairesRapport => {
    const choix = chaineDe(auteur)
    const d = choix[0]
    return { destinataire: d ? personne(d.id) : null, motif: d ? (d.lien === 'CHAINE' ? 'CHOISI' : d.lien) : null, depot: !d, choix }
  }
  const rapportDuJour = (qui: string, j: string): EtatRapportDuJour => rapports.find((r) => r.auteur.id === qui && r.type === 'QUOTIDIEN' && r.periode === j)?.etat ?? 'ABSENT'
  const documents = new Map<string, { blob: Blob; nom: string; typeMime: string; taille: number }>()
  const page = <T>(l: T[], p = 0, taille = 20) => ({ contenu: l.slice(p * taille, (p + 1) * taille), total: l.length })

  return {
    agenda: (du, au, collaborateur) => attendre(() => {
      const qui = lisible(collaborateur)
      return evenements.filter((e) => concerne(e, qui) && chevauche(e, du, au)).sort((a, b) => a.debut.localeCompare(b.debut)).map((e) => vueEvenement(e, qui !== moi.id ? qui : undefined))
    }),
    creerEvenement: (x) => attendre(() => {
      valider(x)
      const e = evenement(moi.id, x.titre.trim(), x.categorie, x.debut, x.fin, [...new Set(x.participants)].filter((p) => p !== moi.id).map((p) => [p, 'EN_ATTENTE']), {
        description: x.description?.trim() || null, lieu: x.lieu?.trim() || null, journeeEntiere: x.journeeEntiere,
      })
      return vueEvenement(e)
    }),
    modifierEvenement: (eid, x) => attendre(() => {
      valider(x)
      const e = sienEvenement(eid)
      const participants = [...new Set(x.participants)].filter((p) => p !== moi.id).map((p) => e.participants.find((q) => q.id === p) ?? { ...personne(p), reponse: 'EN_ATTENTE' as const })
      Object.assign(e, { titre: x.titre.trim(), description: x.description?.trim() || null, lieu: x.lieu?.trim() || null, debut: x.debut, fin: x.fin, journeeEntiere: x.journeeEntiere, categorie: x.categorie, participants })
      return vueEvenement(e)
    }),
    supprimerEvenement: (eid) => attendre(() => {
      const e = sienEvenement(eid)
      evenements.splice(evenements.indexOf(e), 1)
    }),
    repondre: (eid, reponse) => attendre(() => {
      const e = evenements.find((x) => x.id === eid)
      const p = e?.participants.find((x) => x.id === moi.id)
      if (!e || !p) throw refus(404, 'PIL_EVENEMENT_INTROUVABLE', 'Vous n’êtes pas invité à cet événement.')
      p.reponse = reponse
      return vueEvenement(e)
    }),

    activites: (du, au, collaborateur) => attendre(() => trier(minutesDuJour(lisible(collaborateur), du, au))),
    creerActivite: (x) => attendre(() => {
      if (!x.titre.trim()) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Intitulé obligatoire.')
      return activite(moi.id, x.jour, x.titre.trim(), 'A_FAIRE', x.dureeMinutes, { description: x.description?.trim() || null, tacheId: x.tacheId ?? null })
    }),
    modifierActivite: (aid, x) => attendre(() => {
      const a = sienne(aid)
      if (!x.titre.trim()) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Intitulé obligatoire.')
      if (x.dureeMinutes !== null && (x.dureeMinutes < 0 || x.dureeMinutes > 1440)) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Durée de 0 à 1440 minutes.')
      Object.assign(a, { jour: x.jour, titre: x.titre.trim(), description: x.description?.trim() || null, dureeMinutes: x.dureeMinutes, ordre: x.ordre ?? a.ordre, modifieeLe: maintenant() })
      return a
    }),
    changerStatut: (aid, statut, reporteeAu) => attendre(() => {
      const a = sienne(aid)
      if (statut === 'REPORTEE') {
        if (!reporteeAu || reporteeAu <= a.jour) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Indiquez un jour postérieur.')
        activite(moi.id, reporteeAu, a.titre, 'A_FAIRE', null, { description: a.description, tacheId: a.tacheId })
      }
      Object.assign(a, { statut, reporteeAu: statut === 'REPORTEE' ? reporteeAu : null, modifieeLe: maintenant() })
      return a
    }),
    supprimerActivite: (aid) => attendre(() => {
      activites.splice(activites.indexOf(sienne(aid)), 1)
    }),

    taches: (q) => attendre(() => {
      const equipe = equipeDe(moi.id).map((x) => x.membre.id)
      const l = taches
        .filter((t) => (q.vue === 'moi' ? t.responsable?.id === moi.id : q.vue === 'creees' ? t.createur.id === moi.id && t.responsable?.id !== moi.id
          : q.vue === 'unite' ? aPrendreVisible(t) : !!t.responsable && equipe.includes(t.responsable.id)))
        .filter((t) => !q.etat || t.etat === q.etat)
        .map(vueTache)
        .sort((a, b) => Number(!ouverte(a)) - Number(!ouverte(b)) || (a.echeance ?? '9999').localeCompare(b.echeance ?? '9999'))
        .map(({ historique: _h, ...t }) => t)
      return page(l, q.page, q.taille)
    }),
    tache: (tid) => attendre(() => vueTache(trouverTache(tid))),
    creerGroupe: (x) => attendre(() => {
      if (!x.titre.trim()) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Intitulé obligatoire.')
      const l = [...new Set(x.responsables)]
      if (l.length < LIMITES_GROUPE.min || l.length > LIMITES_GROUPE.max) throw refus(400, 'PIL_REQUETE_INVALIDE', 'De 2 à 50 personnes.')
      if (l.some((p) => !confiable(p))) throw refus(403, 'PIL_HORS_EQUIPE', 'Un compte n’est pas dans votre périmètre.')
      const gid = id('GRP')
      l.forEach((p) => {
        const t = tache(p, moi.id, x.titre.trim(), 'OUVERTE', x.echeance, { description: x.description?.trim() || null, priorite: x.priorite, creeeLe: maintenant(), historique: [], groupeId: gid })
        t.historique.push({ quand: maintenant(), acteur: moi, action: 'CREEE', detail: `Confiée à ${personne(p).nomAffiche}` })
      })
      return vueGroupe(gid)
    }),
    creerTache: (x) => attendre(() => {
      if (!x.titre.trim()) throw refus(400, 'PIL_REQUETE_INVALIDE', 'Intitulé obligatoire.')
      const commun = { description: x.description?.trim() || null, priorite: x.priorite, creeeLe: maintenant(), historique: [] }
      if (x.uniteCible) {
        // Tâche à prendre (D-81 §6) : sans responsable, visible des membres de l'unité.
        if (!magasin.couvreUnite(moi.id, 'TACHE_CONFIER', x.uniteCible)) throw refus(403, 'PIL_HORS_EQUIPE', 'Cette unité n’est pas dans votre périmètre.')
        const t = tache(null, moi.id, x.titre.trim(), 'A_PRENDRE', x.echeance, { ...commun, uniteCible: x.uniteCible })
        t.historique.push({ quand: maintenant(), acteur: moi, action: 'CREEE', detail: `Confiée à l’unité ${unites.find((u) => u.code === x.uniteCible)?.libelle ?? x.uniteCible}` })
        return vueTache(t)
      }
      const responsable = x.responsableId ?? moi.id
      if (!confiable(responsable)) throw refus(403, 'PIL_HORS_EQUIPE', 'Ce compte n’est pas dans votre équipe.')
      const t = tache(responsable, moi.id, x.titre.trim(), 'OUVERTE', x.echeance, commun)
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'CREEE', detail: responsable !== moi.id ? `Confiée à ${personne(responsable).nomAffiche}` : null })
      return vueTache(t)
    }),
    modifierTache: (tid, x) => attendre(() => {
      const t = trouverTache(tid)
      if (t.createur.id !== moi.id && t.responsable?.id !== moi.id) throw refus(403, 'PIL_ACTION_RESERVEE', 'Seuls le créateur et le responsable modifient la tâche.')
      Object.assign(t, { titre: x.titre.trim(), description: x.description?.trim() || null, echeance: x.echeance, priorite: x.priorite })
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'MODIFIEE', detail: null })
      if (x.responsableId && x.responsableId !== t.responsable?.id) {
        if (deProcessus(t)) throw refus(409, 'PIL_TACHE_PROCESSUS', 'La tâche suit l’étape de son processus : réaffectez l’étape.')
        if (t.createur.id !== moi.id) throw refus(403, 'PIL_ACTION_RESERVEE', 'Seul le créateur réaffecte la tâche.')
        if (!confiable(x.responsableId)) throw refus(403, 'PIL_HORS_EQUIPE', 'Ce compte n’est pas dans votre équipe.')
        t.responsable = personne(x.responsableId)
        t.historique.push({ quand: maintenant(), acteur: moi, action: 'REAFFECTEE', detail: `À ${t.responsable.nomAffiche}` })
      }
      return vueTache(t)
    }),
    transition: (tid, etat, motif) => attendre(() => {
      const t = trouverTache(tid)
      const autorise = t.responsable?.id === moi.id || (etat === 'ANNULEE' && t.createur.id === moi.id)
      if (!autorise || !TRANSITIONS_TACHE[t.etat].includes(etat)) throw refus(409, 'PIL_TRANSITION_INTERDITE', 'Transition non permise.')
      if (deProcessus(t) && (etat === 'TERMINEE' || etat === 'ANNULEE')) throw refus(409, 'PIL_TACHE_PROCESSUS', 'La tâche suit l’étape de son processus.')
      // DDR-28 : comme MS-PILOTAGE, la tâche d'une décision se clôt quand le service prononce ou refuse.
      if (t.origine.type === 'DECISION' && (etat === 'TERMINEE' || etat === 'ANNULEE')) throw refus(409, 'PIL_TACHE_DECISION', 'La tâche d’une décision se clôt quand le service prononce ou refuse la proposition.')
      if (motifExige(t.etat, etat) && !motif?.trim()) throw refus(422, 'PIL_MOTIF_OBLIGATOIRE', 'Motif obligatoire.')
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'TRANSITION', detail: `${t.etat} → ${etat}${motif?.trim() ? ` : ${motif.trim()}` : ''}` })
      Object.assign(t, { etat, termineeLe: etat === 'TERMINEE' ? maintenant() : null })
      return vueTache(t)
    }),
    groupe: (gid) => attendre(() => vueGroupe(gid)),
    prendre: (tid) => attendre(() => {
      const t = trouverTache(tid)
      if (t.etat !== 'A_PRENDRE') throw refus(409, 'PIL_TRANSITION_INTERDITE', 'La tâche n’est plus à prendre.')
      const mienne = membreDe(moi.id)
      if (t.uniteCible && (!mienne || !magasin.sousUnites(t.uniteCible).includes(mienne))) throw refus(403, 'PIL_HORS_EQUIPE', 'Vous n’êtes pas membre de cette unité.')
      if (!porteProfil(t) || !porteProfils(t)) throw refus(403, 'PIL_PROFIL_REQUIS', 'Seuls les porteurs du profil requis prennent cette tâche.')
      if (!portePermission(t)) throw refus(403, 'PIL_PERMISSION_REQUISE', 'Seuls les porteurs de la permission requise prennent cette tâche.')
      pont.verifierPrise?.(t.id, moi.id, false)
      Object.assign(t, { responsable: moi, etat: 'OUVERTE', aPrendre: false })
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'PRISE', detail: null })
      pont.surPrise?.(t.id, moi)
      return vueTache(t)
    }),
    attribuer: (tid, responsableId) => attendre(() => {
      const t = trouverTache(tid)
      if (t.etat !== 'A_PRENDRE') throw refus(409, 'PIL_TRANSITION_INTERDITE', 'La tâche n’est plus à prendre.')
      if (t.createur.id !== moi.id && !magasin.unitesDirigees(moi.id).includes(t.uniteCible!)) throw refus(403, 'PIL_ACTION_RESERVEE', 'Seuls le créateur et le responsable de l’unité attribuent la tâche.')
      const unite = membreDe(responsableId)
      if (!unite || !magasin.sousUnites(t.uniteCible!).includes(unite)) throw refus(422, 'PIL_RESPONSABLE_INVALIDE', 'Le responsable doit être membre de l’unité.')
      if (!confiable(responsableId)) throw refus(403, 'PIL_HORS_EQUIPE', 'Ce compte n’est pas dans votre périmètre.')
      pont.verifierPrise?.(t.id, responsableId, true)
      Object.assign(t, { responsable: personne(responsableId), etat: 'OUVERTE' })
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'ATTRIBUEE', detail: `À ${personne(responsableId).nomAffiche}` })
      pont.surPrise?.(t.id, personne(responsableId))
      return vueTache(t)
    }),
    relancer: (tid) => attendre(() => {
      const t = trouverTache(tid)
      if (!t.responsable || !ouverte(t) || t.responsable.id === moi.id) throw refus(409, 'PIL_RELANCE_IMPOSSIBLE', 'Seule une tâche confiée et encore ouverte se relance.')
      if (t.createur.id !== moi.id && !magasin.couvre(moi.id, 'TACHE_SUIVRE', t.responsable.id)) throw refus(403, 'PIL_HORS_EQUIPE', 'Ce compte n’est pas dans votre périmètre.')
      const derniere = relances.get(t.id)
      if (derniere && Date.now() - derniere < 86_400_000) throw refus(429, 'PIL_RELANCE_RECENTE', 'Une relance a déjà été envoyée il y a moins de 24 heures.')
      relances.set(t.id, Date.now())
      t.historique.push({ quand: maintenant(), acteur: moi, action: 'RELANCEE', detail: null })
    }),

    rapports: (q) => attendre(() => {
      const l = rapports
        .filter((r) => (q.vue === 'moi' ? r.auteur.id === moi.id : r.auteur.id !== moi.id && r.etat !== 'BROUILLON' && (r.destinataire?.id === moi.id || dansEquipe(r.auteur.id))))
        .filter((r) => (!q.type || r.type === q.type) && (!q.etat || r.etat === q.etat))
        .sort((a, b) => (b.soumisLe ?? `${b.periode}~`).localeCompare(a.soumisLe ?? `${a.periode}~`) || b.periode.localeCompare(a.periode))
        .map(vueRapport)
      return page(l, q.page, q.taille)
    }),
    rapport: (rid) => attendre(() => vueRapport(trouverRapport(rid))),
    preremplir: (type, periode) => attendre(() => {
      if (futur(type, periode)) throw refus(422, 'PIL_PERIODE_FUTURE', 'La période n’est pas encore commencée.')
      return { contenu: preremplissage(type, periode) }
    }),
    creerRapport: (x) => attendre(() => {
      if (futur(x.type, x.periode)) throw refus(422, 'PIL_PERIODE_FUTURE', 'La période n’est pas encore commencée.')
      if (rapports.some((r) => r.auteur.id === moi.id && r.type === x.type && r.periode === x.periode)) throw refus(409, 'PIL_RAPPORT_EXISTANT', 'Un rapport existe déjà pour cette période.')
      const r: EtatRapport = {
        id: id('RAP'), type: x.type, periode: x.periode, mode: x.mode, contenu: x.contenu?.trim() || null, document: null, etat: 'BROUILLON', auteur: moi, destinataire: null,
        soumisLe: null, viseLe: null, visa: null, indicateurs: indicateurs(moi.id, x.type, x.periode),
      }
      rapports.push(r)
      return vueRapport(r)
    }),
    modifierRapport: (rid, x) => attendre(() => {
      const r = brouillon(rid)
      const doc = x.document ? documents.get(x.document.ticket) : undefined
      // Le service relit nom, type et taille du document auprès de MS-DOCUMENT : la demande ne porte que le ticket.
      if (x.document && !doc) throw refus(422, 'PIL_DOCUMENT_INVALIDE', 'Document inconnu de MS-DOCUMENT.')
      Object.assign(r, { mode: x.mode, contenu: x.contenu?.trim() || null, ...(x.document && doc ? { document: { ticket: x.document.ticket, nom: doc.nom, typeMime: doc.typeMime, taille: doc.taille }, documentBlob: doc.blob } : {}) })
      return vueRapport(r)
    }),
    // Destinataire résolu (n+1, chef de l'unité, chef d'une unité parente) ou choisi dans la chaîne ; personne : DEPOSE.
    soumettre: (rid, destinataireId) => attendre(() => {
      const r = brouillon(rid)
      if (!r.contenu && !r.document) throw refus(422, 'PIL_RAPPORT_INCOMPLET', 'Le rapport n’a ni texte ni document.')
      const d = destinatairesDe(moi.id)
      if (destinataireId && !d.choix.some((c) => c.id === destinataireId)) {
        throw refus(422, 'PIL_DESTINATAIRE_INVALIDE', 'Le destinataire choisi doit appartenir à votre chaîne hiérarchique.')
      }
      const id = destinataireId ?? d.destinataire?.id ?? null
      const motif = !id ? null : destinataireId && destinataireId !== d.destinataire?.id ? 'CHOISI' : d.motif
      Object.assign(r, { etat: id ? 'SOUMIS' : 'DEPOSE', destinataire: id ? personne(id) : null, destinataireMotif: motif, soumisLe: maintenant(), indicateurs: indicateurs(moi.id, r.type, r.periode) })
      return vueRapport(r)
    }),
    destinatairesRapport: (rid) => attendre(() => {
      const r = trouverRapport(rid)
      if (r.auteur.id !== moi.id) throw refus(403, 'PIL_ACTION_RESERVEE', 'Seul l’auteur soumet son rapport.')
      return destinatairesDe(moi.id)
    }),
    viser: (rid, commentaire) => attendre(() => {
      const r = trouverRapport(rid)
      if (r.auteur.id === moi.id || r.etat !== 'SOUMIS') throw refus(409, 'PIL_TRANSITION_INTERDITE', 'Seul un rapport soumis se vise, par son destinataire.')
      Object.assign(r, { etat: 'VISE', viseLe: maintenant(), visa: { par: moi, commentaire: commentaire.trim() || null } })
      return vueRapport(r)
    }),
    // Le contenu n'est pas copié (un Blob ne se clone pas) : contrôle d'accès simulé, puis le document tel quel.
    documentRapport: (rid) => attendre(() => {
      const r = trouverRapport(rid)
      if (!r.documentBlob) throw refus(404, 'PIL_DOCUMENT_INTROUVABLE', 'Aucun document joint.')
      return rid
    }).then((ok) => ({ blob: rapports.find((r) => r.id === ok)!.documentBlob!, empreinte: null })),
    // Dépôt fragmenté simulé (D-74) : progression en dix étapes, vérification, ticket.
    deposerDocument: async (fichier: File, _rapportId: string, o: OptionsTeleversement = {}): Promise<Televersement> => {
      if (/carte|pan/i.test(fichier.name)) throw refus(422, 'DOC_UPLOAD_REJECTED', 'Le document contient un numéro de carte non masqué.')
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
        if (i < 10) await pause(DUREE_ETAPE_DOCUMENT_MS)
      }
      o.surProgression?.({ phase: 'verification', envoye: total, total })
      await pause(DUREE_ETAPE_DOCUMENT_MS)
      const ticket = id('DOC')
      documents.set(ticket, { blob: fichier, nom: fichier.name, typeMime: fichier.type || 'application/octet-stream', taille: total })
      o.surProgression?.({ phase: 'pret', envoye: total, total })
      return { documentId: ticket, sha256: `sim${total.toString(16).padStart(8, '0')}`, dedoublonne: false, depotId: id('DEPOT') }
    },

    equipe: (j) => attendre(() => equipeDe(moi.id).map(({ membre, niveau }): MembreEquipe => {
      const l = activites.filter((a) => a.auteur.id === membre.id && a.jour === j)
      const ts = taches.filter((t) => t.responsable?.id === membre.id).map(vueTache).filter(ouverte)
      return {
        collaborateur: { id: membre.id, nomAffiche: membre.nomAffiche, fonction: membre.fonction, unite: membre.unite, niveau },
        activites: { prevues: l.filter((a) => a.statut !== 'ANNULEE' && a.statut !== 'REPORTEE').length, faites: l.filter((a) => a.statut === 'FAITE').length, enCours: l.filter((a) => a.statut === 'EN_COURS').length },
        rapportDuJour: rapportDuJour(membre.id, j),
        tachesOuvertes: ts.length,
        tachesEnRetard: ts.filter((t) => t.enRetard).length,
        absent: evenements.some((e) => e.categorie === 'ABSENCE' && e.proprietaire.id === membre.id && chevauche(e, j, j)),
      }
    })),
    synthese: (j) => attendre(() => {
      const l = activites.filter((a) => a.auteur.id === moi.id && a.jour === j)
      const ts = taches.filter((t) => t.responsable?.id === moi.id).map(vueTache).filter(ouverte)
      const limite = ajouterJours(j, 3)
      const prochains = evenements.filter((e) => concerne(e, moi.id) && new Date(e.fin).getTime() > Date.now()).sort((a, b) => a.debut.localeCompare(b.debut)).slice(0, 5).map((e) => vueEvenement(e))
      return {
        activites: { prevues: l.filter((a) => a.statut !== 'ANNULEE' && a.statut !== 'REPORTEE').length, faites: l.filter((a) => a.statut === 'FAITE').length, enCours: l.filter((a) => a.statut === 'EN_COURS').length },
        evenements: prochains,
        taches: { ouvertes: ts.length, enRetard: ts.filter((t) => t.enRetard).length, echeanceProche: ts.filter((t) => !t.enRetard && t.echeance && t.echeance <= limite).length },
        rapportDuJour: rapportDuJour(moi.id, j),
        rapportsARecevoir: rapports.filter((r) => r.destinataire?.id === moi.id && r.etat === 'SOUMIS').length,
      }
    }),

    unites: () => attendre(() => unites),
    organisation: () => attendre(() => rattachementDemo(moi.id, moi.nomAffiche)),
  }
}
