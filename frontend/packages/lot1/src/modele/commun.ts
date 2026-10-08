// Types communs des réponses des services du lot 1 (PI-GIMAC-01, cartographie des contrats).
export type { Permitted } from '@gimac/socle'

/** Taux de couverture calculé par MS-HOMOLOGATION (HOM-17) ; jamais recalculé par l'interface. */
export type CoverageValue = { taux: number | null; numerateur: number; denominateur: number; calculable: boolean; motifNonCalculable?: string }

/**
 * Type de projet (D-91) : code d'un enregistrement du catalogue des types de projet de MS-HOMOLOGATION
 * (^[A-Z][A-Z0-9_]{2,39}$), plus une énumération ; les trois types repris gardent leurs codes.
 */
export type TypeProjet = string
/** Les trois types repris (données de démonstration et libellés de repli) ; la liste réelle est servie. */
export const TYPES_PROJET_REPRIS = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'] as const
export type Segment = 'TRONC_COMMUN_AMONT' | 'BRANCHE' | 'TRONC_COMMUN_AVAL'
export type Partie = 'DES' | 'PARTICIPANT'

/** Requête de liste paginée côté serveur (DS-GIMAC-01 §7.4 : jamais de défilement infini). */
export type ListQuery = { page: number; taille: number; tri?: string; recherche?: string; filtres?: Record<string, string | boolean | undefined> }
export type Page<T> = { contenu: T[]; total: number }

export type Option = { value: string; label: string; hint?: string }
export type EvenementChronologie = { event: string; at: string; actor?: string; correlationId?: string }
