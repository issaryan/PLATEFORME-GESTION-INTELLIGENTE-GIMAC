import { http } from '../http/client'
import { CONNEXION, DECONNEXION } from '../http/prefixes'

// Déconnexion et reconnexion par la passerelle (modèle BFF, D-62). Aucune page du fournisseur
// d'identité n'est ouverte en dehors de ses écrans d'authentification aux couleurs du GIMAC.

/** Navigation du document ; remplaçable dans les tests. */
export const navigation = { vers: (adresse: string) => window.location.assign(adresse) }

/**
 * Déconnexion complète : la passerelle invalide sa session et renvoie l'adresse de fin de session
 * du fournisseur d'identité, qui ramène ensuite à l'application. À défaut, retour à la connexion.
 */
export async function deconnecter(): Promise<void> {
  let adresse = CONNEXION
  try {
    const r = await http.post<{ logoutUrl?: string } | undefined>(DECONNEXION)
    if (r?.logoutUrl) adresse = r.logoutUrl
  } catch {
    /* session déjà close : retour à la connexion */
  }
  navigation.vers(adresse)
}

/**
 * Reconnexion (actions requises du fournisseur d'identité : mot de passe, second facteur, courriel) :
 * la session de la passerelle est close puis une nouvelle authentification est ouverte ; le
 * fournisseur d'identité exécute alors les actions requises sur ses pages aux couleurs du GIMAC.
 */
export async function reconnecter(): Promise<void> {
  try {
    await http.post(DECONNEXION)
  } catch {
    /* session déjà close : l'authentification suit */
  }
  navigation.vers(CONNEXION)
}
