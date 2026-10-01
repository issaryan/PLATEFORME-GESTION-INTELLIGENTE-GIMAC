// Session de démonstration des services simulés (D-65) : ouverte par la connexion simulée, lue par les
// applications pour choisir leur profil et le premier accès. Rangée dans le stockage de l'onglet
// (sessionStorage) : ni mot de passe, ni code, ni jeton ; aucun usage avec les services réels.

export type SessionDemo = {
  identifiant: string
  /** Profil de démonstration (ROLE_…) ; absent, l'application garde son profil par défaut. */
  role?: string
  /** Session restreinte du premier accès (acr 1) : l'assistant remplace toutes les pages. */
  premierAcces?: boolean
  /** Étapes du premier accès annoncées par la connexion. */
  etapes?: string[]
}

export const CLE_SESSION_DEMO = 'gm.sessionDemo'

/** Session de démonstration de l'onglet, ou null (aucune, stockage indisponible ou illisible). */
export function lireSessionDemo(): SessionDemo | null {
  try {
    const brut = window.sessionStorage.getItem(CLE_SESSION_DEMO)
    if (!brut) return null
    const s = JSON.parse(brut) as SessionDemo
    return s && typeof s.identifiant === 'string' ? s : null
  } catch {
    return null
  }
}

export function ouvrirSessionDemo(s: SessionDemo): void {
  try {
    window.sessionStorage.setItem(CLE_SESSION_DEMO, JSON.stringify(s))
  } catch {
    /* stockage indisponible : la démonstration reste sans session */
  }
}

export function fermerSessionDemo(): void {
  try {
    window.sessionStorage.removeItem(CLE_SESSION_DEMO)
  } catch {
    /* stockage indisponible */
  }
}
