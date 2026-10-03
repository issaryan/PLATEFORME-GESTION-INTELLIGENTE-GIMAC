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
}

export type Conversation = {
  id: string
  type: 'DIRECTE' | 'GROUPE'
  titre: string | null
  liaison: EtiquetteLiaison | null
  membres: Correspondant[]
  dernierMessage: { id: string; auteurNom: string; extrait: string; envoyeLe: string; piecesJointes: number; evenement?: EvenementGroupe | null } | null
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
export type Envoi = { contenu: string; etiquettes: EtiquetteLiaison[]; fichiers: File[]; documents?: string[]; transferts?: string[] }
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
} as const

/** Références de liaison citées dans un texte (LIA-AAAA-NNN), rendues en liens. */
export const MOTIF_LIAISON = /\bLIA-\d{4}-\d{3,6}\b/g
