// Messagerie instantanée de travail entre acteurs du GIMAC (D-64, docs/lot1/messagerie.md) : représentations
// servies par MS-COLLABORATION. L'interface n'en calcule rien : non-lus, masquage RG-52 et droits viennent du service.

/** Personne joignable de l'annuaire (compte interne actif) ; avatarVersion : photo de profil (mon-compte.md §1), null sans photo. */
export type Correspondant = { id: string; nom: string; fonction: string | null; profils?: string[]; avatarVersion?: string | null }

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
}

export type Conversation = {
  id: string
  type: 'DIRECTE' | 'GROUPE'
  titre: string | null
  liaison: EtiquetteLiaison | null
  membres: Correspondant[]
  dernierMessage: { id: string; auteurNom: string; extrait: string; envoyeLe: string; piecesJointes: number } | null
  nonLus: number
  misAJourLe: string
  creeeLe: string
}

export type PageMessagerie<T> = { contenu: T[]; total: number; page: number; taille: number }
export type FilMessages = { contenu: Message[]; plusAnciens: boolean }

export type Envoi = { contenu: string; etiquettes: EtiquetteLiaison[]; fichiers: File[] }
export type NouvelleConversation = Envoi & { destinataires: string[]; titre: string | null; liaisonId: string | null }

/** Bornes du contrat (docs/lot1/messagerie.md §1), annoncées à l'utilisateur avant l'envoi. */
export const LIMITES_MESSAGERIE = {
  contenuMax: 4000,
  destinatairesMax: 50,
  etiquettesMax: 10,
  piecesMax: 5,
  tailleMaxOctets: 20 * 1024 * 1024,
  types: '.pdf,.png,.jpg,.jpeg,.txt,.csv,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip',
} as const

/** Références de liaison citées dans un texte (LIA-AAAA-NNN), rendues en liens. */
export const MOTIF_LIAISON = /\bLIA-\d{4}-\d{3,6}\b/g
