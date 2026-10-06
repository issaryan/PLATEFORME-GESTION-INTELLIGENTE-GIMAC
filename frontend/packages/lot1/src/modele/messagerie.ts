// Messagerie instantanée de travail entre acteurs du GIMAC (D-64, docs/lot1/messagerie.md) : représentations
// servies par MS-COLLABORATION. L'interface n'en calcule rien : non-lus, masquage RG-52 et droits viennent du service.

/** Personne joignable de l'annuaire (compte interne actif) ; avatarVersion : photo de profil (mon-compte.md §1), null sans photo. */
export type Correspondant = { id: string; nom: string; fonction: string | null; profils?: string[]; avatarVersion?: string | null; administrateur?: boolean }

/** Événement de la vie d'un groupe (D-68), inscrit dans le fil : l'auteur agit, les cibles sont les noms concernés. */
export type TypeEvenementGroupe = 'GROUPE_CREE' | 'GROUPE_RENOMME' | 'MEMBRES_AJOUTES' | 'MEMBRE_RETIRE' | 'MEMBRE_PARTI'
export type EvenementGroupe = { type: TypeEvenementGroupe; cibles: string[] }

/** Étiquette de projet : lien direct vers le dossier de la liaison. */
export type EtiquetteLiaison = { liaisonId: string; reference: string }

export type PieceJointe = { id: string; nom: string; taille: number; typeMime: string; empreinte: string }

export type Message = {
  id: string
  conversationId: string
  auteur: Correspondant
  /** Message de l'utilisateur courant (aligné à droite). */
  moi: boolean
  contenu: string
  envoyeLe: string
  etiquettes: EtiquetteLiaison[]
  piecesJointes: PieceJointe[]
  /** Événement de groupe (D-68) : contenu vide, rendu en ligne centrée. */
  evenement?: EvenementGroupe | null
  /** Message cité (D-82) ; `supprime` : purgé ou illisible (auteur et date nuls, extrait vide). */
  reponseA?: MessageCite | null
  /** Membres mentionnés avec « @ » (D-82), nom à l'envoi. */
  mentions?: MembreMentionne[]
  /** Le lecteur est mentionné, ou le message répond à l'un des siens (jamais pour son propre message). */
  mentionneMoi?: boolean
  /** Accusé de lecture (D-88) : seulement pour mes messages, absent pour ceux des autres et les événements. */
  lecture?: LectureMessage | null
}

/**
 * Accusé de lecture d'un de mes messages (D-88, docs/lot1/accuses-lecture.md §1) : ENVOYE (personne), LU_PAR_CERTAINS
 * (groupe, au moins un mais pas tous), LU (tous les destinataires présents à l'envoi et encore membres).
 */
export type EtatLecture = 'ENVOYE' | 'LU_PAR_CERTAINS' | 'LU'
export type LectureMessage = { etat: EtatLecture; lus: number; destinataires: number }
/** Ligne du détail « Lu par » (GET …/messages/{messageId}/lectures) : `lu` sans `luLe` = lu, heure inconnue. */
export type LecteurMessage = { membre: { id: string; nom: string }; lu: boolean; luLe: string | null }

/** Message cité par une réponse (D-82, docs/lot1/messagerie.md §2) : extrait de 120 caractères, nombre de pièces. */
export type MessageCite = { id: string; auteur: { id: string; nom: string } | null; extrait: string; piecesJointes: number; envoyeLe: string | null; supprime: boolean }
export type MembreMentionne = { id: string; nom: string }

export type Conversation = {
  id: string
  type: 'DIRECTE' | 'GROUPE'
  titre: string | null
  liaison: EtiquetteLiaison | null
  membres: Correspondant[]
  dernierMessage: { id: string; auteurNom: string; extrait: string; envoyeLe: string; piecesJointes: number; evenement?: EvenementGroupe | null; lecture?: LectureMessage | null } | null
  nonLus: number
  misAJourLe: string
  creeeLe: string
}

export type PageMessagerie<T> = { contenu: T[]; total: number; page: number; taille: number }
export type FilMessages = { contenu: Message[]; plusAnciens: boolean }

/**
 * Pièces d'un message : `fichiers` (envoi direct, 20 Mo au plus, hérité), `documents` (tickets de dépôts
 * fragmentés, D-74, jusqu'à 20 Go) et `transferts` (pièces existantes repartagées par leur ticket, sans copie).
 */
export type Envoi = {
  contenu: string; etiquettes: EtiquetteLiaison[]; fichiers: File[]; documents?: string[]; transferts?: string[]
  /** Message cité (D-82) : même conversation ; à la création, seulement si la conversation directe est réutilisée. */
  reponseA?: string | null
  /** Membres mentionnés (D-82) : 20 au plus, membres de la conversation, jamais l'auteur. */
  mentions?: string[]
}
/** « groupe » (D-68) : groupe de discussion nommé ; le premier message y est facultatif. */
export type NouvelleConversation = Envoi & { destinataires: string[]; titre: string | null; liaisonId: string | null; groupe?: boolean }

/** Bornes du contrat (docs/lot1/messagerie.md §1), annoncées à l'utilisateur avant l'envoi. */
export const LIMITES_MESSAGERIE = {
  contenuMax: 4000,
  destinatairesMax: 50,
  etiquettesMax: 10,
  piecesMax: 5,
  tailleMaxOctets: 20 * 1024 * 1024,
  types: '.pdf,.png,.jpg,.jpeg,.txt,.csv,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip',
  /** Dépôt fragmenté (D-74) : enregistrements d'ateliers, archives ; 20 Go par fichier. */
  tailleMaxDepotOctets: 20 * 1024 ** 3,
  typesDepot: '.pdf,.png,.jpg,.jpeg,.txt,.csv,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.mp4,.mov,.webm,.mkv,.avi,.mp3,.wav,.m4a,.7z,.tar,.gz',
  /** Au-delà, pas d'aperçu : téléchargement direct, sans charger le fichier en mémoire. */
  apercuMaxOctets: 50 * 1024 * 1024,
  /** Mentions d'un message (D-82). */
  mentionsMax: 20,
} as const

/**
 * Extrait d'un message cité (D-82, comme MS-COLLABORATION) : espaces réduits ; au-delà de 120 caractères, coupé
 * à la dernière espace des 30 derniers caractères (sinon à 119) et terminé par « … ».
 */
export function extraitCitation(contenu: string): string {
  const texte = contenu.replace(/\s+/g, ' ').trim()
  const signes = Array.from(texte)
  if (signes.length <= 120) return texte
  const debut = signes.slice(0, 119).join('')
  const espace = debut.lastIndexOf(' ')
  return `${espace >= debut.length - 30 ? debut.slice(0, espace) : debut}…`
}

/** Références de liaison citées dans un texte (LIA-AAAA-NNN), rendues en liens. */
export const MOTIF_LIAISON = /\bLIA-\d{4}-\d{3,6}\b/g
